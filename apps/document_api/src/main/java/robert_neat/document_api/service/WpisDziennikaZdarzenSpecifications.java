package robert_neat.document_api.service;

import org.springframework.data.jpa.domain.Specification;
import robert_neat.document_api.entity.WpisDziennikaZdarzenEntity;

import java.time.OffsetDateTime;

/** Predykaty opcjonalnego filtru dla GET /api/audit-log - budowane tylko dla niepustych parametrów. */
final class WpisDziennikaZdarzenSpecifications {

    private WpisDziennikaZdarzenSpecifications() {
    }

    static Specification<WpisDziennikaZdarzenEntity> typOperacjiEquals(String typOperacji) {
        if (typOperacji == null || typOperacji.isBlank()) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("typOperacji"), typOperacji);
    }

    static Specification<WpisDziennikaZdarzenEntity> dataCzasOd(OffsetDateTime od) {
        if (od == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("dataCzas"), od);
    }

    /** Górna granica wyłączna (< do), tak by parametr `do` zadany jako data kalendarzowa
     * obejmował cały ten dzień po przeliczeniu na "początek następnego dnia" w serwisie. */
    static Specification<WpisDziennikaZdarzenEntity> dataCzasDo(OffsetDateTime doWylacznie) {
        if (doWylacznie == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.lessThan(root.get("dataCzas"), doWylacznie);
    }
}
