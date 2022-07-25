package org.hff.utils;

import emu.grasscutter.game.mail.Mail;
import emu.grasscutter.game.player.Player;
import express.http.Response;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hff.api.ApiCode;
import org.hff.api.ApiResult;
import org.hff.i18n.Language;
import org.hff.i18n.LanguageManager;
import org.hff.i18n.Locale;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class MailUtil {

    private static final Map<String, VerifyCodeMail> verifyCodeMailMap = new ConcurrentHashMap<>();

    public static boolean sendVerifyCodeMail(Player player, String username, Locale locale, Response response) {
        var mail = new Mail();
        mail.mailContent.sender = "grasscutter-plugin";
        mail.mailContent.title = LanguageManager.getMail(locale, "verifyCode.title");
        String verifyCode = Util.getRandomNum(6);
        mail.mailContent.content = LanguageManager.getMail(locale, "verifyCode.content").formatted(verifyCode);

        try {
            player.sendMail(mail);
        } catch (Exception e) {
            response.json(ApiResult.result(ApiCode.MAIL_SEND_FAIL, locale));
            return false;
        }

        VerifyCodeMail verifyCodeMail = new VerifyCodeMail(verifyCode, System.currentTimeMillis() + 1000 * 60);
        verifyCodeMailMap.put(username, verifyCodeMail);
        return true;
    }

    public static boolean checkVerifyCode(String username, String verifyCode, Locale locale, Response response) {
        VerifyCodeMail verifyCodeMail = verifyCodeMailMap.get(username);

        if (verifyCodeMail == null) {
            response.json(ApiResult.result(ApiCode.MAIL_NOT_FOUND, locale));
            return false;
        }

        if (verifyCodeMail.getExpireTime() < System.currentTimeMillis()) {
            response.json(ApiResult.result(ApiCode.MAIL_VERIFY_EXPIRED, locale));
            return false;
        }

        int retries = verifyCodeMail.getRetries();
        if (!verifyCodeMail.getVerifyCode().equals(verifyCode)) {
            if (retries >= 3) {
                verifyCodeMailMap.remove(username);
            }
            verifyCodeMail.setRetries(retries++);
            response.json(ApiResult.result(ApiCode.MAIL_VERIFY_FAIL, locale).setArgs(3 - retries));
            return false;
        }

        verifyCodeMailMap.remove(username);
        return true;
    }

    @Getter
    @Setter
    static class VerifyCodeMail {
        private String verifyCode;
        private long expireTime;
        private int Retries = 0;

        public VerifyCodeMail(String verifyCode, long expireTime) {
            this.verifyCode = verifyCode;
            this.expireTime = expireTime;
        }
    }
}
