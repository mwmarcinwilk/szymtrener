package pl.szymtrener.crm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Typ po treści, limity i nazwy plików w wątku. */
class AttachmentPolicyTest {

    static final byte[] PDF = "%PDF-1.7\n...".getBytes(StandardCharsets.ISO_8859_1);
    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};
    static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0};
    static final byte[] EXE = {'M', 'Z', (byte) 0x90, 0, 3, 0};
    static final byte[] ZIP = {'P', 'K', 3, 4, 20, 0};

    private static MultipartFile upload(String name, byte[] data) {
        return new MockMultipartFile("pliki", name, "application/pdf", data);
    }

    @Test
    @DisplayName("typ rozpoznany po treści, nie po nazwie")
    void detectsByContent() {
        assertThat(AttachmentPolicy.detect(PDF, "skan.jpg")).isEqualTo("application/pdf");
        assertThat(AttachmentPolicy.detect(PNG, "zdjecie.pdf")).isEqualTo("image/png");
        assertThat(AttachmentPolicy.detect(ZIP, "plan.docx")).isEqualTo(AttachmentPolicy.DOCX);
        assertThat(AttachmentPolicy.detect(ZIP, "plan.zip")).isEqualTo("application/zip");
        assertThat(AttachmentPolicy.detect("<html><script>".getBytes(), "wyniki.pdf")).isNull();
        assertThat(AttachmentPolicy.detect("a;b\n1;2".getBytes(), "pomiary.csv")).isEqualTo("text/csv");
    }

    @Test
    @DisplayName("wysyłka: dozwolone pliki przechodzą z typem z treści")
    void outgoingAccepts() {
        AttachmentPolicy.Outgoing result = AttachmentPolicy.outgoing(List.of(upload("plan.pdf", PDF), upload("fotka.png", PNG)));

        assertThat(result.ok()).isTrue();
        assertThat(result.files()).extracting(AttachmentFile::mimeType).containsExactly("application/pdf", "image/png");
    }

    @Test
    @DisplayName("wysyłka: exe przemianowany na pdf, HTML i za dużo plików są odrzucane z komunikatem")
    void outgoingRejects() {
        assertThat(AttachmentPolicy.outgoing(List.of(upload("plan.pdf", EXE))).error()).contains("niedozwolony typ");
        assertThat(AttachmentPolicy.outgoing(List.of(upload("strona.pdf", "<html>".getBytes()))).error()).contains("niedozwolony typ");
        assertThat(AttachmentPolicy.outgoing(Collections.nCopies(6, upload("a.pdf", PDF))).error()).contains("najwyżej 5");
        byte[] big = new byte[(int) AttachmentPolicy.OUT_MAX_FILE + 1];
        System.arraycopy(PDF, 0, big, 0, PDF.length);
        assertThat(AttachmentPolicy.outgoing(List.of(upload("duzy.pdf", big))).error()).contains("więcej niż 10 MB");
        assertThat(AttachmentPolicy.outgoing(null).ok()).isTrue();
    }

    @Test
    @DisplayName("odbiór: wykonywalne i aktywne pliki zostają w skrzynce, nieznane typy przechodzą jako octet-stream")
    void incoming() {
        List<String> notes = new ArrayList<>();

        assertThat(AttachmentPolicy.incoming("faktura.pdf", EXE, false, 0, 0, notes)).isNull();
        assertThat(AttachmentPolicy.incoming("wykres.svg", "<svg onload=alert(1)>".getBytes(), false, 0, 0, notes)).isNull();
        assertThat(AttachmentPolicy.incoming("x.txt", "\uFEFF  <!DOCTYPE html>".getBytes(StandardCharsets.UTF_8), false, 0, 0, notes)).isNull();
        assertThat(AttachmentPolicy.incoming("duzy.zip", new byte[0], true, 0, 0, notes)).isNull();
        assertThat(AttachmentPolicy.incoming("dane.bin", new byte[]{1, 2, 3}, false, 0, 0, notes).mimeType())
                .isEqualTo("application/octet-stream");
        assertThat(AttachmentPolicy.incoming("wyniki.pdf", PDF, false, AttachmentPolicy.IN_MAX_FILES, 0, notes)).isNull();
        assertThat(AttachmentPolicy.incoming("wyniki.js.", "alert(1)".getBytes(), false, 0, 0, notes)).isNull();
        assertThat(AttachmentPolicy.incoming("zdjecie\u202Egpj.hta. ", "<script>".getBytes(), false, 0, 0, notes)).isNull();
        assertThat(AttachmentPolicy.rejectBeforeReading("skrot.url", 0, notes)).isTrue();
        assertThat(AttachmentPolicy.rejectBeforeReading("evil.js\u00A0", 0, notes)).isTrue();
        assertThat(notes).hasSize(9).allMatch(n -> n.contains("jest w skrzynce"));
    }

    @Test
    @DisplayName("nazwa bez ścieżki, znaków sterujących i nagłówkowych, z polskimi literami i limitem długości")
    void safeName() {
        assertThat(AttachmentPolicy.safeName("../../etc/passwd")).isEqualTo("passwd");
        assertThat(AttachmentPolicy.safeName("C:\\Users\\jan\\wyniki badań.pdf")).isEqualTo("wyniki badań.pdf");
        // CRLF wycięte (brak wstrzyknięcia nagłówka), a „/" traktowany jak ścieżka.
        assertThat(AttachmentPolicy.safeName("a\r\nX-Header: 1.pdf")).isEqualTo("aX-Header_ 1.pdf");
        assertThat(AttachmentPolicy.safeName("...ukryty")).isEqualTo("ukryty");
        // Końcowe kropki i spacje: Windows je obcina, więc „evil.js." zapisałby się jako „evil.js".
        assertThat(AttachmentPolicy.safeName("evil.js.")).isEqualTo("evil.js");
        assertThat(AttachmentPolicy.safeName("evil.bat .")).isEqualTo("evil.bat");
        // U+202E odwraca kierunek tekstu: „wyniki\u202Efdp.js" wyglądałoby jak „wyniki.sj.pdf".
        assertThat(AttachmentPolicy.safeName("wyniki\u202Efdp.js")).isEqualTo("wynikifdp.js");
        assertThat(AttachmentPolicy.safeName("a\u200Bb\u2066.pdf")).isEqualTo("ab.pdf");
        // Spacje Unicode na końcu (NBSP, U+2007, U+202F, U+3000 przed kropką) też nie ukryją rozszerzenia.
        assertThat(AttachmentPolicy.safeName("evil.js\u00A0")).isEqualTo("evil.js");
        assertThat(AttachmentPolicy.safeName("evil.js\u202F")).isEqualTo("evil.js");
        assertThat(AttachmentPolicy.safeName("evil.js\u3000.")).isEqualTo("evil.js");
        assertThat(AttachmentPolicy.safeName("evil\u3164.js")).isEqualTo("evil.js");
        assertThat(AttachmentPolicy.safeName(" .\u00A0. ")).isEqualTo("plik");
        assertThat(AttachmentPolicy.safeName(null)).isEqualTo("plik");
        String longName = "a".repeat(300) + ".pdf";
        assertThat(AttachmentPolicy.safeName(longName)).hasSize(150).endsWith(".pdf");
        // Bardzo długie „rozszerzenie" nie wywraca skracania (wcześniej ujemny indeks i pominięta wiadomość).
        assertThat(AttachmentPolicy.safeName("a.b" + "c".repeat(200))).hasSize(150).doesNotStartWith(".");
        assertThat(AttachmentPolicy.safeName("x." + "y".repeat(149))).hasSizeLessThanOrEqualTo(150).doesNotStartWith(".");
        assertThat(AttachmentPolicy.safeName("." + "z".repeat(300))).hasSize(150);
    }
}
