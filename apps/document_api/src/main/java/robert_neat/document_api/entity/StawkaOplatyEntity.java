package robert_neat.document_api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/** Mapowanie tabeli `stawka_oplaty`. */
@Entity
@Table(name = "stawka_oplaty")
public class StawkaOplatyEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String nazwa;

    @Column(nullable = false)
    private BigDecimal kwota;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNazwa() {
        return nazwa;
    }

    public void setNazwa(String nazwa) {
        this.nazwa = nazwa;
    }

    public BigDecimal getKwota() {
        return kwota;
    }

    public void setKwota(BigDecimal kwota) {
        this.kwota = kwota;
    }
}
