package com.emall.util;

import java.nio.charset.StandardCharsets;
import java.security.*;

/** Salted SHA-256 hashing (hash = SHA-256(salt + password), hex). */
public final class PasswordUtil {
    private PasswordUtil() {}
    private static final SecureRandom RNG = new SecureRandom();

    public static String newSalt() {
        byte[] b = new byte[16]; RNG.nextBytes(b); return hex(b);
    }
    public static String hash(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return hex(md.digest((salt + password).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    public static boolean matches(String password, String salt, String expectedHash) {
        return MessageDigest.isEqual(hash(password, salt).getBytes(StandardCharsets.UTF_8),
                                     expectedHash.getBytes(StandardCharsets.UTF_8));
    }
    private static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }
}
