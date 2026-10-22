package robert_neat.document_api.dto.dokumenty;

import java.time.LocalDate;

public record Dokument(
        String id,
        String sprawaId,
        String typDokumentuId,
        String nazwa,
        int aktualnaWersja,
        LocalDate dataUtworzenia
) {
}
