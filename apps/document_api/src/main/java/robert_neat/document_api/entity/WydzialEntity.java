package robert_neat.document_api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Mapowanie tabeli `wydzial`. Stanowiska dociągane osobnym zapytaniem w serwisie
 * (StrukturaOrganizacyjnaService), a nie przez @OneToMany, żeby uniknąć N+1 bez LAZY-fetch-join. */
@Entity
@Table(name = "wydzial")
public class WydzialEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String nazwa;

    private String kierownik;

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

    public String getKierownik() {
        return kierownik;
    }

    public void setKierownik(String kierownik) {
        this.kierownik = kierownik;
    }
}
