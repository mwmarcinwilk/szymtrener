package pl.szymtrener.crm;

import jakarta.mail.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
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
                   Instant sentAt, String body) {

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
        if (content.attachments > 0) {
            body = (body.isBlank() ? "" : body + "\n\n") + "[pominięto załączniki: " + content.attachments + "]";
        }
        if (body.isBlank()) body = "[wiadomość bez treści]";

        Instant sent = message.getSentDate() == null ? null : message.getSentDate().toInstant();
        if (sent == null || sent.isAfter(now)) sent = now;

        return new InboundMail(firstId(message.getMessageID()), replyTo(message), sender(message),
                automatic(message), sent, body);
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
        String disposition = part.getDisposition();
        boolean attachment = Part.ATTACHMENT.equalsIgnoreCase(disposition)
                || (part.getFileName() != null && !part.isMimeType("multipart/*"));
        if (attachment || part.isMimeType("message/rfc822")) {
            content.attachments++;
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
            log.debug("Nie udalo sie odczytac czesci tekstowej: {}", e.getMessage());
            return null;
        }
    }

    private static Object read(Part part) throws MessagingException {
        try {
            return part.getContent();
        } catch (IOException e) {
            return null;
        }
    }

    private static final class Content {
        String plain;
        String html;
        int attachments;
    }

    /** Dla testow i logow: sama tresc nigdy nie idzie do logu. */
    @Override
    public String toString() {
        return "InboundMail[" + messageId + ", od " + (from == null ? "?" : MailConfig.mask(from)) + "]";
    }
}
