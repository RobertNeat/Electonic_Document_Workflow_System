package robert_neat.document_api.dto.sprawy;

import jakarta.validation.constraints.NotBlank;

public record ZmienStanSprawyRequest(
        @NotBlank String nowyStan
) {
}
