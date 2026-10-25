package robert_neat.document_api.dto.dokumenty;

import java.time.LocalDate;

/** Metadane certyfikatu pobrane z sign_service (proxy, zob. SignServiceClient.listaCertyfikatow) -
 * pola 1:1 z CertyfikatDto tego serwisu, bez certyfikat_der/keystore_pkcs12. */
public record Certyfikat(
        String id,
        String podmiot,
        String numerSeryjny,
        LocalDate dataWydania,
        LocalDate dataWygasniecia,
        String status,
        String wydawca
) {
}
