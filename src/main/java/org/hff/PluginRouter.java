package org.hff;

import emu.grasscutter.server.http.Router;
import io.javalin.Javalin;

public final class PluginRouter implements Router {

    @Override
    public void applyRoutes(Javalin javalin) {
        javalin.post("/plugin/admin/auth", PluginHandler::adminAuth);
        javalin.post("/plugin/admin/createAccount", PluginHandler::adminCreateAccount);
        javalin.get("/plugin/admin/command", PluginHandler::adminCommand);
        javalin.post("/plugin/admin/seed", PluginHandler::seed);
        javalin.get("/plugin/mail/verifyCode", PluginHandler::mailVerifyCode);
        javalin.post("/plugin/player/authByVerifyCode", PluginHandler::playerAuthByVerifyCode);
        javalin.post("/plugin/player/authByPassword", PluginHandler::playerAuthByPassword);
        javalin.get("/plugin/player/command", PluginHandler::playerCommand);
        javalin.get("/plugin/player/levelUpAll", PluginHandler::levelUpAll);
        javalin.get("/plugin/player/getProps", PluginHandler::getProps);
        javalin.get("/plugin/player/cdr", PluginHandler::cdr);
    }
}
