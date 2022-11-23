package org.hff;

import emu.grasscutter.data.excels.AvatarSkillDepotData;
import emu.grasscutter.database.DatabaseHelper;
import emu.grasscutter.game.Account;
import emu.grasscutter.game.avatar.Avatar;
import emu.grasscutter.game.entity.EntityAvatar;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.game.props.PlayerProperty;
import emu.grasscutter.game.world.Scene;
import emu.grasscutter.game.world.World;
import emu.grasscutter.server.packet.send.PacketAvatarFetterDataNotify;
import emu.grasscutter.server.packet.send.PacketEntityFightPropUpdateNotify;
import emu.grasscutter.server.packet.send.PacketSceneEntityAppearNotify;
import emu.grasscutter.utils.Position;
import io.javalin.http.Context;
import io.jsonwebtoken.Claims;
import org.hff.api.ApiCode;
import org.hff.api.ApiResult;
import org.hff.api.param.AccountParam;
import org.hff.api.param.AdminAuthParam;
import org.hff.api.param.AuthByVerifyCodeParam;
import org.hff.api.vo.PropsVo;
import org.hff.permission.RoleEnum;
import org.hff.utils.MailUtil;

import java.util.List;
import java.util.Map;

import static emu.grasscutter.Grasscutter.getAuthenticationSystem;
import static emu.grasscutter.game.props.FightProperty.FIGHT_PROP_SKILL_CD_MINUS_RATIO;
import static org.hff.utils.ResultUtil.*;

public final class PluginHandler {

    public static void adminAuth(Context ctx) {
        AdminAuthParam param = ctx.bodyAsClass(AdminAuthParam.class);
        if (checkParamFail(param, ctx)) return;
        if (checkAdminVoucherFail(param.getAdminVoucher(), ctx)) return;

        generateToken(RoleEnum.ADMIN, "admin", ctx);
    }

    public static void adminCreateAccount(Context ctx) {
        if (checkAdminFail(ctx)) return;

        AccountParam param = ctx.bodyAsClass(AccountParam.class);
        if (checkParamFail(param, ctx)) return;

        Account account = DatabaseHelper.getAccountByName(param.getUsername());
        if (account != null) {
            ctx.json(ApiResult.result(ApiCode.ACCOUNT_IS_EXIST, ctx));
            return;
        }

        getAuthenticationSystem().createAccount(param.getUsername(), param.getPassword());

        ctx.json(ApiResult.success(ctx));
    }

    public static void adminCommand(Context ctx) {
        if (checkAdminFail(ctx)) return;

        String command = getQueryParam("command", ctx);
        if (command == null) return;

        invokeCommand(null, command, "admin", ctx);
    }

    public static void mailVerifyCode(Context ctx) {
        String username = getQueryParam("username", ctx);
        if (username == null) return;

        Player player = getPlayerByUsername(username, ctx);
        if (player == null) return;

        boolean flag = MailUtil.sendVerifyCodeMail(player, ctx);
        if (!flag) return;

        ctx.json(ApiResult.success(ctx));
    }

    public static void playerAuthByVerifyCode(Context ctx) {
        AuthByVerifyCodeParam param = ctx.bodyAsClass(AuthByVerifyCodeParam.class);
        if (checkParamFail(param, ctx)) return;

        Account account = getAccountByUsername(param.getUsername(), ctx);
        if (account == null) return;

        boolean flag = MailUtil.checkVerifyCode(account.getId(), param.getVerifyCode(), ctx);
        if (!flag) return;

        generateToken(RoleEnum.PLAYER, account.getId(), ctx);
    }

    public static void playerAuthByPassword(Context ctx) {
        AccountParam param = ctx.bodyAsClass(AccountParam.class);
        if (checkParamFail(param, ctx)) return;

        Account account = getAccountByUsername(param.getUsername(), ctx);
        if (account == null) return;

        if (!param.getPassword().equals(account.getPassword())) {
            ctx.json(ApiResult.result(ApiCode.AUTH_FAIL, ctx));
            return;
        }

        generateToken(RoleEnum.PLAYER, account.getId(), ctx);
    }

    public static void playerCommand(Context ctx) {
        Claims claims = parsePlayerToken(ctx);
        if (claims == null) return;

        String command = getQueryParam("command", ctx);
        if (command == null) return;

        String accountId = claims.get("accountId").toString();
        Player player = getPlayerByAccountId(accountId, ctx);
        if (player == null) return;

        invokeCommand(player, command, accountId, ctx);
    }


    public static void levelUpAll(Context ctx) {
        Claims claims = parsePlayerToken(ctx);
        if (claims == null) return;

        String accountId = claims.get("accountId").toString();
        Player player = getPlayerByAccountId(accountId, ctx);
        if (player == null) return;

        Integer type = ctx.queryParamAsClass("type", Integer.class).getOrDefault(0);

        boolean flag = false;

        if (type == 0) {
            for (Avatar avatar : player.getAvatars()) {
                flag = levelUpConstellation(avatar);
                levelUpSkill(avatar);
                levelUpFetter(player, avatar);
                avatar.save();
            }
        } else if (type == 1) {
            for (Avatar avatar : player.getAvatars()) {
                flag = levelUpConstellation(avatar);
                avatar.save();
            }
        } else if (type == 2) {
            for (Avatar avatar : player.getAvatars()) {
                levelUpSkill(avatar);
                avatar.save();
            }
        } else if (type == 3) {
            for (Avatar avatar : player.getAvatars()) {
                levelUpFetter(player, avatar);
                avatar.save();
            }
        }

        if (flag) {
            World world = player.getWorld();
            Scene scene = player.getScene();
            Position pos = player.getPosition();
            world.transferPlayerToScene(player, 1, pos);
            world.transferPlayerToScene(player, scene.getId(), pos);
            scene.broadcastPacket(new PacketSceneEntityAppearNotify(player));
        }

        ctx.json(ApiResult.success(ctx));
    }


