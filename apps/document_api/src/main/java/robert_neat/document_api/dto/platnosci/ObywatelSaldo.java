package robert_neat.document_api.dto.platnosci;

import java.math.BigDecimal;

public record ObywatelSaldo(
        String obywatelId,
        BigDecimal doZaplaty
) {
}
