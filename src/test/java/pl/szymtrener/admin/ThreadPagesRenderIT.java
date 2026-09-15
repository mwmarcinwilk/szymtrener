package pl.szymtrener.admin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import pl.szymtrener.PostgresTestBase;
import pl.szymtrener.crm.MessageService;
import pl.szymtrener.crm.TraineeService;
import pl.szymtrener.submission.Submission;
import pl.szymtrener.submission.SubmissionRepository;
import pl.szymtrener.submission.SubmissionType;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Strony z watkiem renderuja sie do konca. Blad w szablonie w polowie widoku daje w przegladarce
 * urwana strone ze statusem 200, wiec sprawdzamy znacznik z samego dolu formularza.
 */
@AutoConfigureMockMvc
class ThreadPagesRenderIT extends PostgresTestBase {

    @Autowired MockMvc mvc;
    @Autowired SubmissionRepository submissions;
    @Autowired TraineeService trainees;
    @Autowired MessageService messages;

    @Test
    @DisplayName("zgłoszenie i profil klienta renderują się z polem plików i przyciskiem wysyłki")
    void threadPagesRenderCompletely() throws Exception {
        Submission s = new Submission();
        s.setType(SubmissionType.ONLINE);
        s.setName("Marcin Wilk");
        s.setEmail("mw." + System.nanoTime() + "@example.test");
        s.setCity("Bratoszewice");
        s = submissions.save(s);
        messages.recordSubmission(s.getId(), "Ćwiczę u Ciebie.");
        var admin = user("admin@example.com").roles("ADMIN");

        mvc.perform(get("/admin/zgloszenia/" + s.getId()).with(admin))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"cmp-pliki\"")))
                .andExpect(content().string(containsString("</html>")));

        Long traineeId = trainees.fromSubmission(s.getId()).getId();
        messages.attachToTrainee(s.getId(), traineeId);
        mvc.perform(get("/admin/klienci/" + traineeId).with(admin))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"cmp-pliki\"")))
                .andExpect(content().string(containsString("</html>")));
    }
}
