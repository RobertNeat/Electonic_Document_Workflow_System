package robert_neat.document_api.dto.dokumenty;

import java.time.LocalDate;

/** Java records nie wspierają dziedziczenia — pola Dokument powielone + rozszerzenie listowe. */
public record DokumentListElement(
        String id,
        String sprawaId,
        String typDokumentuId,
        String nazwa,
        int aktualnaWersja,
        LocalDate dataUtworzenia,
        String znakSprawy,
        StatusPodpisu statusPodpisuAktualnej
) {
}
