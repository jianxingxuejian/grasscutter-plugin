package org.hff.utils;

import java.util.UUID;

public class AuthUtil {

    public static String generateAdminVoucher() {
        return UUID.randomUUID().toString();
    }

    public static boolean checkAdminVoucher(String voucher) {
        return voucher.equals(generateAdminVoucher());
    }
}
