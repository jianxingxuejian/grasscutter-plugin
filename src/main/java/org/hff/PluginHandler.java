package org.hff;

import emu.grasscutter.command.CommandMap;
import emu.grasscutter.database.DatabaseHelper;
import emu.grasscutter.game.Account;
import emu.grasscutter.game.player.Player;
import express.http.Request;
import express.http.Response;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.hff.api.ApiCode;
import org.hff.api.ApiResult;
import org.hff.api.param.*;
import org.hff.api.vo.TokenVo;
import org.hff.i18n.LanguageManager;
import org.hff.i18n.Locale;
import org.hff.permission.RoleEnum;
import org.hff.utils.AuthUtil;
import org.hff.utils.JwtUtil;
import org.hff.utils.MailUtil;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

import static emu.grasscutter.Grasscutter.getGameServer;

public final class PluginHandler {

    private final Request request;
    private final Response response;
    private final Locale locale;
    private final String token;

    public PluginHandler(@NotNull Request request, @NotNull Response response) {
        this.request = request;
        this.response = response;
        this.locale = LanguageManager.getLocale(request.get("locale"));
        this.token = request.get("token");
        if (token == null) {
            response.json(ApiResult.result(ApiCode.TOKEN_NOT_FOUND, locale));
        }

    }

    public void adminAuth() {
        AdminAuthParam param = request.body(AdminAuthParam.class);
        if (checkParamFail(param)) {
            return;
        }

        if (!AuthUtil.checkAdminVoucher(param.getAdminVoucher())) {
            response.json(ApiResult.result(ApiCode.AUTH_FAIL, locale));
            return;
        }

        String token = JwtUtil.generateToken(RoleEnum.ADMIN, "0", "admin");
        TokenVo tokenVo = new TokenVo(token);
        response.json(ApiResult.result(ApiCode.SUCCESS, locale, tokenVo));
    }

    public void adminCreateAccount(){
        AccountParam param = request.body(AccountParam.class);
        if (checkParamFail(param)) {
            return;
        }

        response.json(ApiResult.result(ApiCode.SUCCESS, locale));
    }

    public void adminCommand() {
        if (token == null) {
            return;
        }

        Claims claims = parseToken();
        if (claims == null || checkAdminFail(claims)) {
            return;
        }

        CommandParam param = request.body(CommandParam.class);
        if (checkParamFail(param)) {
            return;
        }

        invokeCommand(null, param.getCommand(), claims.get("uid").toString());
    }

    public void mailVerifyCode() {
        MailVerifyCodeParam param = request.body(MailVerifyCodeParam.class);
        if (checkParamFail(param)) {
            return;
        }

        Player player = getPlayerByUsername(param.getUsername());
        if (player == null) {
            return;
        }

        try {
            boolean flag = MailUtil.sendVerifyCodeMail(player, param.getUsername(), locale);
            if (!flag) {
                return;
            }
        } catch (Exception e) {
            response.json(ApiResult.result(ApiCode.MAIL_SEND_FAIL, locale));
            return;
        }

        response.json(ApiResult.success(locale));
    }

    public void playerAuthByVerifyCode() {
        VerifyCodeAuthParam param = request.body(VerifyCodeAuthParam.class);
        if (checkParamFail(param)) {
            return;
        }

        boolean flag = MailUtil.checkVerifyCode(param.getUsername(), param.getVerifyCode(), locale, response);
        if (!flag) {
            return;
        }

        String username = param.getUsername();
        Account account = getAccountByUsername(username);
        if (account == null) {
            return;
        }

        String token = JwtUtil.generateToken(RoleEnum.PLAYER, username, account.getId());
        TokenVo tokenVo = new TokenVo(token);
        response.json(ApiResult.success(locale, tokenVo));
    }

    public void playerAuthByPassword(){
        AccountParam param = request.body(AccountParam.class);
        if (checkParamFail(param)) {
            return;
        }
    }

    public void playerCommand() {
        if (token == null) {
            return;
        }

        Claims claims = parseToken();
        if (claims == null) {
            return;
        }

        CommandParam param = request.body(CommandParam.class);
        if (checkParamFail(param)) {
            return;
        }

        Player player = getPlayerByUid(claims.get("uid").toString());
        if (player == null) {
            return;
        }

        invokeCommand(player, param.getCommand(), claims.get("uid").toString());
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

    private Claims parseToken() {
        try {
            return JwtUtil.parseToken(token);
        } catch (ExpiredJwtException e) {
            response.json(ApiResult.result(ApiCode.TOKEN_EXPIRED, locale));
        } catch (Exception e) {
            response.json(ApiResult.result(ApiCode.TOKEN_PARSE_EXCEPTION, locale));
        }
        return null;
    }

    private boolean checkAdminFail(Claims claims) {
        RoleEnum role = claims.get("role", RoleEnum.class);
        if (role != RoleEnum.ADMIN) {
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

    private Player getPlayerByUid(String uid) {
        Player player = getGameServer().getPlayerByAccountId(uid);
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
        Player player = getPlayerByUid(account.getId());
        if (player == null) {
            response.json(ApiResult.result(ApiCode.PLAYER_NOT_ONLINE, locale));
        }
        return player;
    }

    private void invokeCommand(Player player, String command, String uid) {
        try {
            CommandMap.getInstance().invoke(player, player, command);
            String message = EventListeners.getMessage(uid);
            if (message != null) {
                response.json(ApiResult.success(message));
            } else {
                response.json(ApiResult.success(locale));
            }
        } catch (Exception e) {
            response.json(ApiResult.fail(locale));
        }
    }
}
