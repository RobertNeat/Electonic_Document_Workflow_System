package robert_neat.document_api.dto.obywatele;

public record Obywatel(
        String id,
        TypObywatela typ,
        String imie,
        String nazwisko,
        String nazwaPodmiotu,
        String pesel,
        String nip,
        String adresDoreczenEDE,
        String email,
        String telefon,
        AdresObywatela adres
) {
}
