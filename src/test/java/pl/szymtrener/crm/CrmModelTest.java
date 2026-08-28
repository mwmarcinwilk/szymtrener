package pl.szymtrener.crm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.szymtrener.submission.SubmissionNote;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reguly, na ktorych opiera sie ekran klienta i watek rozmowy.
 *
 * Handoff jest tu jednoznaczny: odwolany trening NIE zuzywa wejscia z pakietu,
 * chyba ze trener zaznaczy inaczej. Blad w te strone kosztuje klienta trening,
 * w druga — trenera pieniadze.
 */
class CrmModelTest {

    @Test
    @DisplayName("odwołany trening domyślnie nie zużywa wejścia z pakietu")
    void cancelledDoesNotConsume() {
        TrainingSession s = new TrainingSession();
        s.setStartsAt(Instant.now());
        s.setStatus(SessionStatus.CANCELLED);
        s.setConsumesPackage(false);

        assertThat(s.isConsumesPackage()).isFalse();
        assertThat(s.getStatus().label()).isEqualTo("Odwołany");
        assertThat(s.getStatus().badge()).isEqualTo("arch");
    }

    @Test
    @DisplayName("wartość pakietu to cena za trening razy liczba treningów")
    void packageValue() {
        TrainingPackage p = new TrainingPackage();
        p.setTotalSessions(12);
        p.setPricePerSessionGr(15000);

        assertThat(p.valueGr()).isEqualTo(180000);
        assertThat(pl.szymtrener.offer.Money.format(p.valueGr())).isEqualTo("1 800 zł");
    }

    @Test
    @DisplayName("notatka z tagiem Zdrowie albo Ważne trafia do „Na co uważać”")
    void warningNotes() {
        SubmissionNote n = new SubmissionNote();
        n.setTags("Zdrowie, Ważne");
        assertThat(n.tagList()).containsExactly("Zdrowie", "Ważne");
        assertThat(n.warning()).isTrue();

        n.setTags("Sprzedaż");
        assertThat(n.warning()).isFalse();

        n.setTags(null);
        assertThat(n.tagList()).isEmpty();
        assertThat(n.warning()).isFalse();
    }

    @Test
    @DisplayName("zdarzenie systemowe i nieudana wysyłka są rozpoznawalne w wątku")
    void systemAndFailedMessages() {
        Message m = new Message();
        m.setChannel(MessageChannel.SYSTEM);
        assertThat(m.system()).isTrue();
        assertThat(m.outgoing()).isTrue();

        m.setMailStatus("FAILED");
        assertThat(m.failed()).isTrue();

        m.setChannel(MessageChannel.PHONE);
        assertThat(m.system()).isFalse();
        assertThat(m.getChannel().label()).isEqualTo("Telefon · notatka");
        assertThat(m.getChannel().css()).isEqualTo("tel");
    }

    @Test
    @DisplayName("brak kontaktu odróżnia się od kontaktu dzisiaj")
    void contactLabels() {
        Trainee never = new Trainee();
        assertThat(never.daysSinceContact()).isEqualTo(-1);
        assertThat(never.lastContactLabel()).isEqualTo("brak kontaktu");

        Trainee today = new Trainee();
        today.setLastContactAt(Instant.now());
        assertThat(today.lastContactLabel()).isEqualTo("dzisiaj");

        Trainee old = new Trainee();
        old.setLastContactAt(Instant.now().minus(java.time.Duration.ofDays(21)));
        assertThat(old.daysSinceContact()).isEqualTo(21);
        assertThat(old.lastContactLabel()).isEqualTo("21 dni");
    }
}
