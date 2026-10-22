package robert_neat.document_api.dto.sprawy;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record UtworzSpraweRequest(
        @NotBlank String definicjaObieguId,
        @NotBlank String jrwa,
        @NotNull KategoriaArchiwalna kategoriaArchiwalna,
        @NotBlank String tytul,
        @NotBlank String obywatelId,
        @NotBlank String wydzial,
        @NotBlank String referent,
        LocalDate terminUstawowy
) {
}
