package org.hff.permission;

import at.favre.lib.crypto.bcrypt.BCrypt;
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
        String address = request.getContext().ip();
        response.retcode = -201;

        if (getGameServer().getPlayers().size() >= ACCOUNT.maxPlayer && ACCOUNT.maxPlayer > -1) {
            getLogger().info(translate("messages.dispatch.account.login_max_player_limit", address));
            response.message = translate("messages.dispatch.account.server_max_player_limit");
            return response;
        }

        String password = requestData.password;
        if (password == null) {
            getLogger().info(translate("messages.dispatch.account.login_password_error", address));
            response.message = translate("messages.dispatch.account.password_error");
            return response;
        }

        try {
            password = AuthUtil.decryptPassword(password);
        } catch (Exception ignored) {
        }

        Account account = DatabaseHelper.getAccountByName(requestData.account);
        if (account == null) {
            if (!ACCOUNT.autoCreate) {
                getLogger().info(translate("messages.dispatch.account.account_login_exist_error", address));
                response.message = translate("messages.dispatch.account.username_error");
                return response;
            }

            account = DatabaseHelper.createAccountWithUid(requestData.account, 0);
            if (account == null) {
                getLogger().info(translate("messages.dispatch.account.account_login_create_error", address));
                response.message = translate("messages.dispatch.account.username_create_error");
                return response;
            }

            account.setPassword(BCrypt.withDefaults().hashToString(12, password.toCharArray()));
            account.save();
            getLogger().info(translate("messages.dispatch.account.account_login_create_success", address, response.data.account.uid));
        } else {
            String accountPassword = account.getPassword();
            if (accountPassword != null && !accountPassword.isEmpty()) {
                if (!BCrypt.verifyer().verify(password.toCharArray(), accountPassword).verified && !accountPassword.equals(password)) {
                    getLogger().info(translate("messages.dispatch.account.login_password_error", address));
                    response.message = translate("messages.dispatch.account.password_error");
                    return response;
                }
            }
        }

        getLogger().info(translate("messages.dispatch.account.login_success", address, account.getId()));

        response.retcode = 0;
        response.message = "OK";
        response.data.account.uid = account.getId();
        response.data.account.token = account.generateSessionKey();
        response.data.account.email = account.getEmail();
        return response;
    }
}
