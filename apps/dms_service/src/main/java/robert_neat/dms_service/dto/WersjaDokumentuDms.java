package robert_neat.dms_service.dto;

import java.time.LocalDate;

public record WersjaDokumentuDms(
        String id,
        String dokumentId,
        int wersja,
        boolean podpisana,
        LocalDate dataUtworzenia,
        String autor,
        String statusPodpisu
) {
}
