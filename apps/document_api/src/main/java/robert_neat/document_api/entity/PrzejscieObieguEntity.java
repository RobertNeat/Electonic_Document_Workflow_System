package robert_neat.document_api.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapowanie tabeli `przejscie_obiegu` + join-tables `przejscie_obiegu_do_stanu` i
 * `przejscie_obiegu_wymagany_typ_dokumentu` jako @ElementCollection (proste tabele łączące, bez
 * potrzeby osobnych encji).
 */
@Entity
@Table(name = "przejscie_obiegu")
public class PrzejscieObieguEntity {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "definicja_obiegu_id", nullable = false)
    private DefinicjaObieguEntity definicjaObiegu;

    // Nazwa pola celowo bez wielbłądziej notacji po "z" ("zstanu" nie "zStanu") - JavaBean
    // introspection (Introspector.decapitalize) dla gettera getZStanu() dawał property "ZStanu"
    // (dwie wielkie litery po "get"), co psuło JPQL/Spring Data derived queries na tym polu.
    @Column(name = "z_stanu", nullable = false)
    private String zstanu;

    @Column(name = "wymagana_rola")
    private String wymaganaRola;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "przejscie_obiegu_do_stanu", joinColumns = @JoinColumn(name = "przejscie_obiegu_id"))
    @Column(name = "do_stanu")
    private List<String> doStanow = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "przejscie_obiegu_wymagany_typ_dokumentu", joinColumns = @JoinColumn(name = "przejscie_obiegu_id"))
    @Column(name = "typ_dokumentu_id")
    private List<String> wymaganeTypyDokumentow = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public DefinicjaObieguEntity getDefinicjaObiegu() {
        return definicjaObiegu;
    }

    public void setDefinicjaObiegu(DefinicjaObieguEntity definicjaObiegu) {
        this.definicjaObiegu = definicjaObiegu;
    }

    public String getZstanu() {
        return zstanu;
    }

    public void setZstanu(String zstanu) {
        this.zstanu = zstanu;
    }

    public String getWymaganaRola() {
        return wymaganaRola;
    }

    public void setWymaganaRola(String wymaganaRola) {
        this.wymaganaRola = wymaganaRola;
    }

    public List<String> getDoStanow() {
        return doStanow;
    }

    public void setDoStanow(List<String> doStanow) {
        this.doStanow = doStanow;
    }

    public List<String> getWymaganeTypyDokumentow() {
        return wymaganeTypyDokumentow;
    }

    public void setWymaganeTypyDokumentow(List<String> wymaganeTypyDokumentow) {
        this.wymaganeTypyDokumentow = wymaganeTypyDokumentow;
    }
}
