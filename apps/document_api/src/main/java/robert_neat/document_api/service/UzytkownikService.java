package robert_neat.document_api.service;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import robert_neat.document_api.dto.administracja.AktualizujRoleUzytkownikaRequest;
import robert_neat.document_api.dto.administracja.RolaUzytkownika;
import robert_neat.document_api.dto.administracja.UtworzUzytkownikaRequest;
import robert_neat.document_api.dto.administracja.Uzytkownik;
import robert_neat.document_api.entity.UzytkownikEntity;
import robert_neat.document_api.exception.BadRequestException;
import robert_neat.document_api.exception.NotFoundException;
import robert_neat.document_api.mapper.AdministracjaMapper;
import robert_neat.document_api.repository.UzytkownikRepository;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UzytkownikService {

    private final UzytkownikRepository uzytkownikRepository;

    public UzytkownikService(UzytkownikRepository uzytkownikRepository) {
        this.uzytkownikRepository = uzytkownikRepository;
    }

    public List<Uzytkownik> lista(String wydzial, RolaUzytkownika rola) {
        String rolaDbValue = rola == null ? null : AdministracjaMapper.rolaToDb(rola);
        Specification<UzytkownikEntity> spec = Specification.allOf(
                UzytkownikSpecifications.wydzialEquals(wydzial),
                UzytkownikSpecifications.maRole(rolaDbValue)
        );
        return uzytkownikRepository.findAll(spec).stream()
                .map(AdministracjaMapper::toDto)
                .toList();
    }

    @Transactional
    public Uzytkownik utworz(UtworzUzytkownikaRequest request) {
        if (uzytkownikRepository.existsByLogin(request.login())) {
            throw new BadRequestException("Użytkownik o loginie '" + request.login() + "' już istnieje");
        }

        UzytkownikEntity entity = new UzytkownikEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setLogin(request.login());
        entity.setImie(request.imie());
        entity.setNazwisko(request.nazwisko());
        entity.setEmail(request.email());
        entity.setWydzial(request.wydzial());
        entity.setStanowisko(request.stanowisko());
        entity.setRole(AdministracjaMapper.roleToDb(request.role()));
        uzytkownikRepository.save(entity);

        return AdministracjaMapper.toDto(entity);
    }

    @Transactional
    public Uzytkownik aktualizujRole(String id, AktualizujRoleUzytkownikaRequest request) {
        UzytkownikEntity entity = uzytkownikRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Użytkownik o id=" + id + " nie istnieje"));
        entity.setRole(AdministracjaMapper.roleToDb(request.role()));
        uzytkownikRepository.save(entity);
        return AdministracjaMapper.toDto(entity);
    }
}
