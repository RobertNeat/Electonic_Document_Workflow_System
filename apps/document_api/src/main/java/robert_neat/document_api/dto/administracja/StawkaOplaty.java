package robert_neat.document_api.dto.administracja;

import java.math.BigDecimal;

public record StawkaOplaty(
        String id,
        String nazwa,
        BigDecimal kwota
) {
}
