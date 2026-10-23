# Kontrakt: sign_service → document_api (podpis elektroniczny PAdES)

`sign_service` (port 10623, schema Postgres `pki`) wystawia REST API do składania i
weryfikacji podpisu PAdES na dokumentach PDF, z użyciem demonstracyjnej infrastruktury
PKI (jedno uproszczone demo-CA). `document_api` jeszcze NIE woła do tego serwisu (zob.
`main_assumptions.md` sekcja "document_api ↔ sign_service") — poniższy opis jest
kontraktem wystawionym, czekającym na konsumenta.

**Zakres Slice 1 (ten kontrakt)**: PAdES, poziom BASELINE-B (bez znacznika czasu TSA).
Weryfikacja unieważnienia WYŁĄCZNIE z lokalnej tabeli `uniewaznienie` — bez sieciowego
OCSP/CRL/AIA. XAdES, TSA RFC 3161, prawdziwy serwer OCSP/CRL i root+issuing CA jako
odrębne podmioty NIE są objęte tym etapem (zob. `plan.md`). `sign_service` nie pobiera
dokumentu z `dms_service` — żądanie dostarcza bajty PDF bezpośrednio; orkiestracja
(pobranie z DMS, zapis wersji podpisanej) to zakres przyszłej integracji przez
`document_api`.

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

`POST /api/podpisy/weryfikacja` zwraca ten SZERSZY, 4-wartościowy zestaw. Status jest
OBLICZANY przez połączenie kryptograficznej weryfikacji DSS (integralność/autentyczność
podpisu, `SignatureCryptographicVerification.isSignatureValid()`) z lokalnym
sprawdzeniem stanu certyfikatu (tabela `uniewaznienie` + `certyfikat.data_wygasniecia`),
NIE przez odczyt kolumny `status_weryfikacji`.

## `POST /api/podpisy/pades`

Składanie podpisu PAdES BASELINE-B. `multipart/form-data`:
- `plik` — bajty PDF do podpisania.
- `certyfikatId` (string, wymagane) — id wiersza `certyfikat`.
- `dokumentReferencja` (string, wymagane) — płaska referencja do `core.wersja_dokumentu.id`.

Walidacja: certyfikat musi mieć `status='aktywny'` ORAZ `data_wygasniecia > dziś` —
inaczej `400 Bad Request`, podpis NIE jest składany (bez próby podpisania certyfikatem
nieaktywnym/wygasłym).

Odpowiedź: `200 OK`, `Content-Type: application/pdf`, body = podpisane bajty PDF,
nagłówek `X-Wystawiony-Podpis-Id` (id nowego wiersza `wystawiony_podpis`, do korelacji
przez wołającego). Nowy wiersz `wystawiony_podpis`: `format='pades'`,
`status_weryfikacji='poprawny'`, `znacznik_czasu='brak-tsa'` (placeholder — kolumna jest
NOT NULL, brak TSA w tym slice).

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
- certyfikat weryfikuje się kryptograficznie względem klucza publicznego demo-CA
  (`demo_ca.certyfikat_der` jako trust anchor — bez tego certyfikat z podstawionym
  numerem seryjnym, ale samopodpisany przez inne CA, zostałby błędnie zaakceptowany).

Dla potwierdzonej tożsamości status końcowy, w tej kolejności:
1. Certyfikat ma wpis w tabeli `uniewaznienie` → `certyfikat_uniewazniony`.
2. Inaczej `data_wygasniecia <= dziś` (czas weryfikacji, nie czas podpisania — w tym
   slice bez TSA nie da się wiarygodnie rozstrzygnąć czasu podpisania) → `certyfikat_wygasly`.
3. Inaczej → `poprawny`.

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
  "znacznikCzasuTsa": "brak"
}
```
`status` ∈ `poprawny | niepoprawny | certyfikat_wygasly | certyfikat_uniewazniony`.
`statusOcsp`/`statusCrl` są zawsze `"nieznany"`, `znacznikCzasuTsa` zawsze `"brak"` w tym
slice (brak implementacji sieciowego OCSP/CRL i TSA). Pola odpowiadają kontraktowi
`SzczegolyPodpisu` (`packages/document-shared-dto`) + pole `status`.

`400 Bad Request` jeśli plik nie jest parsowalnym PDF-em.

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
  PDF nieparsowalny przy weryfikacji.
- `404 Not Found` — certyfikat o podanym id nie istnieje.
