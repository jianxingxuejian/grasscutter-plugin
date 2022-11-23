package org.hff.utils;

import emu.grasscutter.command.CommandMap;
import emu.grasscutter.database.DatabaseHelper;
import emu.grasscutter.game.Account;
import emu.grasscutter.game.player.Player;
import io.javalin.http.Context;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.hff.Config;
import org.hff.EventListeners;
import org.hff.MyPlugin;
import org.hff.api.ApiCode;
import org.hff.api.ApiResult;
import org.hff.permission.RoleEnum;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

import static emu.grasscutter.Grasscutter.getGameServer;

public class ResultUtil {

    private static final Config config = MyPlugin.getInstance().getConfig();

    public static boolean checkParamFail(Object param, Context ctx) {
        if (param == null) {
            ctx.json(ApiResult.result(ApiCode.PARAM_EMPTY_EXCEPTION, ctx));
            return true;
        }
        for (Field field : param.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object object = field.get(param);
                if (object == null || object.toString().isEmpty()) {
                    ctx.json(ApiResult.result(ApiCode.PARAM_EMPTY_EXCEPTION, ctx));
                    return true;
                }
            } catch (IllegalAccessException e) {
                ctx.json(ApiResult.result(ApiCode.PARAM_ILLEGAL_EXCEPTION, ctx));
                return true;
            }
        }
        return false;
    }

    public static String getQueryParam(@NotNull String key, Context ctx) {
        String param = ctx.queryParam(key);
        if (param == null) {
            ctx.json(ApiResult.result(ApiCode.PARAM_EMPTY_EXCEPTION, ctx));
        }
        return param;
    }

    public static boolean checkAdminVoucherFail(String voucher, Context ctx) {
        boolean equals = config.getAdminVoucher().equals(voucher);
        if (!equals) {
            ctx.json(ApiResult.result(ApiCode.AUTH_FAIL, ctx));
            return true;
        }
        return false;
    }

    public static boolean checkAdminFail(Context ctx) {
        String adminToken = ctx.req.getHeader("admin_token");
        if (adminToken == null) {
            ctx.json(ApiResult.result(ApiCode.TOKEN_NOT_FOUND, ctx));
            return true;
        }
        Claims claims = parseToken(adminToken, ctx);
        if (claims == null) {
            return true;
        }
        if (!RoleEnum.ADMIN.getDesc().equals(claims.get("role").toString())) {
            ctx.json(ApiResult.result(ApiCode.ROLE_ERROR, ctx));
            return true;
        }
        return false;
    }

    public static void generateToken(RoleEnum role, String accountId, Context ctx) {
        try {
            String token = JwtUtil.generateToken(role, accountId);
            ctx.json(ApiResult.success(ctx, token));
        } catch (Exception e) {
            ctx.json(ApiResult.result(ApiCode.TOKEN_GENERATE_FAIL, ctx, null));
        }
    }

    public static Claims parsePlayerToken(Context ctx) {
        String token = ctx.req.getHeader("token");
        if (token == null) {
            ctx.json(ApiResult.result(ApiCode.TOKEN_NOT_FOUND, ctx));
            return null;
        }
        return parseToken(token, ctx);
    }

    private static Claims parseToken(String token, Context ctx) {
        try {
            return JwtUtil.parseToken(token);
        } catch (ExpiredJwtException e) {
            ctx.json(ApiResult.result(ApiCode.TOKEN_EXPIRED, ctx));
        } catch (Exception e) {
            ctx.json(ApiResult.result(ApiCode.TOKEN_PARSE_EXCEPTION, ctx));
        }
        return null;
    }

    public static void invokeCommand(Player player, String command, String accountId, Context ctx) {
        CommandMap.getInstance().invoke(player, player, command);
        String message = EventListeners.getMessage(accountId);
        if (message != null) {
            ctx.json(ApiResult.success(message));
        } else {
            ctx.json(ApiResult.fail(ctx));
        }
    }

    public static Account getAccountByUsername(String username, Context ctx) {
        Account account = DatabaseHelper.getAccountByName(username);
        if (account == null) {
            ctx.json(ApiResult.result(ApiCode.ACCOUNT_NOT_EXIST, ctx));
        }
        return account;
    }

    public static Player getPlayerByAccountId(String accountId, Context ctx) {
        Player player = getGameServer().getPlayerByAccountId(accountId);
        if (player == null) {
            ctx.json(ApiResult.result(ApiCode.PLAYER_NOT_ONLINE, ctx));
        }
        return player;
    }

    public static Player getPlayerByUsername(String username, Context ctx) {
        Account account = getAccountByUsername(username, ctx);
        if (account == null) {
            return null;
        }
        Player player = getGameServer().getPlayerByAccountId(account.getId());
        if (player == null) {
            ctx.json(ApiResult.result(ApiCode.PLAYER_NOT_ONLINE, ctx));
        }
        return player;
    }
}
