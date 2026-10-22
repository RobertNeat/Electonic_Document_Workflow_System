package robert_neat.document_api.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.util.LinkedHashSet;
import java.util.Set;

/** Mapowanie tabeli `uzytkownik` + zbiór ról z `uzytkownik_rola` (PK złożony uzytkownik_id+rola,
 * stąd Set a nie List - duplikat roli w żądaniu nie może powodować naruszenia PK). Role przechowywane
 * jako string DB-value (CHECK ck_uzytkownik_rola: administrator/kancelaria/referent/kierownik/ksiegowosc),
 * konwertowane na RolaUzytkownika w mapperze. */
@Entity
@Table(name = "uzytkownik")
public class UzytkownikEntity {

    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String login;

    @Column(nullable = false)
    private String imie;

    @Column(nullable = false)
    private String nazwisko;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String wydzial;

    @Column(nullable = false)
    private String stanowisko;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "uzytkownik_rola", joinColumns = @JoinColumn(name = "uzytkownik_id"))
    @Column(name = "rola")
    private Set<String> role = new LinkedHashSet<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getWydzial() {
        return wydzial;
    }

    public void setWydzial(String wydzial) {
        this.wydzial = wydzial;
    }

    public String getStanowisko() {
        return stanowisko;
    }

    public void setStanowisko(String stanowisko) {
        this.stanowisko = stanowisko;
    }

    public Set<String> getRole() {
        return role;
    }

    public void setRole(Set<String> role) {
        this.role = role;
    }
}
