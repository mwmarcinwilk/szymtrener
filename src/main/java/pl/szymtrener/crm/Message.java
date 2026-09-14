package pl.szymtrener.crm;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Jedna pozycja w watku rozmowy: zgloszenie z formularza, e-mail w obie strony,
 * notatka z rozmowy telefonicznej albo zdarzenie systemowe.
 *
 * Wszystko w jednej tabeli, bo watek wyswietla je RAZEM, po dacie. Trzy osobne
 * tabele trzeba by scalac przy kazdym otwarciu ekranu.
 */
@Entity
@Table(name = "message")
public class Message {

    private static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "submission_id") private Long submissionId;
    @Column(name = "trainee_id") private Long traineeId;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private MessageDirection direction = MessageDirection.OUT;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private MessageChannel channel = MessageChannel.EMAIL;

    @Column(nullable = false, columnDefinition = "text") private String body;
    @Column(name = "attachment_id") private Long attachmentId;
    @Column(name = "sent_at", nullable = false) private Instant sentAt = Instant.now();
    /** SENT / FAILED dla poczty, null dla notatek i zdarzen. */
    @Column(name = "mail_status") private String mailStatus;

    /** Naglowek Message-ID: wychodzacy do dopasowania odpowiedzi, przychodzacy przeciw duplikatom. */
    @Column(name = "mail_message_id") private String mailMessageId;

    /** Odebrana odpowiedz, ktorej trener jeszcze nie otworzyl. */
    @Column(nullable = false) private boolean unread;

    /** REPLY albo ADDRESS dla odebranych, null dla reszty. */
    @Enumerated(EnumType.STRING) @Column(name = "matched_by") private MatchedBy matchedBy;

    @Transient
    public boolean system() {
        return channel == MessageChannel.SYSTEM;
    }

    @Transient
    public boolean outgoing() {
        return direction == MessageDirection.OUT;
    }

    /** Nieudana wysylka rysuje sie w watku na czerwono. */
    @Transient
    public boolean failed() {
        return "FAILED".equals(mailStatus);
    }

    /** Dopasowana tylko po adresie nadawcy: naglowek From da sie podrobic, wiec watek to mowi. */
    @Transient
    public boolean matchedByAddress() {
        return matchedBy == MatchedBy.ADDRESS;
    }

    @Transient
    public String sentLabel() {
        return DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.forLanguageTag("pl-PL"))
                .format(sentAt.atZone(ZONE));
    }

    public Long getId() { return id; }
    public Long getSubmissionId() { return submissionId; }
    public void setSubmissionId(Long submissionId) { this.submissionId = submissionId; }
    public Long getTraineeId() { return traineeId; }
    public void setTraineeId(Long traineeId) { this.traineeId = traineeId; }
    public MessageDirection getDirection() { return direction; }
    public void setDirection(MessageDirection direction) { this.direction = direction; }
    public MessageChannel getChannel() { return channel; }
    public void setChannel(MessageChannel channel) { this.channel = channel; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public Long getAttachmentId() { return attachmentId; }
    public void setAttachmentId(Long attachmentId) { this.attachmentId = attachmentId; }
    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }
    public String getMailStatus() { return mailStatus; }
    public void setMailStatus(String mailStatus) { this.mailStatus = mailStatus; }
    public String getMailMessageId() { return mailMessageId; }
    public void setMailMessageId(String mailMessageId) { this.mailMessageId = mailMessageId; }
    public boolean isUnread() { return unread; }
    public void setUnread(boolean unread) { this.unread = unread; }
    public MatchedBy getMatchedBy() { return matchedBy; }
    public void setMatchedBy(MatchedBy matchedBy) { this.matchedBy = matchedBy; }
}
