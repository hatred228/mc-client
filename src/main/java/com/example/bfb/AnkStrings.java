package com.example.bfb;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class AnkStrings {

    // "CHANGE_ME_16CH!" ASCII
    private static final int[] P1 = {0x43, 0x48, 0x41, 0x4E};
    private static final int[] P2 = {0x47, 0x45, 0x5F, 0x4D};
    private static final int[] P3 = {0x45, 0x5F, 0x31, 0x36};
    private static final int[] P4 = {0x43, 0x48, 0x21, 0x00};

    private static byte[] KEY;

    private static byte[] key() {
        if (KEY != null) return KEY;
        byte[] k = new byte[16];
        for (int i = 0; i < 4; i++) {
            k[i]      = (byte) P1[i];
            k[i + 4]  = (byte) P2[i];
            k[i + 8]  = (byte) P3[i];
            k[i + 12] = (byte) P4[i];
        }
        KEY = k;
        return k;
    }

    public static String decrypt(String encoded) {
        try {
            byte[] data = Base64.getDecoder().decode(encoded);
            byte[] iv = new byte[12];
            byte[] ct = new byte[data.length - 12];
            System.arraycopy(data, 0, iv, 0, 12);
            System.arraycopy(data, 12, ct, 0, ct.length);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key(), "AES"),
                    new GCMParameterSpec(128, iv));
            return new String(c.doFinal(ct), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    private AnkStrings() {}
}