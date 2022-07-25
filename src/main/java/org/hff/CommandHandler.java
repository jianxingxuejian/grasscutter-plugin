package org.hff;

import emu.grasscutter.command.CommandMap;
import emu.grasscutter.database.DatabaseHelper;
import emu.grasscutter.game.Account;
import emu.grasscutter.game.mail.Mail;
import emu.grasscutter.game.player.Player;
import express.http.Request;
import express.http.Response;
import io.jsonwebtoken.Claims;
import org.hff.api.ApiCode;
import org.hff.api.ApiResult;
import org.hff.api.param.AdminAuthParam;
import org.hff.api.param.AdminCommandParam;
import org.hff.api.param.MailVerifyCodeParam;
import org.hff.api.param.PlayerAuthParam;
import org.hff.api.vo.TokenVo;
import org.hff.i18n.LanguageManager;
import org.hff.i18n.Locale;
import org.hff.permission.RoleEnum;
import org.hff.utils.JwtUtil;
import org.hff.utils.MailUtil;

import java.lang.reflect.Field;

import static emu.grasscutter.Grasscutter.getGameServer;
import static emu.grasscutter.Grasscutter.getLogger;

public class CommandHandler {

    private static final Config config = MyPlugin.config;

    public static void adminAuth(Request request, Response response) {
        Locale locale = getLocale(request);

        AdminAuthParam param = request.body(AdminAuthParam.class);
        if (checkParamFail(param, locale, response)) {
            return;
        }

        if (!param.getAdminVoucher().equals(config.getAdminVoucher())) {
            response.json(ApiResult.result(ApiCode.AUTH_FAIL, locale));
            return;
        }

        String token = JwtUtil.generateToken("admin", RoleEnum.ADMIN);
        TokenVo tokenVo = new TokenVo(token);
        response.json(ApiResult.result(ApiCode.SUCCESS, locale, tokenVo));
    }

    public static void adminCommand(Request request, Response response) {
        String token = getToken(request, response);
        if (token == null) {
            return;
        }

        Locale locale = getLocale(request);

        Claims claims = JwtUtil.parseToken(token, locale, response);
        if (claims == null || checkAdminFail(claims, locale, response)) {
            return;
        }

        AdminCommandParam param = request.body(AdminCommandParam.class);
        if (checkParamFail(param, locale, response)) {
            return;
        }

        try {
            CommandMap.getInstance().invoke(null, null, param.getCommand());
        } catch (Exception e) {
            response.json(ApiResult.fail(locale));
            return;
        }

        response.json(ApiResult.success(locale));
    }

    public static void mailVerifyCode(Request request, Response response) {
        Locale locale = getLocale(request);
        MailVerifyCodeParam param = request.body(MailVerifyCodeParam.class);

        if (checkParamFail(param, locale, response)) {
            return;
        }

        String username = param.getUsername();
        Account account = DatabaseHelper.getAccountByName(username);
        if (account == null) {
            response.json(ApiResult.result(ApiCode.ACCOUNT_NOT_EXIST, locale));
            return;
        }

        Player player = getGameServer().getPlayerByAccountId(account.getId());
        if (player == null) {
            response.json(ApiResult.result(ApiCode.PLAYER_NOT_ONLINE, locale));
            return;
        }

        boolean flag = MailUtil.sendVerifyCodeMail(player, username, locale, response);
        if (!flag) {
            return;
        }

        response.json(ApiResult.success(locale));
    }

    public static void playerAuth(Request request, Response response) {
        Locale locale = getLocale(request);

        PlayerAuthParam param = request.body(PlayerAuthParam.class);
        if (checkParamFail(param, locale, response)) {
            return;
        }

        boolean flag = MailUtil.checkVerifyCode(param.getUsername(), param.getVerifyCode(), locale, response);
        if (!flag) {
            return;
        }

        String token = JwtUtil.generateToken(param.getUsername(), RoleEnum.PLAYER);
        TokenVo tokenVo = new TokenVo(token);
        response.json(ApiResult.success(locale, tokenVo));
    }

    public static void playerCommand(Request request, Response response) {

    }

    private static Locale getLocale(Request request) {
        String locale = request.get("locale");
        return LanguageManager.getLocale(locale);
    }

    private static String getToken(Request request, Response response) {
        String token = request.get("token");
        if (token == null) {
            response.json(ApiResult.result(ApiCode.TOKEN_NOT_FOUND, getLocale(request)));
            return null;
        }
        return token;
    }

    private static boolean checkParamFail(Object param, Locale locale, Response response) {
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

    private static boolean checkAdminFail(Claims claims, Locale locale, Response response) {
        RoleEnum role = claims.get("role", RoleEnum.class);
        if (role != RoleEnum.ADMIN) {
            response.json(ApiResult.result(ApiCode.ROLE_ERROR, locale));
            return true;
        }
        return false;
    }
}
