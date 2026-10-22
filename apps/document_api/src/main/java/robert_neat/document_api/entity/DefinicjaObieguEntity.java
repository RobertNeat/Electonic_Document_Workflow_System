package robert_neat.document_api.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

/** Mapowanie tabeli `definicja_obiegu` + lista stanów z `definicja_obiegu_stan` (porządek wg `pozycja`). */
@Entity
@Table(name = "definicja_obiegu")
public class DefinicjaObieguEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String nazwa;

    private String opis;

    @Column(name = "stan_poczatkowy", nullable = false)
    private String stanPoczatkowy;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "definicja_obiegu_stan", joinColumns = @JoinColumn(name = "definicja_obiegu_id"))
    @OrderColumn(name = "pozycja")
    @Column(name = "stan")
    private List<String> stany = new ArrayList<>();

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

    public String getStanPoczatkowy() {
        return stanPoczatkowy;
    }

    public void setStanPoczatkowy(String stanPoczatkowy) {
        this.stanPoczatkowy = stanPoczatkowy;
    }

    public List<String> getStany() {
        return stany;
    }

    public void setStany(List<String> stany) {
        this.stany = stany;
    }
}
