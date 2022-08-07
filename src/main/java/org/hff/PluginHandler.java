package org.hff;

import emu.grasscutter.command.CommandMap;
import emu.grasscutter.data.excels.AvatarSkillDepotData;
import emu.grasscutter.database.DatabaseHelper;
import emu.grasscutter.game.Account;
import emu.grasscutter.game.avatar.Avatar;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.server.packet.send.PacketAvatarSkillChangeNotify;
import emu.grasscutter.server.packet.send.PacketAvatarSkillUpgradeRsp;
import express.http.Request;
import express.http.Response;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.hff.api.ApiCode;
import org.hff.api.ApiResult;
import org.hff.api.param.AccountParam;
import org.hff.api.param.AuthByVerifyCodeParam;
import org.hff.i18n.LanguageManager;
import org.hff.i18n.Locale;
import org.hff.permission.RoleEnum;
import org.hff.utils.AuthUtil;
import org.hff.utils.JwtUtil;
import org.hff.utils.MailUtil;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.Map;

import static emu.grasscutter.Grasscutter.getAuthenticationSystem;
import static emu.grasscutter.Grasscutter.getGameServer;

public final class PluginHandler {

    private final Request request;
    private final Response response;
    private final Locale locale;
    private final String token;
    private final String adminToken;

    public PluginHandler(@NotNull Request request, @NotNull Response response) {
        this.request = request;
        this.response = response;
        this.locale = LanguageManager.getLocale(request.get("locale"));
        this.token = request.get("token");
        this.adminToken = request.get("admin_token");
    }

    public void adminAuth() {
        String adminVoucher = request.body().get("adminVoucher").toString();
        if (adminVoucher == null) {
            response.json(ApiResult.result(ApiCode.PARAM_EMPTY_EXCEPTION, locale));
            return;
        }

        if (!AuthUtil.checkAdminVoucher(adminVoucher)) {
            response.json(ApiResult.result(ApiCode.AUTH_FAIL, locale));
            return;
        }

        generateToken(RoleEnum.ADMIN, "admin");
    }

    public void adminCreateAccount() {
        AccountParam param = request.body(AccountParam.class);
        if (checkParamFail(param)) {
            return;
        }

        Account account = DatabaseHelper.getAccountByName(param.getUsername());
        if (account != null) {
            response.json(ApiResult.result(ApiCode.ACCOUNT_IS_EXIST, locale));
            return;
        }

        getAuthenticationSystem().createAccount(param.getUsername(), param.getPassword());
        response.json(ApiResult.success(locale));
    }

    public void adminCommand() {
        if (adminToken == null) {
            response.json(ApiResult.result(ApiCode.TOKEN_NOT_FOUND, locale));
            return;
        }

        Claims claims = parseToken(adminToken);
        if (claims == null || checkRoleFail(claims)) {
            return;
        }

        String command = request.body().get("command").toString();
        if (command == null) {
            response.json(ApiResult.result(ApiCode.PARAM_EMPTY_EXCEPTION, locale));
            return;
        }

        invokeCommand(null, command, "admin");
    }

    public void mailVerifyCode() {
        String username = request.body().get("username").toString();
        if (username == null) {
            response.json(ApiResult.result(ApiCode.PARAM_EMPTY_EXCEPTION, locale));
            return;
        }

        Player player = getPlayerByUsername(username);
        if (player == null) {
            return;
        }

        boolean flag = MailUtil.sendVerifyCodeMail(player, locale);
        if (!flag) {
            response.json(ApiResult.result(ApiCode.MAIL_TIME_LIMIT, locale));
            return;
        }

        response.json(ApiResult.success(locale));
    }

    public void playerAuthByVerifyCode() {
        AuthByVerifyCodeParam param = request.body(AuthByVerifyCodeParam.class);
        if (checkParamFail(param)) {
            return;
        }

        Account account = getAccountByUsername(param.getUsername());
        if (account == null) {
            return;
        }

        boolean flag = MailUtil.checkVerifyCode(account.getId(), param.getVerifyCode(), locale, response);
        if (!flag) {
            return;
        }

        generateToken(RoleEnum.PLAYER, account.getId());
    }

    public void playerAuthByPassword() {
        AccountParam param = request.body(AccountParam.class);
        if (checkParamFail(param)) {
            return;
        }

        Account account = getAccountByUsername(param.getUsername());
        if (account == null) {
            return;
        }

        if (!param.getPassword().equals(account.getPassword())) {
            response.json(ApiResult.result(ApiCode.AUTH_FAIL, locale));
            return;
        }

        generateToken(RoleEnum.PLAYER, account.getId());
    }

    public void playerCommand() {
        if (token == null) {
            response.json(ApiResult.result(ApiCode.TOKEN_NOT_FOUND, locale));
            return;
        }

        Claims claims = parseToken(token);
        if (claims == null) {
            return;
        }

        String command = request.body().get("command").toString();
        if (command == null) {
            response.json(ApiResult.result(ApiCode.PARAM_EMPTY_EXCEPTION, locale));
            return;
        }

        String accountId = claims.get("accountId").toString();
        Player player = getPlayerByAccountId(accountId);
        if (player == null) {
            return;
        }

        invokeCommand(player, command, accountId);
    }

