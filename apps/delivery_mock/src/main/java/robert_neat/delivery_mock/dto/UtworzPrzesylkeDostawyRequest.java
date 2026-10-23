package robert_neat.delivery_mock.dto;

import jakarta.validation.constraints.NotBlank;

/** Wejście dla `POST /api/przesylki-dostawy` - rejestracja nowej przesyłki do symulacji.
 * Status początkowy zawsze "nadane", data_nadania/data_statusu = now() - ustawiane przez serwis,
 * nie przyjmowane od klienta. */
public record UtworzPrzesylkeDostawyRequest(
        @NotBlank String numerReferencyjny,
        @NotBlank String kanal,
        String adresAde
) {
}
