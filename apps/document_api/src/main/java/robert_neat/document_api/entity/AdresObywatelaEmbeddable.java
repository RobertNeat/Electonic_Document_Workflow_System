package robert_neat.document_api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Zagnieżdżony adres obywatela - mapowany na kolumny adres_* tabeli obywatel. */
@Embeddable
public class AdresObywatelaEmbeddable {

    @Column(name = "adres_ulica")
    private String ulica;

    @Column(name = "adres_numer_domu")
    private String numerDomu;

    @Column(name = "adres_numer_lokalu")
    private String numerLokalu;

    @Column(name = "adres_kod_pocztowy")
    private String kodPocztowy;

    @Column(name = "adres_miejscowosc")
    private String miejscowosc;

    public String getUlica() {
        return ulica;
    }

    public void setUlica(String ulica) {
        this.ulica = ulica;
    }

    public String getNumerDomu() {
        return numerDomu;
    }

    public void setNumerDomu(String numerDomu) {
        this.numerDomu = numerDomu;
    }

    public String getNumerLokalu() {
        return numerLokalu;
    }

    public void setNumerLokalu(String numerLokalu) {
        this.numerLokalu = numerLokalu;
    }

    public String getKodPocztowy() {
        return kodPocztowy;
    }

    public void setKodPocztowy(String kodPocztowy) {
        this.kodPocztowy = kodPocztowy;
    }

    public String getMiejscowosc() {
        return miejscowosc;
    }

    public void setMiejscowosc(String miejscowosc) {
        this.miejscowosc = miejscowosc;
    }
}
