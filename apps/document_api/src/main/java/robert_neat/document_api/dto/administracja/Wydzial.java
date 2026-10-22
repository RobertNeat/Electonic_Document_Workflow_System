package robert_neat.document_api.dto.administracja;

import java.util.List;

public record Wydzial(
        String id,
        String nazwa,
        String kierownik,
        List<Stanowisko> stanowiska
) {
}
