package pl.szymtrener.common;

import java.text.Normalizer;
import java.util.Locale;

public final class SlugUtil {
    private SlugUtil() {}

    /** „Dlaczego mięśnie to polisa na życie" -> „dlaczego-miesnie-to-polisa-na-zycie" */
    public static String slugify(String input) {
        if (input == null || input.isBlank()) return "";
        String s = input.toLowerCase(Locale.of("pl", "PL"))
                .replace("ł", "l").replace("Ł", "l");
        s = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        s = s.replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return s.length() > 90 ? s.substring(0, 90).replaceAll("-$", "") : s;
    }
}
