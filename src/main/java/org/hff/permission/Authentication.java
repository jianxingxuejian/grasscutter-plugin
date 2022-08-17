package org.hff.permission;

import emu.grasscutter.auth.*;
import emu.grasscutter.database.DatabaseHelper;
import emu.grasscutter.game.Account;
import emu.grasscutter.server.http.objects.ComboTokenResJson;
import emu.grasscutter.server.http.objects.LoginResultJson;

import static emu.grasscutter.Grasscutter.getLogger;

public class Authentication implements AuthenticationSystem {

    private final Authenticator<LoginResultJson> passwordAuthenticator = new PasswordAuthenticator();
    private final Authenticator<LoginResultJson> tokenAuthenticator = new DefaultAuthenticators.TokenAuthenticator();
    private final Authenticator<ComboTokenResJson> sessionKeyAuthenticator = new DefaultAuthenticators.SessionKeyAuthenticator();
    private final ExternalAuthenticator externalAuthenticator = new DefaultAuthenticators.ExternalAuthentication();

    @Override
    public void createAccount(String username, String password) {
        Account account = DatabaseHelper.createAccountWithPassword(username, password);
        if (account != null) {
            account.addPermission("*");
            account.save();
        }
    }

    @Override
    public void resetPassword(String username) {

    }

    @Override
    public Account verifyUser(String s) {
        getLogger().error("verifyUser: " + s);
        return null;
    }

    @Override
    public Authenticator<LoginResultJson> getPasswordAuthenticator() {
        return this.passwordAuthenticator;
    }

    @Override
    public Authenticator<LoginResultJson> getTokenAuthenticator() {
        return this.tokenAuthenticator;
    }

    @Override
    public Authenticator<ComboTokenResJson> getSessionKeyAuthenticator() {
        return this.sessionKeyAuthenticator;
    }

    @Override
    public emu.grasscutter.auth.ExternalAuthenticator getExternalAuthenticator() {
        return this.externalAuthenticator;
    }

    @Override
    public OAuthAuthenticator getOAuthAuthenticator() {
        return null;
    }
}