    private static boolean levelUpConstellation(Avatar avatar) {
        boolean flag = false;
        if (avatar.getLevel() < 90) {
            flag = true;
            avatar.setLevel(90);
        }
        if (avatar.getPromoteLevel() < 6) {
            flag = true;
            avatar.setPromoteLevel(6);
        }
        if (avatar.getCoreProudSkillLevel() < 6) {
            avatar.forceConstellationLevel(6);
        }
        avatar.recalcConstellations();
        avatar.recalcStats(true);
        return flag;
    }

    private static void levelUpSkill(Avatar avatar) {
        AvatarSkillDepotData skillDepot = avatar.getSkillDepot();
        Integer skillIdN = skillDepot.getSkills().get(0);
        Integer skillIdE = skillDepot.getSkills().get(1);
        int skillIdQ = skillDepot.getEnergySkill();
        Map<Integer, Integer> skillLevelMap = avatar.getSkillLevelMap();
        Integer oldLevelN = skillLevelMap.getOrDefault(skillIdN, 0);
        Integer oldLevelE = skillLevelMap.getOrDefault(skillIdE, 0);
        Integer oldLevelQ = skillLevelMap.getOrDefault(skillIdQ, 0);
        if (oldLevelN < 14) {
            avatar.setSkillLevel(skillIdN, 14);
        }
        if (oldLevelE < 12) {
            avatar.setSkillLevel(skillIdE, 12);
        }
        if (oldLevelQ < 12) {
            avatar.setSkillLevel(skillIdQ, 12);
        }
    }

    private static void levelUpFetter(Player player, Avatar avatar) {
        avatar.setFetterLevel(10);
        player.sendPacket(new PacketAvatarFetterDataNotify(avatar));
    }

    public static void getProps(Context ctx) {
        Claims claims = parsePlayerToken(ctx);
        if (claims == null) return;

        String accountId = claims.get("accountId").toString();
        Player player = getPlayerByAccountId(accountId, ctx);
        if (player == null) return;

        Avatar avatar = player.getTeamManager().getCurrentAvatarEntity().getAvatar();
        Map<Integer, Integer> skillLevelMap = avatar.getSkillLevelMap();
        AvatarSkillDepotData skillDepot = avatar.getSkillDepot();
        PropsVo propsVo = new PropsVo();
        propsVo.setInGodMode(player.inGodmode())
                .setUnLimitedStamina(player.getUnlimitedStamina())
                .setUnLimitedEnergy(!player.getEnergyManager().getEnergyUsage())
                .setWorldLevel(player.getWorldLevel())
                .setBpLevel(player.getBattlePassManager().getLevel())
                .setTowerLevel(player.getTowerManager().getRecordMap().size())
                .setPlayerLevel(player.getLevel())
                .setClimateType(player.getClimate().getValue() - 1)
                .setWeatherId(player.getWeatherId())
                .setLockWeather(player.getProperty(PlayerProperty.PROP_IS_WEATHER_LOCKED) == 1)
                .setLockGameTime(player.getProperty(PlayerProperty.PROP_IS_GAME_TIME_LOCKED) == 1)
                .setSkillN(skillLevelMap.get(skillDepot.getSkills().get(0)))
                .setSkillE(skillLevelMap.get(skillDepot.getSkills().get(1)))
                .setSkillQ(skillLevelMap.get(skillDepot.getEnergySkill()))
                .setAvatarLevel(avatar.getLevel())
                .setConstellation(avatar.getCoreProudSkillLevel())
                .setFetterLevel(avatar.getFetterLevel());

        ctx.json(ApiResult.success(ctx, propsVo));
    }

    public static void cdr(Context ctx) {
        Claims claims = parsePlayerToken(ctx);
        if (claims == null) return;

        String accountId = claims.get("accountId").toString();
        Player player = getPlayerByAccountId(accountId, ctx);
        if (player == null) return;

        List<EntityAvatar> team = player.getTeamManager().getActiveTeam();
        for (Avatar avatar : player.getAvatars()) {
            team.stream().filter(e -> e.getAvatar().getAvatarId() == avatar.getAvatarId()).findFirst()
                    .ifPresent(e -> {
                        e.setFightProperty(FIGHT_PROP_SKILL_CD_MINUS_RATIO, 1);
                        e.getWorld().broadcastPacket(new PacketEntityFightPropUpdateNotify(e, FIGHT_PROP_SKILL_CD_MINUS_RATIO));
                    });
            avatar.getFightPropOverrides().put(80, 1);
            avatar.recalcStats();
            avatar.save();
        }

        ctx.json(ApiResult.success(ctx));
    }
}
