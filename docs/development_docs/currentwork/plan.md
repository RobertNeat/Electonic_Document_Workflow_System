## Do zrobienia (bieżąca sesja)

Brak.

## Do zrobienia (kolejne sesje) — wg priorytetu

1. **`document_api` nigdy nie woła `delivery_mock` przy nadaniu pisma.** `KorespondencjaService.nadajPismo`
   zapisuje przesyłkę wyłącznie lokalnie w schemie `core` — żadna realna przesyłka (poza danymi demo
   zgranymi ręcznie w seedach) nie trafia do `delivery_mock`. Wymaga dodania wywołania
   `POST /api/przesylki-dostawy` z `document_api` do `delivery_mock` przy nadaniu pisma.
2. **Naliczanie należności (nowy endpoint) + podłączenie "Generuj wezwanie" (istniejący endpoint).**
   Dwa podzadania do wykonania razem w tej samej sesji:
   - 2a. `PlatnosciController` nie wystawia `POST /api/naleznosci` — jedyne wiersze w tabeli `naleznosc`
     pochodzą z seeda Liquibase. Dodać endpoint + formularz w `document_web` (backend i UI razem, inaczej
     luka z raportu nie jest faktycznie zamknięta). Pola żądania: `sprawaId`, `obywatelId`,
     `typNaleznosciId` (patrz niżej), `tytul` (wolny tekst, opis), `kwota`, `terminPlatnosci`.
     `numerReferencyjny` generowany przez backend (nie wpisywany przez użytkownika), `status` zawsze
     `nierozliczona` przy tworzeniu.
     Nowy słownik **typ należności** (opłata skarbowa / opłata administracyjna / inna) w module
     Administracja → Słowniki, jako piąty słownik z pełnym CRUD (`POST` + `PUT`) — w przeciwieństwie do
     czterech istniejących słowników, które mają tylko `PUT` (dodanie `POST` do nich zostaje osobnym
     punktem, patrz sekcja "Funkcje nieobecne" — nie wchodzi w zakres tego punktu).
   - 2b. Przycisk "Generuj wezwanie" w `platnosci-strona.ts` jest zahardkodowanym mockiem (`potwierdzWezwanie()`
     tylko zamyka dialog) mimo że backendowy endpoint `POST /api/naleznosci/{id}/wezwanie` realnie działa —
     podłączyć front do istniejącego endpointu (dodać metodę w `PlatnosciApi`, wywołać ją z komponentu).
3. **Tworzenie i edycja obywatela/podmiotu z UI (backend + UI).** `ObywatelController` wystawia tylko
   `GET` — brak `POST`/`PUT`. Zakres: pełne tworzenie i edycja, nie tylko uzupełnienie ADE na istniejących.
   - `POST /api/obywatele` — tworzenie, przyjmuje wszystkie pola `ObywatelEntity` (`typ`, `imie`/`nazwisko`
     lub `nazwaPodmiotu` zależnie od `typ`, `pesel`/`nip`, `adresDoreczenEDE`, `email`, `telefon`, `adres`).
   - `PUT /api/obywatele/{id}` — edycja wszystkich pól **poza `pesel`/`nip`, które są niemutowalne po
     utworzeniu** (identyfikują osobę/podmiot; zmiana = korekta błędnych danych wymagająca osobnej
     procedury, nie zwykła edycja — żądanie z próbą zmiany tych pól ma być ignorowane albo odrzucone,
     do ustalenia przy implementacji którą z tych dwóch opcji wybrać).
   - Walidacja `adresDoreczenEDE` przy `POST`/`PUT`: regex wg wzorca z seeda,
     `AE:PL-\d{5}-\d{5}-[A-Z]{5}-\d{2}` (dopasować do realnych wartości w `100-seed.xml` przed
     implementacją) — odrzucić (400) jeśli niezgodny z formatem, pole może być puste/null (brak ADE to
     stan dopuszczalny, tylko blokuje kanał e-Doręczeń).
   - Formularze w `document_web`: "Nowy obywatel/podmiot" oraz edycja na karcie obywatela (zakładka "Dane
     podstawowe", która dziś jest czystym podglądem).

## Funkcje nieobecne - good to have, non blocking manual tests

- **Eksport komunikatów XML** nie istnieje ani w UI, ani w backendzie (`WymianaController` ma tylko
  import) — `KierunekWymiany.EKSPORT` jest martwym kodem, nigdy nie wypełnianym.
- **Powiadomienia o terminach ustawowych** nie istnieją w żadnej formie (ani zadanie w tle, ani e-mail,
  ani osobny komponent UI) — tylko pasywne przeliczenie "dni do terminu" widoczne po otwarciu pulpitu.
- **Dekretacja** dokumentów/korespondencji przychodzącej do wydziału/referenta nie jest zaimplementowana —
  istnieje tylko jako etykieta typu zdarzenia w dzienniku audytu (`TypOperacjiAudytu.DEKRETACJA`), nigdy
  nieemitowana w kodzie.
- **Unieważnienie/rotacja certyfikatu podpisu** niemożliwa z UI — `sign_service` `CertyfikatController`
  wystawia tylko `GET`. Nie da się manualnie doprowadzić podpisu do statusu "certyfikat
  wygasły"/"certyfikat unieważniony" inaczej niż przez dane seedowane na starcie.
- **Edycja struktury obiegów** (stany, przejścia, wymagane role/dokumenty) niemożliwa z UI — `PUT
  /api/definicje-obiegow/{id}` obsługuje tylko `nazwa`/`opis`. Nowe definicje obiegów tylko przez
  Liquibase.
- **CRUD struktury organizacyjnej** (wydziały, stanowiska) niemożliwy z UI — tylko `GET`.
- **Dodawanie nowych pozycji słownikowych** (JRWA, typy dokumentów, dostawcy wysyłki, stawki opłat)
  niemożliwe z UI — kontrolery/UI wsparają tylko edycję istniejących pozycji (`PUT`), brak `POST`.
- **Synchronizacja ról/kont między `core.uzytkownik` a Keycloak** nie istnieje — role w profilu
  `core.uzytkownik` nie wpływają na faktyczną autoryzację (opartą wyłącznie na `realm_access.roles` z
  JWT), a oba zbiory kont nie są ze sobą zsynchronizowane.
