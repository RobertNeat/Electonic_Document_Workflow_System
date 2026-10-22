package robert_neat.document_api.dto.korespondencja;

import java.time.OffsetDateTime;

public record WpisStatusuPrzesylki(
        StatusPrzesylki status,
        OffsetDateTime data
) {
}
