# Przepływ: Karta obywatela/podmiotu

## Zakres

Moduł `features/obywatele/` w `document_web` (lista obywateli/podmiotów, karta obywatela z
zakładkami Dane podstawowe / Sprawy / Dokumenty / Korespondencja / Rozrachunki) oraz odpowiadający
mu `ObywatelController` w `document_api`, wraz z filtrami `obywatelId` wystawionymi przez
`SprawaController`, `DokumentController`, `PrzesylkaController` i `PlatnosciController`. Analiza
sprawdza, czy cały przepływ (wyszukanie, dodanie, edycja, przegląd danych powiązanych) da się
wykonać z poziomu UI, bez surowych zapytań REST czy działań poza aplikacjami z `./apps`.

## Kroki przepływu (UI)

1. Lista obywateli/podmiotów — ekran `/obywatele` (`ObywateleLista`), pole wyszukiwania (imię,
   nazwisko, nazwa podmiotu, PESEL, NIP) z debounce 300 ms. Endpoint: `GET /api/obywatele?query=...`.
2. Wybór wiersza na liście → przejście do karty obywatela `/obywatele/:id`. Endpoint:
   `GET /api/obywatele/:id` (wołany w `ObywatelKarta`, wynik udostępniony pod-stronom przez
   `KartaObywatelaKontekst`).
3. Zakładka "Dane podstawowe" (`ObywatelDane`, domyślna pod-strona) — wyłącznie odczyt danych już
   wczytanych przez `ObywatelKarta` (imię/nazwisko lub nazwa podmiotu, PESEL/NIP, adres, e-mail,
   telefon, adres ADE jeśli ustawiony). Brak dodatkowego wywołania HTTP — ten widok nie inicjuje
   żadnego żądania własnego.
4. Zakładka "Sprawy" (`ObywatelSprawy`) — lista spraw obywatela. Endpoint:
   `GET /api/sprawy?obywatelId=<id>&rozmiarStrony=200` (filtr `obywatelId` stosowany w
   `SprawaService.szukaj` przez specyfikację JPA na kolumnie `sprawa.obywatel_id`; `rozmiarStrony`
   nie ma górnego limitu w backendzie — `PageRequest.of(..., Math.max(rozmiarStrony, 1), ...)` — więc
   200 nie ucina wyniku). Wiersze mają link do `/sprawy/:id` (drill-down dostępny).
5. Zakładka "Dokumenty" (`ObywatelDokumenty`) — lista dokumentów powiązanych ze sprawami obywatela.
   Endpoint: `GET /api/dokumenty?obywatelId=<id>` (filtr przekazywany do
   `DokumentService.listaDokumentow`). Tabela nie ma linku do szczegółów dokumentu (brak drill-down
   z tego widoku — zob. sekcja braków).
6. Zakładka "Korespondencja" (`ObywatelKorespondencja`) — lista przesyłek obywatela. Endpoint:
   `GET /api/przesylki?obywatelId=<id>` (filtr przekazywany do
   `KorespondencjaService.listaPrzesylek`). Dane pochodzą z `document_api` (tabela `przesylka`), nie
   z wywołania `delivery_mock` z poziomu UI — `document_api` jest jedynym backendem wołanym przez
   front; aktualizacje statusu z `delivery_mock` trafiają do `document_api` asynchronicznie przez
   RabbitMQ (`PrzesylkaStatusuConsumer`), zanim trafią do tego widoku. Tabela nie ma linku do
   szczegółów przesyłki (brak drill-down).
7. Zakładka "Rozrachunki" (`ObywatelRozrachunki`) — dwa równoległe żądania (`forkJoin`): lista
   należności i lista wpłat. Endpointy: `GET /api/naleznosci?obywatelId=<id>` i
   `GET /api/wplaty?obywatelId=<id>` (filtry przekazywane do `PlatnosciService.listaNaleznosci` /
   `listaWplat`, oba przez specyfikacje JPA — dla wpłat `obywatelId` jest rozwiązywany przez
   powiązanie `wplata.naleznosc.obywatel`, bo tabela `wplata` nie ma własnej kolumny `obywatel_id`).
   Suma "do zapłaty" liczona po stronie klienta z wyniku `naleznosci`.
