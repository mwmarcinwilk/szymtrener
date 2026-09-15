package pl.szymtrener.crm;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Co wolno przyjac jako zalacznik w watku i pod jakim typem go podawac.
 *
 * Typ rozpoznajemy po TRESCI (magiczne bajty), nigdy po nazwie ani naglowku z przegladarki
 * czy maila: plik „wyniki.pdf" z kodem strony HTML podany z typem z naglowka otworzylby sie
 * w przegladarce trenera jako strona. Wysylka z panelu ma biala liste typow; od klientow
 * przyjmujemy wszystko poza plikami wykonywalnymi i aktywnymi (HTML, SVG, skrypty).
 */
public final class AttachmentPolicy {

    public static final int OUT_MAX_FILES = 5;
    public static final long OUT_MAX_FILE = 10L * 1024 * 1024;
    public static final long OUT_MAX_TOTAL = 20L * 1024 * 1024;
    public static final int IN_MAX_FILES = 10;
    public static final long IN_MAX_FILE = 15L * 1024 * 1024;
    public static final long IN_MAX_TOTAL = 25L * 1024 * 1024;
    /** Obrazek inline z Content-ID mniejszy od tego to ozdoba stopki (logo), nie zdjecie od klienta. */
    public static final int INLINE_DECORATION_BYTES = 30_000;

    /** Tylko te typy wolno pokazac jako miniature (inline). */
    static final Set<String> PREVIEWABLE = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    static final String PDF = "application/pdf";
    static final String DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    static final String DOC = "application/msword";
    static final String XLS = "application/vnd.ms-excel";
    static final String OCTET = "application/octet-stream";

    private static final Set<String> OUTGOING = Set.of(PDF, "image/jpeg", "image/png", "image/webp", "image/heic",
            DOCX, XLSX, DOC, XLS, "text/plain", "text/csv");
    private static final Set<String> BLOCKED_EXTENSIONS = Set.of("exe", "com", "bat", "cmd", "ps1", "js", "mjs", "vbs",
            "vbe", "wsf", "wsh", "jse", "scr", "pif", "cpl", "msc", "msi", "msp", "jar", "app", "dmg", "pkg", "sh",
            "command", "html", "htm", "xhtml", "xht", "shtml", "mht", "mhtml", "svg", "svgz", "hta", "chm", "lnk",
            "url", "library-ms", "appref-ms", "settingcontent-ms", "reg", "iso", "apk");
    private static final Set<String> HEIC_BRANDS = Set.of("heic", "heix", "heim", "heis", "hevc", "hevx", "mif1", "msf1");
    private static final int NAME_LIMIT = 150;
    private static final int MAX_EXTENSION = 20;
    private static final java.util.regex.Pattern INVISIBLE =
            java.util.regex.Pattern.compile("[\\p{Cc}\\p{Cf}\\p{Zl}\\p{Zp}\\u115F\\u1160\\u3164\\uFFA0]");
    private static final java.util.regex.Pattern UNICODE_SPACE = java.util.regex.Pattern.compile("\\p{Zs}");
    private static final java.util.regex.Pattern EDGE_DOTS_AND_SPACES = java.util.regex.Pattern.compile("^[.\\s]+|[.\\s]+$");

    private AttachmentPolicy() {}

    /** Wynik walidacji plikow z formularza: albo pliki, albo komunikat dla trenera. */
    public record Outgoing(List<AttachmentFile> files, String error) {
        public boolean ok() { return error == null; }
    }

    /** Pliki do odpowiedzi z panelu; rozmowa telefoniczna nie wysyla plikow. */
    public static Outgoing forReply(String way, List<MultipartFile> uploads) {
        Outgoing files = outgoing(uploads);
        if (files.ok() && "tel".equals(way) && !files.files().isEmpty()) {
            return new Outgoing(List.of(), "Pliki można wysłać tylko e-mailem.");
        }
        return files;
    }

