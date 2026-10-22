package robert_neat.document_api.dto.administracja;

import java.time.OffsetDateTime;

public record WpisDziennikaZdarzen(
        String id,
        OffsetDateTime dataCzas,
        String uzytkownik,
        TypOperacjiAudytu typOperacji,
        String obiekt,
        String szczegoly
) {
}
