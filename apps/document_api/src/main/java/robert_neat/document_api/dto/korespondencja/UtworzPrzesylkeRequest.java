package robert_neat.document_api.dto.korespondencja;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UtworzPrzesylkeRequest(
        @NotBlank String sprawaId,
        @NotBlank String dokumentId,
        @NotBlank String obywatelId,
        @NotNull KanalWysylki kanal
) {
}
