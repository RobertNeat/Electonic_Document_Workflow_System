# Przepływ: Dokumenty, szablony i podpis elektroniczny

## Zakres

Scenariusz testowy do manualnego przejścia: wgranie szablonu Jasper, wygenerowanie dokumentu z
szablonu i danych sprawy, podgląd PDF, podpisanie dokumentu certyfikatem, sprawdzenie szczegółów
podpisu. Aplikacja: `document_web` (`http://localhost:10600`).

## Aktorzy i role

| Login w Keycloaku    | Rola          | Kiedy potrzebny                                                 |
| -------------------- | ------------- | --------------------------------------------------------------- |
| `demo.administrator` | administrator | wgranie nowego szablonu Jasper (krok 1)                         |
| `demo.referent`      | referent      | generowanie dokumentu, podpisywanie, podgląd szczegółów podpisu |

Hasło obu kont: wspólne dla kont demo, zmienna `KEYCLOAK_DEMO_USER_PASSWORD` (domyślnie
`change-me`).

## Kroki przepływu

1. **Wgranie szablonu Jasper** — zaloguj się jako `demo.administrator`. Przejdź do menu
   **Dokumenty i szablony**, kliknij przycisk **"Szablony Jasper"**, a na ekranie szablonów
   kliknij **"Wgraj nowy szablon"**. W dialogu "Wgraj nowy szablon (.jrxml)" wypełnij:
   - Typ dokumentu: `"Decyzja administracyjna"` [select, required — jedna z 4 wartości zdefiniowanych w słowniku: "Pełnomocnictwo", "Potwierdzenie opłaty", "Decyzja administracyjna", "Wezwanie do zapłaty"]
   - Plik: `apps/jasper_service/src/test/resources/testowy-szablon.jrxml` [file, required, rozszerzenie `.jrxml`, format JasperReports 7.x — walidacja w tym dialogu sprawdza wyłącznie rozszerzenie nazwy pliku; dowolny plik `.jrxml` w starym formacie (namespace + `<band>` + `<reportElement>`) przejdzie upload, ale zostanie odrzucony później, przy generowaniu dokumentu]

   Zatwierdź.
2. **Przegląd listy i historii wersji szablonu** — na liście szablonów kliknij wiersz (nie
   przycisk) — otworzy się dialog "Historia wersji szablonu".
3. Wyloguj się i zaloguj ponownie jako `demo.referent`.
4. **Wygenerowanie dokumentu z szablonu i danych sprawy** — przejdź do menu **Wymiana EDI / XML**
   i otwórz zakładkę **"Akcje zbiorowe"**. Na karcie "Masowe generowanie dokumentów z szablonu":
   - Sprawa: zaznacz checkboxem sprawę o znaku `WO.6220.14.2026` ("Wniosek o wydanie decyzji
     środowiskowej", strona: Jan Kowalski) [checkbox, required — min. 1 zaznaczona sprawa z listy istniejących]
   - Szablon: `"decyzja_srodowiskowa.jrxml"` [select, required — lista pokazuje nazwy plików, nie
     typy dokumentów; to jedyny seedowany szablon typu "Decyzja administracyjna" i generuje się
     poprawnie od razu, bez potrzeby wgrywania własnego z kroku 1]

   Kliknij **"Uruchom generowanie"**. Poczekaj, aż pasek postępu dojdzie do 100%.
5. **Lista dokumentów sprawy** — przejdź do menu **Dokumenty i szablony**. Nowo wygenerowany
   dokument powinien być widoczny na liście (filtruj po sprawie, jeśli potrzeba).
6. **Szczegóły dokumentu i osi czasu wersji** — kliknij dokument na liście. Zobaczysz oś czasu
   wersji z przyciskami: **"Podgląd PDF"**, **"Pobierz"**, i albo **"Podpisz"** (wersja
   niepodpisana) albo **"Szczegóły podpisu"** (wersja już podpisana). Dokument "Pełnomocnictwo
   J. Kowalski" na sprawie `WO.6220.14.2026` jest niepodpisany — użyj go w kroku 8, żeby
   zademonstrować podpisywanie od zera, a potem w kroku 9 obejrzyj jego realne szczegóły podpisu.
   **Nie używaj do kroku 9 dokumentu "Decyzja środowiskowa"** — jego seedowana wersja ma status
   podpisu `poprawny` tylko w metadanych bazy; fizyczny plik to placeholder PDF bez prawdziwie
   wbudowanego podpisu PAdES, więc "Szczegóły podpisu" na nim zwróci puste/"nieznany" pola
   (sign_service realnie parsuje bajty pliku, nie tylko metadane).
7. **Podgląd PDF** — kliknij "Podgląd PDF" na aktualnej wersji — plik otworzy się w dialogu, bez
   pobierania na dysk.
8. **Podpisanie dokumentu certyfikatem** — na niepodpisanej wersji ("Pełnomocnictwo J. Kowalski")
   kliknij **"Podpisz"**. W dialogu "Wybór certyfikatu do podpisu":
   - Certyfikat: `"Katarzyna Zielińska"` [radio, required — wybieralny tylko gdy `status =
     aktywny` ORAZ `dataWygasniecia > data dzisiejsza`; to jedyny certyfikat w danych demo, który
     faktycznie da się zaznaczyć, ważny do `2027-01-10`]. Pozostałe dwa certyfikaty na liście są
     wyszarzone/niewybieralne: "Piotr Lewandowski" (`status = wygasły`, data wygaśnięcia
     `2025-05-15`) i "Marta Wiśniewska" (`status = unieważniony`, powód: skompromitowany klucz —
     niewybieralny mimo że data wygaśnięcia `2027-02-01` jest w przyszłości, bo liczy się tylko
     status).

   Kliknij **"Podpisz"**.
9. **Podgląd szczegółów podpisu** — na właśnie podpisanej wersji "Pełnomocnictwo J. Kowalski"
   z kroku 8 kliknij **"Szczegóły podpisu"**. Dialog "Szczegóły podpisu elektronicznego" pokaże:
   podpisującego, czas podpisu, odcisk certyfikatu (SHA-256), status OCSP, status CRL, znacznik
   czasu (TSA).

## Znane ograniczenia (czego nie da się zrobić w aplikacji)

1. **Nie da się unieważnić ani wygenerować nowego certyfikatu z aplikacji.** Certyfikaty są
   wyłącznie tymi, które są obecne w danych demo od startu systemu — nie da się doprowadzić
   świeżo podpisanego dokumentu do statusu "certyfikat wygasły"/"certyfikat unieważniony" w trakcie
   testu; te dwa statusy można zobaczyć tylko na dokumentach podpisanych wcześniej, zanim
   certyfikat wygasł/został unieważniony.
2. **Przycisk "Wygeneruj wezwanie" w module Płatności nie generuje realnego dokumentu** — to pusty
   dialog potwierdzenia (zob. dokument "Rozliczanie płatności"). Wezwanie do zapłaty jako
   dokument da się wygenerować tylko przez zakładkę Akcje zbiorowe z dowolnym szablonem, nie
   jednym kliknięciem dedykowanym tej należności.
3. **Podgląd samej treści pliku `.jrxml` szablonu nie jest dostępny z aplikacji** — tylko metadane
   i historia wersji.
4. **Podpis dokumentów/komunikatów XML (XAdES) nie jest używany w aplikacji** — tylko podpis PDF
   (PAdES) opisany w kroku 8.
