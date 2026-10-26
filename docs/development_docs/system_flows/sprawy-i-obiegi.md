# Przepływ: Sprawy i obiegi

## Zakres

Dokument weryfikuje, czy rejestracja i obsługa sprawy administracyjnej — rejestracja z nadaniem
znaku JRWA, przypisanie do obiegu, przypisanie wydziału/referenta, zmiana stanu zgodnie z
przejściami dozwolonymi w definicji obiegu i przegląd metryki sprawy — da się przejść w całości z
UI `document_web`, bez surowych zapytań REST, SQL czy edycji plików. Obejmuje moduł `sprawy`
(`apps\document_web\src\app\features\sprawy`) i moduł administracyjny `obiegi`
(`apps\document_web\src\app\features\administracja\obiegi`), skonfrontowane z endpointami
`SprawaController`/`DokumentController`/`ObywatelController` w `document_api`. Weryfikuje też
osobno koncepcję „dekretacji” w rozumieniu EZD (dekretacja dokumentu przychodzącego do
wydziału/referenta) opisaną w `main_assumptions.md`.

## Kroki przepływu (UI)

1. Przegląd definicji obiegów (opcjonalnie, przed rejestracją) — ekran *Administracja → Obiegi*
   (`apps\document_web\src\app\features\administracja\obiegi\obiegi.ts`), lista kart z podglądem
   sekwencji stanów — endpoint REST użyty: `GET /api/definicje-obiegow`.
2. Lista spraw, wyszukiwanie/filtrowanie po stanie, wydziale, treści, paginacja i sortowanie —
   ekran *Sprawy* (`sprawy-strona.ts`/`.html`) — endpoint REST użyty: `GET /api/sprawy`.
3. Rejestracja nowej sprawy — przycisk „Nowa sprawa” na liście spraw prowadzi na `/sprawy/nowa`
   (`sprawa-nowa.ts`). Formularz wymaga: tytułu, pozycji JRWA (po wyborze automatycznie uzupełnia
   kategorię archiwalną), definicji obiegu, obywatela/podmiotu, wydziału (lista statyczna w
   komponencie) i referenta (domyślnie zalogowany użytkownik, edytowalne) oraz opcjonalnie terminu
   ustawowego — endpointy REST użyte: `GET /api/definicje-obiegow`, `GET /api/slowniki/jrwa`,
   `GET /api/obywatele` (lista obywateli do wyboru), a po zapisaniu formularza
   `POST /api/sprawy`. Znak sprawy JRWA jest nadawany automatycznie przez backend
   (`SprawaService.generujZnakSprawy`) — użytkownik nie wpisuje go ręcznie.
4. Przypisanie sprawy do wydziału/referenta — realizowane wyłącznie **w momencie rejestracji
   sprawy**, poprzez pola `wydzial`/`referent` formularza z kroku 3 — endpoint REST użyty:
   `POST /api/sprawy` (pola `wydzial`, `referent` w `UtworzSpraweRequest`). To NIE jest
   „dekretacja” w sensie EZD (dekretacja dokumentu przychodzącego do wydziału/referenta) — to
   tylko pole tekstowe na nowo tworzonej sprawie. Prawdziwa dekretacja nie jest zaimplementowana
   wcale (zob. sekcja Braki, pkt 3).
5. Szczegóły sprawy — po zapisaniu formularza użytkownik jest przekierowywany na
   `/sprawy/{id}` (`sprawa-szczegoly.ts`/`.html`), gdzie widoczne są dane podstawowe, obieg sprawy
   z paskiem stanów, metryka i link do dokumentów sprawy — endpointy REST użyte:
   `GET /api/sprawy/{id}`, `GET /api/definicje-obiegow`, `GET /api/slowniki/typy-dokumentow`,
   `GET /api/obywatele/{id}`, `GET /api/dokumenty?sprawaId={id}`, `GET /api/sprawy/{id}/metryka`.
6. Zmiana stanu sprawy — na ekranie szczegółów, dla każdego dozwolonego przejścia z aktualnego
   stanu (wyliczonego z `DefinicjaObiegu.przejscia` w `sprawy.utils.ts::przejsciaZeStanu`)
   wyświetlany jest przycisk „Przejdź do: {stan}”; przycisk jest aktywny tylko gdy użytkownik ma
   wymaganą rolę i sprawa ma wymagane typy dokumentów. Backend wymusza te same reguły niezależnie
   od frontendu (`SprawaService.zmienStan`/`powodBlokadyKandydata`) — endpoint REST użyty:
   `PATCH /api/sprawy/{id}/stan`. Po sukcesie metryka jest doczytywana ponownie — endpoint REST
   użyty: `GET /api/sprawy/{id}/metryka`.
