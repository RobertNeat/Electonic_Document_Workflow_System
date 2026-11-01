# Przepływ: Sprawy i obiegi

## Zakres

Scenariusz testowy do manualnego przejścia: rejestracja sprawy administracyjnej, przegląd jej
szczegółów, oraz przeprowadzenie sprawy przez wszystkie stany obiegu — w tym krok wymagający innej
roli (kierownik) i dołączenia dokumentu wymaganego typu. Aplikacja: `document_web`
(`http://localhost:10600`).

## Aktorzy i role

| Login w Keycloaku | Rola      | Osoba               | Kiedy potrzebny w tym przepływie                        |
| ----------------- | --------- | ------------------- | ------------------------------------------------------- |
| `demo.referent`   | referent  | Katarzyna Zielińska | rejestracja sprawy, przejścia stanów niewymagające roli |
| `demo.kierownik`  | kierownik | Piotr Lewandowski   | przejście stanu "Do akceptacji → Zakończona"            |

Hasło obu kont: wspólne dla kont demo, zmienna `KEYCLOAK_DEMO_USER_PASSWORD` (domyślnie
`change-me`).

## Kroki przepływu

1. Zaloguj się jako `demo.referent`. Przejdź do menu **Sprawy**.
2. Zobaczysz listę spraw z wyszukiwaniem i filtrami (stan, wydział, treść). Kliknij przycisk
   **"Nowa sprawa"**.
3. W formularzu "Rejestracja nowej sprawy" wypełnij pola (scenariusz 1 — sprawa, na której w
   kroku 7 zademonstrujesz przejście wymagające roli kierownik):
   - Definicja obiegu: `"Obieg wniosku o wydanie decyzji administracyjnej"` [select, required — jedna z 4 wartości zdefiniowanych w danych demo: "Obieg wniosku o wydanie decyzji administracyjnej", "Obieg skargi / wniosku", "Obieg faktury kosztowej", "Obieg wewnętrznego pisma okólnego"]
   - Pozycja JRWA: `"6220 — Decyzje o środowiskowych uwarunkowaniach"` [select, required — jedna z 3 pozycji w danych demo: `1510` (Skargi i wnioski, kat. B5), `3120` (Faktury i rachunki, kat. B5), `6220` (Decyzje o środowiskowych uwarunkowaniach, kat. A)]
   - Kategoria archiwalna: `A` — uzupełnia się automatycznie po wyborze pozycji JRWA, nie wpisuj ręcznie (pole tylko do odczytu)
   - Tytuł sprawy: `"Wniosek o wydanie decyzji o środowiskowych uwarunkowaniach dla inwestycji przy ul. Polnej 14 w Rzeszowie"` [text, required, max 512 znaków]
   - Obywatel/podmiot: `"Jan Kowalski"` [select, required — wybór z istniejącej listy obywateli, PESEL `80010112345`]
   - Wydział: `"Wydział Ochrony Środowiska"` [select, required — lista zawsze złożona z tych samych 4 wartości: "Wydział Ochrony Środowiska", "Wydział Organizacyjny", "Wydział Księgowości", "Wydział Komunikacji"]
   - Referent: pozostaw wartość domyślną, `Katarzyna Zielińska` [text, required, auto-wypełniane imieniem i nazwiskiem zalogowanego użytkownika, edytowalne — nie zmieniaj]
   - Termin ustawowy: data za 30 dni od dnia rejestracji, np. `2026-12-01` [date, optional, format ISO-8601 rrrr-mm-dd — pole nie ma żadnego ograniczenia min/max, przyjmuje też datę przeszłą]

   Kliknij **"Zarejestruj sprawę"**. Znak sprawy (numer JRWA) nadaje się sam — nie wpisuj
   go ręcznie, bo nie ma takiego pola. Żadne z powyższych pól tekstowych (Tytuł, Referent) nie ma
   walidacji formatu poza sprawdzeniem niepustości — jedyny realny limit to 512 znaków na tytule
   (kolumna bazy danych).

   **Alternatywnie (scenariusz 2 — prosty obieg, bez wymogów roli/dokumentu):** Definicja obiegu
   `"Obieg skargi / wniosku"`, Pozycja JRWA `"1510 — Skargi i wnioski"` (kategoria archiwalna
   uzupełni się jako `B5`), Tytuł sprawy `"Skarga na niewłaściwe oznakowanie przejścia dla
   pieszych przy ul. Kolejowej 7"`, Obywatel/podmiot `"Anna Nowak"` (PESEL `92051587654`, bez
   adresu do e-Doręczeń), Wydział `"Wydział Organizacyjny"`, Referent — wartość domyślna. Sekwencja
   stanów tego obiegu: Zarejestrowana → W toku → Odpowiedź przygotowana → Zakończona (żadne z tych
   przejść nie wymaga roli ani dokumentu).
