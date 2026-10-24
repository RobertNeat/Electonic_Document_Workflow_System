package robert_neat.sign_service;

import org.springframework.http.HttpStatus;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import robert_neat.sign_service.revocation.CrlResponderService;
import robert_neat.sign_service.revocation.OcspResponderService;

import java.io.IOException;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;

/**
 * Domyslne stuby MockRestServiceServer delegujace do PRAWDZIWEJ logiki respondera (zob.
 * OcspResponderService/CrlResponderService) - zamiast zaszyc fikcyjna odpowiedz DER, kazde
 * zadanie testowe faktycznie przechodzi przez logike OCSP/CRL respondera dzialajaca na tej samej
 * bazie testowej (`uniewaznienie`), tylko bez realnego socketu HTTP. Dzieki temu istniejace testy
 * PAdES/XAdES (cert-1 "poprawny", cert-3 "certyfikat_uniewazniony") nadal dostaja odpowiedzi
 * zgodne z rzeczywistym stanem bazy, nie z zahardkodowanym "good".
 */
final class RevocationStubs {

    private RevocationStubs() {
    }

    /** Rejestruje domyslne (wielokrotne) oczekiwania na adres OCSP/CRL respondera tego samego
     * serwisu (zob. PkiProperties.publicBaseUrl) - wywolaj w @BeforeEach PO server.reset(). */
    static void stubujDomyslneOdpowiedzi(MockRestServiceServer server, String publicBaseUrl,
                                           OcspResponderService ocspResponderService,
                                           CrlResponderService crlResponderService) {
        server.expect(ExpectedCount.manyTimes(), requestTo(publicBaseUrl + "/api/ocsp"))
                .andExpect(method(POST))
                .andRespond(delegujDoOcspRespondera(ocspResponderService));

        server.expect(ExpectedCount.manyTimes(), requestTo(publicBaseUrl + "/api/crl/issuing-ca.crl"))
                .andExpect(method(GET))
                .andRespond(delegujDoCrlRespondera(crlResponderService));
    }

    private static ResponseCreator delegujDoOcspRespondera(OcspResponderService ocspResponderService) {
        return request -> {
            byte[] requestBody = ((org.springframework.mock.http.client.MockClientHttpRequest) request).getBodyAsBytes();
            byte[] responseBody = ocspResponderService.handle(requestBody);
            return respond(responseBody, "application/ocsp-response");
        };
    }

    private static ResponseCreator delegujDoCrlRespondera(CrlResponderService crlResponderService) {
        return request -> {
            byte[] responseBody = crlResponderService.buildCrl();
            return respond(responseBody, "application/pkix-crl");
        };
    }

    private static MockClientHttpResponse respond(byte[] body, String contentType) throws IOException {
        MockClientHttpResponse response = new MockClientHttpResponse(body, HttpStatus.OK);
        response.getHeaders().add("Content-Type", contentType);
        return response;
    }
}
