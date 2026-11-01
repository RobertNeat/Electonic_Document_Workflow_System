# Przepływ: Karta obywatela/podmiotu

## Zakres

Scenariusz testowy do manualnego przejścia: wyszukanie obywatela/podmiotu, przegląd jego karty z
pięcioma zakładkami (Dane podstawowe / Sprawy / Dokumenty / Korespondencja / Rozrachunki) oraz
"przypięcie" obywatela do kontekstu aplikacji. Aplikacja: `document_web`
(`http://localhost:10600`).

## Aktorzy i role

| Login w Keycloaku | Rola     | Osoba               |
| ----------------- | -------- | ------------------- |
| `demo.referent`   | referent | Katarzyna Zielińska |

Hasło: wspólne dla kont demo, zmienna `KEYCLOAK_DEMO_USER_PASSWORD` (domyślnie `change-me`). Ten
przepływ działa identycznie dla każdej roli — wystarczy jedno konto.

## Kroki przepływu

1. Zaloguj się jako `demo.referent`. Przejdź do menu **Obywatele**.
2. Wpisz w polu wyszukiwania [text, optional — wyszukiwanie częściowe, case-insensitive, po
   imieniu/nazwisku/nazwie podmiotu/PESEL/NIP; brak walidacji formatu lub minimalnej długości
   zapytania] frazę `"Kowalski"` — albo `"80010112345"` (PESEL Jana Kowalskiego), albo `"Budimex"`.
   Lista zawęzi się automatycznie.
3. Kliknij wiersz **Jan Kowalski** — otworzy się karta z nazwą/imieniem i nazwiskiem w
   nagłówku oraz pięcioma zakładkami. (Jan Kowalski — PESEL `80010112345` [**system nie waliduje
   formatu ani sumy kontrolnej PESEL**, pole przyjmuje dowolny ciąg znaków], adres: ul. Kwiatowa
   12, 00-001 Warszawa, e-mail `jan.kowalski@example.pl`, telefon `500100200`, ma adres do
   e-Doręczeń — ma powiązane sprawy, dokumenty, przesyłki i płatności, więc jest najlepszym
   kandydatem do przejścia wszystkich pięciu zakładek z niepustymi wynikami.)
4. Zakładka **"Dane podstawowe"** (domyślna) — podgląd danych: imię/nazwisko lub nazwa podmiotu,
   PESEL/NIP, adres, e-mail, telefon, adres do e-Doręczeń (jeśli ustawiony). Te dane są wyłącznie
   do wyszukania i podglądu — karta obywatela nie ma formularza do edycji żadnego z tych pól (zob.
   "Znane ograniczenia", pkt 2).
5. Zakładka **"Sprawy"** — lista spraw tego obywatela. Kliknij wiersz, żeby przejść do pełnych
   szczegółów sprawy.
6. Zakładka **"Dokumenty"** — lista dokumentów powiązanych ze sprawami tego obywatela (bez
   możliwości kliknięcia wiersza — zob. "Znane ograniczenia", pkt 2).
7. Zakładka **"Korespondencja"** — lista przesyłek tego obywatela (również bez kliknięcia
   wiersza).
8. Zakładka **"Rozrachunki"** — lista należności i wpłat tego obywatela, z sumą "do zapłaty".
9. **Przypięcie obywatela** — na karcie szczegółów sprawy (moduł Sprawy) kliknij przycisk
   **"Przypnij"** obok nazwiska strony. W lewym menu bocznym pojawi się dodatkowa sekcja
   "Karta obywatela" z pięcioma skróconymi pozycjami (te same zakładki co wyżej) i chip z nazwiskiem
   obywatela w górnym pasku — kliknięcie "x" na chipie usuwa przypięcie.
10. (Opcjonalnie, żeby zademonstrować ograniczenie kanału e-Doręczeń) wyszukaj `"Anna Nowak"` —
    PESEL `92051587654`, adres: ul. Słoneczna 5/3, 30-002 Kraków, **bez adresu do e-Doręczeń** —
    dobry przykład do pokazania ograniczenia z dokumentu "Korespondencja i wysyłka pism" (kanał
    e-Doręczenia zablokowany dla tej osoby). Albo wyszukaj `"Budimex"` — podmiot "Budimex Serwis
    Sp. z o.o.", NIP `5261234567` [**brak walidacji formatu/sumy kontrolnej NIP w systemie**],
    adres: ul. Przemysłowa 44, 61-003 Poznań, e-mail `kontakt@budimex-serwis.pl`.

## Znane ograniczenia (czego nie da się zrobić w aplikacji)

1. **Nie da się dodać nowego obywatela/podmiotu z aplikacji** — ani jako samodzielna akcja, ani
   przy rejestracji nowej sprawy (formularz pozwala wybrać tylko z istniejącej listy).
2. **Nie da się edytować danych kontaktowych/adresowych obywatela, w tym adresu do
   e-Doręczeń** — zakładka "Dane podstawowe" jest czystym podglądem, bez żadnego przycisku
   "Edytuj". W praktyce oznacza to, że obywatel bez adresu do e-Doręczeń (np. Anna Nowak) nigdy nie
   będzie mógł otrzymać korespondencji tym kanałem — nic w aplikacji nie pozwala mu tego adresu
   uzupełnić.
3. **Nie da się przejść do szczegółów dokumentu/przesyłki wprost z zakładek "Dokumenty" i
   "Korespondencja" karty obywatela.** Żeby zobaczyć szczegóły (podgląd pliku, podpis, historia
   statusu), trzeba przejść przez moduł Dokumenty/Korespondencja i ponownie wyszukać ten sam
   rekord.
4. **Przycisk "Akcja zbiorowa" na liście Obywateli jest trwale wyszarzony** — nie ma żadnej akcji
   zbiorowej zdefiniowanej dla obywateli.
