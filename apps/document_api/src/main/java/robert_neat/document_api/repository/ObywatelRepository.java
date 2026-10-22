package robert_neat.document_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import robert_neat.document_api.entity.ObywatelEntity;

import java.util.List;

public interface ObywatelRepository extends JpaRepository<ObywatelEntity, String> {

    @Query("""
            SELECT o FROM ObywatelEntity o
            WHERE LOWER(COALESCE(o.imie, '')) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(COALESCE(o.nazwisko, '')) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(COALESCE(o.nazwaPodmiotu, '')) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(COALESCE(o.pesel, '')) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(COALESCE(o.nip, '')) LIKE LOWER(CONCAT('%', :q, '%'))
            """)
    List<ObywatelEntity> search(@Param("q") String q);
}
