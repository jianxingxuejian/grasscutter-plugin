package org.hff.utils;

import emu.grasscutter.game.mail.Mail;
import emu.grasscutter.game.player.Player;
import io.javalin.http.Context;
import lombok.Getter;
import lombok.Setter;
import net.jodah.expiringmap.ExpiringMap;
import org.hff.api.ApiCode;
import org.hff.api.ApiResult;
import org.hff.i18n.LanguageManager;
import org.hff.i18n.Locale;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static emu.grasscutter.Grasscutter.getLogger;

public class MailUtil {

    public static final Map<String, VerifyCodeMail> verifyCodeMailMap = ExpiringMap.builder().expiration(2, TimeUnit.MINUTES).build();

    public static boolean sendVerifyCodeMail(Player player, Context ctx) {
        Locale locale = LanguageManager.getLocale(ctx.req.getHeader("locale"));

        String accountId = player.getAccount().getId();
        VerifyCodeMail oldMail = verifyCodeMailMap.get(accountId);
        if (oldMail != null && oldMail.getExpireTime() > System.currentTimeMillis()) {
            ctx.json(ApiResult.result(ApiCode.MAIL_TIME_LIMIT, ctx));
            return false;
        }

        var mail = new Mail();
        mail.mailContent.sender = "grasscutter-plugin";
        mail.mailContent.title = LanguageManager.getMail(locale, "verifyCode.title");
        String verifyCode = Util.getRandomNum(6);
        getLogger().info(verifyCode);
        mail.mailContent.content = LanguageManager.getMail(locale, "verifyCode.content").formatted(verifyCode);

        player.sendMail(mail);

        VerifyCodeMail verifyCodeMail = new VerifyCodeMail(verifyCode, System.currentTimeMillis() + 1000 * 60);
        verifyCodeMailMap.put(accountId, verifyCodeMail);
        return true;
    }

    public static boolean checkVerifyCode(String accountId, String verifyCode, Context ctx) {
        VerifyCodeMail verifyCodeMail = verifyCodeMailMap.get(accountId);

        if (verifyCodeMail == null) {
            ctx.json(ApiResult.result(ApiCode.MAIL_VERIFY_NOT_FOUND, ctx));
            return false;
        }

        if (verifyCodeMail.getExpireTime() < System.currentTimeMillis()) {
            verifyCodeMailMap.remove(accountId);
            ctx.json(ApiResult.result(ApiCode.MAIL_VERIFY_EXPIRED, ctx));
            return false;
        }

        if (!verifyCodeMail.getVerifyCode().equals(verifyCode)) {
            verifyCodeMail.setRetries(verifyCodeMail.getRetries() - 1);
            if (verifyCodeMail.getRetries() <= 0) {
                verifyCodeMailMap.remove(accountId);
            }
            ctx.json(ApiResult.result(ApiCode.MAIL_VERIFY_FAIL, ctx).setArgs(verifyCodeMail.getRetries()));
            return false;
        }

        verifyCodeMailMap.remove(accountId);
        return true;
    }

    @Getter
    @Setter
    public static class VerifyCodeMail {
        private String verifyCode;
        private long expireTime;
        private int Retries = 3;

        public VerifyCodeMail(String verifyCode, long expireTime) {
            this.verifyCode = verifyCode;
            this.expireTime = expireTime;
        }
    }
}
