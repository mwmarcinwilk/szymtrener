package pl.szymtrener.crm;

import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.UIDFolder;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

/**
 * INBOX przez IMAPS, otwarty READ_ONLY (komenda EXAMINE) i z PEEK: odbior nie zmienia flag,
 * wiec wiadomosci w Mailu na Macu wygladaja dokladnie tak jak przed odczytem przez aplikacje.
 */
public final class ImapMailbox implements Mailbox {

    private static final Logger log = LoggerFactory.getLogger(ImapMailbox.class);

    private final Store store;
    private final Folder folder;
    private final UIDFolder uids;

    private ImapMailbox(Store store, Folder folder) {
        this.store = store;
        this.folder = folder;
        this.uids = (UIDFolder) folder;
    }

    public static ImapMailbox open(String host, int port, String user, String password) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", host);
        props.put("mail.imaps.port", String.valueOf(port));
        props.put("mail.imaps.ssl.checkserveridentity", "true");
        props.put("mail.imaps.peek", "true");
        // Domyslnie JavaMail czeka w nieskonczonosc; zawieszony serwer zablokowalby watek harmonogramu.
        props.put("mail.imaps.connectiontimeout", "5000");
        props.put("mail.imaps.timeout", "20000");
        props.put("mail.imaps.writetimeout", "20000");

        Store store = Session.getInstance(props).getStore("imaps");
        store.connect(host, port, user, password);
        try {
            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);
            return new ImapMailbox(store, inbox);
        } catch (MessagingException | RuntimeException e) {
            closeQuietly(store);
            throw e;
        }
    }

    @Override
    public long uidValidity() throws MessagingException {
        return uids.getUIDValidity();
    }

    @Override
    public long highestUid() throws MessagingException {
        int count = folder.getMessageCount();
        return count == 0 ? 0 : uids.getUID(folder.getMessage(count));
    }

    @Override
    public List<Fetched> fetchAfter(long uid, int limit) throws MessagingException {
        // Zakres „N:*" w IMAP zwraca ostatnia wiadomosc nawet wtedy, gdy N przekracza najwyzszy UID,
        // wiec filtr ponizej nie jest nadmiarowy: bez niego ostatni mail wracalby przy kazdym odbiorze.
        List<Fetched> result = new ArrayList<>();
        for (Message message : uids.getMessagesByUID(uid + 1, UIDFolder.MAXUID)) {
            if (!(message instanceof MimeMessage mime)) continue;
            long messageUid = uids.getUID(message);
            if (messageUid > uid) result.add(new Fetched(messageUid, mime));
        }
        result.sort(Comparator.comparingLong(Fetched::uid));
        return result.size() > limit ? result.subList(0, limit) : result;
    }

    @Override
    public void close() {
        try {
            if (folder.isOpen()) folder.close(false);
        } catch (MessagingException e) {
            log.debug("Zamkniecie INBOX: {}", e.getMessage());
        }
        closeQuietly(store);
    }

    private static void closeQuietly(Store store) {
        try {
            store.close();
        } catch (MessagingException e) {
            log.debug("Zamkniecie polaczenia IMAP: {}", e.getMessage());
        }
    }
}
