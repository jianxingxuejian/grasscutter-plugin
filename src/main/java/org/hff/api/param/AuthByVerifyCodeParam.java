package org.hff.api.param;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthByVerifyCodeParam {

    private String username;

    private String verifyCode;

}
