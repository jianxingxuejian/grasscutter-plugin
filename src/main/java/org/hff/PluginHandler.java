package org.hff;

import emu.grasscutter.command.CommandMap;
import emu.grasscutter.database.DatabaseHelper;
import emu.grasscutter.game.Account;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.utils.MessageHandler;
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

import java.lang.reflect.Field;

import static emu.grasscutter.Grasscutter.getGameServer;

public final class PluginHandler {

    private final Request request;
    private final Response response;
    private final Locale locale;
    private final String token;

    public PluginHandler(Request request, Response response) {
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

        String token = JwtUtil.generateToken("admin", RoleEnum.ADMIN);
        TokenVo tokenVo = new TokenVo(token);
        response.json(ApiResult.result(ApiCode.SUCCESS, locale, tokenVo));
    }

    public void adminCommand() {
        if (token == null) {
            return;
        }

        Claims claims = parseToken();
        if (claims == null || checkAdminFail(claims)) {
            return;
        }

        AdminCommandParam param = request.body(AdminCommandParam.class);
        if (checkParamFail(param)) {
            return;
        }

        try {
            var handler = new MessageHandler();
            CommandMap.getInstance().invoke(null, null, param.getCommand());
            handler.getMessage();
        } catch (Exception e) {
            response.json(ApiResult.fail(locale));
            return;
        }

        response.json(ApiResult.success(locale));
    }

    public void mailVerifyCode() {
        MailVerifyCodeParam param = request.body(MailVerifyCodeParam.class);
        if (checkParamFail(param)) {
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

        try {
            boolean flag = MailUtil.sendVerifyCodeMail(player, username, locale);
            if (!flag) {
                return;
            }
        } catch (Exception e) {
            response.json(ApiResult.result(ApiCode.MAIL_SEND_FAIL, locale));
            return;
        }

        response.json(ApiResult.success(locale));
    }

    public void playerAuth() {
        PlayerAuthParam param = request.body(PlayerAuthParam.class);
        if (checkParamFail(param)) {
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

    public void playerCommand() {
        if (token == null) {
            return;
        }

        Claims claims = parseToken();
        if (claims == null) {
            return;
        }

        PlayerCommandParam param = request.body(PlayerCommandParam.class);
        if (checkParamFail(param)) {
            return;
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
}
