package robert_neat.document_api.dto.platnosci;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Naleznosc(
        String id,
        String sprawaId,
        String obywatelId,
        String numerReferencyjny,
        String tytul,
        BigDecimal kwota,
        BigDecimal kwotaRozliczona,
        LocalDate terminPlatnosci,
        StatusRozliczenia status
) {
}