    public void levelUpAllSkill() {
        if (token == null) {
            response.json(ApiResult.result(ApiCode.TOKEN_NOT_FOUND, locale));
            return;
        }

        Claims claims = parseToken(token);
        if (claims == null) {
            return;
        }

        String accountId = claims.get("accountId").toString();
        Player player = getPlayerByAccountId(accountId);
        if (player == null) {
            return;
        }

        for (Avatar avatar : player.getAvatars()) {
            AvatarSkillDepotData skillDepot = avatar.getData().getSkillDepot();
            int skillIdN = skillDepot.getSkills().get(0);
            int skillIdE = skillDepot.getSkills().get(1);
            int skillIdQ = skillDepot.getEnergySkill();
            Map<Integer, Integer> skillLevelMap = avatar.getSkillLevelMap();
            Integer oldLevelN = skillLevelMap.get(skillIdN);
            Integer oldLevelE = skillLevelMap.get(skillIdE);
            Integer oldLevelQ = skillLevelMap.get(skillIdQ);
            if (oldLevelN < 15) {
                skillLevelMap.put(skillIdN, 15);
                player.sendPacket(new PacketAvatarSkillChangeNotify(avatar, skillIdN, oldLevelN, 15));
                player.sendPacket(new PacketAvatarSkillUpgradeRsp(avatar, skillIdN, oldLevelN, 15));
            }
            if (oldLevelE < 15) {
                skillLevelMap.put(skillIdE, 15);
                player.sendPacket(new PacketAvatarSkillChangeNotify(avatar, skillIdE, oldLevelE, 15));
                player.sendPacket(new PacketAvatarSkillUpgradeRsp(avatar, skillIdE, oldLevelE, 15));
            }
            if (oldLevelQ < 15) {
                skillLevelMap.put(skillIdQ, 15);
                player.sendPacket(new PacketAvatarSkillChangeNotify(avatar, skillIdQ, oldLevelQ, 15));
                player.sendPacket(new PacketAvatarSkillUpgradeRsp(avatar, skillIdQ, oldLevelQ, 15));
            }
            avatar.save();
        }

        response.json(ApiResult.success(locale));
    }

    private void generateToken(RoleEnum role, String accountId) {
        try {
            String token = JwtUtil.generateToken(role, accountId);
            response.json(ApiResult.success(locale, token));
        } catch (Exception e) {
            response.json(ApiResult.result(ApiCode.TOKEN_GENERATE_FAIL, locale, token));
        }
    }


    private boolean checkParamFail(Object param) {
        for (Field field : param.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object object = field.get(param);
                if (object == null || object.toString().isEmpty()) {
                    response.json(ApiResult.result(ApiCode.PARAM_EMPTY_EXCEPTION, locale));
                    return true;
                }
            } catch (IllegalAccessException e) {
                response.json(ApiResult.result(ApiCode.PARAM_ILLEGAL_EXCEPTION, locale));
                return true;
            }
        }
        return false;
    }

    private Claims parseToken(String token) {
        try {
            return JwtUtil.parseToken(token);
        } catch (ExpiredJwtException e) {
            response.json(ApiResult.result(ApiCode.TOKEN_EXPIRED, locale));
        } catch (Exception e) {
            response.json(ApiResult.result(ApiCode.TOKEN_PARSE_EXCEPTION, locale));
        }
        return null;
    }

    private boolean checkRoleFail(@NotNull Claims claims) {
        if (!RoleEnum.ADMIN.getDesc().equals(claims.get("role").toString())) {
            response.json(ApiResult.result(ApiCode.ROLE_ERROR, locale));
            return true;
        }
        return false;
    }

    private Account getAccountByUsername(String username) {
        Account account = DatabaseHelper.getAccountByName(username);
        if (account == null) {
            response.json(ApiResult.result(ApiCode.ACCOUNT_NOT_EXIST, locale));
        }
        return account;
    }

    private Player getPlayerByAccountId(String accountId) {
        Player player = getGameServer().getPlayerByAccountId(accountId);
        if (player == null) {
            response.json(ApiResult.result(ApiCode.PLAYER_NOT_ONLINE, locale));
        }
        return player;
    }

    private Player getPlayerByUsername(String username) {
        Account account = getAccountByUsername(username);
        if (account == null) {
            return null;
        }
        Player player = getGameServer().getPlayerByAccountId(account.getId());
        if (player == null) {
            response.json(ApiResult.result(ApiCode.PLAYER_NOT_ONLINE, locale));
        }
        return player;
    }


    private void invokeCommand(Player player, String command, String accountId) {
        CommandMap.getInstance().invoke(player, player, command);
        String message = EventListeners.getMessage(accountId);
        if (message != null) {
            response.json(ApiResult.success(message));
        } else {
            response.json(ApiResult.fail(locale));
        }
    }
}
