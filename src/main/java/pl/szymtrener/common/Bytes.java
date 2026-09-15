package pl.szymtrener.common;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

/** Rozmiar pliku do wyswietlenia i skrot SHA-256 — wspolne dla biblioteki mediow i zalacznikow w watku. */
public final class Bytes {

    private static final Locale PL = Locale.forLanguageTag("pl-PL");

    private Bytes() {}

    /** „512 B", „120 KB", „1,2 MB". */
    public static String human(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return Math.round(bytes / 1024.0) + " KB";
        return String.format(PL, "%.1f MB", bytes / 1048576.0);
    }

    public static String sha256(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 jest wymagane przez kazda JVM", e);
        }
    }
}
