package org.hff.api;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ApiCode {

    SUCCESS(200),

    FAIL(400),

    AUTH_FAIL(1001),

    ACCOUNT_NOT_EXIST(1002),

    NOT_ROLE(1003),

    JWT_EXPIRED(2001),

    JWT_PARSE_EXCEPTION(2002),

    PARAM_EMPTY_EXCEPTION(3001),

    PARAM_ILLEGAL_EXCEPTION(3002),

    PLAYER_NOT_ONLINE(4001),

    ;

    private final int code;
}
