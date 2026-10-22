package robert_neat.document_api.dto.sprawy;

import java.util.List;

public record DefinicjaObiegu(
        String id,
        String nazwa,
        String opis,
        List<String> stany,
        String stanPoczatkowy,
        List<PrzejscieObiegu> przejscia
) {
}