7. Przegląd metryki sprawy (historia czynności) — widoczna na ekranie szczegółów jako osadzona
   linia czasu (`p-timeline`) — endpoint REST użyty: `GET /api/sprawy/{id}/metryka`.
8. Przegląd dokumentów sprawy — link „Zobacz dokumenty sprawy” przenosi do modułu Dokumenty z
   filtrem `sprawaId` — obsługa dokumentów sprawy leży poza zakresem tego dokumentu (moduł
   Dokumenty ma własny przepływ).
9. Podgląd i edycja ograniczonej części definicji obiegu (tylko rola `administrator`) — ekran
   *Administracja → Obiegi*, dialog „Edycja definicji obiegu” pozwala zmienić `nazwa` i `opis` —
   endpoint REST użyty: `PUT /api/definicje-obiegow/{id}`.

## Dane testowe do manualnego przejścia przepływu

Rejestracja sprawy administracyjnej o wydanie decyzji środowiskowej:

- Tytuł sprawy: `Wniosek o wydanie decyzji o środowiskowych uwarunkowaniach dla inwestycji
  przy ul. Polnej 14 w Rzeszowie`
- Pozycja JRWA: `6220 — Decyzje o środowiskowych uwarunkowaniach` (kategoria archiwalna uzupełni
  się automatycznie na podstawie wybranej pozycji)
- Definicja obiegu: `Obieg wniosku o wydanie decyzji administracyjnej`
- Obywatel/podmiot: `Jan Kowalski` (istniejący w słowniku obywateli — zob. sekcja Braki co do
  dodawania nowych obywateli)
- Wydział: `Wydział Ochrony Środowiska`
- Referent: `Tomasz Zieliński` (lub zalogowany użytkownik demo — pole jest domyślnie wypełnione
  jego imieniem i nazwiskiem)
- Termin ustawowy: data za 30 dni od dnia rejestracji (np. `2026-11-25`)

Rejestracja sprawy ze skargi/wniosku (alternatywny scenariusz):

- Tytuł sprawy: `Skarga na niewłaściwe oznakowanie przejścia dla pieszych przy ul. Kolejowej 7`
- Pozycja JRWA: `1510 — Skargi i wnioski`
- Definicja obiegu: `Obieg skargi / wniosku`
- Obywatel/podmiot: `Anna Nowak`
- Wydział: `Wydział Organizacyjny`
- Referent: imię i nazwisko zalogowanego użytkownika demo (np. `demo.referent`)

Po rejestracji sprawy w obiegu `Obieg wniosku o wydanie decyzji administracyjnej` sekwencja
stanów i przejść (z seeda `100-seed.xml`, changeset `100-03`/`100-04`) jest następująca:

1. `Zarejestrowana → W toku` — bez wymaganej roli, bez wymaganych dokumentów. Dostępne dla
   każdego zalogowanego użytkownika (np. `demo.referent`).
2. `W toku → Do akceptacji` — bez wymaganej roli, bez wymaganych dokumentów.
3. `Do akceptacji → Zakończona` — wymaga roli `kierownik` (zalogować się jako użytkownik z tą
   rolą) ORAZ dokumentu typu `td-3` — „Decyzja administracyjna” — powiązanego ze sprawą
   (`przejscie_obiegu_wymagany_typ_dokumentu`, `przejscie_obiegu_id = prz-decyzja-3`). Dokument
   tego typu da się dołączyć do sprawy z UI, ale nie na ekranie szczegółów sprawy, tylko przez
   ekran *Wymiana → Akcje zbiorowe* (`apps\document_web\src\app\features\wymiana\akcje-zbiorowe`):
   akcja „Generowanie dokumentów” pozwala wybrać sprawę i szablon Jasper
   `decyzja_srodowiskowa.jrxml` (`szablon_jasper.id = sz-1`, powiązany z typem `td-3`) — endpoint
   REST użyty: `POST /api/akcje-zbiorowe` (`typAkcji: GENEROWANIE_DOKUMENTOW`), wynik odpytywany
   przez `GET /api/akcje-zbiorowe/{id}/status`. Backend (`AkcjaZbiorowaProcessingService`) renderuje
   PDF przez `jasper_service`, zapisuje treść przez `dms_service` i tworzy nowy rekord `Dokument`
   typu `td-3` przypisany do sprawy — to jedyna ścieżka UI tworząca nową instancję dokumentu na
   sprawie (zob. uwaga w sekcji Braki, pkt 2, o niskiej odkrywalności tej ścieżki).
