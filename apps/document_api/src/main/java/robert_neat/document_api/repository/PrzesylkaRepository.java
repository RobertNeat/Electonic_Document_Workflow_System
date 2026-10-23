package robert_neat.document_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import robert_neat.document_api.entity.PrzesylkaEntity;

import java.util.List;
import java.util.Optional;

public interface PrzesylkaRepository extends JpaRepository<PrzesylkaEntity, String>, JpaSpecificationExecutor<PrzesylkaEntity> {

    /** Do potencjalnego reużycia w module Obywatele (historia korespondencji obywatela). */
    List<PrzesylkaEntity> findByObywatel_Id(String obywatelId);

    /** Odnalezienie przesyłki po numerze referencyjnym - pole łączące z delivery_mock, używane
     * przez PrzesylkaStatusuConsumer do rozwiązania zdarzenia zmiany statusu z RabbitMQ. */
    Optional<PrzesylkaEntity> findByNumerReferencyjny(String numerReferencyjny);

    /** Numery referencyjne danego kanału/roku, do wyliczenia kolejnego numeru (parsowanie max+1
     * po stronie serwisu, analogicznie do SprawaRepository.findZnakiSprawyByPrefix). */
    @Query("SELECT p.numerReferencyjny FROM PrzesylkaEntity p WHERE p.numerReferencyjny LIKE :prefix")
    List<String> findNumeryReferencyjneByPrefix(@Param("prefix") String prefix);
}
