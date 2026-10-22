package robert_neat.document_api.dto.administracja;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record UtworzUzytkownikaRequest(
        @NotBlank String login,
        @NotBlank String imie,
        @NotBlank String nazwisko,
        @NotBlank @Email String email,
        List<RolaUzytkownika> role,
        @NotBlank String wydzial,
        @NotBlank String stanowisko
) {
}
