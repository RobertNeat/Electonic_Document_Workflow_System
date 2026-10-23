package robert_neat.dms_service.dto;

import java.time.LocalDate;

/** DTO wire-format odpowiedzi REST. `sciezkaPliku` jest szczegółem przechowywania - nieujawniana na zewnątrz. */
public record SzablonJasper(
        String id,
        String typDokumentuId,
        String nazwa,
        int wersja,
        LocalDate dataWgrania,
        String autor
) {
}
