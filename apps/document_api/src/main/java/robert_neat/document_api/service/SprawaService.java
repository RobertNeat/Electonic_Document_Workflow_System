package robert_neat.document_api.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import robert_neat.document_api.dto.administracja.EdytujDefinicjeObieguRequest;
import robert_neat.document_api.dto.sprawy.DefinicjaObiegu;
import robert_neat.document_api.dto.sprawy.Sprawa;
import robert_neat.document_api.dto.sprawy.UtworzSpraweRequest;
import robert_neat.document_api.dto.sprawy.WpisMetryki;
import robert_neat.document_api.dto.sprawy.ZmienStanSprawyRequest;
import robert_neat.document_api.dto.wspolne.StronaWynikow;
import robert_neat.document_api.entity.DefinicjaObieguEntity;
import robert_neat.document_api.entity.ObywatelEntity;
import robert_neat.document_api.entity.PrzejscieObieguEntity;
import robert_neat.document_api.entity.SprawaEntity;
import robert_neat.document_api.entity.WpisMetrykiEntity;
import robert_neat.document_api.exception.BadRequestException;
import robert_neat.document_api.exception.IllegalStateTransitionException;
import robert_neat.document_api.exception.NotFoundException;
import robert_neat.document_api.mapper.SprawyMapper;
import robert_neat.document_api.repository.DefinicjaObieguRepository;
import robert_neat.document_api.repository.ObywatelRepository;
import robert_neat.document_api.repository.PrzejscieObieguRepository;
import robert_neat.document_api.repository.SprawaRepository;
import robert_neat.document_api.repository.WpisMetrykiRepository;

import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional(readOnly = true)
public class SprawaService {

    private static final Pattern NUMER_PATTERN = Pattern.compile("^[^.]+\\.[^.]+\\.(\\d+)\\.\\d+$");

    private final SprawaRepository sprawaRepository;
    private final ObywatelRepository obywatelRepository;
    private final DefinicjaObieguRepository definicjaObieguRepository;
    private final PrzejscieObieguRepository przejscieObieguRepository;
    private final WpisMetrykiRepository wpisMetrykiRepository;

    public SprawaService(SprawaRepository sprawaRepository,
                          ObywatelRepository obywatelRepository,
                          DefinicjaObieguRepository definicjaObieguRepository,
                          PrzejscieObieguRepository przejscieObieguRepository,
                          WpisMetrykiRepository wpisMetrykiRepository) {
        this.sprawaRepository = sprawaRepository;
        this.obywatelRepository = obywatelRepository;
        this.definicjaObieguRepository = definicjaObieguRepository;
        this.przejscieObieguRepository = przejscieObieguRepository;
        this.wpisMetrykiRepository = wpisMetrykiRepository;
    }

