package org.hff;

import emu.grasscutter.command.CommandMap;
import emu.grasscutter.data.excels.AvatarSkillDepotData;
import emu.grasscutter.database.DatabaseHelper;
import emu.grasscutter.game.Account;
import emu.grasscutter.game.avatar.Avatar;
import emu.grasscutter.game.entity.EntityAvatar;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.game.props.PlayerProperty;
import emu.grasscutter.game.world.Scene;
import emu.grasscutter.game.world.World;
import emu.grasscutter.server.packet.send.PacketAvatarFetterDataNotify;
import emu.grasscutter.server.packet.send.PacketEntityFightPropUpdateNotify;
import emu.grasscutter.server.packet.send.PacketSceneEntityAppearNotify;
import emu.grasscutter.utils.Position;
import io.javalin.http.Context;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.hff.api.ApiCode;
import org.hff.api.ApiResult;
import org.hff.api.param.AccountParam;
import org.hff.api.param.AuthByVerifyCodeParam;
import org.hff.api.vo.PropsVo;
import org.hff.i18n.LanguageManager;
import org.hff.i18n.Locale;
import org.hff.permission.RoleEnum;
import org.hff.utils.AuthUtil;
import org.hff.utils.JwtUtil;
import org.hff.utils.MailUtil;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static emu.grasscutter.Grasscutter.*;
import static emu.grasscutter.game.props.FightProperty.FIGHT_PROP_SKILL_CD_MINUS_RATIO;

public final class PluginHandler {

    private final Context ctx;
    private final Locale locale;
    private final String token;
    private final String adminToken;

    public PluginHandler(@NotNull Context ctx) {
        this.ctx = ctx;
        this.locale = LanguageManager.getLocale(ctx.req.getHeader("locale"));
        this.token = ctx.req.getHeader("token");
        this.adminToken = ctx.req.getHeader("admin_token");
    }

    public void adminAuth() {
        String adminVoucher = ctx.queryParam("adminVoucher");
        if (adminVoucher == null) {
            ctx.json(ApiResult.result(ApiCode.PARAM_EMPTY_EXCEPTION, locale));
            return;
        }

        if (!AuthUtil.checkAdminVoucher(adminVoucher)) {
            ctx.json(ApiResult.result(ApiCode.AUTH_FAIL, locale));
            return;
        }

        generateToken(RoleEnum.ADMIN, "admin");
    }

    public void adminCreateAccount() {
        Claims claims = parseAdminToken();
        if (claims == null) {
            return;
        }

        AccountParam param = ctx.bodyAsClass(AccountParam.class);
        if (checkParamFail(param)) {
            return;
        }

        Account account = DatabaseHelper.getAccountByName(param.getUsername());
        if (account != null) {
            ctx.json(ApiResult.result(ApiCode.ACCOUNT_IS_EXIST, locale));
            return;
        }

        getAuthenticationSystem().createAccount(param.getUsername(), param.getPassword());
        ctx.json(ApiResult.success(locale));
    }

    public void adminCommand() {
        Claims claims = parseAdminToken();
        if (claims == null) {
            return;
        }

        String command = ctx.queryParam("command");
        if (command == null) {
            ctx.json(ApiResult.result(ApiCode.PARAM_EMPTY_EXCEPTION, locale));
            return;
        }

        invokeCommand(null, command, "admin");
    }

    public void mailVerifyCode() {
        String username = ctx.queryParam("username");
        if (username == null) {
            ctx.json(ApiResult.result(ApiCode.PARAM_EMPTY_EXCEPTION, locale));
            return;
        }

        Player player = getPlayerByUsername(username);
        if (player == null) {
            return;
        }

        boolean flag = MailUtil.sendVerifyCodeMail(player, locale);
        if (!flag) {
            ctx.json(ApiResult.result(ApiCode.MAIL_TIME_LIMIT, locale));
            return;
        }

        ctx.json(ApiResult.success(locale));
    }

    public void playerAuthByVerifyCode() {
        AuthByVerifyCodeParam param = ctx.bodyAsClass(AuthByVerifyCodeParam.class);
        if (checkParamFail(param)) {
            return;
        }

        Account account = getAccountByUsername(param.getUsername());
        if (account == null) {
            return;
        }

        boolean flag = MailUtil.checkVerifyCode(account.getId(), param.getVerifyCode(), locale, ctx);
        if (!flag) {
            return;
        }

        generateToken(RoleEnum.PLAYER, account.getId());
    }

    public void playerAuthByPassword() {
        AccountParam param = ctx.bodyAsClass(AccountParam.class);
        if (checkParamFail(param)) {
            return;
        }

        Account account = getAccountByUsername(param.getUsername());
        if (account == null) {
            return;
        }

        if (!param.getPassword().equals(account.getPassword())) {
            ctx.json(ApiResult.result(ApiCode.AUTH_FAIL, locale));
            return;
        }

        generateToken(RoleEnum.PLAYER, account.getId());
    }

