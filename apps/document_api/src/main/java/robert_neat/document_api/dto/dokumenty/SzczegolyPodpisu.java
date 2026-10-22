package robert_neat.document_api.dto.dokumenty;

import java.time.OffsetDateTime;

public record SzczegolyPodpisu(
        String podpisujacy,
        OffsetDateTime czasPodpisu,
        String odciskCertyfikatu,
        String statusOcsp,
        String statusCrl,
        OffsetDateTime znacznikCzasuTsa
) {
}
