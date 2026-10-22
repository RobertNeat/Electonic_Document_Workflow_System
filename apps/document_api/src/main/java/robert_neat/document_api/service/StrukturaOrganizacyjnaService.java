package robert_neat.document_api.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import robert_neat.document_api.dto.administracja.Stanowisko;
import robert_neat.document_api.dto.administracja.Wydzial;
import robert_neat.document_api.entity.StanowiskoEntity;
import robert_neat.document_api.entity.WydzialEntity;
import robert_neat.document_api.mapper.AdministracjaMapper;
import robert_neat.document_api.repository.StanowiskoRepository;
import robert_neat.document_api.repository.WydzialRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class StrukturaOrganizacyjnaService {

    private final WydzialRepository wydzialRepository;
    private final StanowiskoRepository stanowiskoRepository;

    public StrukturaOrganizacyjnaService(WydzialRepository wydzialRepository, StanowiskoRepository stanowiskoRepository) {
        this.wydzialRepository = wydzialRepository;
        this.stanowiskoRepository = stanowiskoRepository;
    }

    /** Dwa niezależne zapytania + grupowanie w pamięci zamiast @OneToMany + fetch-join - unika N+1
     * bez ryzyka kartezjańskiego produktu przy dalszym rozszerzaniu wydziału o inne kolekcje. */
    public List<Wydzial> lista() {
        List<WydzialEntity> wydzialy = wydzialRepository.findAll(Sort.by("id"));
        List<StanowiskoEntity> stanowiska = stanowiskoRepository.findAll(Sort.by("id"));

        Map<String, List<Stanowisko>> stanowiskaPoWydziale = new LinkedHashMap<>();
        for (StanowiskoEntity s : stanowiska) {
            stanowiskaPoWydziale
                    .computeIfAbsent(s.getWydzial().getId(), k -> new java.util.ArrayList<>())
                    .add(AdministracjaMapper.toDto(s));
        }

        return wydzialy.stream()
                .map(w -> AdministracjaMapper.toDto(w, stanowiskaPoWydziale.getOrDefault(w.getId(), List.of())))
                .toList();
    }
}
