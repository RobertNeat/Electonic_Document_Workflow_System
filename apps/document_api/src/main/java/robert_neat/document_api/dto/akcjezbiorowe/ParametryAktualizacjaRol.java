package robert_neat.document_api.dto.akcjezbiorowe;

import robert_neat.document_api.dto.administracja.RolaUzytkownika;

import java.util.List;

public record ParametryAktualizacjaRol(
        List<RolaUzytkownika> dodajRole,
        List<RolaUzytkownika> usunRole
) {
}
