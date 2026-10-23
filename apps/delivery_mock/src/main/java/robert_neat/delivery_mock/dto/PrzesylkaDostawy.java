package robert_neat.delivery_mock.dto;

import java.time.OffsetDateTime;

public record PrzesylkaDostawy(
        String id,
        String numerReferencyjny,
        String kanal,
        String adresAde,
        String status,
        OffsetDateTime dataNadania,
        OffsetDateTime dataStatusu
) {
}
