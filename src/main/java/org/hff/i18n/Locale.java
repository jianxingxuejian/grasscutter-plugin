package org.hff.i18n;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Locale {
    zh_CN("zh-CN"),
    en_US("en-US"),
    ;

    private final String desc;
}