    public StronaWynikow<Sprawa> szukaj(String stan, String wydzial, String q, String obywatelId, int strona, int rozmiarStrony) {
        Specification<SprawaEntity> spec = Specification.allOf(
                SprawaSpecifications.stanEquals(stan),
                SprawaSpecifications.wydzialEquals(wydzial),
                SprawaSpecifications.znakOrTytulContains(q),
                SprawaSpecifications.obywatelIdEquals(obywatelId)
        );
        Pageable pageable = PageRequest.of(Math.max(strona, 0), Math.max(rozmiarStrony, 1));
        Page<SprawaEntity> page = sprawaRepository.findAll(spec, pageable);
        List<Sprawa> dane = page.getContent().stream().map(SprawyMapper::toDto).toList();
        return new StronaWynikow<>(dane, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    public Sprawa getById(String id) {
        return SprawyMapper.toDto(getEntityById(id));
    }

    private SprawaEntity getEntityById(String id) {
        return sprawaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Sprawa o id=" + id + " nie istnieje"));
    }

    public List<DefinicjaObiegu> listaDefinicjiObiegow() {
        return definicjaObieguRepository.findAll().stream()
                .map(def -> SprawyMapper.toDto(def, przejscieObieguRepository.findByDefinicjaObiegu_Id(def.getId())))
                .toList();
    }

    /** Edycja ograniczona do nazwy/opisu (zgodnie z kontraktem administracja.md) - stany/przejścia/role
     * pozostają niezmienione, więc wystarczy zmodyfikować encję bez przebudowy kolekcji stanów. */
    @Transactional
    public DefinicjaObiegu edytujDefinicjeObiegu(String id, EdytujDefinicjeObieguRequest request) {
        DefinicjaObieguEntity definicja = definicjaObieguRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Definicja obiegu o id=" + id + " nie istnieje"));
        definicja.setNazwa(request.nazwa());
        definicja.setOpis(request.opis());
        definicjaObieguRepository.save(definicja);
        return SprawyMapper.toDto(definicja, przejscieObieguRepository.findByDefinicjaObiegu_Id(id));
    }

    public List<WpisMetryki> pobierzMetryke(String sprawaId) {
        // 404 jeśli sprawa nie istnieje, zamiast cichej puste listy.
        getEntityById(sprawaId);
        return wpisMetrykiRepository.findBySprawa_IdOrderByDataAsc(sprawaId).stream()
                .map(SprawyMapper::toDto)
                .toList();
    }

    @Transactional
    public Sprawa rejestrujSprawe(UtworzSpraweRequest request) {
        DefinicjaObieguEntity definicja = definicjaObieguRepository.findById(request.definicjaObieguId())
                .orElseThrow(() -> new NotFoundException("Definicja obiegu o id=" + request.definicjaObieguId() + " nie istnieje"));
        ObywatelEntity obywatel = obywatelRepository.findById(request.obywatelId())
                .orElseThrow(() -> new NotFoundException("Obywatel o id=" + request.obywatelId() + " nie istnieje"));

        String symbol = WydzialSymbolDictionary.symbolFor(request.wydzial());
        if (symbol == null) {
            throw new BadRequestException("Nieznany wydział: " + request.wydzial() + " - brak symbolu w słowniku backendu");
        }

        int rok = Year.now().getValue();
        String znakSprawy = generujZnakSprawy(symbol, request.jrwa(), rok);

        SprawaEntity sprawa = new SprawaEntity();
        sprawa.setId(UUID.randomUUID().toString());
        sprawa.setZnakSprawy(znakSprawy);
        sprawa.setTytul(request.tytul());
        sprawa.setJrwa(request.jrwa());
        sprawa.setDefinicjaObiegu(definicja);
        sprawa.setStan(definicja.getStanPoczatkowy());
        sprawa.setObywatel(obywatel);
        sprawa.setWydzial(request.wydzial());
        sprawa.setReferent(request.referent());
        sprawa.setDataWszczecia(LocalDate.now());
        sprawa.setTerminUstawowy(request.terminUstawowy());
        sprawa.setKategoriaArchiwalna(request.kategoriaArchiwalna().getWartosc());
        sprawa.setZamknieta(false);
        sprawaRepository.save(sprawa);

        WpisMetrykiEntity wpis = new WpisMetrykiEntity();
        wpis.setId(UUID.randomUUID().toString());
        wpis.setSprawa(sprawa);
        wpis.setData(LocalDate.now());
        wpis.setCzynnosc("Rejestracja sprawy");
        wpis.setOsoba(request.referent());
        wpis.setOpis("Nadano znak sprawy " + znakSprawy);
        wpisMetrykiRepository.save(wpis);

        return SprawyMapper.toDto(sprawa);
    }

    /** Numer = max(NUMER istniejących znaków tego wydziału+jrwa+roku) + 1, nie licznik wierszy -
     * licznik wierszy psułby się przy nieciągłych numerach w seedzie (złamanie unique constraint). */
    private String generujZnakSprawy(String symbol, String jrwa, int rok) {
        String prefix = symbol + "." + jrwa + ".%." + rok;
        List<String> istniejace = sprawaRepository.findZnakiSprawyByPrefix(prefix);
        int maxNumer = 0;
        for (String znak : istniejace) {
            Matcher m = NUMER_PATTERN.matcher(znak);
            if (m.matches()) {
                int numer = Integer.parseInt(m.group(1));
                if (numer > maxNumer) {
                    maxNumer = numer;
                }
            }
        }
        return symbol + "." + jrwa + "." + (maxNumer + 1) + "." + rok;
    }

    @Transactional
    public Sprawa zmienStan(String sprawaId, ZmienStanSprawyRequest request, String osoba) {
        SprawaEntity sprawa = getEntityById(sprawaId);
        String staryStan = sprawa.getStan();
        String nowyStan = request.nowyStan();

        PrzejscieObieguEntity przejscie = przejscieObieguRepository
                .findZeStanu(sprawa.getDefinicjaObiegu().getId(), staryStan)
                .orElseThrow(() -> new IllegalStateTransitionException(
                        "Brak zdefiniowanych przejść ze stanu '" + staryStan + "' w obiegu " + sprawa.getDefinicjaObiegu().getId()));

        if (!przejscie.getDoStanow().contains(nowyStan)) {
            throw new IllegalStateTransitionException(
                    "Niedozwolone przejście: '" + staryStan + "' -> '" + nowyStan + "'");
        }

        sprawa.setStan(nowyStan);
        List<String> stany = sprawa.getDefinicjaObiegu().getStany();
        boolean jestOstatnim = !stany.isEmpty() && stany.get(stany.size() - 1).equals(nowyStan);
        if (jestOstatnim) {
            sprawa.setZamknieta(true);
        }

        WpisMetrykiEntity wpis = new WpisMetrykiEntity();
        wpis.setId(UUID.randomUUID().toString());
        wpis.setSprawa(sprawa);
        wpis.setData(LocalDate.now());
        wpis.setCzynnosc("Zmiana stanu: " + staryStan + " → " + nowyStan);
        wpis.setOsoba(osoba);
        wpisMetrykiRepository.save(wpis);

        return SprawyMapper.toDto(sprawa);
    }
}
