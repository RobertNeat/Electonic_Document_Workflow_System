# Przepływ: Pulpit użytkownika

## Zakres

Pulpit (`apps/document_web/src/app/features/pulpit/pulpit-strona/`) to strona startowa po
zalogowaniu (`redirectTo: 'pulpit'` w `app.routes.ts`, podobnie trasa wildcard `**`). Wyświetla
4 kafelki liczbowe (sprawy w toku, terminy krytyczne, nierozliczone należności, przesyłki
oczekujące), tabelę "Moje sprawy" i listę "Terminy ustawowe" z etykietą dni do terminu /
przeterminowania. Dane w kafelkach, tabeli spraw i liście terminów pochodzą z rzeczywistych endpointów REST
(`GET /api/sprawy`, `GET /api/naleznosci`, `GET /api/przesylki`, `GET /api/obywatele/:id`) —
poza dwoma polami w nagłówku strony (`stanowisko`, `wydzial`), które są zawsze pustym stringiem
niezależnie od API (zob. Braki, pkt 2). Backend nie udostępnia dedykowanych endpointów "pulpitowych"
(nie istnieje kontroler/serwis `Pulpit*`) — cała logika agregacji i filtrowania "moje"/"wydziału"
żyje po stronie frontendu.

## Kroki przepływu (UI)

1. Zalogowanie (Keycloak) → przekierowanie na `/pulpit` (domyślna trasa) — ekran
   `pulpit-strona.html`, bez dodatkowego kliknięcia.
2. Pobranie danych do kafelków i tabel, wykonywane równolegle w konstruktorze komponentu
   (`forkJoin`):
   - lista spraw referenta — endpoint REST użyty: `GET /api/sprawy?rozmiarStrony=200`
     (bez filtra `referent` — filtrowanie `referent === "<imie> <nazwisko>" && !zamknieta`
     wykonywane lokalnie w `mojeSprawy()`).
   - należności wydziału — endpoint REST użyty: `GET /api/naleznosci` (bez filtra statusu/wydziału,
     filtrowanie lokalne w `nierozliczoneNaleznosci()`).
   - przesyłki wydziału — endpoint REST użyty: `GET /api/przesylki` (bez filtra statusu/wydziału,
     filtrowanie lokalne w `przesylkiOczekujace()`).
3. Dla każdej unikalnej strony (obywatela) spraw referenta — dociągnięcie nazwy strony:
   endpoint REST użyty: `GET /api/obywatele/:id` (wywoływane per‑id w `effect()`, wynik cache'owany
   w mapie w pamięci komponentu).
4. Kafelek "Sprawy w toku" — `liczbaSprawWToku()` = długość `mojeSprawy()` (czysto klientowe
   zliczanie, bez dodatkowego wywołania).
5. Kafelek "Terminy zbliżające się / przekroczone" — `liczbaTerminowKrytycznych()` =
   liczba spraw referenta z `terminUstawowy` ustawionym i `dniDoTerminu <= 7` (liczone w
   przeglądarce z pola `Sprawa.terminUstawowy` zwróconego przez `GET /api/sprawy`; brak
   odrębnego endpointu "terminy/powiadomienia").
6. Kafelek "Nierozliczone należności (wydział)" i "Przesyłki oczekujące na status" — analogicznie
   zliczanie klientowe na podstawie już pobranych list z kroku 2.
7. Tabela "Moje sprawy" — kliknięcie wiersza (`(click)="otworzSprawe(sprawa.id)"`) → nawigacja
   `router.navigate(['/sprawy', sprawaId])`, czyli przejście do widoku szczegółów sprawy
   (`apps/document_web/src/app/features/sprawy/sprawa-szczegoly/`, trasa `sprawy/:id` istnieje w
   `sprawy.routes.ts`). Endpoint REST wywoływany dalej przez ten widok: `GET /api/sprawy/:id`
   (poza zakresem pulpitu, ale potwierdza że nawigacja prowadzi do pełnego, działającego ekranu).
8. Lista "Terminy ustawowe" — każdy wpis to link (`routerLink="['/sprawy', wpis.sprawa.id]"`) z tą
   samą nawigacją do szczegółów sprawy co w kroku 7.

Kafelki "Nierozliczone należności" i "Przesyłki oczekujące" są tylko liczbami — nie mają
nawigacji dalej (nie są linkiem ani elementem klikalnym w `pulpit-strona.html`).

## Dane testowe do manualnego przejścia przepływu

