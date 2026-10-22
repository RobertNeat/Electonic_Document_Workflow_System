package robert_neat.document_api.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import robert_neat.document_api.dto.administracja.DostawcaWysylki;
import robert_neat.document_api.dto.administracja.PozycjaJrwa;
import robert_neat.document_api.dto.administracja.StawkaOplaty;
import robert_neat.document_api.dto.dokumenty.TypDokumentu;
import robert_neat.document_api.entity.DostawcaWysylkiEntity;
import robert_neat.document_api.entity.PozycjaJrwaEntity;
import robert_neat.document_api.entity.StawkaOplatyEntity;
import robert_neat.document_api.entity.TypDokumentuEntity;
import robert_neat.document_api.exception.BadRequestException;
import robert_neat.document_api.exception.NotFoundException;
import robert_neat.document_api.mapper.AdministracjaMapper;
import robert_neat.document_api.mapper.DokumentyMapper;
import robert_neat.document_api.repository.DostawcaWysylkiRepository;
import robert_neat.document_api.repository.PozycjaJrwaRepository;
import robert_neat.document_api.repository.StawkaOplatyRepository;
import robert_neat.document_api.repository.TypDokumentuRepository;

import java.math.BigDecimal;
import java.util.List;

/** Wszystkie słowniki administracyjne wystawiane pod /api/slowniki/* - jeden serwis, bo każda
 * operacja jest trywialnym get/update-by-key bez współdzielonej logiki biznesowej wymagającej
 * rozdzielenia na osobne klasy. typy-dokumentow reużywa TypDokumentuEntity/Repository z modułu
 * dokumenty (tylko do odczytu/aktualizacji nazwa+opis) - encja/repozytorium NIE są duplikowane. */
@Service
@Transactional(readOnly = true)
public class SlownikiService {

    private final PozycjaJrwaRepository pozycjaJrwaRepository;
    private final TypDokumentuRepository typDokumentuRepository;
    private final DostawcaWysylkiRepository dostawcaWysylkiRepository;
    private final StawkaOplatyRepository stawkaOplatyRepository;

    public SlownikiService(PozycjaJrwaRepository pozycjaJrwaRepository,
                            TypDokumentuRepository typDokumentuRepository,
                            DostawcaWysylkiRepository dostawcaWysylkiRepository,
                            StawkaOplatyRepository stawkaOplatyRepository) {
        this.pozycjaJrwaRepository = pozycjaJrwaRepository;
        this.typDokumentuRepository = typDokumentuRepository;
        this.dostawcaWysylkiRepository = dostawcaWysylkiRepository;
        this.stawkaOplatyRepository = stawkaOplatyRepository;
    }

    // --- jrwa ---

    public List<PozycjaJrwa> listaJrwa() {
        return pozycjaJrwaRepository.findAll().stream().map(AdministracjaMapper::toDto).toList();
    }

    @Transactional
    public PozycjaJrwa aktualizujJrwa(String symbol, PozycjaJrwa zmiany) {
        PozycjaJrwaEntity entity = pozycjaJrwaRepository.findById(symbol)
                .orElseThrow(() -> new NotFoundException("Pozycja JRWA o symbolu=" + symbol + " nie istnieje"));
        if (zmiany.haslo() == null || zmiany.haslo().isBlank()) {
            throw new BadRequestException("Hasło pozycji JRWA nie może być puste");
        }
        if (zmiany.kategoriaArchiwalna() == null) {
            throw new BadRequestException("Kategoria archiwalna jest wymagana");
        }
        entity.setHaslo(zmiany.haslo());
        entity.setKategoriaArchiwalna(zmiany.kategoriaArchiwalna().getWartosc());
        pozycjaJrwaRepository.save(entity);
        return AdministracjaMapper.toDto(entity);
    }

    // --- typy dokumentow ---

    public List<TypDokumentu> listaTypowDokumentow() {
        return typDokumentuRepository.findAll().stream().map(DokumentyMapper::toDto).toList();
    }

    @Transactional
    public TypDokumentu aktualizujTypDokumentu(String id, TypDokumentu zmiany) {
        TypDokumentuEntity entity = typDokumentuRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Typ dokumentu o id=" + id + " nie istnieje"));
        if (zmiany.nazwa() == null || zmiany.nazwa().isBlank()) {
            throw new BadRequestException("Nazwa typu dokumentu nie może być puste");
        }
        entity.setNazwa(zmiany.nazwa());
        entity.setOpis(zmiany.opis());
        typDokumentuRepository.save(entity);
        return DokumentyMapper.toDto(entity);
    }

    // --- dostawcy wysylki ---

    public List<DostawcaWysylki> listaDostawcowWysylki() {
        return dostawcaWysylkiRepository.findAll().stream().map(AdministracjaMapper::toDto).toList();
    }

    @Transactional
    public DostawcaWysylki aktualizujDostawceWysylki(String id, DostawcaWysylki zmiany) {
        DostawcaWysylkiEntity entity = dostawcaWysylkiRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Dostawca wysyłki o id=" + id + " nie istnieje"));
        if (zmiany.nazwa() == null || zmiany.nazwa().isBlank()) {
            throw new BadRequestException("Nazwa dostawcy wysyłki nie może być puste");
        }
        if (zmiany.typ() == null) {
            throw new BadRequestException("Typ dostawcy wysyłki jest wymagany");
        }
        entity.setNazwa(zmiany.nazwa());
        entity.setTyp(zmiany.typ().getWartosc());
        entity.setOperatorPocztowy(zmiany.operatorPocztowy());
        entity.setEmail(zmiany.email());
        entity.setAktywny(zmiany.aktywny());
        dostawcaWysylkiRepository.save(entity);
        return AdministracjaMapper.toDto(entity);
    }

    // --- stawki oplat ---

    public List<StawkaOplaty> listaStawekOplat() {
        return stawkaOplatyRepository.findAll().stream().map(AdministracjaMapper::toDto).toList();
    }

    @Transactional
    public StawkaOplaty aktualizujStawkeOplaty(String id, StawkaOplaty zmiany) {
        StawkaOplatyEntity entity = stawkaOplatyRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Stawka opłaty o id=" + id + " nie istnieje"));
        if (zmiany.nazwa() == null || zmiany.nazwa().isBlank()) {
            throw new BadRequestException("Nazwa stawki opłaty nie może być puste");
        }
        if (zmiany.kwota() == null || zmiany.kwota().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Kwota stawki opłaty musi być nieujemna");
        }
        entity.setNazwa(zmiany.nazwa());
        entity.setKwota(zmiany.kwota());
        stawkaOplatyRepository.save(entity);
        return AdministracjaMapper.toDto(entity);
    }
}
