package eu.netward.util;

import java.security.SecureRandom;
import java.time.Instant;

public class RequestIdGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    public static String generate(String suffix) {
        // 8 bytes timestamp + 8 bytes randomness = 16 bytes (128-bit)
        long time = Instant.now().toEpochMilli();
        byte[] random = new byte[8];
        RANDOM.nextBytes(random);

        StringBuilder sb = new StringBuilder(40);

        // encode timestamp as 8 bytes of hex (most significant first)
        for (int i = 7; i >= 0; i--) {
            int b = (int) ((time >> (i * 8)) & 0xFF);
            sb.append(HEX[(b >>> 4) & 0xF]).append(HEX[b & 0xF]);
        }

        // encode random bytes
        for (byte b : random) {
            int v = b & 0xFF;
            sb.append(HEX[(v >>> 4) & 0xF]).append(HEX[v & 0xF]);
        }

        // add suffix like "-LHR"
        if (suffix != null && !suffix.isEmpty()) {
            sb.append('-').append(suffix.toUpperCase());
        }

        return sb.toString();
    }
}