    /** Pliki wybrane w panelu. Rozmiary sprawdzamy przed czytaniem bajtow. */
    public static Outgoing outgoing(List<MultipartFile> uploads) {
        List<MultipartFile> chosen = uploads == null ? List.of()
                : uploads.stream().filter(f -> f != null && !f.isEmpty()).toList();
        if (chosen.size() > OUT_MAX_FILES) {
            return new Outgoing(List.of(), "Możesz dołączyć najwyżej " + OUT_MAX_FILES + " plików.");
        }
        long total = chosen.stream().mapToLong(MultipartFile::getSize).sum();
        if (total > OUT_MAX_TOTAL) {
            return new Outgoing(List.of(), "Pliki razem mają więcej niż " + (OUT_MAX_TOTAL >> 20) + " MB" + ". Wyślij je w dwóch wiadomościach.");
        }
        List<AttachmentFile> files = new ArrayList<>();
        for (MultipartFile upload : chosen) {
            String name = safeName(upload.getOriginalFilename());
            if (upload.getSize() > OUT_MAX_FILE) {
                return new Outgoing(List.of(), "Plik „" + name + "” ma więcej niż " + (OUT_MAX_FILE >> 20) + " MB" + ".");
            }
            byte[] data;
            try {
                data = upload.getBytes();
            } catch (IOException e) {
                return new Outgoing(List.of(), "Nie udało się odczytać pliku „" + name + "”.");
            }
            String type = detect(data, name);
            if (type == null || !OUTGOING.contains(type)) {
                return new Outgoing(List.of(), "Plik „" + name + "” ma niedozwolony typ. Dozwolone: PDF, zdjęcia (JPG, PNG, WEBP, HEIC), Word, Excel, TXT, CSV.");
            }
            files.add(new AttachmentFile(name, type, data));
        }
        return new Outgoing(files, null);
    }

    /**
     * Czy czesc maila odrzucic, zanim przeczytamy jej bajty: kolejny plik ponad limit albo
     * zablokowane rozszerzenie nie powinny kosztowac pobrania 15 MB z serwera.
     */
    static boolean rejectBeforeReading(String rawName, int acceptedSoFar, List<String> notes) {
        String name = safeName(rawName);
        if (acceptedSoFar >= IN_MAX_FILES) {
            notes.add("[" + name + ": za dużo załączników w jednej wiadomości, jest w skrzynce]");
            return true;
        }
        if (BLOCKED_EXTENSIONS.contains(extension(name))) {
            notes.add("[" + name + ": ten typ pliku nie jest pokazywany w panelu, jest w skrzynce]");
            return true;
        }
        return false;
    }

    /** Obrazek wstawiony w tresc (Content-ID, nie „attachment") i maly to ozdoba stopki, np. logo. */
    static boolean decoration(boolean inlineWithContentId, boolean image, int size) {
        return inlineWithContentId && image && size < INLINE_DECORATION_BYTES;
    }

    /**
     * Zalacznik odebrany od klienta. Zwraca plik do zapisu albo null z dopiskiem w {@code notes},
     * dlaczego zostal tylko w skrzynce.
     *
     * @param truncated bajty ucieto na limicie, czyli plik jest wiekszy niz {@link #IN_MAX_FILE}
     */
    static AttachmentFile incoming(String rawName, byte[] data, boolean truncated, int acceptedSoFar,
                                   long bytesSoFar, List<String> notes) {
        String name = safeName(rawName);
        if (truncated) {
            notes.add("[" + name + ": większy niż 15 MB, jest w skrzynce]");
            return null;
        }
        if (acceptedSoFar >= IN_MAX_FILES || bytesSoFar + data.length > IN_MAX_TOTAL) {
            notes.add("[" + name + ": za dużo załączników w jednej wiadomości, jest w skrzynce]");
            return null;
        }
        if (dangerous(data, name)) {
            notes.add("[" + name + ": ten typ pliku nie jest pokazywany w panelu, jest w skrzynce]");
            return null;
        }
        String type = detect(data, name);
        return new AttachmentFile(name, type == null ? OCTET : type, data);
    }

    /** Typ po tresci pliku albo null, gdy nie rozpoznano. Nazwa rozstrzyga tylko warianty tego samego kontenera. */
    static String detect(byte[] d, String name) {
        String ext = extension(name);
        if (startsWith(d, "%PDF-")) return PDF;
        if (startsWith(d, 0xFF, 0xD8, 0xFF)) return "image/jpeg";
        if (startsWith(d, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)) return "image/png";
        if (startsWith(d, "GIF87a") || startsWith(d, "GIF89a")) return "image/gif";
        if (startsWith(d, "RIFF") && d.length >= 12 && ascii(d, 8, 4).equals("WEBP")) return "image/webp";
        if (d.length >= 12 && ascii(d, 4, 4).equals("ftyp") && HEIC_BRANDS.contains(ascii(d, 8, 4))) return "image/heic";
        if (startsWith(d, 'P', 'K', 3, 4)) {
            return switch (ext) {
                case "docx" -> DOCX;
                case "xlsx" -> XLSX;
                default -> "application/zip";
            };
        }
        if (startsWith(d, 0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1)) {
            return switch (ext) {
                case "doc" -> DOC;
                case "xls" -> XLS;
                default -> OCTET;
            };
        }
        if ((ext.equals("txt") || ext.equals("csv")) && plainText(d)) return ext.equals("csv") ? "text/csv" : "text/plain";
        return null;
    }

