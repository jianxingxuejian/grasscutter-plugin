package org.hff.permission;

import emu.grasscutter.Grasscutter;
import emu.grasscutter.auth.*;
import emu.grasscutter.database.DatabaseHelper;
import emu.grasscutter.game.Account;
import emu.grasscutter.server.http.objects.ComboTokenResJson;
import emu.grasscutter.server.http.objects.LoginResultJson;

import static emu.grasscutter.config.Configuration.ACCOUNT;
import static emu.grasscutter.utils.lang.Language.translate;

public class Authentication implements AuthenticationSystem {
    private final Authenticator<LoginResultJson> passwordAuthenticator;
    private final Authenticator<LoginResultJson> tokenAuthenticator = new DefaultAuthenticators.TokenAuthenticator();
    private final Authenticator<ComboTokenResJson> sessionKeyAuthenticator = new DefaultAuthenticators.SessionKeyAuthenticator();
    private final Authenticator<Account> sessionTokenValidator = new DefaultAuthenticators.SessionTokenValidator();

    private final ExternalAuthenticator externalAuthenticator = new DefaultAuthenticators.ExternalAuthentication();
    private final OAuthAuthenticator oAuthAuthenticator = new DefaultAuthenticators.OAuthAuthentication();
    private final HandbookAuthenticator handbookAuthenticator = new DefaultAuthenticators.HandbookAuthentication();

    public Authentication() {
        if (ACCOUNT.EXPERIMENTAL_RealPassword) {
            passwordAuthenticator = new DefaultAuthenticators.ExperimentalPasswordAuthenticator();
        } else {
            passwordAuthenticator = new DefaultAuthenticators.PasswordAuthenticator();
        }
    }

    @Override
    public void createAccount(String username, String password) {
        Account account = DatabaseHelper.createAccountWithPassword(username, password);
        if (account != null) {
            account.addPermission("*");
            account.save();
        }
    }

    @Override
    public void resetPassword(String username) {}

    @Override
    public Account verifyUser(String s) {
        Grasscutter.getLogger().info(translate("messages.dispatch.authentication.default_unable_to_verify"));
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
    public Authenticator<Account> getSessionTokenValidator() {
        return this.sessionTokenValidator;
    }

    @Override
    public ExternalAuthenticator getExternalAuthenticator() {
        return this.externalAuthenticator;
    }

    @Override
    public OAuthAuthenticator getOAuthAuthenticator() {
        return this.oAuthAuthenticator;
    }

    @Override
    public HandbookAuthenticator getHandbookAuthenticator() {
        return this.handbookAuthenticator;
    }
}
