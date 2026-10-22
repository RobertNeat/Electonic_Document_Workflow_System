package robert_neat.document_api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Mapowanie tabeli `obywatel`. Pole `typ` przechowywane jako string DB-value ('osoba_fizyczna'/'podmiot'). */
@Entity
@Table(name = "obywatel")
public class ObywatelEntity {

    @Id
    private String id;

    /** Wartość zgodna z CHECK constraint (ck_obywatel_typ): 'osoba_fizyczna' lub 'podmiot'. */
    @Column(name = "typ", nullable = false)
    private String typ;

    private String imie;

    private String nazwisko;

    @Column(name = "nazwa_podmiotu")
    private String nazwaPodmiotu;

    private String pesel;

    private String nip;

    @Column(name = "adres_doreczen_ede")
    private String adresDoreczenEDE;

    private String email;

    private String telefon;

    @Embedded
    private AdresObywatelaEmbeddable adres = new AdresObywatelaEmbeddable();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTyp() {
        return typ;
    }

    public void setTyp(String typ) {
        this.typ = typ;
    }

    public String getImie() {
        return imie;
    }

    public void setImie(String imie) {
        this.imie = imie;
    }

    public String getNazwisko() {
        return nazwisko;
    }

    public void setNazwisko(String nazwisko) {
        this.nazwisko = nazwisko;
    }

    public String getNazwaPodmiotu() {
        return nazwaPodmiotu;
    }

    public void setNazwaPodmiotu(String nazwaPodmiotu) {
        this.nazwaPodmiotu = nazwaPodmiotu;
    }

    public String getPesel() {
        return pesel;
    }

    public void setPesel(String pesel) {
        this.pesel = pesel;
    }

    public String getNip() {
        return nip;
    }

    public void setNip(String nip) {
        this.nip = nip;
    }

    public String getAdresDoreczenEDE() {
        return adresDoreczenEDE;
    }

    public void setAdresDoreczenEDE(String adresDoreczenEDE) {
        this.adresDoreczenEDE = adresDoreczenEDE;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefon() {
        return telefon;
    }

    public void setTelefon(String telefon) {
        this.telefon = telefon;
    }

    public AdresObywatelaEmbeddable getAdres() {
        return adres;
    }

    public void setAdres(AdresObywatelaEmbeddable adres) {
        this.adres = adres;
    }
}
