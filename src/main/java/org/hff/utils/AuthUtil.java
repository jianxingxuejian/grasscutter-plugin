package org.hff.utils;

import emu.grasscutter.utils.FileUtils;
import emu.grasscutter.utils.Utils;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.UUID;

public class AuthUtil {

    public static String generateAdminVoucher() {
        return UUID.randomUUID().toString();
    }

    public static String decryptPassword(String password) throws Exception {
        byte[] key = FileUtils.readResource("/keys/auth_private-key.der");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(key);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        RSAPrivateKey private_key = (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, private_key);
        return new String(cipher.doFinal(Utils.base64Decode(password)), StandardCharsets.UTF_8);
    }
}
