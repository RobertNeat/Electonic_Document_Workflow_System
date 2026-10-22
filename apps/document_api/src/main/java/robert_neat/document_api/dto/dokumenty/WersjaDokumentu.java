package robert_neat.document_api.dto.dokumenty;

import java.time.LocalDate;

public record WersjaDokumentu(
        String id,
        String dokumentId,
        int wersja,
        boolean podpisana,
        LocalDate dataUtworzenia,
        String autor,
        StatusPodpisu statusPodpisu
) {
}
