# Kontrakt: sign_service → document_api (podpis elektroniczny PAdES/XAdES)

`sign_service` (port 10623, schema Postgres `pki`) wystawia REST API do składania i
weryfikacji podpisu PAdES na dokumentach PDF oraz XAdES na dokumentach/komunikatach XML,
z użyciem demonstracyjnej infrastruktury PKI (dwupoziomowe CA — self-signed root wydający
jeden certyfikat issuing-CA, a issuing-CA podpisujący certyfikaty leaf/TSA). `document_api`
woła już do `POST /api/podpisy/pades` (zob. `SignServiceClient`, użyty w
`DokumentService.podpiszDokument` przy orkiestracji podpisania PAdES - pobranie wersji z
`dms_service`, podpisanie tutaj, upload podpisanego wyniku jako nowej wersji w `dms_service`)
oraz do `GET /api/certyfikaty` (proxy `CertyfikatController`/`SignServiceClient.listaCertyfikatow`,
zasilający cert-picker przy podpisywaniu dokumentu we froncie - zwraca WSZYSTKIE certyfikaty,
bez filtrowania po tożsamości użytkownika). XAdES (`/xades`, `/weryfikacja-xades`) i weryfikacja
PAdES (`/weryfikacja`) nie są jeszcze konsumowane przez `document_api` (zob. `main_assumptions.md`
sekcja "document_api ↔ sign_service") — ta część kontraktu wciąż czeka na konsumenta.

**Zakres (ten kontrakt)**: PAdES i XAdES, oba na poziomie BASELINE-T — znacznik czasu
RFC 3161 pochodzi z mock-TSA (niepubliczny/niekwalifikowany własny serwer zgodny ze
standardem, zob. `main_assumptions.md`). Weryfikacja unieważnienia woła prawdziwy OCSP/CRL
responder tego serwisu (adres z rozszerzeń AIA/CDP certyfikatu), z fail-closed gdy oba kanały
są nieosiągalne/nieautentyczne — zob. `main_assumptions.md` sekcja "sign_service" i sekcja
"Rozbieżność statusów podpisu" poniżej co do znaczenia `niepoprawny` w tym przypadku.
`sign_service` nie pobiera dokumentu z `dms_service` — żądanie dostarcza bajty PDF/XML
bezpośrednio; orkiestracja (pobranie z DMS, zapis wersji podpisanej) to zakres
przyszłej integracji przez `document_api`. `sign_service` nie interpretuje treści XML
(typ komunikatu, schemat XSD) — to zakres `WymianaService`/`document_api`, nie tego
serwisu, który podpisuje dowolne bajty XML tak samo jak dowolne bajty PDF.

## Rozbieżność statusów podpisu (ważne dla konsumenta)

W projekcie istnieją DWA zestawy wartości statusu podpisu:
- `pki.wystawiony_podpis.status_weryfikacji` (wewnętrzna kolumna tego serwisu): tylko 3
  wartości — `poprawny`/`niepoprawny`/`nieznany`. Ustawiana tylko przy zapisie nowo
  złożonego podpisu (`poprawny`, bo podpisujemy świeżo aktywnym certyfikatem) — NIE jest
  źródłem odpowiedzi endpointu weryfikacji.
- `StatusPodpisu` (TypeScript, `packages/document-shared-dto/src/dokumenty.ts`) i
  `core.wersja_dokumentu.status_podpisu` (document_api): 5 wartości, z czego 4 dotyczą
  podpisu istniejącego — `poprawny`/`niepoprawny`/`certyfikat_wygasly`/`certyfikat_uniewazniony`
  (`brak` dotyczy tylko braku podpisu, nie jest zwracane przez ten endpoint).

`POST /api/podpisy/weryfikacja` (PAdES) i `POST /api/podpisy/weryfikacja-xades` (XAdES)
zwracają ten SZERSZY, 4-wartościowy zestaw. Status jest OBLICZANY przez połączenie
kryptograficznej weryfikacji DSS (integralność/autentyczność podpisu,
`SignatureCryptographicVerification.isSignatureValid()`, tożsamość certyfikatu i łańcucha
zaufania) ze sprawdzeniem stanu certyfikatu przez sieciowy OCSP (fallback CRL) — zob.
`main_assumptions.md` sekcja "sign_service" — NIE przez odczyt kolumny `status_weryfikacji`.
`niepoprawny` pokrywa WIELE przyczyn dla konsumenta nierozróżnialnych w samym statusie:
integralność podpisu, tożsamość/łańcuch certyfikatu, ALBO status unieważnienia niemożliwy do
ustalenia (OCSP i CRL oba nieosiągalne/nieautentyczne — fail-closed, nie "poprawny" przez
domysł).

