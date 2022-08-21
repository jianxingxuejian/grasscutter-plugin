package org.hff;

import emu.grasscutter.server.http.Router;
import express.Express;
import io.javalin.Javalin;

public final class PluginRouter implements Router {

    @Override
    public void applyRoutes(Express express, Javalin javalin) {
        express.get("/plugin/admin/auth",
                (request, response) -> new PluginHandler(request, response).adminAuth());

        express.get("/plugin/admin/createAccount",
                (request, response) -> new PluginHandler(request, response).adminCreateAccount());

        express.get("/plugin/admin/command",
                (request, response) -> new PluginHandler(request, response).adminCommand());

        express.get("/plugin/mail/verifyCode",
                (request, response) -> new PluginHandler(request, response).mailVerifyCode());

        express.get("/plugin/player/authByVerifyCode",
                (request, response) -> new PluginHandler(request, response).playerAuthByVerifyCode());

        express.get("/plugin/player/authByPassword",
                (request, response) -> new PluginHandler(request, response).playerAuthByPassword());

        express.get("/plugin/player/command",
                (request, response) -> new PluginHandler(request, response).playerCommand());

        express.get("/plugin/player/levelUpAllSkill",
                (request, response) -> new PluginHandler(request, response).levelUpAllSkill());

        express.get("/plugin/player/getProps",
                (request, response) -> new PluginHandler(request, response).getProps());

        express.get("/plugin/player/cdr",
                (request, response) -> new PluginHandler(request, response).cdr());
    }
}
