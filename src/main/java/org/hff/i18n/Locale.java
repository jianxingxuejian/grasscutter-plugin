package org.hff.i18n;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Locale {

    en("en"),

    zh_CN("zh-CN"),

    ;

    private final String desc;
}
