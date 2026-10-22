package robert_neat.document_api.service;

import org.springframework.data.jpa.domain.Specification;
import robert_neat.document_api.entity.UzytkownikEntity;

import jakarta.persistence.criteria.Join;

/** Predykaty opcjonalnego filtru dla GET /api/uzytkownicy - budowane tylko dla niepustych parametrów. */
final class UzytkownikSpecifications {

    private UzytkownikSpecifications() {
    }

    static Specification<UzytkownikEntity> wydzialEquals(String wydzial) {
        if (wydzial == null || wydzial.isBlank()) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("wydzial"), wydzial);
    }

    /** Join osobny od ewentualnego @EntityGraph fetch-join użytego do dociągnięcia pełnego zbioru ról
     * w findAll - filtrowanie na tym samym fetch-joinie obcięłoby kolekcję do tylko pasującej roli. */
    static Specification<UzytkownikEntity> maRole(String rolaDbValue) {
        if (rolaDbValue == null || rolaDbValue.isBlank()) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> {
            query.distinct(true);
            Join<UzytkownikEntity, String> role = root.join("role");
            return cb.equal(role, rolaDbValue);
        };
    }
}