4. `Zakończona → Zamknięta` — bez wymaganej roli, bez wymaganych dokumentów (stan końcowy).

Obieg `Obieg skargi / wniosku` (`Zarejestrowana → W toku → Odpowiedź przygotowana →
Zakończona`) nie ma żadnych wymaganych ról/dokumentów na żadnym przejściu (sprawdzone w
`100-seed.xml`, changeset `100-04-przejscia-obiegow` — żaden wiersz `prz-skarga-*` nie ma
`wymagana_rola` ani wpisu w `przejscie_obiegu_wymagany_typ_dokumentu`) — da się przejść w całości
z UI jako zwykły zalogowany użytkownik, bez potrzeby korzystania z akcji zbiorowych.

## Braki / działania niemożliwe z UI

1. **Tworzenie nowej definicji obiegu (stany, przejścia, wymagane dokumenty, role) nie jest
   możliwe z UI.** `main_assumptions.md` wprost stwierdza: „Definicja obiegu (stany, przejścia,
   wymagane dokumenty, role) jest konfigurowalna przez administratora” — ale w `document_api`
   istnieje wyłącznie `GET /api/definicje-obiegow` (lista) i `PUT /api/definicje-obiegow/{id}`
   (`SprawaController.java`, linie 74–83), a `PUT` operuje na `EdytujDefinicjeObieguRequest`
   (`nazwa`, `opis` — plik
   `apps\document_api\src\main\java\robert_neat\document_api\dto\administracja\EdytujDefinicjeObieguRequest.java`).
   Nie istnieje endpoint `POST /api/definicje-obiegow` do tworzenia nowego obiegu, ani endpoint do
   edycji `stany`/`przejscia`/`wymaganaRola`/`wymaganeTypyDokumentow`. Nowe obiegi są wyłącznie
   seedowane migracją Liquibase (`apps\document_api\src\main\resources\db\changelog\demo\100-seed.xml`,
   tabele `definicja_obiegu`, `definicja_obiegu_stan`, `przejscie_obiegu`,
   `przejscie_obiegu_do_stanu`, `przejscie_obiegu_wymagany_typ_dokumentu`). Frontend sam to
   przyznaje w dialogu edycji (`obiegi.html`, linie 72–75): „Pełny wizualny edytor stanów/przejść
   to zakres poza tym modułem — tu edytowana jest tylko nazwa i opis obiegu.” Dodanie nowego
   rodzaju sprawy (nowego obiegu) wymaga więc edycji pliku migracji/bazy danych poza UI. Kontrakt
   `docs\development_docs\currentwork\contracts\document_api_administracja.md` nie wymienia tego
   jako otwartego zobowiązania — jedyny gap, jaki tam odnotowano, dotyczy zarządzania strukturą
   organizacyjną, nie definicji obiegów.