    public void playerCommand() {
        Claims claims = parsePlayerToken();
        if (claims == null) {
            return;
        }

        String command = ctx.queryParam("command");
        if (command == null) {
            ctx.json(ApiResult.result(ApiCode.PARAM_EMPTY_EXCEPTION, locale));
            return;
        }

        String accountId = claims.get("accountId").toString();
        Player player = getPlayerByAccountId(accountId);
        if (player == null) {
            return;
        }

        invokeCommand(player, command, accountId);
    }

    public void levelUpAll() {
        Claims claims = parsePlayerToken();
        if (claims == null) {
            return;
        }

        String accountId = claims.get("accountId").toString();
        Player player = getPlayerByAccountId(accountId);
        if (player == null) {
            return;
        }

        Integer type = ctx.queryParamAsClass("type", Integer.class).getOrDefault(0);

        boolean flag = false;

        if (type == 0) {
            for (Avatar avatar : player.getAvatars()) {
                flag = levelUpConstellation(avatar);
                levelUpSkill(avatar);
                levelUpFetter(player,avatar);
                avatar.save();
            }
        } else if (type == 1) {
            for (Avatar avatar : player.getAvatars()) {
                flag = levelUpConstellation(avatar);
                avatar.save();
            }
        } else if (type == 2) {
            for (Avatar avatar : player.getAvatars()) {
                levelUpSkill(avatar);
                avatar.save();
            }
        } else if (type == 3) {
            for (Avatar avatar : player.getAvatars()) {
                levelUpFetter(player,avatar);
                avatar.save();
            }
        }

        if (flag) {
            World world = player.getWorld();
            Scene scene = player.getScene();
            Position pos = player.getPosition();
            world.transferPlayerToScene(player, 1, pos);
            world.transferPlayerToScene(player, scene.getId(), pos);
            scene.broadcastPacket(new PacketSceneEntityAppearNotify(player));
        }

        ctx.json(ApiResult.success(locale));
    }


    private boolean levelUpConstellation(Avatar avatar) {
        boolean flag = false;
        if (avatar.getLevel() < 90) {
            flag = true;
            avatar.setLevel(90);
        }
        if (avatar.getPromoteLevel() < 6) {
            flag = true;
            avatar.setPromoteLevel(6);
        }
        if (avatar.getCoreProudSkillLevel() < 6) {
            avatar.forceConstellationLevel(6);
        }
        avatar.recalcConstellations();
        avatar.recalcStats(true);
        return flag;
    }

    private void levelUpSkill(Avatar avatar) {
        AvatarSkillDepotData skillDepot = avatar.getSkillDepot();
        Integer skillIdN = skillDepot.getSkills().get(0);
        Integer skillIdE = skillDepot.getSkills().get(1);
        int skillIdQ = skillDepot.getEnergySkill();
        Map<Integer, Integer> skillLevelMap = avatar.getSkillLevelMap();
        Integer oldLevelN = skillLevelMap.getOrDefault(skillIdN, 0);
        Integer oldLevelE = skillLevelMap.getOrDefault(skillIdE, 0);
        Integer oldLevelQ = skillLevelMap.getOrDefault(skillIdQ, 0);
        if (oldLevelN < 14) {
            avatar.setSkillLevel(skillIdN, 14);
        }
        if (oldLevelE < 12) {
            avatar.setSkillLevel(skillIdE, 12);
        }
        if (oldLevelQ < 12) {
            avatar.setSkillLevel(skillIdQ, 12);
        }
    }

    private void levelUpFetter(Player player,Avatar avatar) {
        avatar.setFetterLevel(10);
        player.sendPacket(new PacketAvatarFetterDataNotify(avatar));
    }

    public void getProps() {
        Claims claims = parsePlayerToken();
        if (claims == null) {
            return;
        }

        String accountId = claims.get("accountId").toString();
        Player player = getPlayerByAccountId(accountId);
        if (player == null) {
            return;
        }

        Avatar avatar = player.getTeamManager().getCurrentAvatarEntity().getAvatar();
        Map<Integer, Integer> skillLevelMap = avatar.getSkillLevelMap();
        AvatarSkillDepotData skillDepot = avatar.getSkillDepot();
        PropsVo propsVo = new PropsVo();
        propsVo.setInGodMode(player.inGodmode())
                .setUnLimitedStamina(player.getUnlimitedStamina())
                .setUnLimitedEnergy(!player.getEnergyManager().getEnergyUsage())
                .setWorldLevel(player.getWorldLevel())
                .setBpLevel(player.getBattlePassManager().getLevel())
                .setTowerLevel(player.getTowerManager().getRecordMap().size())
                .setPlayerLevel(player.getLevel())
                .setClimateType(player.getClimate().getValue() - 1)
                .setWeatherId(player.getWeatherId())
                .setLockWeather(player.getProperty(PlayerProperty.PROP_IS_WEATHER_LOCKED) == 1)
                .setLockGameTime(player.getProperty(PlayerProperty.PROP_IS_GAME_TIME_LOCKED) == 1)
                .setSkillN(skillLevelMap.get(skillDepot.getSkills().get(0)))
                .setSkillE(skillLevelMap.get(skillDepot.getSkills().get(1)))
                .setSkillQ(skillLevelMap.get(skillDepot.getEnergySkill()))
                .setAvatarLevel(avatar.getLevel())
                .setConstellation(avatar.getCoreProudSkillLevel())
                .setFetterLevel(avatar.getFetterLevel());

        ctx.json(ApiResult.success(locale, propsVo));
    }

