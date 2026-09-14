package pl.szymtrener.crm;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.util.List;

/**
 * Otwarta skrzynka odbiorcza, tylko do odczytu. Cienka fasada nad IMAP-em: serwis odbioru
 * rozmawia z tym interfejsem, wiec jego logike (UID, UIDVALIDITY, bledy pojedynczych
 * wiadomosci) da sie sprawdzic bez serwera pocztowego.
 */
public interface Mailbox extends AutoCloseable {

    /** Zmienia sie, gdy serwer przenumerowal skrzynke; zapamietany UID traci wtedy znaczenie. */
    long uidValidity() throws MessagingException;

    /** Najwyzszy UID w skrzynce albo 0 dla pustej. */
    long highestUid() throws MessagingException;

    /** Wiadomosci o UID wiekszym niz podany, rosnaco, najwyzej {@code limit}. */
    List<Fetched> fetchAfter(long uid, int limit) throws MessagingException;

    record Fetched(long uid, MimeMessage message) {}

    @Override
    void close();
}
