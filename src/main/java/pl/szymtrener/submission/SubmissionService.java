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

    public SubmissionService(SubmissionRepository repository, SubmissionNoteRepository notes,
                             MailService mail, AppProperties props) {
        this.repository = repository;
        this.notes = notes;
        this.mail = mail;
        this.props = props;
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
        mail.sendNotifications(saved);   // asynchronicznie, po zapisie
        return saved;
    }

    @Transactional
    public void changeStatus(Long id, SubmissionStatus status, Instant callAt) {
        Submission s = repository.findById(id).orElseThrow();
        s.setStatus(status);
        s.setCallAt(callAt);
        repository.save(s);
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
        SubmissionNote note = new SubmissionNote();
        note.setSubmissionId(submissionId);
        note.setAuthor(author);
        note.setBody(body);
        notes.save(note);
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
