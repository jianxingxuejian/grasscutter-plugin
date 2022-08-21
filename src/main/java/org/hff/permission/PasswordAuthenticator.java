package org.hff.permission;

import emu.grasscutter.auth.AuthenticationSystem;
import emu.grasscutter.auth.Authenticator;
import emu.grasscutter.database.DatabaseHelper;
import emu.grasscutter.game.Account;
import emu.grasscutter.server.http.objects.LoginAccountRequestJson;
import emu.grasscutter.server.http.objects.LoginResultJson;
import org.hff.utils.AuthUtil;

import static emu.grasscutter.Grasscutter.getGameServer;
import static emu.grasscutter.Grasscutter.getLogger;
import static emu.grasscutter.config.Configuration.ACCOUNT;
import static emu.grasscutter.utils.Language.translate;

public class PasswordAuthenticator implements Authenticator<LoginResultJson> {

    @Override
    public LoginResultJson authenticate(AuthenticationSystem.AuthenticationRequest request) {
        LoginAccountRequestJson requestData = request.getPasswordRequest();
        assert requestData != null;

        var response = new LoginResultJson();
        String address = request.getRequest().ip();
        response.retcode = -201;

        if (getGameServer().getPlayers().size() >= ACCOUNT.maxPlayer && ACCOUNT.maxPlayer > -1) {
            getLogger().info(translate("messages.dispatch.account.login_max_player_limit", address));
            response.message = translate("messages.dispatch.account.server_max_player_limit");
            return response;
        }

        Account account = DatabaseHelper.getAccountByName(requestData.account);
        if (account == null && !ACCOUNT.autoCreate) {
            getLogger().info(translate("messages.dispatch.account.account_login_exist_error", address));
            response.message = translate("messages.dispatch.account.username_error");
            return response;
        }

        if (account == null) {
            account = DatabaseHelper.createAccountWithUid(requestData.account, 0);
            if (account == null) {
                getLogger().info(translate("messages.dispatch.account.account_login_create_error", address));
                response.message = translate("messages.dispatch.account.username_create_error");
                return response;
            } else {
                getLogger().info(translate("messages.dispatch.account.account_login_create_success", address, response.data.account.uid));
            }
        }

        if (account.getPassword() != null) {
            try {
                String password = AuthUtil.decryptPassword(requestData.password);
                if (!password.equals(account.getPassword())) {
                    response.message = translate("messages.dispatch.account.password_error");
                    return response;
                }
            } catch (Exception e) {
                getLogger().error("Error with decrypt password: " + e.getMessage());
            }
        }

        getLogger().info(translate("messages.dispatch.account.login_success", address, account.getId()));

        response.message = "OK";
        response.data.account.uid = account.getId();
        response.data.account.token = account.generateSessionKey();
        response.data.account.email = account.getEmail();
        return response;
    }
}
