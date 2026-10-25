package robert_neat.document_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import robert_neat.document_api.entity.TypDokumentuEntity;

import java.util.Optional;

public interface TypDokumentuRepository extends JpaRepository<TypDokumentuEntity, String> {

    /** findFirst, nie findBy - "nazwa" nie ma unikalnego constraintu w bazie, więc duplikat
     * nie może rzucić IncorrectResultSizeDataAccessException przy odrobinę niefortunnych danych. */
    Optional<TypDokumentuEntity> findFirstByNazwa(String nazwa);
}
