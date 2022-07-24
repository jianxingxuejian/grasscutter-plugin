package org.hff.utils;

import java.util.concurrent.ThreadLocalRandom;

public class Util {

    public static final String BASE_NUMBER = "0123456789";

    public static String getRandomNum(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int num = ThreadLocalRandom.current().nextInt(BASE_NUMBER.length());
            sb.append(BASE_NUMBER.charAt(num));
        }
        return sb.toString();
    }
}
