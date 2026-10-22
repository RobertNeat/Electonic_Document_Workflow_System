package robert_neat.document_api.dto.korespondencja;

import java.time.OffsetDateTime;
import java.util.List;

public record Przesylka(
        String id,
        String sprawaId,
        String dokumentId,
        String obywatelId,
        KanalWysylki kanal,
        StatusPrzesylki status,
        String numerReferencyjny,
        OffsetDateTime dataNadania,
        OffsetDateTime dataStatusu,
        Kierunek kierunek,
        List<WpisStatusuPrzesylki> historiaStatusow
) {
}
