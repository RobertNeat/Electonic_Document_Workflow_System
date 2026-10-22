package robert_neat.document_api.service;

import org.springframework.data.jpa.domain.Specification;
import robert_neat.document_api.entity.PrzesylkaEntity;

/** Predykaty opcjonalnych filtrów dla GET /api/przesylki - filtrowanie po surowych wartościach
 * DB (wire-format), bo kolumny kanal/status/kierunek przechowują stringi zgodne z CHECK constraint. */
final class PrzesylkaSpecifications {

    private PrzesylkaSpecifications() {
    }

    static Specification<PrzesylkaEntity> statusEquals(String status) {
        if (status == null || status.isBlank()) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    static Specification<PrzesylkaEntity> kanalEquals(String kanal) {
        if (kanal == null || kanal.isBlank()) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("kanal"), kanal);
    }

    static Specification<PrzesylkaEntity> kierunekEquals(String kierunek) {
        if (kierunek == null || kierunek.isBlank()) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("kierunek"), kierunek);
    }
}
