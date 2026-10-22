package robert_neat.document_api.dto.sprawy;

import java.time.LocalDate;

public record WpisMetryki(
        String id,
        String sprawaId,
        LocalDate data,
        String czynnosc,
        String osoba,
        String opis
) {
}
