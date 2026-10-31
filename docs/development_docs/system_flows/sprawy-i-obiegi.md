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
3. W formularzu "Rejestracja nowej sprawy" wypełnij pola: Definicja obiegu, Pozycja JRWA (po
   wyborze kategoria archiwalna uzupełni się automatycznie), Tytuł sprawy, Obywatel/podmiot,
   Wydział, Referent (domyślnie Twoje imię i nazwisko — zostaw bez zmian), opcjonalnie Termin
   ustawowy. Kliknij **"Zarejestruj sprawę"**. Znak sprawy (numer JRWA) nadaje się sam — nie wpisuj
   go ręcznie, bo nie ma takiego pola.
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
   - żeby dołączyć wymagany dokument, otwórz w nowej karcie przeglądarki adres
     `http://localhost:10600/wymiana/akcje-zbiorowe` (ten ekran nie jest dostępny z żadnego menu —
     trzeba wpisać adres wprost, zob. "Znane ograniczenia", pkt 1), zaznacz checkboxem swoją sprawę w
     karcie **"Masowe generowanie dokumentów z szablonu"**, wybierz szablon powiązany z typem
     "Decyzja administracyjna" i kliknij **"Uruchom generowanie"**. Poczekaj, aż pasek postępu
     dojdzie do 100% (odpytywanie co ok. 1-2 s).
   - wyloguj się i zaloguj ponownie jako `demo.kierownik`.
   - wróć do szczegółów tej samej sprawy (menu Sprawy → znajdź sprawę po znaku/tytule). Przycisk
     "Przejdź do: Zakończona" powinien być teraz aktywny.
8. Ostatnie przejście w obiegu ("Zakończona → Zamknięta") nie wymaga żadnej roli — można je
   wykonać jako `demo.referent` albo `demo.kierownik`.
9. (Opcjonalnie, tylko jako `demo.administrator`) Przegląd definicji obiegów przed rejestracją
   sprawy: menu **Administracja → Definicje obiegów**, przycisk "Podgląd stanów" na karcie obiegu
   pokazuje pełną sekwencję stanów i przejść.

## Dane testowe do manualnego przejścia przepływu

**Scenariusz 1 — sprawa z przejściem wymagającym roli kierownik:**

- Tytuł: `Wniosek o wydanie decyzji o środowiskowych uwarunkowaniach dla inwestycji przy
ul. Polnej 14 w Rzeszowie`
- Pozycja JRWA: `6220 — Decyzje o środowiskowych uwarunkowaniach`
- Definicja obiegu: `Obieg wniosku o wydanie decyzji administracyjnej`
- Obywatel/podmiot: `Jan Kowalski`
- Wydział: `Wydział Ochrony Środowiska`
- Referent: pozostaw wartość domyślną (Katarzyna Zielińska)
- Termin ustawowy: data za 30 dni od dnia rejestracji

**Scenariusz 2 — sprawa bez żadnych wymogów roli/dokumentu (prosty obieg):**

- Tytuł: `Skarga na niewłaściwe oznakowanie przejścia dla pieszych przy ul. Kolejowej 7`
- Pozycja JRWA: `1510 — Skargi i wnioski`
- Definicja obiegu: `Obieg skargi / wniosku`
- Obywatel/podmiot: `Anna Nowak`
- Wydział: `Wydział Organizacyjny`
- Referent: pozostaw wartość domyślną

Sekwencja stanów obiegu "Obieg wniosku o wydanie decyzji administracyjnej": Zarejestrowana → W
toku → Do akceptacji → Zakończona → Zamknięta. Sekwencja obiegu "Obieg skargi / wniosku":
Zarejestrowana → W toku → Odpowiedź przygotowana → Zakończona (żadne z tych przejść nie wymaga
roli ani dokumentu).

## Znane ograniczenia (czego nie da się zrobić w aplikacji)

1. **Ekran "Akcje zbiorowe" (potrzebny w kroku 7, żeby wygenerować wymagany dokument) nie jest
   dostępny z żadnego menu ani linku w aplikacji.** Trzeba wpisać adres
   `http://localhost:10600/wymiana/akcje-zbiorowe` ręcznie w przeglądarce — ani menu "Wymiana EDI /
   XML", ani lista Obywateli nie prowadzą do niego.
2. **Nie da się utworzyć nowej definicji obiegu (nowy rodzaj sprawy) z aplikacji** — ekran
   "Definicje obiegów" w Administracji pozwala tylko zmienić nazwę i opis istniejącego obiegu, nie
   jego stany, przejścia, role czy wymagane dokumenty.
3. **Nie da się dołączyć dokumentu do sprawy z samego ekranu szczegółów sprawy.** Jedyna droga to
   ekran "Akcje zbiorowe" (patrz pkt 1) — ekran szczegółów sprawy nie informuje o tym w żaden
   sposób, mimo że to tam widać, że przejście jest blokowane brakiem dokumentu.
4. **Lista wydziałów w formularzu rejestracji sprawy jest zawsze taka sama** (cztery nazwy) —
   dodanie nowego wydziału do wyboru nie jest możliwe z aplikacji.
5. **Nie da się dodać nowego obywatela/podmiotu w trakcie rejestracji sprawy** — formularz pozwala
   wybrać tylko z istniejącej listy (zob. dokument "Karta obywatela/podmiotu").
6. **"Dekretacja" dokumentu przychodzącego (w sensie: przydzielenie do wydziału/referenta jeszcze
   przed rejestracją sprawy) nie istnieje jako samodzielna funkcja** — pola Wydział/Referent
   wypełnia się tylko raz, przy rejestracji sprawy.
