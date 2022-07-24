package org.hff;

import emu.grasscutter.server.http.Router;
import express.Express;
import io.javalin.Javalin;

public class PluginRouter implements Router {

    @Override
    public void applyRoutes(Express express, Javalin javalin) {
        express.get("/plugin/admin/auth", CommandHandler::adminAuth);
        express.get("/plugin/admin/command", CommandHandler::adminCommand);
        express.get("/plugin/mail/verifyCode", CommandHandler::mailVerifyCode);
        express.get("/plugin/player/auth", CommandHandler::playerAuth);
        express.get("/plugin/player/command", CommandHandler::playerCommand);
    }
}
