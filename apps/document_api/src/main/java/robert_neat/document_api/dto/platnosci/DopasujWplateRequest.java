package robert_neat.document_api.dto.platnosci;

import jakarta.validation.constraints.NotBlank;

public record DopasujWplateRequest(
        @NotBlank String naleznoscId
) {
}
