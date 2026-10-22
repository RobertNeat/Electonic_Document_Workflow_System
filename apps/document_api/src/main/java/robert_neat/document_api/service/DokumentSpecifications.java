package robert_neat.document_api.service;

import org.springframework.data.jpa.domain.Specification;
import robert_neat.document_api.entity.DokumentEntity;

/** Predykaty opcjonalnych filtrów dla GET /api/dokumenty - budowane tylko dla niepustych parametrów. */
final class DokumentSpecifications {

    private DokumentSpecifications() {
    }

    static Specification<DokumentEntity> sprawaIdEquals(String sprawaId) {
        if (sprawaId == null || sprawaId.isBlank()) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("sprawa").get("id"), sprawaId);
    }

    static Specification<DokumentEntity> typDokumentuIdEquals(String typDokumentuId) {
        if (typDokumentuId == null || typDokumentuId.isBlank()) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("typDokumentu").get("id"), typDokumentuId);
    }
}
