package pl.szymtrener.submission;

import jakarta.validation.constraints.*;

/** Dane z obu formularzy publicznych. Nazwy pol = atrybuty name= w HTML. */
public final class FormRequests {

    private FormRequests() {}

    public record OnlineForm(
            @NotBlank(message = "Podaj imię") @Size(max = 120) String name,
            @NotBlank @Email(message = "Sprawdź adres e-mail") @Size(max = 180) String email,
            @Size(max = 40) String phone,
            @NotBlank(message = "Podaj miejscowość") @Size(max = 120) String city,
            @NotBlank @Size(max = 2000) String currentTraining,
            @NotBlank @Size(max = 2000) String goal,
            @NotBlank @Size(max = 120) String equipment,
            @Size(max = 120) String source,
            @AssertTrue(message = "Wymagana jest zgoda na przetwarzanie danych") boolean consent,
            Boolean botcheck
    ) {}

    public record ContactForm(
            @NotBlank(message = "Podaj imię i nazwisko") @Size(max = 120) String name,
            @Size(max = 40) String phone,
            @NotBlank @Email(message = "Sprawdź adres e-mail") @Size(max = 180) String email,
            @Size(max = 160) String interest,
            @Size(max = 4000) String message,
            @AssertTrue(message = "Wymagana jest zgoda na przetwarzanie danych") boolean consent,
            Boolean botcheck
    ) {}
}
