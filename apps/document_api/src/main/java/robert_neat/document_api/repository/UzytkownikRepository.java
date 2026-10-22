package robert_neat.document_api.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import robert_neat.document_api.entity.UzytkownikEntity;

import java.util.List;

public interface UzytkownikRepository
        extends JpaRepository<UzytkownikEntity, String>, JpaSpecificationExecutor<UzytkownikEntity> {

    boolean existsByLogin(String login);

    /** @EntityGraph na findAll(spec) - dociąga pełny zbiór ról w jednym zapytaniu (bez N+1),
     * niezależnie od tego, czy spec filtruje po roli przez osobny join na tej samej kolekcji. */
    @Override
    @EntityGraph(attributePaths = "role")
    List<UzytkownikEntity> findAll(Specification<UzytkownikEntity> spec);
}
