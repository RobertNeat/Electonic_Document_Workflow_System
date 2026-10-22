package robert_neat.document_api.dto.dokumenty;

import java.time.LocalDate;

/** Java records nie wspierają dziedziczenia — pola DokumentListElement powielone + rozszerzenie. */
public record DokumentObywatelaListElement(
        String id,
        String sprawaId,
        String typDokumentuId,
        String nazwa,
        int aktualnaWersja,
        LocalDate dataUtworzenia,
        String znakSprawy,
        StatusPodpisu statusPodpisuAktualnej,
        String typDokumentuNazwa
) {
}
