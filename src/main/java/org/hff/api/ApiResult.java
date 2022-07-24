package org.hff.api;

import lombok.*;
import org.hff.i18n.LanguageManager;
import org.hff.i18n.Locale;

@Getter
@Setter
@AllArgsConstructor
public final class ApiResult {

    private int code;

    private String msg;

    private Object data;

    public static ApiResult success(Locale locale) {
        return result(ApiCode.SUCCESS, locale);
    }

    public static ApiResult success(String msg) {
        return result(ApiCode.SUCCESS, msg);
    }

    public static ApiResult success(Locale locale, Object data) {
        return result(ApiCode.SUCCESS, locale, data);
    }

    public static ApiResult fail(Locale locale) {
        return result(ApiCode.FAIL, locale);
    }

    public static ApiResult fail(String msg) {
        return result(ApiCode.FAIL, msg);
    }

    public static ApiResult fail(Locale locale, Object data) {
        return result(ApiCode.FAIL, locale, data);
    }

    public static ApiResult result(ApiCode code, Locale locale) {
        return new ApiResult(code.getCode(), getMessage(locale, code), null);
    }

    public static ApiResult result(ApiCode code, String msg) {
        return new ApiResult(code.getCode(), msg, null);
    }

    public static ApiResult result(ApiCode code, Locale locale, Object data) {
        return new ApiResult(code.getCode(), getMessage(locale, code), data);
    }

    private static String getMessage(Locale locale, ApiCode code) {
        return LanguageManager.getMsg(locale, code);
    }
}
