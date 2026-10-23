package robert_neat.jasper_service.dto;

import java.time.LocalDate;

/** Metadane szablonu Jasper pobierane z dms_service (DTO SzablonJasper w kontrakcie dms_service). */
public record SzablonMetadane(
        String id,
        String typDokumentuId,
        String nazwa,
        int wersja,
        LocalDate dataWgrania,
        String autor) {
}