    /** Pliki wykonywalne i aktywne: po rozszerzeniu ORAZ po tresci (exe przemianowany na pdf tez tu wpada). */
    static boolean dangerous(byte[] d, String name) {
        if (BLOCKED_EXTENSIONS.contains(extension(name))) return true;
        if (startsWith(d, 'M', 'Z') || startsWith(d, 0x7F, 'E', 'L', 'F') || startsWith(d, '#', '!')
                || startsWith(d, 0xCA, 0xFE, 0xBA, 0xBE) || startsWith(d, 0xCF, 0xFA, 0xED, 0xFE)
                || startsWith(d, 0xFE, 0xED, 0xFA, 0xCE)) {
            return true;
        }
        // Czytamy jako ISO-8859-1, wiec BOM UTF-8 widac jako trzy znaki „ï»¿".
        String head = new String(d, 0, Math.min(d.length, 1024), StandardCharsets.ISO_8859_1)
                .replace("ï»¿", "").stripLeading().toLowerCase(Locale.ROOT);
        return head.startsWith("<!doctype html") || head.startsWith("<html") || head.startsWith("<svg")
                || head.startsWith("<script") || (head.startsWith("<?xml") && head.contains("<svg"));
    }

    /**
     * Nazwa bez sciezki, znakow niewidocznych i znakow zakazanych w systemach plikow; polskie litery zostaja.
     *
     * Kazdy krok zamyka konkretne obejscie blokady rozszerzen:
     * - znaki sterujace i formatujace (Cc, Cf, w tym U+202E odwracajacy tekst: „wyniki.sj.pdf" zamiast .js),
     *   separatory linii i niewidoczne wypelniacze Hangul znikaja;
     * - spacje Unicode (NBSP, U+3000…) zamieniamy na zwykla spacje, zeby dalo sie je obciac;
     * - kropki i spacje z obu stron: Windows i Firefox obcinaja koncowe przy zapisie, wiec „evil.js." albo
     *   „evil.js" z NBSP na koncu zapisalyby sie jako „evil.js";
     * - skracanie nigdy nie tnie z ujemnym indeksem („rozszerzenie" dluzsze niz sensowne to czesc nazwy).
     */
    static String safeName(String raw) {
        String name = raw == null ? "" : raw;
        name = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1);
        name = INVISIBLE.matcher(name).replaceAll("");
        name = UNICODE_SPACE.matcher(name).replaceAll(" ");
        name = name.replaceAll("[\"<>|:*?]", "_");
        name = trimDotsAndSpaces(name);
        if (name.length() > NAME_LIMIT) {
            String ext = extension(name);
            if (ext.isEmpty() || ext.length() > MAX_EXTENSION) {
                name = trimDotsAndSpaces(name.substring(0, NAME_LIMIT));
            } else {
                String base = trimDotsAndSpaces(name.substring(0, NAME_LIMIT - ext.length() - 1));
                name = base.isEmpty() ? ext : base + "." + ext;
            }
        }
        return name.isEmpty() ? "plik" : name;
    }

    private static String trimDotsAndSpaces(String name) {
        return EDGE_DOTS_AND_SPACES.matcher(name).replaceAll("");
    }

    private static String extension(String name) {
        if (name == null) return "";
        int dot = name.lastIndexOf('.');
        return dot < 0 || dot == name.length() - 1 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static boolean plainText(byte[] d) {
        for (int i = 0; i < Math.min(d.length, 8192); i++) {
            if (d[i] == 0) return false;
        }
        return true;
    }

    private static boolean startsWith(byte[] d, String prefix) {
        return startsWith(d, prefix.chars().toArray());
    }

    private static boolean startsWith(byte[] d, int... prefix) {
        if (d.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if ((d[i] & 0xFF) != prefix[i]) return false;
        }
        return true;
    }

    private static String ascii(byte[] d, int from, int length) {
        return new String(d, from, length, StandardCharsets.ISO_8859_1);
    }
}
