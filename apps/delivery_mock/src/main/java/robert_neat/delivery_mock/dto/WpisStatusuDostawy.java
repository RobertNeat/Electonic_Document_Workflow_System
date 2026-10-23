package robert_neat.delivery_mock.dto;

import java.time.OffsetDateTime;

public record WpisStatusuDostawy(
        String id,
        String przesylkaDostawyId,
        String status,
        OffsetDateTime data,
        String szczegoly
) {
}
