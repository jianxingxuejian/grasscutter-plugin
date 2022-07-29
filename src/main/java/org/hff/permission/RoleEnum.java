package org.hff.permission;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RoleEnum {

    ADMIN("admin"),

    PLAYER("player");

    private final String desc;
}
