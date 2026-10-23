package robert_neat.delivery_mock.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import robert_neat.delivery_mock.entity.PrzesylkaDostawyEntity;
import robert_neat.delivery_mock.entity.WpisStatusuDostawyEntity;
import robert_neat.delivery_mock.exception.NotFoundException;
import robert_neat.delivery_mock.repository.PrzesylkaDostawyRepository;
import robert_neat.delivery_mock.repository.WpisStatusuDostawyRepository;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Zapis lokalnej zmiany statusu (wpis historii + aktualizacja przesyłki), w odrębnym beanie
 * z własną granicą @Transactional - publikacja zdarzenia RabbitMQ w SymulacjaDostawyService musi
 * być POZA tą transakcją, żeby awaria brokera nie wycofywała lokalnego zapisu (patrz komentarz
 * w SymulacjaDostawyService). Self-invocation przez proxy Springa nie działa, stąd osobny bean. */
@Component
public class ZmianaStatusuDostawyWriter {

    private final PrzesylkaDostawyRepository przesylkaRepository;
    private final WpisStatusuDostawyRepository wpisRepository;

    public ZmianaStatusuDostawyWriter(PrzesylkaDostawyRepository przesylkaRepository,
                                       WpisStatusuDostawyRepository wpisRepository) {
        this.przesylkaRepository = przesylkaRepository;
        this.wpisRepository = wpisRepository;
    }

    @Transactional
    public PrzesylkaDostawyEntity zapisz(String przesylkaDostawyId, String nowyStatus, String szczegoly) {
        PrzesylkaDostawyEntity przesylka = przesylkaRepository.findById(przesylkaDostawyId)
                .orElseThrow(() -> new NotFoundException("Przesyłka dostawy o id " + przesylkaDostawyId + " nie istnieje."));

        OffsetDateTime teraz = OffsetDateTime.now();

        WpisStatusuDostawyEntity wpis = new WpisStatusuDostawyEntity();
        wpis.setId(UUID.randomUUID().toString());
        wpis.setPrzesylkaDostawyId(przesylkaDostawyId);
        wpis.setStatus(nowyStatus);
        wpis.setData(teraz);
        wpis.setSzczegoly((szczegoly == null || szczegoly.isBlank()) ? null : szczegoly);
        wpisRepository.save(wpis);

        przesylka.setStatus(nowyStatus);
        przesylka.setDataStatusu(teraz);
        przesylkaRepository.save(przesylka);

        return przesylka;
    }
}
