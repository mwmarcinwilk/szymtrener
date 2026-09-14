package pl.szymtrener.crm;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/** Skrzynka w pamieci dla testow odbioru: UID z naglowka X-Test-Uid, Message-ID stale „<m{uid}@example.test>". */
final class FakeMailbox implements Mailbox {

    private final long validity;
    private final List<Fetched> all = new ArrayList<>();

    FakeMailbox(long validity, MimeMessage... messages) throws MessagingException {
        this.validity = validity;
        for (MimeMessage m : messages) all.add(new Fetched(Long.parseLong(m.getHeader("X-Test-Uid", null)), m));
    }

    static MimeMessage mail(long uid, String from) throws MessagingException {
        MimeMessage m = new MimeMessage(Session.getInstance(new Properties())) {
            @Override
            protected void updateMessageID() throws MessagingException {
                setHeader("Message-ID", "<m" + uid + "@example.test>");
            }
        };
        m.setFrom(new InternetAddress(from));
        m.setHeader("X-Test-Uid", String.valueOf(uid));
        m.setText("Treść " + uid, "UTF-8");
        m.saveChanges();
        return m;
    }

    @Override public long uidValidity() { return validity; }
    @Override public long highestUid() { return all.stream().mapToLong(Fetched::uid).max().orElse(0); }
    @Override public List<Fetched> fetchAfter(long uid, int limit) {
        return all.stream().filter(f -> f.uid() > uid).limit(limit).toList();
    }
    @Override public void close() {}
}
