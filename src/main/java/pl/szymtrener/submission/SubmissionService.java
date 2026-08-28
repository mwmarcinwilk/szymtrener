package pl.szymtrener.submission;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.szymtrener.config.AppProperties;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SubmissionService {

    private final SubmissionRepository repository;
    private final SubmissionNoteRepository notes;
    private final MailService mail;
    private final AppProperties props;
    private final pl.szymtrener.crm.MessageService messages;

    public SubmissionService(SubmissionRepository repository, SubmissionNoteRepository notes,
                             MailService mail, AppProperties props,
                             pl.szymtrener.crm.MessageService messages) {
        this.repository = repository;
        this.notes = notes;
        this.mail = mail;
        this.props = props;
        this.messages = messages;
    }

    @Transactional
    public Submission acceptOnline(FormRequests.OnlineForm form, String ip, String userAgent) {
        Submission s = new Submission();
        s.setType(SubmissionType.ONLINE);
        s.setName(form.name());
        s.setEmail(form.email());
        s.setPhone(form.phone());
        s.setCity(form.city());
        s.setCurrentTraining(form.currentTraining());
        s.setGoal(form.goal());
        s.setEquipment(form.equipment());
        s.setSource(form.source());
        s.setOfferPath(form.offerPath());
        s.setOfferPackage(form.offerPackage());
        return persist(s, ip, userAgent);
    }

    @Transactional
    public Submission acceptContact(FormRequests.ContactForm form, String ip, String userAgent) {
        Submission s = new Submission();
        s.setType(SubmissionType.CONTACT);
        s.setName(form.name());
        s.setEmail(form.email());
        s.setPhone(form.phone());
        s.setInterest(form.interest());
        s.setMessage(form.message());
        return persist(s, ip, userAgent);
    }

    private Submission persist(Submission s, String ip, String userAgent) {
        s.setConsentAt(Instant.now());
        s.setIpHash(hash(ip));
        s.setUserAgent(userAgent != null && userAgent.length() > 300 ? userAgent.substring(0, 300) : userAgent);
        Submission saved = repository.save(s);

        // Watek zaczyna sie od tego, co klient sam napisal — inaczej trener
        // otwieralby rozmowe od pustego ekranu i musial wracac do ankiety.
        String opening = opening(saved);
        if (opening != null) messages.recordSubmission(saved.getId(), opening);

        mail.sendNotifications(saved);   // asynchronicznie, po zapisie
        return saved;
    }

    /** Tresc pierwszej pozycji watku: to, co klient wpisal w formularzu. */
    private static String opening(Submission s) {
        StringBuilder sb = new StringBuilder();
        if (s.getCurrentTraining() != null) sb.append(s.getCurrentTraining().trim());
        if (s.getGoal() != null) {
            if (sb.length() > 0) sb.append("\n\n");
            sb.append("Cel: ").append(s.getGoal().trim());
        }
        if (s.getMessage() != null) {
            if (sb.length() > 0) sb.append("\n\n");
            sb.append(s.getMessage().trim());
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    @Transactional
    public void changeStatus(Long id, SubmissionStatus status, Instant callAt) {
        Submission s = repository.findById(id).orElseThrow();
        s.setStatus(status);
        s.setCallAt(callAt);
        stamp(s, status);
        repository.save(s);
    }

    /**
     * Zmiana etapu ze sciezki w panelu. Zapisuje date wejscia w etap, bo sciezka
     * pokazuje pod nazwa kroku, KIEDY sie wydarzyl — sam status tego nie niesie.
     */
    @Transactional
    public Submission stage(Long id, SubmissionStatus status) {
        Submission s = repository.findById(id)
                .orElseThrow(() -> new pl.szymtrener.common.NotFoundException("Nie ma zgłoszenia " + id));
        s.setStatus(status);
        stamp(s, status);
        return repository.save(s);
    }

    /** Data wejscia w etap ustawiana raz — powrot do wczesniejszego kroku jej nie kasuje. */
    private static void stamp(Submission s, SubmissionStatus status) {
        Instant now = Instant.now();
        switch (status) {
            case IN_CONTACT -> { if (s.getContactedAt() == null) s.setContactedAt(now); }
            case CALL_BOOKED -> { if (s.getCallBookedAt() == null) s.setCallBookedAt(now); }
            case CLIENT -> { if (s.getConvertedAt() == null) s.setConvertedAt(now); }
            case ARCHIVED -> { if (s.getArchivedAt() == null) s.setArchivedAt(now); }
            case NEW -> { }
        }
    }

    /**
     * Przypomnienie o zgloszeniu. Ustawienie nowego terminu kasuje „zalatwione",
     * bo trener wlasnie powiedzial, ze chce o tym pamietac ponownie.
     */
    @Transactional
    public void remind(Long id, Instant at) {
        Submission s = repository.findById(id)
                .orElseThrow(() -> new pl.szymtrener.common.NotFoundException("Nie ma zgłoszenia " + id));
        s.setRemindAt(at);
        s.setRemindDone(at == null);
        repository.save(s);
    }

    @Transactional
    public void remindDone(Long id) {
        repository.findById(id).ifPresent(s -> {
            s.setRemindDone(true);
            repository.save(s);
        });
    }

    /**
     * Twarde usuniecie na zadanie klienta (RODO, art. 17). Notatki znikaja razem
     * ze zgloszeniem — klucz obcy ma ON DELETE CASCADE.
     */
    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    /**
     * Komplet danych jednego zgloszenia — realizacja prawa dostepu (RODO, art. 15).
     * LinkedHashMap, zeby kolejnosc pol w pliku byla czytelna dla czlowieka.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> export(Long id) {
        Submission s = repository.findById(id)
                .orElseThrow(() -> new pl.szymtrener.common.NotFoundException("Nie ma zgłoszenia " + id));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", s.getId());
        data.put("typ", s.getType());
        data.put("imie", s.getName());
        data.put("email", s.getEmail());
        data.put("telefon", s.getPhone());
        data.put("miejscowosc", s.getCity());
        data.put("trening_teraz", s.getCurrentTraining());
        data.put("cel", s.getGoal());
        data.put("sprzet", s.getEquipment());
        data.put("zainteresowanie", s.getInterest());
        data.put("wiadomosc", s.getMessage());
        data.put("skad_trafil", s.getSource());
        data.put("sciezka_oferty", s.getOfferPath());
        data.put("pakiet", s.getOfferPackage());
        data.put("status", s.getStatus());
        data.put("termin_rozmowy", s.getCallAt());
        data.put("zgoda_rodo", s.getConsentAt());
        data.put("zgloszono", s.getCreatedAt());
        data.put("notatki", notes.findBySubmissionIdOrderByCreatedAtDesc(id).stream()
                .map(n -> {
                    Map<String, Object> note = new LinkedHashMap<>();
                    note.put("autor", n.getAuthor());
                    note.put("tresc", n.getBody());
                    note.put("data", n.getCreatedAt());
                    return note;
                }).toList());
        // Swiadomie bez ip_hash i user_agent: to dane techniczne przeciwko nadużyciom,
        // nie tresc zgloszenia — i tak sa nieodwracalnie zahaszowane.
        return data;
    }

    @Transactional
    public void addNote(Long submissionId, String author, String body) {
        addNote(submissionId, null, author, body, null);
    }

    @Transactional
    public void addNote(Long submissionId, Long traineeId, String author, String body, String tags) {
        SubmissionNote note = new SubmissionNote();
        note.setSubmissionId(submissionId);
        note.setTraineeId(traineeId);
        note.setAuthor(author);
        note.setBody(body);
        note.setTags(tags == null || tags.isBlank() ? null : tags.trim());
        notes.save(note);
    }

    /** Przypina albo odpina notatke. Zwraca nowy stan, zeby panel mogl o nim powiedziec. */
    @Transactional
    public boolean togglePin(Long noteId) {
        SubmissionNote note = notes.findById(noteId)
                .orElseThrow(() -> new pl.szymtrener.common.NotFoundException("Nie ma notatki " + noteId));
        note.setPinned(!note.isPinned());
        notes.save(note);
        return note.isPinned();
    }

    @Transactional
    public void deleteNote(Long noteId) {
        notes.deleteById(noteId);
    }

    private String hash(String ip) {
        if (ip == null) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest((ip + props.analytics().salt()).getBytes())).substring(0, 32);
        } catch (Exception e) {
            return null;
        }
    }
}
