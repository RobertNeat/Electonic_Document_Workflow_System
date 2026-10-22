package robert_neat.document_api.dto.wspolne;

import java.util.List;

/** Generyczna strona wyników listowych endpointów z paginacją. */
public record StronaWynikow<T>(List<T> dane, int strona, int rozmiarStrony, long liczbaWszystkich) {
}
