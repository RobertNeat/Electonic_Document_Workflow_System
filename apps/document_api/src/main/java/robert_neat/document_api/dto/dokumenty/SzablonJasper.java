package robert_neat.document_api.dto.dokumenty;

import java.time.LocalDate;

public record SzablonJasper(
        String id,
        String typDokumentuId,
        String nazwa,
        int wersja,
        LocalDate dataWgrania,
        String autor
) {
}
