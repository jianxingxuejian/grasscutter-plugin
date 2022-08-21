package org.hff.api.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class PropsVo {

    private boolean inGodMode;

    private boolean unLimitedStamina;

    private boolean unLimitedEnergy;

    private int worldLevel;

    private int bpLevel;

    private int towerLevel;

    private int climateType;

    private int weatherId;

    private boolean isLockWeather;

    private boolean isLockGameTime;

    private int playerLevel;

    private int skillN;

    private int skillE;

    private int skillQ;

    private int avatarLevel;

    private int constellation;

    private int fetterLevel;
}