8. Przypięcie obywatela do globalnego kontekstu — przy udanym wczytaniu karty `ObywatelKarta`
   (`apps/document_web/.../obywatel-karta/obywatel-karta.ts:83`) i po zarejestrowaniu korespondencji
   w module Sprawy (`apps/document_web/.../sprawa-szczegoly/sprawa-szczegoly.ts:198`) wołane jest
   `CitizenContextService.przypnij(obywatel)` (zapis też do `sessionStorage`), niezależnie od
   lokalnego `KartaObywatelaKontekst` (który żyje tylko w ramach `providers` komponentu karty i
   służy wyłącznie pod-stronom tej samej karty). Przypięty obywatel jest odczytywany (bez
   dodatkowego wywołania HTTP) w dwóch miejscach layoutu: `topbar.ts`/`topbar.html` renderuje go
   jako usuwalny chip w górnym pasku (usunięcie woła `odepnij()`), a `sidebar.ts` generuje z niego
   skrócone pozycje menu prowadzące do zakładek karty obywatela (`kartaObywatelaMenu(obywatel.id)`).
9. Powiązanie obywatela ze sprawą przy rejestracji nowej sprawy (`/sprawy/nowa`, `SprawaNowa`) —
   pole wyboru obywatela zasilane przez `GET /api/obywatele` (pełna lista, bez filtra zapytania —
   `obywateleApi.szukaj()` wołane bez argumentu w `sprawa-nowa.ts`); wybrany `obywatelId` trafia w
   żądaniu `POST /api/sprawy`. To jedyne miejsce w UI, w którym obywatel jest "dołączany" do nowego
   obiegu — wyłącznie przez wybór z istniejącej listy (zob. sekcja braków, p. 1).

## Dane testowe do manualnego przejścia przepływu

Repo nie udostępnia z UI żadnej ścieżki tworzenia obywatela/podmiotu (zob. braki), więc
pełnoprawne manualne przejście przepływu (zakładki z niepustymi danymi) wymaga obywateli już
zasianych migracją Liquibase `apps/document_api/src/main/resources/db/changelog/demo/100-seed.xml`
(changeset `100-01-obywatele`), połączonych w kolejnych changesetach ze sprawami/dokumentami/
przesyłkami/płatnościami:

- **Osoba fizyczna z ADE** — id `ob-1`, Jan Kowalski, PESEL `80010112345`, adres: ul. Kwiatowa 12,
  00-001 Warszawa, e-mail `jan.kowalski@example.pl`, tel. `500100200`, adres ADE
  `AE:PL-12345-67890-ABCDE-12`. Ma powiązane sprawy, dokumenty, przesyłki i płatności — najlepszy
  kandydat do przejścia wszystkich pięciu zakładek z niepustymi wynikami, w tym wysyłki pisma
  kanałem e-Doręczenia (ma ustawiony ADE).
- **Osoba fizyczna bez ADE** — id `ob-2`, Anna Nowak, PESEL `92051587654`, adres: ul. Słoneczna
  5/3, 30-002 Kraków. Dobry przypadek do zademonstrowania braku z p. 2 (wysyłka e-Doręczeniami
  zablokowana przez backend z powodu braku ADE, bez możliwości uzupełnienia go z UI).
- **Podmiot** — id `ob-3`, Budimex Serwis Sp. z o.o., NIP `5261234567`, adres: ul. Przemysłowa 44,
  61-003 Poznań, e-mail `kontakt@budimex-serwis.pl`.

Przykładowy fikcyjny rekord "nowego" obywatela/podmiotu (do ręcznego wstawienia migracją/insertem,
bo UI nie pozwala go wprowadzić — zob. braki):

- Osoba fizyczna: Maria Zielińska, PESEL `88071545600` (suma kontrolna poprawna, cyfra płci wskazuje
  kobietę), adres zamieszkania: ul. Ogrodowa 18/4, 20-123 Lublin, e-mail
  `maria.zielinska@przyklad.pl`, telefon `511222333`, adres ADE `AE:PL-55512-34567-LUBXX-07`
  (format zgodny z konwencją ADE użytą w danych demo).
- Osoba fizyczna: Piotr Wiśniewski, PESEL `90112311237` (suma kontrolna poprawna, cyfra płci
  wskazuje mężczyznę), adres: ul. Leśna 3, 87-100 Toruń, e-mail `piotr.wisniewski@przyklad.pl`,
  telefon `602111222`.
- Podmiot: Zakład Usług Komunalnych "EkoMiasto" Sp. z o.o., NIP `7346289010` (suma kontrolna
  poprawna), adres: ul. Fabryczna 9, 85-010 Bydgoszcz, e-mail `biuro@ekomiasto.pl`.

## Braki / działania niemożliwe z UI

