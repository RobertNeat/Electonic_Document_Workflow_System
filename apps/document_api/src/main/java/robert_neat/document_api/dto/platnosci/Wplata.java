package robert_neat.document_api.dto.platnosci;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Wplata(
        String id,
        String naleznoscId,
        BigDecimal kwota,
        LocalDate data,
        String tytul,
        String kontrahent,
        boolean dopasowanaAutomatycznie
) {
}
