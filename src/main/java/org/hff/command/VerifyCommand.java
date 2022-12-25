package org.hff.command;

import emu.grasscutter.command.Command;
import emu.grasscutter.command.CommandHandler;
import emu.grasscutter.game.Account;
import emu.grasscutter.game.player.Player;
import org.hff.api.ApiCode;
import org.hff.i18n.Locale;
import org.hff.utils.MailUtil;

import java.util.List;
import java.util.Map;

import static org.hff.i18n.Language.getLocale;
import static org.hff.i18n.LanguageManager.getMail;
import static org.hff.i18n.LanguageManager.getMsg;


@Command(label = "verify")
public class VerifyCommand implements CommandHandler {

    static final Map<String, MailUtil.VerifyCodeMail> verifyCodeMailMap = MailUtil.verifyCodeMailMap;

    @Override
    public void execute(Player player, Player targetPlayer, List<String> args) {
        Account account = player.getAccount();
        String accountId = account.getId();
        Locale locale = getLocale(account);
        MailUtil.VerifyCodeMail verifyCodeMail = verifyCodeMailMap.get(accountId);

        if (verifyCodeMail == null) {
            String msg = getMsg(locale, ApiCode.MAIL_VERIFY_NOT_FOUND);
            CommandHandler.sendMessage(player, msg);
            return;
        }

        if (verifyCodeMail.getExpireTime() < System.currentTimeMillis()) {
            verifyCodeMailMap.remove(accountId);
            String msg = getMsg(locale, ApiCode.MAIL_VERIFY_EXPIRED);
            CommandHandler.sendMessage(player, msg);
            return;
        }

        String msg = getMail(locale, "verifyCode.content").formatted(verifyCodeMail.getVerifyCode());
        CommandHandler.sendMessage(player, msg);
    }

}
