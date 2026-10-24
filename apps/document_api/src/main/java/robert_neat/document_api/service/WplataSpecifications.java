package robert_neat.document_api.service;

import org.springframework.data.jpa.domain.Specification;
import robert_neat.document_api.entity.WplataEntity;

/** Predykaty opcjonalnych filtrów dla GET /api/wplaty - budowane tylko dla niepustych parametrów.
 * obywatelId filtruje przez relację dwuetapową (WplataEntity.naleznosc.obywatel) - wpłata
 * niedopasowana (naleznosc_id IS NULL) nie wygeneruje w SQL dopasowania do żadnego obywatelId
 * (ścieżka Criteria na @ManyToOne staje się INNER JOIN, nie dereferencją Java), więc poprawnie
 * wypada z wyniku bez wyjątku - nie trzeba explicite strażnika isNotNull. */
final class WplataSpecifications {

    private WplataSpecifications() {
    }

    static Specification<WplataEntity> naleznoscIdEquals(String naleznoscId) {
        if (naleznoscId == null || naleznoscId.isBlank()) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("naleznosc").get("id"), naleznoscId);
    }

    static Specification<WplataEntity> obywatelIdEquals(String obywatelId) {
        if (obywatelId == null || obywatelId.isBlank()) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("naleznosc").get("obywatel").get("id"), obywatelId);
    }
}
