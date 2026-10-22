package robert_neat.document_api.dto.wymiana;

import java.math.BigDecimal;

public record PozycjaFakturyUbl(
        String nazwa,
        BigDecimal ilosc,
        BigDecimal cenaJednostkowa,
        BigDecimal wartosc
) {
}
