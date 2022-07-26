package org.hff.utils;

import org.hff.Config;
import org.hff.MyPlugin;

import java.util.UUID;

public class AuthUtil {

    private static final Config config = MyPlugin.getInstance().getConfig();

    public static String generateAdminVoucher() {
        return UUID.randomUUID().toString();
    }

    public static boolean checkAdminVoucher(String voucher) {
        return config.getAdminVoucher().equals(generateAdminVoucher());
    }
}
