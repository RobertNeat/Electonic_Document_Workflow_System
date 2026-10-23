package robert_neat.sign_service.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import robert_neat.sign_service.entity.CertyfikatEntity;
import robert_neat.sign_service.exception.NotFoundException;
import robert_neat.sign_service.repository.CertyfikatRepository;

import java.util.List;

@Service
public class CertyfikatService {

    private final CertyfikatRepository certyfikatRepository;

    public CertyfikatService(CertyfikatRepository certyfikatRepository) {
        this.certyfikatRepository = certyfikatRepository;
    }

    @Transactional(readOnly = true)
    public List<CertyfikatEntity> getAll() {
        return certyfikatRepository.findAll();
    }

    @Transactional(readOnly = true)
    public CertyfikatEntity getById(String id) {
        return certyfikatRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Certyfikat o id " + id + " nie istnieje"));
    }
}
