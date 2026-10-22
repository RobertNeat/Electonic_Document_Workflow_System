package robert_neat.document_api.dto.administracja;

import robert_neat.document_api.dto.sprawy.KategoriaArchiwalna;

public record PozycjaJrwa(
        String symbol,
        String haslo,
        KategoriaArchiwalna kategoriaArchiwalna
) {
}
