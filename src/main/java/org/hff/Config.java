package org.hff;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Getter
@Setter
@ToString
@Accessors(chain = true)
public class Config {

    private String adminVoucher;

    private String secret;

    private long jwtExpire = 7 * 24 * 60 * 60 * 1000;

}
