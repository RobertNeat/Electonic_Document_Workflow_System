package robert_neat.document_api.service;

import org.springframework.data.jpa.domain.Specification;
import robert_neat.document_api.entity.SprawaEntity;

/** Predykaty opcjonalnych filtrów dla GET /api/sprawy - budowane tylko dla niepustych parametrów. */
final class SprawaSpecifications {

    private SprawaSpecifications() {
    }

    static Specification<SprawaEntity> stanEquals(String stan) {
        if (stan == null || stan.isBlank()) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("stan"), stan);
    }

    static Specification<SprawaEntity> wydzialEquals(String wydzial) {
        if (wydzial == null || wydzial.isBlank()) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("wydzial"), wydzial);
    }

    static Specification<SprawaEntity> obywatelIdEquals(String obywatelId) {
        if (obywatelId == null || obywatelId.isBlank()) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("obywatel").get("id"), obywatelId);
    }

    static Specification<SprawaEntity> znakOrTytulContains(String q) {
        if (q == null || q.isBlank()) {
            return Specification.unrestricted();
        }
        String pattern = "%" + q.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("znakSprawy")), pattern),
                cb.like(cb.lower(root.get("tytul")), pattern)
        );
    }
}