    public void cdr() {
        Claims claims = parsePlayerToken();
        if (claims == null) {
            return;
        }

        String accountId = claims.get("accountId").toString();
        Player player = getPlayerByAccountId(accountId);
        if (player == null) {
            return;
        }

        List<EntityAvatar> team = player.getTeamManager().getActiveTeam();
        for (Avatar avatar : player.getAvatars()) {
            team.stream().filter(e -> e.getAvatar().getAvatarId() == avatar.getAvatarId()).findFirst()
                    .ifPresent(e -> {
                        e.setFightProperty(FIGHT_PROP_SKILL_CD_MINUS_RATIO, 1);
                        e.getWorld().broadcastPacket(new PacketEntityFightPropUpdateNotify(e, FIGHT_PROP_SKILL_CD_MINUS_RATIO));
                    });
            avatar.getFightPropOverrides().put(80, 1);
            avatar.recalcStats();
            avatar.save();
        }

        ctx.json(ApiResult.success(locale));
    }


    private Claims parseAdminToken() {
        if (adminToken == null) {
            ctx.json(ApiResult.result(ApiCode.TOKEN_NOT_FOUND, locale));
            return null;
        }
        Claims claims = parseToken(adminToken);
        if (claims == null) {
            return null;
        }
        if (!RoleEnum.ADMIN.getDesc().equals(claims.get("role").toString())) {
            ctx.json(ApiResult.result(ApiCode.ROLE_ERROR, locale));
            return null;
        }
        return claims;
    }

    private Claims parsePlayerToken() {
        if (token == null) {
            ctx.json(ApiResult.result(ApiCode.TOKEN_NOT_FOUND, locale));
            return null;
        }
        return parseToken(token);
    }

    private Claims parseToken(String token) {
        try {
            return JwtUtil.parseToken(token);
        } catch (ExpiredJwtException e) {
            ctx.json(ApiResult.result(ApiCode.TOKEN_EXPIRED, locale));
        } catch (Exception e) {
            ctx.json(ApiResult.result(ApiCode.TOKEN_PARSE_EXCEPTION, locale));
        }
        return null;
    }

    private void generateToken(RoleEnum role, String accountId) {
        try {
            String token = JwtUtil.generateToken(role, accountId);
            ctx.json(ApiResult.success(locale, token));
        } catch (Exception e) {
            ctx.json(ApiResult.result(ApiCode.TOKEN_GENERATE_FAIL, locale, token));
        }
    }


    private boolean checkParamFail(Object param) {
        for (Field field : param.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object object = field.get(param);
                if (object == null || object.toString().isEmpty()) {
                    ctx.json(ApiResult.result(ApiCode.PARAM_EMPTY_EXCEPTION, locale));
                    return true;
                }
            } catch (IllegalAccessException e) {
                ctx.json(ApiResult.result(ApiCode.PARAM_ILLEGAL_EXCEPTION, locale));
                return true;
            }
        }
        return false;
    }

    private Account getAccountByUsername(String username) {
        Account account = DatabaseHelper.getAccountByName(username);
        if (account == null) {
            ctx.json(ApiResult.result(ApiCode.ACCOUNT_NOT_EXIST, locale));
        }
        return account;
    }

    private Player getPlayerByAccountId(String accountId) {
        Player player = getGameServer().getPlayerByAccountId(accountId);
        if (player == null) {
            ctx.json(ApiResult.result(ApiCode.PLAYER_NOT_ONLINE, locale));
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
            ctx.json(ApiResult.result(ApiCode.PLAYER_NOT_ONLINE, locale));
        }
        return player;
    }


    private void invokeCommand(Player player, String command, String accountId) {
        CommandMap.getInstance().invoke(player, player, command);
        String message = EventListeners.getMessage(accountId);
        if (message != null) {
            ctx.json(ApiResult.success(message));
        } else {
            ctx.json(ApiResult.fail(locale));
        }
    }
}
