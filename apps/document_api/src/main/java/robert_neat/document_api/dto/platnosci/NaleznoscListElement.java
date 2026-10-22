package robert_neat.document_api.dto.platnosci;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Java records nie wspierają dziedziczenia — pola Naleznosc powielone + rozszerzenie listowe. */
public record NaleznoscListElement(
        String id,
        String sprawaId,
        String obywatelId,
        String numerReferencyjny,
        String tytul,
        BigDecimal kwota,
        BigDecimal kwotaRozliczona,
        LocalDate terminPlatnosci,
        StatusRozliczenia status,
        String znakSprawy,
        String obywatelNazwa
) {
}
