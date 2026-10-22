package robert_neat.document_api.dto.administracja;

import java.util.List;

public record Uzytkownik(
        String id,
        String login,
        String imie,
        String nazwisko,
        String email,
        List<RolaUzytkownika> role,
        String wydzial,
        String stanowisko
) {
}