## `POST /api/podpisy/pades`

Składanie podpisu PAdES BASELINE-T (ze znacznikiem czasu z mock-TSA). `multipart/form-data`:
- `plik` — bajty PDF do podpisania.
- `certyfikatId` (string, wymagane) — id wiersza `certyfikat`.
- `dokumentReferencja` (string, wymagane) — płaska referencja do `core.wersja_dokumentu.id`.

Walidacja: certyfikat musi mieć `status='aktywny'` ORAZ `data_wygasniecia > dziś` —
inaczej `400 Bad Request`, podpis NIE jest składany (bez próby podpisania certyfikatem
nieaktywnym/wygasłym).

Odpowiedź: `200 OK`, `Content-Type: application/pdf`, body = podpisane bajty PDF,
nagłówek `X-Wystawiony-Podpis-Id` (id nowego wiersza `wystawiony_podpis`, do korelacji
przez wołającego). Nowy wiersz `wystawiony_podpis`: `format='pades'`,
`status_weryfikacji='poprawny'`, `znacznik_czasu` = ISO-8601 instant (`genTime` znacznika
czasu RFC 3161 osadzonego w podpisie przez mock-TSA, np. `2026-10-23T10:15:00Z`).

## `POST /api/podpisy/weryfikacja`

Weryfikacja podpisu PAdES. `multipart/form-data`:
- `plik` — bajty podpisanego PDF.

Logika: DSS odczytuje podpis z PDF, sprawdza integralność kryptograficzną
(`checkSignatureIntegrity()`), wyodrębnia certyfikat podpisujący i czas podpisania.
Tożsamość certyfikatu podpisującego jest uznana za potwierdzoną TYLKO gdy wszystkie
poniższe warunki są spełnione (inaczej → `niepoprawny`, bez względu na dalsze kroki):
- integralność podpisu (`SignatureCryptographicVerification.isSignatureValid()`) OK,
- numer seryjny certyfikatu z podpisu (porównanie `BigInteger`, nie string) odpowiada
  wierszowi `certyfikat` w bazie,
- DER certyfikatu z podpisu jest bajtowo identyczny z `certyfikat.certyfikat_der`,
- certyfikat weryfikuje się kryptograficznie w dwóch krokach łańcucha: leaf względem
  klucza publicznego issuing-CA (`demo_issuing_ca.certyfikat_der`), a issuing-CA względem
  klucza publicznego root-CA (`demo_ca.certyfikat_der` jako trust anchor) — bez tego
  certyfikat z podstawionym numerem seryjnym, ale samopodpisany przez inne CA, zostałby
  błędnie zaakceptowany; certyfikat podpisany bezpośrednio przez root (z pominięciem
  issuing-CA) jest również odrzucany, bo łańcuch musi iść przez obie warstwy.

Dla potwierdzonej tożsamości status unieważnienia pochodzi z sieciowego OCSP (adres z
rozszerzenia AIA certyfikatu), a jeśli OCSP zawiedzie/jest nieautentyczny — z CRL jako fallback
(adres z CDP, podpis CRL również weryfikowany kluczem publicznym issuing-CA); status końcowy,
w tej kolejności:
1. OCSP lub CRL zgłasza unieważnienie → `certyfikat_uniewazniony`.
2. OBA kanały (OCSP i CRL) nieosiągalne/nieautentyczne → `niepoprawny` (fail-closed, nie można
   potwierdzić braku unieważnienia).
3. Inaczej `data_wygasniecia <= dziś` (czas weryfikacji, nie czas podpisania) →
   `certyfikat_wygasly`.
4. Inaczej → `poprawny`.

PDF parsowalny, ale bez żadnego podpisu → `niepoprawny` (nie błąd HTTP).

Odpowiedź: `200 OK` (niezależnie od statusu — to NIE jest błąd HTTP, to wynik weryfikacji
negatywnej), body `WynikWeryfikacji`:
```json
{
  "status": "poprawny",
  "podpisujacy": "CN=Katarzyna Zielińska, O=Urząd, C=PL",
  "czasPodpisu": "2026-10-23T10:15:00Z",
  "odciskCertyfikatu": "a1b2c3...sha256hex",
  "statusOcsp": "nieznany",
  "statusCrl": "nieznany",
  "znacznikCzasuTsa": "2026-10-23T10:15:01Z"
}
```
`status` ∈ `poprawny | niepoprawny | certyfikat_wygasly | certyfikat_uniewazniony`.
`statusOcsp`/`statusCrl` są zawsze `"nieznany"` — pola rezerwowe w DTO, status unieważnienia
jest wyrażony wyłącznie przez pole `status` (`certyfikat_uniewazniony`/`niepoprawny` w razie
fail-closed), zob. wyżej.
`znacznikCzasuTsa` jest ISO-8601 instantem `genTime` pierwszego znacznika czasu osadzonego
w podpisie (BASELINE-T), albo `"brak"` dla podpisów bez znacznika (poziom BASELINE-B).
Pola odpowiadają kontraktowi `SzczegolyPodpisu` (`packages/document-shared-dto`) + pole
`status` — UWAGA: `document_api` `SzczegolyPodpisu.znacznikCzasuTsa` jest typu
`OffsetDateTime`, nie `String` jak tutaj i w TS-owym DTO; ten serwis nie jest jeszcze
wołany przez `document_api` (zob. akapit wyżej), więc przy faktycznym podłączeniu
konsumenta trzeba obsłużyć wartość nie-datową `"brak"`.

