package robert_neat.document_api.dto.administracja;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AktualizujRoleUzytkownikaRequest(
        @NotEmpty List<RolaUzytkownika> role
) {
}
