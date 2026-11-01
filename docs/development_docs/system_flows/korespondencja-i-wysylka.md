# Przepływ: Korespondencja i wysyłka pism

## Zakres

Scenariusz testowy do manualnego przejścia: nadanie podpisanego pisma do adresata, śledzenie
statusu doręczenia i symulacja zmiany statusu. Aplikacja: `document_web`
(`http://localhost:10600`) oraz panel symulacji doręczeń `delivery_mock`
(`http://localhost:10624`).

## Aktorzy i role

| Login w Keycloaku | Rola     | Osoba               |
| ----------------- | -------- | ------------------- |
| `demo.referent`   | referent | Katarzyna Zielińska |

Hasło: wspólne dla kont demo, zmienna `KEYCLOAK_DEMO_USER_PASSWORD` (domyślnie `change-me`).
Panel symulacji doręczeń (`delivery_mock`) nie wymaga logowania.

## Kroki przepływu

1. Zaloguj się jako `demo.referent`. Przejdź do menu **Korespondencja**.
2. Zobaczysz rejestr z dwiema zakładkami: "Wychodzące" i "Wpływające", oraz filtry statusu i
   kanału.
3. Kliknij przycisk **"Nadaj pismo"**.
4. W formularzu "Nadanie nowego pisma" wypełnij:
   - Sprawa: `"WO.6220.14.2026"` [select, required] — "Wniosek o wydanie decyzji środowiskowej", strona Jan Kowalski (ma
     adres do e-Doręczeń `AE:PL-12345-67890-ABCDE-12`).
   - Dokument: `"Decyzja środowiskowa"` [select, required — lista pokazuje tylko wersje ze statusem podpisu `poprawny`; wersje podpisane certyfikatem wygasłym/unieważnionym nie pojawią się na liście, mimo że formalnie są "podpisane"]. Na tej sprawie dokument ma już podpisaną wersję — jeśli u Ciebie nie ma jeszcze żadnej wersji podpisanej, wróć do modułu Dokumenty i podpisz go najpierw (zob. dokument "Dokumenty, szablony i podpis elektroniczny").
   - Adresat/obywatel: pozostaw wartość domyślną — Jan Kowalski [select, required, domyślnie strona przypisana do sprawy, edytowalne].
   - Kanał wysyłki: `"e-Doręczenia"` [select, required — jedna z 3 wartości enuma: `e-doreczenia`, `operator_pocztowy`, `email`]. **Wysłanie kanałem e-Doręczenia jest blokowane przez backend (400), jeśli obywatel nie ma adresu ADE** — front tylko ostrzega, backend odrzuca żądanie.

   Kliknij **"Nadaj pismo"**.
5. Po sukcesie zobaczysz kartę potwierdzenia "Pismo zostało nadane" z przyciskiem **"Przejdź do
   szczegółów przesyłki"**. Kliknij go.
6. Na ekranie szczegółów przesyłki zobaczysz numer referencyjny, status i kartę "Śledzenie
   statusu" (oś czasu). Status odświeża się automatycznie co 8 sekund.
7. **Symulacja zmiany statusu** — otwórz w nowej karcie przeglądarki panel
   `http://localhost:10624/przesylki`. Znajdź przesyłkę o numerze referencyjnym **PL2026000998**
   (kanał operator pocztowy, status "nadane", strona Anna Nowak — bez adresu do e-Doręczeń; to
   właściwy kandydat do tego kroku, w przeciwieństwie do przesyłki demo **ADE/2026/00123**, która
   jest już w stanie końcowym "doręczone" i nie da się na niej zademonstrować zmiany statusu, zob.
   "Znane ograniczenia", pkt 3) i kliknij "Szczegóły". W sekcji "Symuluj zmianę statusu":
   - Nowy status: `"doreczono"` (albo `"zwrot"`/`"awizo"`) [select, required — wartości enuma `StatusPrzesylki`: `nadane`, `doreczone`, `odebrane`, `zwrot`, `awizo`]
   - Szczegóły: opcjonalny opis tekstowy, np. `"Doręczono osobiście adresatowi"` [text, optional]

   Kliknij **"Symuluj"**. Po jednej symulacji status tej przesyłki zmieni się trwale — powtórny
   test zmiany statusu wymaga nadania nowego pisma.
8. Wróć do karty z `document_web` (krok 6) i poczekaj do 8 sekund — status przesyłki powinien się
   zaktualizować automatycznie, bez odświeżania strony.

## Znane ograniczenia (czego nie da się zrobić w aplikacji)

1. **Pismo nadane przez `document_web` (poza dwoma wpisami z danych demo) nie pojawi się
   automatycznie w panelu symulacji `delivery_mock`.** Panel symulacji nie ma formularza do
   ręcznego dodania przesyłki — dla pisma nadanego w kroku 4 nie da się przejść kroku 7 (symulacja
   zmiany statusu) z poziomu samej aplikacji. Można tylko obserwować status jako "nadane" na stałe
   w `document_web`.
2. **Rejestracja przesyłki PRZYCHODZĄCEJ (wpływającej) nie jest możliwa z aplikacji w żadnej
   formie.** Zakładka "Wpływające" w rejestrze korespondencji będzie zawsze pusta — nie ma
   przycisku, formularza ani żadnej akcji, która zapisałaby przesyłkę przychodzącą.
3. **Status przesyłki ADE/2026/00123 jest już w stanie końcowym ("doręczone")** — nie da się na
   niej zademonstrować zmiany statusu; użyj PL2026000998 zamiast niej (zob. krok 7).
4. **Status nie odświeża się natychmiast** — `document_web` odpytuje status co 8 sekund (nie ma
   mechanizmu powiadomienia w czasie rzeczywistym), więc zmiana widoczna jest z opóźnieniem do 8 s.