2. **Dołączenie dokumentu wymaganego do przejścia stanu jest możliwe z UI, ale wyłącznie przez
   ekran niepowiązany z modułem Sprawy — niska odkrywalność, nie blokada.**
   `DokumentController.java` (moduł Dokumenty) nie wystawia żadnego endpointu tworzącego nowy
   rekord `Dokument` — ma tylko odczyt (`GET /api/dokumenty*`), podpisywanie PAdES istniejącej
   wersji (`POST /api/dokumenty/{id}/podpisz` — tworzy nową WERSJĘ już istniejącego dokumentu, nie
   nowy dokument) i upload szablonów Jasper (`POST /api/szablony` — metadane szablonu, nie
   instancja dokumentu na sprawie). Jedyna ścieżka tworząca nowy `Dokument` powiązany ze sprawą to
   moduł **Wymiana → Akcje zbiorowe** (`AkcjaZbiorowaProcessingService.wykonajGenerowanieDokumentu`,
   wywoływane przez `POST /api/akcje-zbiorowe` z `typAkcji: GENEROWANIE_DOKUMENTOW`) — renderuje
   PDF z szablonu Jasper przez `jasper_service`, zapisuje przez `dms_service` i zapisuje
   `DokumentEntity` z `typDokumentu` wziętym ze szablonu, powiązany z wybraną sprawą. Dla obiegu
   `Obieg wniosku o wydanie decyzji administracyjnej`, przejście `Do akceptacji → Zakończona`
   wymaga dokumentu typu `td-3` (Decyzja administracyjna); szablon `sz-1`
   (`decyzja_srodowiskowa.jrxml`) jest właśnie tego typu, więc wygenerowanie dokumentu tym
   szablonem przez Akcje zbiorowe w pełni odblokowuje przejście — potwierdzone, że backend
   faktycznie wymusza regułę (`SprawaService.zmienStan`/`powodBlokadyKandydata`, linie 216–297),
   a nie tylko UI (komentarz w `sprawa-szczegoly.ts`, linie 99–102, który twierdzi, że backend NIE
   sprawdza tych reguł, jest nieaktualny i nie odpowiada rzeczywistej implementacji
   `SprawaService` — warto go poprawić/usunąć przy najbliższej okazji, żeby nie dezinformował).
   Gap pozostaje jedynie w ergonomii: ekran szczegółów sprawy (`sprawa-szczegoly.html`) nie
   podsuwa użytkownikowi informacji, że blokujący wymóg dokumentu należy rozwiązać w zupełnie
   innym module (Wymiana), nie w module Dokumenty powiązanym linkiem na tym samym ekranie.

3. **Dekretacja dokumentu przychodzącego do wydziału/referenta (w sensie EZD, zgodnie z
   `main_assumptions.md` linia 23: „Dekretacja dokumentów przychodzących do właściwego
   wydziału/referenta”) nie jest zaimplementowana wcale — ani w backendzie, ani w UI.** Enum
   `TypOperacjiAudytu.DEKRETACJA` (`"Dekretacja"`) istnieje jako nazwana wartość słownika audytu
   (`apps\document_api\src\main\java\robert_neat\document_api\dto\administracja\TypOperacjiAudytu.java`,
   linia 12), ale nie jest nigdzie emitowany — żadne wywołanie
   `auditLogService.zarejestrujZdarzenie(TypOperacjiAudytu.DEKRETACJA, ...)` nie istnieje w całym
   `document_api`. Nie ma odrębnego endpointu do dekretacji przesyłki/dokumentu przychodzącego do
   wydziału/referenta (sprawdzono `PrzesylkaController.java` i `DokumentController.java` — żaden
   nie przypisuje wydziału/referenta). Jedyny mechanizm przypominający dekretację to pola
   `wydzial`/`referent` wypełniane ręcznie przy rejestracji sprawy (`POST /api/sprawy`) — to
   podstawienie semantyczne, nie prawdziwa dekretacja korespondencji przychodzącej.

4. **Lista wydziałów w formularzu rejestracji jest zakodowana na trwałe w komponencie, nie
   pochodzi ze słownika backendu.** Stała `WYDZIALY` w `sprawa-nowa.ts` (linie 37–44) zawiera
   cztery nazwy wydziałów wpisane w kodzie frontendu, z komentarzem: „brak dedykowanego endpointu
   słownikowego do wyboru wydziału w formularzu”. Dodanie nowego wydziału do wyboru w formularzu
   wymaga zmiany kodu frontendu (deploy); nie blokuje to istniejącego przepływu z realistycznymi
   danymi testowymi poniżej, ale ogranicza elastyczność konfiguracji przez administratora z
   poziomu UI.

5. **Dodanie nowego obywatela/podmiotu nie jest możliwe z UI** (zależność poboczna, wpływa na
   krok 3 przepływu, ale nie jest blokadą rejestracji sprawy dla istniejącego obywatela —
   `GET /api/obywatele` i `GET /api/obywatele/{id}` istnieją i działają,
   `ObywatelController.java`). `ObywatelController.java` wystawia wyłącznie dwa `@GetMapping`
   (lista z wyszukiwaniem, szczegóły po ID) — brak `@PostMapping`. Formularz rejestracji sprawy
   pozwala wybrać tylko obywatela z listy zwróconej przez `GET /api/obywatele` — rejestracja
   sprawy dla osoby nieobecnej w bazie wymagałaby wstawienia rekordu SQL/migracją poza UI.