`400 Bad Request` jeśli plik nie jest parsowalnym PDF-em.

## `POST /api/podpisy/xades`

Składanie podpisu XAdES BASELINE-T (ze znacznikiem czasu z mock-TSA), analogicznie do
`/pades` — ten sam materiał PKI (ta sama tabela `certyfikat`), ta sama walidacja
certyfikatu, różni się wyłącznie formatem wejścia/wyjścia (XML zamiast PDF) i biblioteką
DSS użytą do podpisu (`XAdESService`/`XAdESSignatureParameters` zamiast
`PAdESService`/`PAdESSignatureParameters`, pakowanie `ENVELOPED`). `multipart/form-data`:
- `plik` — bajty XML do podpisania (dowolny dokument/komunikat XML — ten serwis nie
  interpretuje jego schematu).
- `certyfikatId` (string, wymagane) — id wiersza `certyfikat`.
- `dokumentReferencja` (string, wymagane) — płaska referencja do `core.wersja_dokumentu.id`.

Walidacja: identyczna jak dla `/pades` — certyfikat musi mieć `status='aktywny'` ORAZ
`data_wygasniecia > dziś`, inaczej `400 Bad Request`.

Odpowiedź: `200 OK`, `Content-Type: application/xml`, body = podpisane bajty XML
(sygnatura `<ds:Signature>` osadzona enveloped w dokumencie), nagłówek
`X-Wystawiony-Podpis-Id`. Nowy wiersz `wystawiony_podpis`: `format='xades'`, reszta pól
analogicznie jak dla `/pades`.

## `POST /api/podpisy/weryfikacja-xades`

Weryfikacja podpisu XAdES, odrębny endpoint od `/weryfikacja` (PAdES) — żadnego
wykrywania formatu na podstawie treści pliku, wołający wybiera endpoint zgodnie z tym,
co podpisywał. `multipart/form-data`:
- `plik` — bajty podpisanego XML.

Logika i format odpowiedzi IDENTYCZNE jak dla `/weryfikacja` (ten sam `WynikWeryfikacji`,
te same 4 wartości statusu, te same reguły potwierdzenia tożsamości certyfikatu —
integralność podpisu, numer seryjny, DER-match, łańcuch leaf → issuing-CA → root-CA) —
różni się wyłącznie parserem DSS użytym do odczytania podpisu z pliku
(`XMLDocumentValidator` zamiast `PDFDocumentValidator`; obie klasy dziedziczą po tym
samym `SignedDocumentValidator`, więc logika oceny podpisu jest współdzielonym kodem,
nie duplikatem).

`400 Bad Request` jeśli plik nie jest parsowalnym XML-em.

## `GET /api/certyfikaty` i `GET /api/certyfikaty/{id}`

Odczyt metadanych certyfikatów (bez `certyfikat_der`/`keystore_pkcs12` — szczegóły
wewnętrzne, nieujawniane). DTO `CertyfikatDto`:
```json
{
  "id": "cert-1",
  "podmiot": "CN=Katarzyna Zielińska, O=Urząd, C=PL",
  "numerSeryjny": "7A3F1B2C9D0E4455",
  "dataWydania": "2026-01-10",
  "dataWygasniecia": "2027-01-10",
  "status": "aktywny",
  "wydawca": "Demo CA Issuing"
}
```
`GET /api/certyfikaty/{id}` → `404 Not Found` (`{ "komunikat": string }`) jeśli nie istnieje.

## Błędy

Ujednolicony format błędu (analogiczny do `dms_service`/`document_api`):
```json
{ "komunikat": "string" }
```
- `400 Bad Request` — certyfikat nieaktywny/wygasły przy podpisywaniu, brak pliku, plik
  PDF/XML nieparsowalny przy weryfikacji.
- `404 Not Found` — certyfikat o podanym id nie istnieje.