Aby pulpit pokazał sprawę z bliskim/przekroczonym terminem ustawowym, trzeba ją najpierw
utworzyć w module Sprawy (`/sprawy/nowa`) — formularz `sprawa-nowa.ts`/`.html` ma pola
`Wydział`, `Referent` (domyślnie wypełniane imieniem/nazwiskiem zalogowanego użytkownika,
edytowalne) i `Termin ustawowy` (data, pole bez ustawionego `minDate` — dopuszcza też datę w
przeszłości), które mapują się 1:1 na pola `UtworzSpraweRequest.wydzial/referent/terminUstawowy`
wysyłane w `POST /api/sprawy`. `SprawaController`/`POST /api/sprawy` nie ma żadnej adnotacji
`@PreAuthorize` ograniczającej rolę, a trasa `/sprawy/nowa` nie jest objęta osobnym
route-guardem rolowym w `sprawy.routes.ts` — jeden zalogowany użytkownik (referent) może
samodzielnie utworzyć sprawę i zobaczyć efekt na własnym pulpicie, bez potrzeby drugiego
użytkownika/roli.

Konkretne, realne dane z seeda demo (`apps/document_api/src/main/resources/db/changelog/demo/
100-seed.xml`, kontekst Liquibase `demo`) do wykorzystania w UI:

- Zaloguj się jako `demo.referent` (seed użytkownika: imię `Katarzyna`, nazwisko `Zielińska`,
  wydział `Wydział Ochrony Środowiska`, stanowisko `Referent`). Ważne: jeśli konto Keycloak
  `demo.referent` nie ma ustawionych atrybutów `given_name`/`family_name` zgodnych z tymi
  wartościami, formularz "Nowa sprawa" wypełni pole `Referent` samym loginem
  (`sprawa-nowa.ts:93`, fallback na `uzytkownik.login` gdy `imie`/`nazwisko` są puste) i sprawa
  nie dopasuje się do żadnego referenta na pulpicie (pulpit porównuje dokładny string
  `"<imie> <nazwisko>"`).
- Definicja obiegu: `Obieg wniosku o wydanie decyzji administracyjnej` (seed id
  `obieg-decyzja`).
- Strona (obywatel): `Jan Kowalski` (seed id `ob-1`) albo `Anna Nowak` (seed id `ob-2`).
- Wydział: `Wydział Ochrony Środowiska` — ten sam wydział co `demo.referent`, potrzebny również,
  żeby kafelki "Nierozliczone należności"/"Przesyłki oczekujące" (liczone po
  `przyblizonyWydzial()`) mogły zareagować na tę sprawę.
- Kategoria archiwalna: `A`.
- Tytuł: dowolny, np. `Wniosek o wydanie decyzji — test terminu`.
- Referent: pozostawić wartość domyślną wypełnioną automatycznie (`Katarzyna Zielińska`) — zmiana
  tego pola na inną wartość spowoduje, że sprawa NIE pojawi się na pulpicie zalogowanego
  użytkownika.
- Termin ustawowy: data = dziś + 3 dni (scenariusz "zbliżający się", próg kafelka `<= 7 dni`)
  albo data = dziś − 2 dni (scenariusz "przeterminowany").

Po zapisaniu sprawa natychmiast pojawi się na `/pulpit`: kafelek "Sprawy w toku" wzrośnie o 1,
sprawa pojawi się w tabeli "Moje sprawy", a w sekcji "Terminy ustawowe" pojawi się wpis z
etykietą `etykietaTerminu` — "3 dni" (severity `warn`) albo "Przeterminowane (2 dni)" (severity
`danger`); kafelek "Terminy zbliżające się / przekroczone" wzrośnie o 1 i pokaże tag
"Wymaga uwagi" (ten tag jest renderowany na kafelku, nie na pozycji listy terminów). Działa to
natychmiast po otwarciu/odświeżeniu `/pulpit`, bez dodatkowej konfiguracji.

## Braki / działania niemożliwe z UI

