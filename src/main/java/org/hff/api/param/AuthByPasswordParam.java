package org.hff.api.param;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthByPasswordParam {

    private String uid;

    private String password;
}