4. Po zapisaniu trafisz na ekran szczegółów sprawy: dane podstawowe, karta "Obieg sprawy" z
   paskiem stanów, karta "Metryka sprawy" (historia czynności) i przycisk **"Zobacz dokumenty
   sprawy"**.
5. Na karcie "Obieg sprawy" zobaczysz przycisk **"Przejdź do: {nazwa następnego stanu}"** dla
   każdego dozwolonego przejścia z aktualnego stanu. Kliknij go, żeby przejść sprawę dalej —
   przycisk jest aktywny tylko wtedy, gdy Twoja rola i dokumenty na sprawie spełniają wymagania
   danego przejścia (krok 7 poniżej opisuje przejście, które wymaga zmiany użytkownika).
6. Po każdej zmianie stanu karta "Metryka sprawy" aktualizuje się automatycznie — sprawdź, że na
   liście czynności widać nowy wpis.
7. **Przejście wymagające roli i dokumentu** (dotyczy obiegu "Obieg wniosku o wydanie decyzji
   administracyjnej", przejście "Do akceptacji → Zakończona"):
   - to przejście wymaga roli **kierownik** oraz dołączonego do sprawy dokumentu typu
     "Decyzja administracyjna". Dopóki te dwa warunki nie są spełnione, przycisk przejścia nie
     pojawi się/nie zadziała.
   - żeby dołączyć wymagany dokument, przejdź do menu **Wymiana EDI / XML** i otwórz zakładkę
     **"Akcje zbiorowe"**, zaznacz checkboxem swoją sprawę w
     karcie **"Masowe generowanie dokumentów z szablonu"**, w polu wyboru szablonu [select, required
     — lista pokazuje nazwy plików, nie typy dokumentów] wybierz `"decyzja_srodowiskowa.jrxml"`
     (jedyny szablon powiązany z typem "Decyzja administracyjna" — wygenerowany dokument dziedziczy
     typ z szablonu, niezależnie od tego, czy zostanie potem podpisany) i kliknij
     **"Uruchom generowanie"**. Poczekaj, aż pasek postępu dojdzie do 100% (odpytywanie co ok.
     1-2 s).
   - wyloguj się i zaloguj ponownie jako `demo.kierownik`.
   - wróć do szczegółów tej samej sprawy (menu Sprawy → znajdź sprawę po znaku/tytule). Przycisk
     "Przejdź do: Zakończona" powinien być teraz aktywny.
8. Ostatnie przejście w obiegu ("Zakończona → Zamknięta") nie wymaga żadnej roli — można je
   wykonać jako `demo.referent` albo `demo.kierownik`.
9. (Opcjonalnie, tylko jako `demo.administrator`) Przegląd definicji obiegów przed rejestracją
   sprawy: menu **Administracja → Definicje obiegów**, przycisk "Podgląd stanów" na karcie obiegu
   pokazuje pełną sekwencję stanów i przejść.

Sekwencja stanów obiegu "Obieg wniosku o wydanie decyzji administracyjnej" (scenariusz 1):
Zarejestrowana → W toku → Do akceptacji → Zakończona → Zamknięta.

## Znane ograniczenia (czego nie da się zrobić w aplikacji)

1. **Nie da się utworzyć nowej definicji obiegu (nowy rodzaj sprawy) z aplikacji** — ekran
   "Definicje obiegów" w Administracji pozwala tylko zmienić nazwę i opis istniejącego obiegu, nie
   jego stany, przejścia, role czy wymagane dokumenty.
2. **Nie da się dołączyć dokumentu do sprawy z samego ekranu szczegółów sprawy.** Jedyna droga to
   zakładka "Akcje zbiorowe" w module Wymiana EDI / XML — ekran szczegółów sprawy nie informuje o
   tym w żaden sposób, mimo że to tam widać, że przejście jest blokowane brakiem dokumentu.
4. **Lista wydziałów w formularzu rejestracji sprawy jest zawsze taka sama** (cztery nazwy) —
   dodanie nowego wydziału do wyboru nie jest możliwe z aplikacji.
5. **Nie da się dodać nowego obywatela/podmiotu w trakcie rejestracji sprawy** — formularz pozwala
   wybrać tylko z istniejącej listy (zob. dokument "Karta obywatela/podmiotu").
6. **"Dekretacja" dokumentu przychodzącego (w sensie: przydzielenie do wydziału/referenta jeszcze
   przed rejestracją sprawy) nie istnieje jako samodzielna funkcja** — pola Wydział/Referent
   wypełnia się tylko raz, przy rejestracji sprawy.
