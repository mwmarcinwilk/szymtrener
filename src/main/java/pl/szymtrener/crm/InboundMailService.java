package pl.szymtrener.crm;

import jakarta.mail.FolderClosedException;
import jakarta.mail.MessagingException;
import jakarta.mail.StoreClosedException;
import jakarta.mail.internet.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import pl.szymtrener.config.AppProperties;
import pl.szymtrener.settings.SettingsService;

import java.time.Instant;
import java.util.Optional;

/**
 * Odbior odpowiedzi klientow do watku rozmowy.
 *
 * Postep trzyma UID, nie flaga „przeczytane": trener czyta ta sama skrzynke w programie
 * pocztowym, wiec flaga SEEN nic nie mowi o tym, co aplikacja juz widziala. Zmiana
 * UIDVALIDITY (serwer przenumerowal skrzynke) i pierwsze uruchomienie zaczynaja od biezacego
 * konca skrzynki: import calej historii wrzucilby do watkow stare maile jako „nowe odpowiedzi".
 *
 * Granica bledu to pojedyncza wiadomosc. Zepsuty mail jest pomijany i nie blokuje kolejnych;
 * zerwane polaczenie przerywa odbior bez przesuwania postepu, wiec nic nie przepada.
 */
@Service
public class InboundMailService {

    private static final Logger log = LoggerFactory.getLogger(InboundMailService.class);
    /** Na jeden odbior. Reszta przyjdzie za dwie minuty, a watek harmonogramu nie wisi na setkach maili. */
    static final int BATCH = 100;
    /** Tyle odbiorow z rzedu moze sie wywracac ta sama wiadomosc, zanim ja pominiemy. */
    static final int MAX_ATTEMPTS = 3;

    public record Result(int fetched, int recorded, int skipped, int failed) {}

    private final MessageService messages;
    private final InboundMatcher matcher;
    private final SettingsService settings;
    private final AppProperties props;

    public InboundMailService(MessageService messages, InboundMatcher matcher, SettingsService settings,
                              AppProperties props) {
        this.messages = messages;
        this.matcher = matcher;
        this.settings = settings;
        this.props = props;
    }

    public Result poll(Mailbox mailbox, String ownAddress) throws MessagingException {
        long validity = mailbox.uidValidity();
        String knownValidity = settings.get(SettingsService.INBOX_UID_VALIDITY, null);
        if (!String.valueOf(validity).equals(knownValidity)) {
            long start = mailbox.highestUid();
            settings.set(SettingsService.INBOX_UID_VALIDITY, String.valueOf(validity));
            settings.set(SettingsService.INBOX_LAST_UID, String.valueOf(start));
            log.info("Odbior poczty: nowa numeracja skrzynki ({}), zaczynam od UID {} bez importu historii",
                    validity, start);
            return new Result(0, 0, 0, 0);
        }

        long lastUid = settings.getLong(SettingsService.INBOX_LAST_UID, 0);
        int recorded = 0, skipped = 0, failed = 0;
        var batch = mailbox.fetchAfter(lastUid, BATCH);
        for (Mailbox.Fetched fetched : batch) {
            try {
                if (record(fetched, validity, ownAddress)) recorded++; else skipped++;
            } catch (FolderClosedException | StoreClosedException e) {
                // Polaczenie zerwane: ta i kolejne wiadomosci przyjda przy nastepnym odbiorze.
                throw e;
            } catch (MessagingException e) {
                // Blad serwera albo sieci przy tej wiadomosci (nie jej tresci): sprobujemy przy nastepnym
                // odbiorze. Wiadomosc, ktora wywraca sie za kazdym razem, nie moze jednak zablokowac skrzynki.
                if (!(e instanceof ParseException) && attempt(fetched.uid()) < MAX_ATTEMPTS) throw e;
                failed++;
                log.warn("Odbior poczty: pomijam wiadomosc UID {} ({}: {})", fetched.uid(),
                        e.getClass().getSimpleName(), e.getMessage());
            } catch (RuntimeException | StackOverflowError e) {
                // StackOverflowError: Angus dekoduje quoted-printable rekurencyjnie, wiec ~30 KB „=\r\n"
                // od kogokolwiek z zewnatrz wywraca watek. Bez tego jeden mail zatrzymalby odbior na zawsze.
                failed++;
                log.warn("Odbior poczty: pomijam wiadomosc UID {} ({}: {})", fetched.uid(),
                        e.getClass().getSimpleName(), e.getMessage());
            }
            settings.set(SettingsService.INBOX_LAST_UID, String.valueOf(fetched.uid()));
        }
        if (settings.get(SettingsService.INBOX_FAILED_UID, null) != null) settings.set(SettingsService.INBOX_FAILED_UID, null);
        if (recorded > 0 || failed > 0) {
            log.info("Odbior poczty: pobrano {}, do watkow {}, pominieto {}, bledy {}",
                    batch.size(), recorded, skipped, failed);
        }
        return new Result(batch.size(), recorded, skipped, failed);
    }

    /** Kolejna nieudana proba tej samej wiadomosci; licznik w ustawieniach jako „uid:proby". */
    private int attempt(long uid) {
        String[] previous = settings.get(SettingsService.INBOX_FAILED_UID, "").split(":");
        int attempts = previous.length == 2 && previous[0].equals(String.valueOf(uid))
                ? Integer.parseInt(previous[1]) + 1 : 1;
        settings.set(SettingsService.INBOX_FAILED_UID, uid + ":" + attempts);
        return attempts;
    }

    private boolean record(Mailbox.Fetched fetched, long validity, String ownAddress) throws MessagingException {
        InboundMail mail = InboundMail.parse(fetched.message(), Instant.now());
        if (mail.automatic()) return false;
        if (mail.from() == null || mail.from().equalsIgnoreCase(ownAddress) || mail.from().equalsIgnoreCase(props.mail().from())) {
            return false;
        }
        // Bez Message-ID duplikaty rozpoznajemy po miejscu w skrzynce.
        String id = mail.messageId() != null ? mail.messageId() : "<uid-" + validity + "-" + fetched.uid() + "@inbox>";
        if (messages.alreadyRecorded(id)) return false;

        Optional<InboundMatcher.Target> target = matcher.match(mail);
        if (target.isEmpty()) {
            log.info("Odbior poczty: {} nie pasuje do zadnego zgloszenia ani klienta", mail);
            return false;
        }
        try {
            messages.recordInbound(target.get(), mail, id);
            return true;
        } catch (DataIntegrityViolationException e) {
            // Zwykle ten sam Message-ID zapisany rownolegle, ale takze zgloszenie usuniete miedzy
            // dopasowaniem a zapisem. Bez tresci maila w logu.
            log.warn("Odbior poczty: nie zapisano UID {} ({})", fetched.uid(), e.getMostSpecificCause().getClass().getSimpleName());
            return false;
        }
    }
}
