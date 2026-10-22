package robert_neat.document_api.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import robert_neat.document_api.dto.obywatele.Obywatel;
import robert_neat.document_api.entity.ObywatelEntity;
import robert_neat.document_api.exception.NotFoundException;
import robert_neat.document_api.mapper.ObywatelMapper;
import robert_neat.document_api.repository.ObywatelRepository;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ObywatelService {

    private final ObywatelRepository obywatelRepository;

    public ObywatelService(ObywatelRepository obywatelRepository) {
        this.obywatelRepository = obywatelRepository;
    }

    /** Wyszukiwanie częściowe case-insensitive po imieniu/nazwisku/nazwie podmiotu/pesel/nip. Brak
     * zapytania -> pełna lista (kontrakt nie wymaga paginacji dla tego endpointu). */
    public List<Obywatel> search(String query) {
        List<ObywatelEntity> wynik = (query == null || query.isBlank())
                ? obywatelRepository.findAll()
                : obywatelRepository.search(query.trim());
        return wynik.stream().map(ObywatelMapper::toDto).toList();
    }

    public Obywatel getById(String id) {
        ObywatelEntity entity = obywatelRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Obywatel o id=" + id + " nie istnieje"));
        return ObywatelMapper.toDto(entity);
    }
}
