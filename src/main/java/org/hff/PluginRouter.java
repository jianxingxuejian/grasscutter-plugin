package org.hff;

import emu.grasscutter.server.http.Router;
import express.Express;
import io.javalin.Javalin;

public final class PluginRouter implements Router {

    @Override
    public void applyRoutes(Express express, Javalin javalin) {
        express.get("/plugin/admin/auth",
                (request, response) -> new PluginHandler(request, response).adminAuth());
        express.get("/plugin/admin/command",
                (request, response) -> new PluginHandler(request, response).adminCommand());
//        express.get("/plugin/admin/createAccount", CommandHandler::adminCreateAccount);
        express.get("/plugin/mail/verifyCode",
                (request, response) -> new PluginHandler(request, response).mailVerifyCode());
        express.get("/plugin/player/auth",
                (request, response) -> new PluginHandler(request, response).playerAuth());
        express.get("/plugin/player/command",
                (request, response) -> new PluginHandler(request, response).playerCommand());
    }
}
