package robert_neat.delivery_mock.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/** Mapowanie tabeli `wpis_statusu_dostawy` - historia statusów przesyłki dostawy.
 * `przesylkaDostawyId` jest płaskim varchar (FK w bazie, ale bez @ManyToOne po stronie encji -
 * historia jest zawsze odczytywana przez repository po tym id, nie przez relację JPA). */
@Entity
@Table(name = "wpis_statusu_dostawy")
public class WpisStatusuDostawyEntity {

    @Id
    private String id;

    @Column(name = "przesylka_dostawy_id", nullable = false)
    private String przesylkaDostawyId;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private OffsetDateTime data;

    @Column
    private String szczegoly;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPrzesylkaDostawyId() {
        return przesylkaDostawyId;
    }

    public void setPrzesylkaDostawyId(String przesylkaDostawyId) {
        this.przesylkaDostawyId = przesylkaDostawyId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getData() {
        return data;
    }

    public void setData(OffsetDateTime data) {
        this.data = data;
    }

    public String getSzczegoly() {
        return szczegoly;
    }

    public void setSzczegoly(String szczegoly) {
        this.szczegoly = szczegoly;
    }
}
