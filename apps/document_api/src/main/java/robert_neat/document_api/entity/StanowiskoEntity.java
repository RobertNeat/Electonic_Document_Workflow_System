package robert_neat.document_api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Mapowanie tabeli `stanowisko`. */
@Entity
@Table(name = "stanowisko")
public class StanowiskoEntity {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wydzial_id", nullable = false)
    private WydzialEntity wydzial;

    @Column(nullable = false)
    private String nazwa;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public WydzialEntity getWydzial() {
        return wydzial;
    }

    public void setWydzial(WydzialEntity wydzial) {
        this.wydzial = wydzial;
    }

    public String getNazwa() {
        return nazwa;
    }

    public void setNazwa(String nazwa) {
        this.nazwa = nazwa;
    }
}
