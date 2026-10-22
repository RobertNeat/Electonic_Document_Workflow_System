package robert_neat.document_api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Mapowanie tabeli `typ_dokumentu`. */
@Entity
@Table(name = "typ_dokumentu")
public class TypDokumentuEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String nazwa;

    @Column
    private String opis;

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

    public String getOpis() {
        return opis;
    }

    public void setOpis(String opis) {
        this.opis = opis;
    }
}
