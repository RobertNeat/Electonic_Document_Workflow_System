package robert_neat.document_api.dto.sprawy;

import java.time.LocalDate;

public record Sprawa(
        String id,
        String znakSprawy,
        String tytul,
        String jrwa,
        String definicjaObieguId,
        String stan,
        String obywatelId,
        String wydzial,
        String referent,
        LocalDate dataWszczecia,
        LocalDate terminUstawowy,
        KategoriaArchiwalna kategoriaArchiwalna,
        boolean zamknieta
) {
}
