# Przepływ: Pulpit użytkownika

## Zakres

Scenariusz testowy do manualnego przejścia: przegląd pulpitu (strona startowa po zalogowaniu) i
weryfikacja, że nowo zarejestrowana sprawa z bliskim terminem ustawowym pojawia się na kafelkach i
listach pulpitu. Aplikacja: `document_web` (`http://localhost:10600`).

## Aktorzy i role

| Login w Keycloaku | Rola     | Osoba (widoczna w nagłówku pulpitu) |
| ----------------- | -------- | ----------------------------------- |
| `demo.referent`   | referent | Katarzyna Zielińska                 |

Każda zalogowana rola ma własny pulpit z własnymi danymi — do tego scenariusza wystarczy jedno
konto, `demo.referent`. Hasło: wspólne dla kont demo, zmienna `KEYCLOAK_DEMO_USER_PASSWORD`
(domyślnie `change-me`).

## Kroki przepływu

1. Zaloguj się jako `demo.referent`. Po zalogowaniu trafiasz automatycznie na ekran **Pulpit** —
   to domyślna strona startowa, nie trzeba nic klikać.
2. Na pulpicie zobaczysz 4 kafelki liczbowe: "Sprawy w toku", "Terminy zbliżające się /
   przekroczone", "Nierozliczone należności (wydział)", "Przesyłki oczekujące na status". Pod nimi
   tabelę **"Moje sprawy"** i listę **"Terminy ustawowe"**.
3. Zapamiętaj aktualną liczbę na kafelku "Sprawy w toku" i na kafelku "Terminy zbliżające się /
   przekroczone".
4. Przejdź do menu **Sprawy** → przycisk **"Nowa sprawa"** i zarejestruj nową sprawę:
   - Tytuł sprawy: `"Wniosek o wydanie decyzji — test terminu"` [text, required, max 512 znaków]
   - Pozycja JRWA: `"6220 — Decyzje o środowiskowych uwarunkowaniach"` [select, required] (kategoria archiwalna `A` uzupełni się automatycznie)
   - Definicja obiegu: `"Obieg wniosku o wydanie decyzji administracyjnej"` [select, required]
   - Obywatel/podmiot: `"Jan Kowalski"` (albo `"Anna Nowak"`) [select, required]
   - Wydział: `"Wydział Ochrony Środowiska"` [select, required — jedna z 4 zahardkodowanych wartości]
   - Referent: pozostaw wartość domyślną — automatycznie wypełnioną imieniem i nazwiskiem
     zalogowanego użytkownika (`Katarzyna Zielińska`) [text, required, auto-wypełniane,
     edytowalne] — **nie zmieniaj tego pola**: zmiana na inną wartość spowoduje, że sprawa NIE
     pojawi się na pulpicie (pulpit dopasowuje sprawy dokładnie po tym polu, exact-match stringa,
     nie dopasowanie rozmyte).
   - Termin ustawowy: data = dziś + 3 dni (scenariusz "zbliżający się"), np. `2026-11-04`, albo
     dziś − 2 dni (scenariusz "przeterminowany"), np. `2026-10-30` [date, optional, format
     ISO-8601 rrrr-mm-dd — pole nie ma żadnego ograniczenia min/max, data przeszła jest
     technicznie akceptowana przez backend]
5. Po zapisaniu formularza wrócisz na szczegóły nowej sprawy. Kliknij **Pulpit** w menu bocznym,
   żeby wrócić i odświeżyć widok.
6. Sprawdź, że:
   - kafelek "Sprawy w toku" wzrósł o 1,
   - nowa sprawa jest widoczna w tabeli "Moje sprawy",
   - w liście "Terminy ustawowe" pojawił się wpis z etykietą "3 dni" (termin za 3 dni) albo
     "Przeterminowane (2 dni)" (termin 2 dni temu) — zależnie od daty wpisanej w formularzu,
   - kafelek "Terminy zbliżające się / przekroczone" wzrósł o 1 i ma tag "Wymaga uwagi".
7. Kliknij wiersz nowej sprawy w tabeli "Moje sprawy" (albo wpis na liście "Terminy ustawowe") —
   przejdziesz do szczegółów sprawy. To potwierdza, że nawigacja z pulpitu prowadzi do pełnych
   szczegółów sprawy.

## Znane ograniczenia (czego nie da się zrobić w aplikacji)

1. **Nie ma żadnych powiadomień o zbliżającym się/przekroczonym terminie** — ani e-mail, ani push,
   ani żadny komunikat w aplikacji. Pulpit tylko pokazuje aktualny stan terminów, kiedy sam go
   otworzysz — jeśli nikt nie zaloguje się do systemu, przekroczony termin przejdzie niezauważony.
2. **Nagłówek pulpitu nie pokazuje stanowiska/wydziału zalogowanego użytkownika** — te dwa pola są
   zawsze puste, niezależnie od zalogowanego konta (dane te nie są dostępne w tokenie logowania).
3. **Kafelki "Nierozliczone należności" i "Przesyłki oczekujące" nie mają przejścia dalej** — to
   same liczby, kliknięcie nie przenosi do żadnej listy. Żeby zobaczyć te listy, trzeba przejść
   przez osobne menu Płatności / Korespondencja.
4. **Filtrowanie "moje sprawy" jest tylko orientacyjne przy dużej liczbie spraw w systemie** — przy
   skali danych demo działa poprawnie, ale nie jest to gwarantowana funkcja API.
5. **Nie istnieje żadna "skrzynka zadań"** — pulpit pokazuje tylko sprawy już przypisane do
   referenta, a nie listę spraw/dokumentów czekających na przydzielenie czy decyzję (tzw.
   dekretacja — zob. ograniczenia w dokumencie "Sprawy i obiegi").
