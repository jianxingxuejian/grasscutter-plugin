package org.hff.utils;

import emu.grasscutter.game.mail.Mail;
import org.hff.i18n.LanguageManager;
import org.hff.i18n.Locale;

public class MailUtil {

    public static Mail generateVerifyCodeMail(Locale locale) {
        var mail = new Mail();
        mail.mailContent.sender = "grasscutter-plugin";
        mail.mailContent.title = LanguageManager.getMail(locale, "verifyCode.title");
        String randomNum = Util.getRandomNum(6);
        mail.mailContent.content = LanguageManager.getMail(locale, "verifyCode.content").formatted(randomNum);
        return mail;
    }
}
