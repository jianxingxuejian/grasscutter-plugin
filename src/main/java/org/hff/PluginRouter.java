package org.hff;

import emu.grasscutter.server.http.Router;
import io.javalin.Javalin;

public final class PluginRouter implements Router {

    @Override
    public void applyRoutes(Javalin javalin) {
        javalin.get("/plugin/admin/auth",
                ctx -> new PluginHandler(ctx).adminAuth());

        javalin.get("/plugin/admin/createAccount",
                ctx -> new PluginHandler(ctx).adminCreateAccount());

        javalin.get("/plugin/admin/command",
                ctx -> new PluginHandler(ctx).adminCommand());

        javalin.get("/plugin/mail/verifyCode",
                ctx -> new PluginHandler(ctx).mailVerifyCode());

        javalin.get("/plugin/player/authByVerifyCode",
                ctx -> new PluginHandler(ctx).playerAuthByVerifyCode());

        javalin.get("/plugin/player/authByPassword",
                ctx -> new PluginHandler(ctx).playerAuthByPassword());

        javalin.get("/plugin/player/command",
                ctx -> new PluginHandler(ctx).playerCommand());

        javalin.get("/plugin/player/levelUpAllSkill",
                ctx -> new PluginHandler(ctx).levelUpAllSkill());

        javalin.get("/plugin/player/getProps",
                ctx -> new PluginHandler(ctx).getProps());

        javalin.get("/plugin/player/cdr",
                ctx -> new PluginHandler(ctx).cdr());
    }
}
