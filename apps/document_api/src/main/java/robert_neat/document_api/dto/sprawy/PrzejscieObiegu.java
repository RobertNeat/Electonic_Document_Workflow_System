package robert_neat.document_api.dto.sprawy;

import java.util.List;

public record PrzejscieObiegu(
        String zStanu,
        List<String> doStanow,
        String wymaganaRola,
        List<String> wymaganeTypyDokumentow
) {
}
