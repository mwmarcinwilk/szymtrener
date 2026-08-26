package pl.szymtrener.crm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.szymtrener.common.NotFoundException;
import pl.szymtrener.submission.Submission;
import pl.szymtrener.submission.SubmissionRepository;
import pl.szymtrener.submission.SubmissionStatus;
import pl.szymtrener.submission.SubmissionType;

import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class TraineeService {

    private static final Logger log = LoggerFactory.getLogger(TraineeService.class);
    private static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");

    private final TraineeRepository trainees;
    private final SubmissionRepository submissions;

    public TraineeService(TraineeRepository trainees, SubmissionRepository submissions) {
        this.trainees = trainees;
        this.submissions = submissions;
    }

    /**
     * „Zrób z tego klienta": przepisuje dane ze zgloszenia i zamyka je statusem
     * CLIENT. Dwa klikniecia na tym samym zgloszeniu nie robia dwoch klientow —
     * zwracamy istniejacego.
     */
    @Transactional
    public Trainee fromSubmission(Long submissionId) {
        Submission source = submissions.findById(submissionId)
                .orElseThrow(() -> new NotFoundException("Nie ma zgłoszenia " + submissionId));

        return trainees.findBySubmissionId(submissionId).orElseGet(() -> {
            Trainee trainee = new Trainee();
            trainee.setSubmissionId(submissionId);
            trainee.setName(source.getName());
            trainee.setCity(source.getCity());
            trainee.setMode(source.getType() == SubmissionType.ONLINE ? TraineeMode.ONLINE : TraineeMode.ONSITE);
            trainee.setStartedAt(LocalDate.now(ZONE));
            trainee.setStatus(TraineeStatus.ACTIVE);
            Trainee saved = trainees.save(trainee);

            source.setStatus(SubmissionStatus.CLIENT);
            submissions.save(source);

            log.info("Zgloszenie {} zamienione na klienta {}", submissionId, saved.getId());
            return saved;
        });
    }

    @Transactional
    public Trainee save(TraineeForm form) {
        Trainee trainee = form.getId() == null
                ? new Trainee()
                : trainees.findById(form.getId())
                        .orElseThrow(() -> new NotFoundException("Nie ma klienta " + form.getId()));

        trainee.setSubmissionId(form.getSubmissionId());
        trainee.setName(form.getName());
        trainee.setCity(blankToNull(form.getCity()));
        trainee.setAge(form.getAge());
        trainee.setMode(form.getMode());
        trainee.setStartedAt(form.getStartedAt());
        trainee.setPlanName(blankToNull(form.getPlanName()));
        trainee.setSessionCount(Math.max(0, form.getSessionCount()));
        trainee.setStatus(form.getStatus());
        return trainees.save(trainee);
    }

    @Transactional
    public void delete(Long id) {
        trainees.deleteById(id);
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
