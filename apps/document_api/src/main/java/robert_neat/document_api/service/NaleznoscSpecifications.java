package robert_neat.document_api.service;

import org.springframework.data.jpa.domain.Specification;
import robert_neat.document_api.entity.NaleznoscEntity;

/** Predykaty opcjonalnego filtru dla GET /api/naleznosci - budowane tylko dla niepustych parametrów. */
final class NaleznoscSpecifications {

    private NaleznoscSpecifications() {
    }

    static Specification<NaleznoscEntity> statusEquals(String status) {
        if (status == null || status.isBlank()) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }
}
