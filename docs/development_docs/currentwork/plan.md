## Do zrobienia (kolejne sesje)

- `dms-service`: katalog `/data/dms` na wolumenie `dms-data` nie istnieje w obrazie i przy pierwszym
  starcie zostanie utworzony przez Docker jako `root`, a kontener działa jako `USER 10001` (zob.
  `spring.Dockerfile`) — pierwszy zapis pliku skończy się `EACCES`. Do rozwiązania przy implementacji
  przechowywania plików (np. init/chown przy starcie albo wcześniejsze przygotowanie katalogu), bez zmiany
  współdzielonego `spring.Dockerfile`.
- Liquibase: właściwe changesety domenowe per serwis (obiegi spraw, sprawy, dokumenty, należności/wpłaty w
  `core`; metadane dokumentów/szablonów w `dms`; certyfikaty/podpisy w `pki`; przesyłki w `delivery`) oraz
  dane seedowe (JRWA, definicje obiegów, użytkownicy demo) w kontekście `demo`.
- `document_api`: model definicji obiegów (stany/przejścia/wymagane dokumenty jako konfiguracja, nie kod),
  model domenowy spraw, dekretacja, rejestr należności/wpłat, import CAMT.053, import/eksport UBL
  (PEPPOL), publikacja akcji zbiorowych do RabbitMQ (konsumenci/workery).
- `document_api`: mapowanie ról realmu Keycloak (`realm_access.roles` w tokenie JWT) na uprawnienia
  Spring Security przez własny `JwtAuthenticationConverter` — obecnie tylko `jwk-set-uri` do weryfikacji
  podpisu tokenu jest skonfigurowane, bez walidacji wydawcy (`issuer-uri`) ani konwersji ról.
- `dms_service`: operacje na plikach (upload/download/wersjonowanie) na wolumenie `dms-data`.
- `jasper_service`: endpoint renderowania `.jrxml` + dane JSON → PDF.
- `sign_service`: składanie/weryfikacja podpisu PAdES/XAdES (EU DSS), demonstracyjne CA/TSA/OCSP.
- `delivery_mock`: UI Thymeleaf do symulacji statusów przesyłek (e-Doręczenia, operator pocztowy) +
  integracja REST z `document_api`.
- `document_web`: moduły UI dla spraw (wielu typów obiegów), dokumentów/szablonów, podpisów, wysyłki pism,
  płatności; logowanie przez Keycloak (OIDC Authorization Code + PKCE, realm `document-workflow`, klient
  `document-web`, użytkownicy demo: `demo.administrator`, `demo.referent`, `demo.kierownik`,
  `demo.ksiegowosc` — hasło w `KEYCLOAK_DEMO_USER_PASSWORD`).