1. **Brak dodania nowego obywatela/podmiotu z UI (nawet niejawnie przy rejestracji sprawy).**
   `ObywatelController` (`apps/document_api/src/main/java/robert_neat/document_api/controller/ObywatelController.java`)
   wystawia tylko `GET /api/obywatele` i `GET /api/obywatele/{id}` — brak `POST`/`PUT`.
   `ObywatelService` jest oznaczony `@Transactional(readOnly = true)`, a `ObywatelRepository`
   (`apps/document_api/.../repository/ObywatelRepository.java`) nie ma w całym kodzie backendu
   żadnego wywołania `.save(...)` — potwierdzone przeszukaniem całego modułu `document_api`.
   Formularz rejestracji nowej sprawy (`apps/document_web/src/app/features/sprawy/sprawa-nowa/sprawa-nowa.ts`)
   wymaga wyboru `obywatelId` z listy zwróconej przez `ObywateleApi.szukaj()` (pole
   `obywatelId = signal<string | null>(null)`) — nie pozwala wpisać nowych danych obywatela ad hoc.
   Jedyny sposób dodania nowego obywatela/podmiotu do systemu to insert poza aplikacją (migracja
   Liquibase/SQL), całkowicie poza zakresem `./apps`.
2. **Brak edycji danych kontaktowych/adresowych, w tym adresu ADE.** Widok
   `apps/document_web/src/app/features/obywatele/obywatel-dane/obywatel-dane.html` jest w całości
   deklaratywnym podglądem (`{{ obywatel.pole }}`) — nie zawiera żadnego formularza, przycisku
   "Edytuj" ani wywołania zapisu. Brak odpowiadającego endpointu `PUT/PATCH /api/obywatele/{id}` w
   `ObywatelController`. Konsekwencja funkcjonalna, nie tylko kosmetyczna: w
   `KorespondencjaService.nadajPismo` (`apps/document_api/.../service/KorespondencjaService.java`,
   ok. linii 126-130) nadanie pisma kanałem `E_DORECZENIA` jest blokowane wyjątkiem
   `BadRequestException`, jeśli `obywatel.getAdresDoreczenEDE()` jest `null`/pusty. Obywatel
   zasiany bez ADE (np. `ob-2` Anna Nowak) nigdy nie może otrzymać korespondencji przez
   e-Doręczenia, a nic w UI nie pozwala uzupełnić mu tego adresu. `delivery_mock` ma własne
   wbudowane UI (Thymeleaf — zob. `docs/development_docs/main_assumptions.md`, sekcja
   `delivery_mock`), ale służy ono wyłącznie do ręcznej symulacji zmiany statusu przesyłki (np.
   "doręczono", "zwrot") na potrzeby demo — nie wystawia żadnego formularza rejestracji/edycji
   adresu ADE obywatela. Adres ADE obywatela żyje w bazie `document_api` (kolumna
   `obywatel.adres_doreczen_ede`, osobny schemat od `delivery_mock`), więc nawet gdyby
   `delivery_mock` pozwalał zarejestrować ADE, nie ma mechanizmu synchronizacji tej wartości do
   `document_api`. Jedyne wyjście to zmiana danych bezpośrednio w bazie `document_api`, poza
   wszystkimi aplikacjami z `./apps`.
3. **Brak drill-down z zakładek "Dokumenty" i "Korespondencja" karty obywatela.** W przeciwieństwie
   do zakładki "Sprawy" (wiersze z `RouterLink` do `/sprawy/:id` — widoczne w `obywatel-sprawy.ts`,
   import `RouterLink`), szablony
   `apps/document_web/src/app/features/obywatele/obywatel-dokumenty/obywatel-dokumenty.html` i
   `.../obywatel-korespondencja/obywatel-korespondencja.html` renderują wiersze tabeli bez żadnego
   linku/akcji — nie importują `RouterLink` w odpowiadających komponentach `.ts`. Użytkownik widzi
   dokument/przesyłkę na liście, ale nie może przejść do jej szczegółów (podgląd pliku, podpis,
   historia statusu) bez ręcznego przejścia przez moduł Dokumenty/Korespondencja i ponownego
   wyszukania tego samego rekordu. Nie jest to blokujące (dane są dostępne gdzie indziej w UI), ale
   ogranicza użyteczność karty jako punktu agregacji.
4. **Akcje zbiorowe na liście obywateli są wyłącznie atrapą UI.** Checkbox-y wyboru wierszy
   (`wybrani`) w `obywatele-lista.html` odblokowują pasek "Akcja zbiorowa", ale przycisk jest trwale
   `[disabled]="true"` z tooltipem "Brak zdefiniowanych akcji zbiorowych dla obywateli w tej
   wersji" — brak odpowiadającego endpointu i brak realnej akcji, zgodnie z treścią tooltipa.

Pozostałe elementy przepływu (wyszukiwanie, przegląd karty ze wszystkimi pięcioma zakładkami,
agregacja danych z `document_api` dla spraw/dokumentów/korespondencji/rozrachunków, przekazywanie
kontekstu obywatela między pod-stronami i do modułu Sprawy) są w pełni dostępne z UI i wołają
rzeczywiste endpointy REST widoczne w `apps/document_web/src/app/core/api/**`.
