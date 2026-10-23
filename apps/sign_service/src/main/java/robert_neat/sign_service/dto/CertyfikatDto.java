package robert_neat.sign_service.dto;

import java.time.LocalDate;

/** Metadane certyfikatu bez certyfikat_der/keystore_pkcs12 - szczegoly wewnetrzne nieujawniane przez API. */
public record CertyfikatDto(
        String id,
        String podmiot,
        String numerSeryjny,
        LocalDate dataWydania,
        LocalDate dataWygasniecia,
        String status,
        String wydawca
) {
}
