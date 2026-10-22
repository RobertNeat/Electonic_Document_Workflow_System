package robert_neat.document_api.dto.wymiana;

import java.math.BigDecimal;
import java.util.List;

public record FakturaUbl(
        String numer,
        String kontrahent,
        String nip,
        BigDecimal kwota,
        List<PozycjaFakturyUbl> pozycje,
        String numerReferencyjnyNaleznosci
) {
}
