## Do zrobienia (bieżąca sesja)

- brak

## Do zrobienia (kolejne sesje) — wg priorytetu

1. **Podłączenie `jasper_service` do `document_api`.** `jasper_service` ma kompletny,
   działający pipeline renderowania PDF z szablonu `.jrxml` (`RenderowanieController` →
   `RenderowanieService`), ale `document_api` go nigdzie nie wywołuje — brak
   `JasperServiceClient` i wpisu `jasper.service.base-url` w `application.properties`.
   To jedyny realny bloker dla dwóch rzeczy niżej (2, 3) i generalnie najważniejsza
   brakująca integracja w systemie, bo usługa po drugiej stronie jest gotowa.
2. **`POST /api/naleznosci/:id/wezwanie`** (wezwanie do zapłaty) — nie istnieje w
   `PlatnosciController`. Wymaga punktu 1. Frontend (`platnosci-strona.ts`) zostaje
   świadomym mockiem do tego czasu (zob. `currentwork/contracts/document_api_platnosci.md`).
3. **Realne generowanie dokumentu w akcji zbiorowej "generowanie dokumentów".**
   `AkcjaZbiorowaProcessingService.wykonajGenerowanieDokumentu()` tworzy obecnie tylko
   metadane dokumentu + niepodpisaną wersję, bez renderowania PDF. Wymaga punktu 1.
4. **Walidacja adresu e-Doręczeń (ADE) przy nadawaniu pisma.**
   `KorespondencjaService.nadajPismo` NIE weryfikuje pola `adresDoreczenEDE` przed
   utworzeniem przesyłki kanałem `e-doreczenia` — przyjmie żądanie nawet gdy obywatel
   nie ma zarejestrowanej skrzynki ADE. Frontend tylko ostrzega, nie blokuje. Prawdziwa
   luka backendu, zob. `currentwork/contracts/document_api_korespondencja.md`.
5. **Podpisywanie dokumentu od frontu (cert-picker).** Backend `POST /api/dokumenty/:id/podpisz`
   jest w pełni zaimplementowany i realnie woła `sign_service`. `GET /api/certyfikaty` już
   istnieje w `sign_service` (`CertyfikatController`) — brakuje proxy/kontrolera w
   `document_api` (analogicznie do `DmsServiceClient`) + cert-picker dialogu we froncie.
   Decyzja produktowa: cert-picker ma wyświetlać tylko certyfikaty przypisane do
   zalogowanego użytkownika (filtrowanie `GET /api/certyfikaty` po tożsamości z JWT),
   nie pełną listę certyfikatów z `sign_service`. `dokument-szczegoly.ts`'s `podpisz()`
   mutuje obecnie tylko lokalny sygnał (fejk).
6. **Podgląd i pobranie PDF dokumentu.** `GET /api/dokumenty/:id/wersje/:wersja/plik`
   (proxy do `dms_service`) nie istnieje w `DokumentController`. `podgladPdf()` w
   `dokument-szczegoly.ts` otwiera dialog z samym tekstem, `pobierz()` to `alert()` —
   zero realnej obsługi plików.
7. **Szczegóły podpisu dokumentu.** `GET /api/dokumenty/:id/wersje/:wersja/podpis` nie ma
   kontrolera (DTO `SzczegolyPodpisu` już istnieje w backendzie). `szczegolyPodpisu()` w
   `dokument-szczegoly.ts` zwraca w całości wymyślone dane (fałszywy odcisk certyfikatu,
   OCSP/CRL/TSA).
8. **Upload szablonu Jasper (`.jrxml`).** `POST /api/szablony` (multipart) nie istnieje
   ani w kliencie, ani w backendzie. `wgraj()` w `szablony-strona.ts` tylko dopisuje
   wiersz lokalnie do sygnału.
9. **Moduł Administracja — podłączenie do rzeczywistego API.** Backend ma w pełni
   zaimplementowane i zabezpieczone rolami endpointy dla użytkowników/ról, struktury
   organizacyjnej, słowników i dziennika zdarzeń, oraz definicji obiegów
   (`GET/PUT /api/definicje-obiegow`). Pięć podstron frontu (`uzytkownicy`, `struktura`,
   `slowniki`, `obiegi`, `dziennik-zdarzen`) nadal w 100% mockowe (`admin-mock-data.ts`) —
   zob. `currentwork/contracts/document_api_administracja.md`.
10. **Moduł Wymiana EDI/XML — podłączenie do rzeczywistego API.** Backend już wystawia
    `POST /api/wymiana/xml`, `POST /api/wymiana/ubl`, `GET /api/wymiana/xml/historia`,
    `GET /api/wymiana/ubl/historia` oraz `POST /api/akcje-zbiorowe` (jeden endpoint z
    dyskryminatorem `typAkcji`) + `GET /api/akcje-zbiorowe/:akcjaId/status` — frontend
    (`features/wymiana/`) wciąż w 100% symuluje działanie w przeglądarce
    (`setTimeout`/`setInterval`), zero wywołań HTTP. Zob.
    `currentwork/contracts/document_api_wymiana.md`.
11. **Moduł Pulpit — brak dedykowanego backendu, do zbudowania z istniejących zasobów.**
    Nadal korzysta z `core/mock/mock-data.ts` i `pulpit-mock.ts` (w tym sprawy z ID
    nieistniejącymi w głównym mocku, więc routerLink do szczegółów sprawy nie działa
    nawet w trybie mock). Docelowo oparty na istniejących `GET /api/sprawy`,
    `GET /api/naleznosci`, `GET /api/przesylki` z odpowiednimi filtrami (`referent`,
    sortowanie po `terminUstawowy`, `wydzial`, `status`) — zob.
    `currentwork/contracts/document_api_pulpit.md` co do dokładnych zapytań.
12. **Drobne pozostałości mockowe w module Sprawy.** `sprawa-szczegoly.ts` korzysta z
    `MOCK_TYPY_DOKUMENTOW` (brak podłączonego słownika typów dokumentów), `sprawa-nowa.ts`
    z `MOCK_OBYWATELE` przy wyborze obywatela/podmiotu dla nowej sprawy — drobne, ale
    realne niedoróbki w module uznanym za "podłączony".
13. **Brak parametru sortowania w `GET /api/sprawy`.** Lista jest tylko paginowana
    (`strona`/`rozmiarStrony`) — do rozważenia `sort=pole,asc|desc` przy przyszłym
    rozszerzeniu, drugorzędne.
14. **Testy jednostkowe `jasper_service`.** Najmniej przetestowany moduł backendowy —
    tylko test ładowania kontekstu i kontrolera. Brak testu `RenderowanieService`
    (kompilacja/fill/eksport PDF, obsługa uszkodzonego `.jrxml`) i jego `DmsServiceClient`.
