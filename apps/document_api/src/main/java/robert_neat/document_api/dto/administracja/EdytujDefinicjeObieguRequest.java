package robert_neat.document_api.dto.administracja;

import jakarta.validation.constraints.NotBlank;

public record EdytujDefinicjeObieguRequest(
        @NotBlank String nazwa,
        String opis
) {
}
