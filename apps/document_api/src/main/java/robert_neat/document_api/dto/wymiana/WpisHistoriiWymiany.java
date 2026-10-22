package robert_neat.document_api.dto.wymiana;

import java.time.OffsetDateTime;

public record WpisHistoriiWymiany(
        String id,
        String nazwaPliku,
        KierunekWymiany kierunek,
        OffsetDateTime data,
        StatusWymiany status,
        int liczbaRekordow,
        String opis
) {
}
