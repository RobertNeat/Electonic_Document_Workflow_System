package robert_neat.document_api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Mapowanie tabeli `dostawca_wysylki`. Pole `typ` przechowywane jako string DB-value
 * (CHECK ck_dostawca_wysylki_typ: e-doreczenia/operator_pocztowy/email), NIE @Enumerated(STRING). */
@Entity
@Table(name = "dostawca_wysylki")
public class DostawcaWysylkiEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String nazwa;

    @Column(nullable = false)
    private String typ;

    @Column(name = "operator_pocztowy")
    private String operatorPocztowy;

    private String email;

    @Column(nullable = false)
    private boolean aktywny;

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

    public String getTyp() {
        return typ;
    }

    public void setTyp(String typ) {
        this.typ = typ;
    }

    public String getOperatorPocztowy() {
        return operatorPocztowy;
    }

    public void setOperatorPocztowy(String operatorPocztowy) {
        this.operatorPocztowy = operatorPocztowy;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isAktywny() {
        return aktywny;
    }

    public void setAktywny(boolean aktywny) {
        this.aktywny = aktywny;
    }
}
