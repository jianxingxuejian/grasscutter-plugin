package org.hff.api;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ApiCode {

    SUCCESS(200),

    FAIL(400),

    TOKEN_GENERATE_FAIL(1000),

    TOKEN_NOT_FOUND(1001),

    TOKEN_EXPIRED(1002),

    TOKEN_PARSE_EXCEPTION(1003),

    PARAM_EMPTY_EXCEPTION(1004),

    PARAM_ILLEGAL_EXCEPTION(1005),

    AUTH_FAIL(1006),

    ROLE_ERROR(1007),

    ACCOUNT_IS_EXIST(2000),

    ACCOUNT_NOT_EXIST(2001),

    PLAYER_NOT_ONLINE(2002),

    MAIL_TIME_LIMIT(2003),

    MAIL_VERIFY_NOT_FOUND(2004),

    MAIL_VERIFY_EXPIRED(2005),

    MAIL_VERIFY_FAIL(2006),

    ;

    private final int code;
}
