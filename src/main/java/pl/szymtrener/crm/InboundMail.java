package pl.szymtrener.crm;

import jakarta.mail.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimePart;
import jakarta.mail.internet.MimeUtility;
import org.slf4j.Logger;
import pl.szymtrener.config.MailConfig;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Odebrany mail sprowadzony do tego, czego potrzebuje watek: kto, na co odpowiada, kiedy i co napisal.
 * Parsowanie jest tu, a nie w serwisie, bo da sie je sprawdzic na wiadomosciach zbudowanych
 * w pamieci, bez serwera IMAP.
 *
 * @param replyTo identyfikatory z In-Reply-To i References, najpierw te, na ktore mail odpowiada wprost
 * @param automatic autoresponder, lista mailingowa albo inna wiadomosc bez czlowieka po drugiej stronie
 */
record InboundMail(String messageId, List<String> replyTo, String from, boolean automatic,
                   Instant sentAt, String body, List<AttachmentFile> attachments) {

    private static final Logger log = LoggerFactory.getLogger(InboundMail.class);
    private static final Pattern MESSAGE_ID = Pattern.compile("<[^<>\\s]{1,990}>");
    /** Czesc tekstowa wieksza od tego nie jest czytana: to nie jest odpowiedz pisana przez czlowieka. */
    private static final int MAX_TEXT_PART_BYTES = 1_000_000;
    private static final int MAX_DEPTH = 10;
    private static final int MAX_REPLY_IDS = 50;

    static InboundMail parse(MimeMessage message, Instant now) throws MessagingException {
        Content content = new Content();
        collect(message, content, 0);

        String text = content.plain != null ? content.plain
                : content.html != null ? InboundMailText.htmlToText(content.html) : "";
        String body = InboundMailText.clean(text);
        if (!content.notes.isEmpty()) {
            body = (body.isBlank() ? "" : body + "\n\n") + String.join("\n", content.notes);
        }
        // Sam plik bez tekstu to normalna odpowiedz („w zalaczniku wyniki"), nie pusta wiadomosc.
        if (body.isBlank() && content.files.isEmpty()) body = "[wiadomość bez treści]";

        Instant sent = message.getSentDate() == null ? null : message.getSentDate().toInstant();
        if (sent == null || sent.isAfter(now)) sent = now;

        return new InboundMail(firstId(message.getMessageID()), replyTo(message), sender(message),
                automatic(message), sent, body, List.copyOf(content.files));
    }

    private static List<String> replyTo(MimeMessage message) throws MessagingException {
        // LinkedHashSet i limit w trakcie: nagłówki References sa sklejane, wiec napastnik moze
        // podac megabajty identyfikatorow, a contains() na liscie dawalo czas kwadratowy.
        Set<String> ids = new LinkedHashSet<>(ids(message.getHeader("In-Reply-To", " ")));
        List<String> references = ids(message.getHeader("References", " "));
        // References rosnie od najstarszej do najnowszej; najblizsza nasza wiadomosc jest na koncu.
        for (int i = references.size() - 1; i >= 0 && ids.size() < MAX_REPLY_IDS; i--) {
            ids.add(references.get(i));
        }
        return ids.stream().limit(MAX_REPLY_IDS).toList();
    }

    private static List<String> ids(String header) {
        if (header == null) return List.of();
        List<String> found = new ArrayList<>();
        Matcher m = MESSAGE_ID.matcher(header);
        while (m.find()) found.add(m.group());
        return found;
    }

    private static String firstId(String header) {
        List<String> found = ids(header);
        return found.isEmpty() ? null : found.get(0);
    }

    private static String sender(MimeMessage message) throws MessagingException {
        Address[] from = message.getFrom();
        if (from == null) return null;
        return Arrays.stream(from)
                .filter(InternetAddress.class::isInstance)
                .map(a -> ((InternetAddress) a).getAddress())
                .filter(a -> a != null && !a.isBlank())
                .map(a -> a.strip().toLowerCase(Locale.ROOT))
                .findFirst().orElse(null);
    }

    /** RFC 3834 (Auto-Submitted) plus naglowki, ktore autorespondery ustawiaja w praktyce. */
    private static boolean automatic(MimeMessage message) throws MessagingException {
        String autoSubmitted = message.getHeader("Auto-Submitted", null);
        if (autoSubmitted != null && !autoSubmitted.strip().equalsIgnoreCase("no")) return true;
        String precedence = message.getHeader("Precedence", null);
        if (precedence != null && precedence.strip().toLowerCase(Locale.ROOT).matches("bulk|junk|list|auto_reply")) return true;
        return message.getHeader("X-Autoreply") != null || message.getHeader("X-Autorespond") != null
                || message.getHeader("List-Id") != null;
    }

    private static void collect(Part part, Content content, int depth) throws MessagingException {
        if (depth > MAX_DEPTH) return;
        if (part.isMimeType("message/rfc822")) {
            content.notes.add("[przekazana wiadomość: jest w skrzynce]");
            return;
        }
        String disposition = part.getDisposition();
        boolean attachment = Part.ATTACHMENT.equalsIgnoreCase(disposition)
                || (part.getFileName() != null && !part.isMimeType("multipart/*"));
        if (attachment) {
            attachment(part, content);
            return;
        }
        if (part.isMimeType("multipart/*")) {
            Object value = read(part);
            if (value instanceof Multipart multipart) {
                for (int i = 0; i < multipart.getCount() && i < 100; i++) {
                    collect(multipart.getBodyPart(i), content, depth + 1);
                }
            }
        } else if (part.isMimeType("text/plain") && content.plain == null) {
            content.plain = text(part);
        } else if (part.isMimeType("text/html") && content.html == null) {
            content.html = text(part);
        }
    }

    private static String text(Part part) throws MessagingException {
        int size = part.getSize();
        if (size > MAX_TEXT_PART_BYTES) return null;
        Object value = read(part);
        if (value instanceof String s) return s;
        // Nieznane kodowanie znakow: JavaMail oddaje strumien zamiast tekstu. Czytamy jako UTF-8.
        try (var in = part.getInputStream()) {
            return new String(in.readNBytes(MAX_TEXT_PART_BYTES), StandardCharsets.UTF_8);
        } catch (IOException e) {
            rethrowConnectionError(e);
            log.debug("Nie udalo sie odczytac czesci tekstowej: {}", e.getMessage());
            return null;
        }
    }

    private static Object read(Part part) throws MessagingException {
        try {
            return part.getContent();
        } catch (IOException e) {
            rethrowConnectionError(e);
            return null;
        }
    }

    private static void attachment(Part part, Content content) throws MessagingException {
        if (AttachmentPolicy.rejectBeforeReading(fileName(part), content.files.size(), content.notes)) return;
        byte[] data;
        boolean truncated;
        try (var in = part.getInputStream()) {
            // Czytamy najwyzej limit + 1 bajt: wiekszy plik nie laduje sie do pamieci w calosci.
            data = in.readNBytes((int) AttachmentPolicy.IN_MAX_FILE + 1);
            truncated = data.length > AttachmentPolicy.IN_MAX_FILE;
        } catch (IOException e) {
            rethrowConnectionError(e);
            content.notes.add("[" + AttachmentPolicy.safeName(fileName(part)) + ": nie udało się odczytać, jest w skrzynce]");
            return;
        }
        // Obrazek wstawiony w tresc z Content-ID i maly to ozdoba stopki (logo), nie plik od klienta.
        boolean inlineWithContentId = !Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())
                && part instanceof MimePart mime && mime.getContentID() != null;
        if (AttachmentPolicy.decoration(inlineWithContentId, part.isMimeType("image/*"), data.length)) return;
        AttachmentFile file = AttachmentPolicy.incoming(fileName(part), truncated ? new byte[0] : data, truncated,
                content.files.size(), content.bytes, content.notes);
        if (file != null) {
            content.files.add(file);
            content.bytes += file.data().length;
        }
    }

    /**
     * Angus opakowuje zerwane polaczenie IMAP w IOException z FolderClosedException albo
     * MessagingException w srodku. To nie jest zepsuty plik, tylko powod do ponownego odbioru:
     * przekazujemy wyzej, zeby InboundMailService nie przesunal UID i nie zgubil zalacznika.
     */
    private static void rethrowConnectionError(IOException e) throws MessagingException {
        if (e.getCause() instanceof MessagingException connection) throw connection;
    }

    private static String fileName(Part part) {
        try {
            String name = part.getFileName();
            return name == null ? null : MimeUtility.decodeText(name);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            return null;
        }
    }

    private static final class Content {
        String plain;
        String html;
        final List<AttachmentFile> files = new ArrayList<>();
        final List<String> notes = new ArrayList<>();
        long bytes;
    }

    /** Dla testow i logow: sama tresc nigdy nie idzie do logu. */
    @Override
    public String toString() {
        return "InboundMail[" + messageId + ", od " + (from == null ? "?" : MailConfig.mask(from)) + "]";
    }
}
