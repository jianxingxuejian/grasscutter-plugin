package org.hff.api;

import io.javalin.http.Context;
import lombok.*;
import lombok.experimental.Accessors;
import org.hff.i18n.LanguageManager;
import org.hff.i18n.Locale;

@Getter
@Setter
@Accessors(chain = true)
@AllArgsConstructor
public final class ApiResult {

    private int code;

    private String msg;

    private Object data;

    public static ApiResult success(Context ctx) {
        return result(ApiCode.SUCCESS, ctx);
    }

    public static ApiResult success(String msg) {
        return result(ApiCode.SUCCESS, msg);
    }

    public static ApiResult success(Context ctx, Object data) {
        return result(ApiCode.SUCCESS, ctx, data);
    }

    public static ApiResult fail(Context ctx) {
        return result(ApiCode.FAIL, ctx);
    }

    public static ApiResult fail(String msg) {
        return result(ApiCode.FAIL, msg);
    }

    public static ApiResult fail(Context ctx, Object data) {
        return result(ApiCode.FAIL, ctx, data);
    }

    public static ApiResult result(ApiCode code, Context ctx) {
        String msg = getMessage(ctx, code);
        return result(code, msg);
    }

    public static ApiResult result(ApiCode code, Context ctx, Object data) {
        String msg = getMessage(ctx, code);
        return result(code, msg, data);
    }

    public static ApiResult result(ApiCode code, String msg) {
        return new ApiResult(code.getCode(), msg, null);
    }

    public static ApiResult result(ApiCode code, String msg, Object data) {
        return new ApiResult(code.getCode(), msg, data);
    }

    private static String getMessage(Context ctx, ApiCode code) {
        Locale locale = LanguageManager.getLocale(ctx.req.getHeader("locale"));
        return LanguageManager.getMsg(locale, code);
    }

    public ApiResult setArgs(Object... args) {
        this.msg = String.format(this.msg, args);
        return this;
    }
}
