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
import org.hff.api.param.AccountParam;
import org.hff.api.param.AuthByPasswordParam;
import org.hff.api.param.AuthByVerifyCodeParam;
import org.hff.i18n.LanguageManager;
import org.hff.i18n.Locale;
import org.hff.permission.RoleEnum;
import org.hff.utils.AuthUtil;
import org.hff.utils.JwtUtil;
import org.hff.utils.MailUtil;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

import static emu.grasscutter.Grasscutter.getGameServer;
import static emu.grasscutter.Grasscutter.getLogger;

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

        String token = JwtUtil.generateToken(RoleEnum.ADMIN, "0");
        response.json(ApiResult.result(ApiCode.SUCCESS, locale, token));
    }

    public void adminCreateAccount() {
        AccountParam param = request.body(AccountParam.class);
        if (checkParamFail(param)) {
            return;
        }

        Account account = DatabaseHelper.createAccountWithPassword(param.getUsername(), param.getPassword());
        if (account == null) {
            response.json(ApiResult.result(ApiCode.ACCOUNT_IS_EXIST, locale));
            return;
        }

        response.json(ApiResult.result(ApiCode.SUCCESS, locale));
    }

    public void adminCommand() {
        if (adminToken == null) {
            response.json(ApiResult.result(ApiCode.TOKEN_NOT_FOUND, locale));
            return;
        }

        Claims claims = parseToken();
        if (claims == null || checkRoleFail(claims, RoleEnum.ADMIN)) {
            return;
        }

        String command = request.body().get("command").toString();
        if (command == null) {
            response.json(ApiResult.result(ApiCode.PARAM_EMPTY_EXCEPTION, locale));
            return;
        }

        invokeCommand(null, command, claims.get("uid").toString());
    }

    public void mailVerifyCode() {
        String uid = request.body().get("uid").toString();
        if (uid == null) {
            response.json(ApiResult.result(ApiCode.PARAM_EMPTY_EXCEPTION, locale));
            return;
        }

        Player player = getPlayerByUid(uid);
        if (player == null) {
            return;
        }

        boolean flag = MailUtil.sendVerifyCodeMail(player, uid, locale);
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

        String uid = param.getUid();
        boolean flag = MailUtil.checkVerifyCode(uid, param.getVerifyCode(), locale, response);
        if (!flag) {
            return;
        }

        Account account = getAccountByUid(uid);
        if (account == null) {
            return;
        }

        String token = JwtUtil.generateToken(RoleEnum.PLAYER, uid);
        response.json(ApiResult.success(locale, token));
    }

    public void playerAuthByPassword() {
        AuthByPasswordParam param = request.body(AuthByPasswordParam.class);
        if (checkParamFail(param)) {
            return;
        }

        Account account = getAccountByUid(param.getUid());
        if (account == null) {
            return;
        }

        if (!param.getPassword().equals(account.getPassword())) {
            response.json(ApiResult.result(ApiCode.AUTH_FAIL, locale));
            return;
        }

        response.json(ApiResult.success(locale, token));
    }

    public void playerCommand() {
        if (token == null) {
            response.json(ApiResult.result(ApiCode.TOKEN_NOT_FOUND, locale));
            return;
        }

        Claims claims = parseToken();
        if (claims == null) {
            return;
        }

        String command = request.body().get("command").toString();
        if (command == null) {
            response.json(ApiResult.result(ApiCode.PARAM_EMPTY_EXCEPTION, locale));
            return;
        }

        Player player = getPlayerByUid(claims.get("uid").toString());
        if (player == null) {
            return;
        }

        invokeCommand(player, command, claims.get("uid").toString());
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

    private boolean checkRoleFail(@NotNull Claims claims, @NotNull RoleEnum role) {
        if (!role.getDesc().equals(claims.get("role").toString())) {
            response.json(ApiResult.result(ApiCode.ROLE_ERROR, locale));
            return true;
        }
        return false;
    }

    private Account getAccountByUid(String uid) {
        Account account = DatabaseHelper.getAccountById(uid);
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


    private void invokeCommand(Player player, String command, String uid) {
        CommandMap.getInstance().invoke(player, player, command);
        String message = EventListeners.getMessage(uid);
        if (message != null) {
            getLogger().info(message);
            response.json(ApiResult.success(message));
        } else {
            response.json(ApiResult.fail(locale));
        }
    }
}
