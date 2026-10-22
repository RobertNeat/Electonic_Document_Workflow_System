package robert_neat.document_api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/** Mapowanie tabeli `akcja_zbiorowa`. typ_akcji/typ_celu/status to surowe wartości CHECK constraint
 * - w przeciwieństwie do większości innych encji w tym module, typ_akcji/typ_celu NIE są konwertowane
 * między wielkością liter: CHECK (zob. 010-akcje-zbiorowe.xml) wprost wymaga nazw stałych enuma Java
 * (np. 'ZMIANA_STANU'), bez @JsonValue po stronie DTO. status natomiast jest małymi literami
 * ('trwajaca'/'zakonczona'), tak jak StatusWykonaniaAkcji.getWartosc(). kryteria/parametry to kolumny
 * jsonb - przechowywane jako pre-zserializowany JSON (String), żeby uniknąć zależności od
 * automatycznego mapowania Hibernate 7 na Map/Object i mieć pełną kontrolę nad (de)serializacją
 * przez wstrzykiwany tools.jackson.databind.ObjectMapper w serwisie/konsumencie. */
@Entity
@Table(name = "akcja_zbiorowa")
public class AkcjaZbiorowaEntity {

    @Id
    private String id;

    @Column(name = "typ_akcji", nullable = false)
    private String typAkcji;

    @Column(name = "typ_celu", nullable = false)
    private String typCelu;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String kryteria;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private String parametry;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private int postep;

    @Column(nullable = false)
    private int lacznie;

    @Column(nullable = false)
    private int przetworzone;

    @Column(nullable = false)
    private int bledne;

    @Column(name = "data_utworzenia", nullable = false)
    private OffsetDateTime dataUtworzenia;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTypAkcji() {
        return typAkcji;
    }

    public void setTypAkcji(String typAkcji) {
        this.typAkcji = typAkcji;
    }

    public String getTypCelu() {
        return typCelu;
    }

    public void setTypCelu(String typCelu) {
        this.typCelu = typCelu;
    }

    public String getKryteria() {
        return kryteria;
    }

    public void setKryteria(String kryteria) {
        this.kryteria = kryteria;
    }

    public String getParametry() {
        return parametry;
    }

    public void setParametry(String parametry) {
        this.parametry = parametry;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getPostep() {
        return postep;
    }

    public void setPostep(int postep) {
        this.postep = postep;
    }

    public int getLacznie() {
        return lacznie;
    }

    public void setLacznie(int lacznie) {
        this.lacznie = lacznie;
    }

    public int getPrzetworzone() {
        return przetworzone;
    }

    public void setPrzetworzone(int przetworzone) {
        this.przetworzone = przetworzone;
    }

    public int getBledne() {
        return bledne;
    }

    public void setBledne(int bledne) {
        this.bledne = bledne;
    }

    public OffsetDateTime getDataUtworzenia() {
        return dataUtworzenia;
    }

    public void setDataUtworzenia(OffsetDateTime dataUtworzenia) {
        this.dataUtworzenia = dataUtworzenia;
    }
}
