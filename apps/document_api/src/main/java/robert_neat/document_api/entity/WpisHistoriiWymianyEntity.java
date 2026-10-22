package robert_neat.document_api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/** Mapowanie tabeli `wpis_historii_wymiany` - jedna wspólna tabela dla historii importu/eksportu
 * XML (komunikaty spraw) i UBL (faktury), rozróżniana kolumną typWymiany. Pola typWymiany/kierunek/status
 * przechowywane jako string DB-value (CHECK constraints), NIE @Enumerated(STRING) - patrz analogiczna
 * uwaga w NaleznoscEntity. */
@Entity
@Table(name = "wpis_historii_wymiany")
public class WpisHistoriiWymianyEntity {

    @Id
    private String id;

    @Column(name = "typ_wymiany", nullable = false)
    private String typWymiany;

    @Column(name = "nazwa_pliku", nullable = false)
    private String nazwaPliku;

    @Column(nullable = false)
    private String kierunek;

    @Column(nullable = false)
    private OffsetDateTime data;

    @Column(nullable = false)
    private String status;

    @Column(name = "liczba_rekordow", nullable = false)
    private int liczbaRekordow;

    private String opis;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTypWymiany() {
        return typWymiany;
    }

    public void setTypWymiany(String typWymiany) {
        this.typWymiany = typWymiany;
    }

    public String getNazwaPliku() {
        return nazwaPliku;
    }

    public void setNazwaPliku(String nazwaPliku) {
        this.nazwaPliku = nazwaPliku;
    }

    public String getKierunek() {
        return kierunek;
    }

    public void setKierunek(String kierunek) {
        this.kierunek = kierunek;
    }

    public OffsetDateTime getData() {
        return data;
    }

    public void setData(OffsetDateTime data) {
        this.data = data;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getLiczbaRekordow() {
        return liczbaRekordow;
    }

    public void setLiczbaRekordow(int liczbaRekordow) {
        this.liczbaRekordow = liczbaRekordow;
    }

    public String getOpis() {
        return opis;
    }

    public void setOpis(String opis) {
        this.opis = opis;
    }
}
