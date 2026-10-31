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
4. W formularzu "Nadanie nowego pisma": wybierz sprawę, następnie podpisany dokument (lista
   pokazuje tylko wersje już podpisane — jeśli dokument sprawy nie jest jeszcze podpisany, wróć do
   modułu Dokumenty i podpisz go najpierw), wybierz adresata (domyślnie obywatel przypisany do
   sprawy) i kanał wysyłki (e-Doręczenia / operator pocztowy / e-mail). Jeśli obywatel nie ma
   adresu do e-Doręczeń, aplikacja ostrzeże o tym przed wysłaniem. Kliknij **"Nadaj pismo"**.
5. Po sukcesie zobaczysz kartę potwierdzenia "Pismo zostało nadane" z przyciskiem **"Przejdź do
   szczegółów przesyłki"**. Kliknij go.
6. Na ekranie szczegółów przesyłki zobaczysz numer referencyjny, status i kartę "Śledzenie
   statusu" (oś czasu). Status odświeża się automatycznie co 8 sekund.
7. **Symulacja zmiany statusu** — otwórz w nowej karcie przeglądarki panel
   `http://localhost:10624/przesylki`. Znajdź przesyłkę o tym samym numerze referencyjnym co w
   kroku 6 i kliknij "Szczegóły". W sekcji "Symuluj zmianę statusu" wybierz nowy status z listy,
   opcjonalnie wpisz szczegóły, kliknij **"Symuluj"**.
8. Wróć do karty z `document_web` (krok 6) i poczekaj do 8 sekund — status przesyłki powinien się
   zaktualizować automatycznie, bez odświeżania strony.

## Dane testowe do manualnego przejścia przepływu

- Sprawa `WO.6220.14.2026` — "Wniosek o wydanie decyzji środowiskowej", strona Jan Kowalski (ma
  adres do e-Doręczeń `AE:PL-12345-67890-ABCDE-12` — użyj kanału e-Doręczenia).
- Dokument "Decyzja środowiskowa" na tej sprawie ma już podpisaną wersję — gotowy do wysyłki bez
  dodatkowego podpisywania.
- Przesyłka już nadana w danych demo: numer referencyjny **ADE/2026/00123**, kanał e-Doręczenia,
  status już "doręczone" (stan końcowy) — **nie nadaje się do demonstracji zmiany statusu w
  panelu symulacji**, bo jest już w stanie końcowym (zob. "Znane ograniczenia", pkt 3).
- Druga przesyłka demo: numer **PL2026000998**, kanał operator pocztowy, status "nadane", strona
  Anna Nowak (nie ma adresu do e-Doręczeń) — **ta przesyłka jest właściwym kandydatem do kroku 7**,
  bo wciąż jest w stanie "nadane" i da się na niej przetestować zmianę statusu na
  "doręczono"/"zwrot"/"awizo". Po jednej symulacji jej status zmieni się trwale — powtórny test
  zmiany statusu wymaga nadania nowego pisma.

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
   niej zademonstrować zmiany statusu; użyj PL2026000998 zamiast niej (zob. "Dane testowe").
4. **Status nie odświeża się natychmiast** — `document_web` odpytuje status co 8 sekund (nie ma
   mechanizmu powiadomienia w czasie rzeczywistym), więc zmiana widoczna jest z opóźnieniem do 8 s.
