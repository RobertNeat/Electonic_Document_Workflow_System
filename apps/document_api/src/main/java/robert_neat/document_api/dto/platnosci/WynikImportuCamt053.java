package robert_neat.document_api.dto.platnosci;

import java.util.List;

public record WynikImportuCamt053(
        int liczbaTransakcji,
        int liczbaDopasowanychAutomatycznie,
        int liczbaDoRekoncyliacji,
        List<Wplata> utworzoneWplaty
) {
}