1. **Powiadomienia o zbliżającym się/przekroczonym terminie nie istnieją — ani w backendzie, ani
   jako osobny mechanizm w UI.** `main_assumptions.md` (linia 27) zakłada: "Pilnowanie terminów
   ustawowych (np. terminy z KPA) wraz z powiadomieniami o zbliżającym się/przekroczonym
   terminie." W całym `apps/document_api/src/main/java` nie ma żadnego zadania `@Scheduled`,
   żadnego kontrolera/serwisu/encji z nazwą zawierającą "powiadomien"/"notification", żadnego
   mechanizmu e-mail/push/in-app. Jedyne co istnieje to klientowe (frontendowe) przeliczenie
   `dniDoTerminu()` w `pulpit-strona.ts` wykonywane dopiero, gdy użytkownik sam otworzy stronę
   pulpitu — to jest pasywny widok ("sprawdź przy wejściu"), nie powiadomienie ("poinformuj mnie
   gdy coś się zbliża"), więc KPA-termin przekroczony podczas gdy użytkownik nie loguje się do
   systemu przejdzie niezauważony. Dowód: brak wyników `grep -r "@Scheduled"` i
   `grep -ri "powiadomien\|notification\|JavaMailSender\|spring.mail"` w całym katalogu `apps`
   (sprawdzone we wszystkich modułach: `document_api`, `document_web`, `dms_service`,
   `jasper_service`, `sign_service`, `delivery_mock`) — żaden moduł nie implementuje wysyłki
   powiadomień e-mail/push/in-app ani zadania w tle liczącego terminy.
2. **Nagłówek pulpitu wyświetla puste dane stanowiska/wydziału użytkownika.** Szablon
   `pulpit-strona.html` renderuje `{{ u.stanowisko }} · {{ u.wydzial }}`, ale
   `apps/document_web/src/app/core/services/auth-claims.ts` (`mapujClaimyNaUzytkownika`)
   ustawia `wydzial: '', stanowisko: ''` na trwałe (komentarz w kodzie: "nie mają odpowiednika
   w tokenie Keycloak - puste do przyszłego uzupełnienia"). W efekcie podtytuł nagłówka pulpitu
   będzie zawsze wyglądał jak `· ` (dwa puste pola rozdzielone separatorem) — to jest
   placeholder, nie dana rzeczywista z API/tokenu. Pulpit obchodzi ten brak przez przybliżenie
   wydziału na podstawie najnowszej sprawy referenta (`przyblizonyWydzial()`), używane tylko do
   filtrowania kafelków należności/przesyłek, a nie do samego nagłówka.
3. **Kafelki "Nierozliczone należności" i "Przesyłki oczekujące" nie mają przejścia dalej.**
   To tylko liczby bez nawigacji — kliknięcie nie przenosi do listy należności/przesyłek
   (potwierdzone w `pulpit-strona.html`: brak `routerLink`/`(click)` na tych kartach). Dostęp do
   pełnych list jest możliwy, ale tylko przez osobne menu (moduły Płatności/Przesyłki), nie z
   samego pulpitu.
4. **Filtrowanie "moje sprawy"/"wydział" jest obejściem frontendowym, nie realną funkcją API** —
   potwierdzone w kodzie i w `docs/development_docs/currentwork/contracts/document_api_pulpit.md`:
   `GET /api/sprawy` wspiera filtr `wydzial`, ale nie `referent` (potwierdzone w
   `SprawaController.szukaj`, linia z `@RequestParam` listą parametrów), więc frontend ściąga do
   200 spraw i filtruje po dokładnym stringu `imie + " " + nazwisko` lokalnie. Przy >200 sprawach
   w systemie część spraw referenta może nie być widoczna na pulpicie (komentarz w kodzie to
   potwierdza, akceptowalne tylko przy obecnej skali danych demo).
5. **Brak "skrzynki zadań" — dekretacja dokumentów/spraw czekających na akcję referenta nie jest
   widoczna na pulpicie (i nie jest zaimplementowana jako funkcja, tylko jako etykieta
   historyczna).** `main_assumptions.md` (linia 23) wymaga: "Dekretacja dokumentów przychodzących
   do właściwego wydziału/referenta." W kodzie "Dekretacja" występuje wyłącznie jako: (a) wartość
   enuma `TypOperacjiAudytu` (`apps/document_api/.../dto/administracja/TypOperacjiAudytu.java`) —
   czyli etykieta typu zdarzenia w dzienniku audytu, oraz (b) przykładowe dane historyczne w
   seedzie demo (`metryka`/`audit_log`, np. wpis `opis="Przekazano do referenta"`,
   `czynnosc="Dekretacja"`). Nie ma żadnego endpointu typu "zdekretuj dokument/sprawę", żadnej
   kolejki/skrzynki "do przydzielenia" ani ekranu z listą dokumentów czekających na przydzielenie
   do referenta. Pulpit pokazuje tylko sprawy już przypisane (`referent` ustawiony w momencie
   rejestracji sprawy) — nie pokazuje "zadań" w sensie rzeczy czekających na akcję użytkownika
   (np. "przydziel", "zaakceptuj", "podejmij decyzję"), tylko statyczną listę spraw i ich
   terminów. To realny gap względem słowa "zadania" z zakresu pulpitu zdefiniowanego w zadaniu.

Poza punktem 1 (brak całego mechanizmu powiadomień) i punktem 2 (martwe pole UI), przepływ
przeglądania pulpitu i przejścia do szczegółów sprawy jest w pełni wykonywalny z poziomu UI
`document_web`, bez surowych zapytań REST.
