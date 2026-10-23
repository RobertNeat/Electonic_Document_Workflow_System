package robert_neat.delivery_mock.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import robert_neat.delivery_mock.dto.PrzesylkaDostawyZHistoria;
import robert_neat.delivery_mock.dto.UtworzPrzesylkeDostawyRequest;
import robert_neat.delivery_mock.entity.PrzesylkaDostawyEntity;
import robert_neat.delivery_mock.entity.WpisStatusuDostawyEntity;
import robert_neat.delivery_mock.exception.BadRequestException;
import robert_neat.delivery_mock.exception.NotFoundException;
import robert_neat.delivery_mock.mapper.DeliveryMapper;
import robert_neat.delivery_mock.repository.PrzesylkaDostawyRepository;
import robert_neat.delivery_mock.repository.WpisStatusuDostawyRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Odczyt listy/szczegółów przesyłek dostawy oraz rejestracja nowej przesyłki do symulacji
 * (REST API, punkt 2 zakresu zadania). Zmiana statusu istniejącej przesyłki (symulacja) jest w
 * SymulacjaDostawyService, bo wiąże się z integracją document_api, nie tylko z zapisem lokalnym. */
@Service
public class PrzesylkaDostawyService {

    /** Zbiór dozwolonych wartości kanału - odpowiada CHECK constraint `ck_przesylka_dostawy_kanal`. */
    public static final Set<String> KANALY = Set.of("e-doreczenia", "operator_pocztowy", "email");

    /** Zbiór dozwolonych statusów - odpowiada CHECK constraint na obu tabelach. */
    public static final Set<String> STATUSY = Set.of("nadane", "doreczone", "odebrane", "zwrot", "awizo");

    private final PrzesylkaDostawyRepository przesylkaRepository;
    private final WpisStatusuDostawyRepository wpisRepository;

    public PrzesylkaDostawyService(PrzesylkaDostawyRepository przesylkaRepository,
                                    WpisStatusuDostawyRepository wpisRepository) {
        this.przesylkaRepository = przesylkaRepository;
        this.wpisRepository = wpisRepository;
    }

    public List<PrzesylkaDostawyEntity> listaWszystkich() {
        return przesylkaRepository.findAll();
    }

    public PrzesylkaDostawyEntity getById(String id) {
        return przesylkaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Przesyłka dostawy o id " + id + " nie istnieje."));
    }

    public PrzesylkaDostawyZHistoria getSzczegoly(String id) {
        PrzesylkaDostawyEntity przesylka = getById(id);
        List<WpisStatusuDostawyEntity> historia = wpisRepository.findByPrzesylkaDostawyIdOrderByDataAsc(id);
        return new PrzesylkaDostawyZHistoria(
                DeliveryMapper.toDto(przesylka),
                historia.stream().map(DeliveryMapper::toDto).toList()
        );
    }

    @Transactional
    public PrzesylkaDostawyEntity rejestrujPrzesylke(UtworzPrzesylkeDostawyRequest request) {
        if (!KANALY.contains(request.kanal())) {
            throw new BadRequestException("Nieznany kanał: " + request.kanal());
        }
        if (przesylkaRepository.existsByNumerReferencyjny(request.numerReferencyjny())) {
            throw new BadRequestException("Przesyłka z numerem referencyjnym " + request.numerReferencyjny() + " już istnieje.");
        }

        OffsetDateTime teraz = OffsetDateTime.now();
        PrzesylkaDostawyEntity entity = new PrzesylkaDostawyEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setNumerReferencyjny(request.numerReferencyjny());
        entity.setKanal(request.kanal());
        entity.setAdresAde(request.adresAde());
        entity.setStatus("nadane");
        entity.setDataNadania(teraz);
        entity.setDataStatusu(teraz);
        przesylkaRepository.save(entity);

        WpisStatusuDostawyEntity wpis = new WpisStatusuDostawyEntity();
        wpis.setId(UUID.randomUUID().toString());
        wpis.setPrzesylkaDostawyId(entity.getId());
        wpis.setStatus("nadane");
        wpis.setData(teraz);
        wpisRepository.save(wpis);

        return entity;
    }
}
