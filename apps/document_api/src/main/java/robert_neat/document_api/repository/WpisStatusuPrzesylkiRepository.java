package robert_neat.document_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import robert_neat.document_api.entity.WpisStatusuPrzesylkiEntity;

import java.util.List;

public interface WpisStatusuPrzesylkiRepository extends JpaRepository<WpisStatusuPrzesylkiEntity, String> {

    List<WpisStatusuPrzesylkiEntity> findByPrzesylka_IdOrderByDataAsc(String przesylkaId);

    /** Do zbiorczego mapowania historiaStatusow na listach przesyłek bez N+1. */
    List<WpisStatusuPrzesylkiEntity> findByPrzesylka_IdInOrderByDataAsc(List<String> przesylkaIds);
}
