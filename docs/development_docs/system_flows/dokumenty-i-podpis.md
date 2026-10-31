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
   kliknij **"Wgraj nowy szablon"**. W dialogu "Wgraj nowy szablon (.jrxml)" wybierz typ dokumentu
   i plik `.jrxml` (zob. "Dane testowe"), zatwierdź.
2. **Przegląd listy i historii wersji szablonu** — na liście szablonów kliknij wiersz (nie
   przycisk) — otworzy się dialog "Historia wersji szablonu".
3. Wyloguj się i zaloguj ponownie jako `demo.referent`.
4. **Wygenerowanie dokumentu z szablonu i danych sprawy** — ten krok NIE jest dostępny z menu.
   Wpisz w przeglądarce adres `http://localhost:10600/wymiana/akcje-zbiorowe` (zob. "Znane
   ograniczenia", pkt 1). Na karcie "Masowe generowanie dokumentów z szablonu" zaznacz checkboxem
   sprawę testową, wybierz szablon z listy rozwijanej i kliknij **"Uruchom generowanie"**. Poczekaj,
   aż pasek postępu dojdzie do 100%.
5. **Lista dokumentów sprawy** — przejdź do menu **Dokumenty i szablony**. Nowo wygenerowany
   dokument powinien być widoczny na liście (filtruj po sprawie, jeśli potrzeba).
6. **Szczegóły dokumentu i osi czasu wersji** — kliknij dokument na liście. Zobaczysz oś czasu
   wersji z przyciskami: **"Podgląd PDF"**, **"Pobierz"**, i albo **"Podpisz"** (wersja
   niepodpisana) albo **"Szczegóły podpisu"** (wersja już podpisana).
7. **Podgląd PDF** — kliknij "Podgląd PDF" na aktualnej wersji — plik otworzy się w dialogu, bez
   pobierania na dysk.
8. **Podpisanie dokumentu certyfikatem** — na niepodpisanej wersji kliknij **"Podpisz"**. W
   dialogu "Wybór certyfikatu do podpisu" wybierz certyfikat z listy (radio button — tylko aktywne
   i niewygasłe certyfikaty można zaznaczyć) i kliknij **"Podpisz"**.
9. **Podgląd szczegółów podpisu** — na już podpisanej wersji kliknij **"Szczegóły podpisu"**.
   Dialog "Szczegóły podpisu elektronicznego" pokaże: podpisującego, czas podpisu, odcisk
   certyfikatu (SHA-256), status OCSP, status CRL, znacznik czasu (TSA).

## Dane testowe do manualnego przejścia przepływu

- **Typ dokumentu do wgrania szablonu**: "Decyzja administracyjna".
- **Plik szablonu**: użyj gotowego, poprawnego pliku z repozytorium:
  `apps/jasper_service/src/test/resources/testowy-szablon.jrxml` — format JasperReports 7.x.
  Dowolny inny plik `.jrxml` w starym formacie (namespace + `<band>` + `<reportElement>`) zostanie
  odrzucony przy generowaniu dokumentu.
- **Szablon już w systemie (seed)**: na liście szablonów widoczny jest `decyzja_srodowiskowa.jrxml`
  powiązany z typem "Decyzja administracyjna" — można użyć go wprost w kroku 4 bez wgrywania
  własnego pliku, **o ile fizyczny plik na serwerze istnieje** (do zweryfikowania empirycznie —
  zob. "Znane ograniczenia", pkt 2; jeśli krok 4 zwróci błąd przy generowaniu, wgraj najpierw własny
  szablon z kroku 1).
- **Sprawa do wygenerowania dokumentu**: znak sprawy `WO.6220.14.2026` ("Wniosek o wydanie decyzji
  środowiskowej"), strona: Jan Kowalski.
- **Certyfikat do podpisania (aktywny)**: w cert-pickerze wybierz certyfikat z podpisującym
  **"Katarzyna Zielińska"** — to jedyny certyfikat w danych demo, który faktycznie da się użyć do
  podpisania (ważny do 2027-01-10).
- **Certyfikaty do testowania ścieżek błędu** (widoczne na liście, ale wyszarzone/niewybieralne):
  certyfikat podpisującego "Piotr Lewandowski" (status wygasły) i certyfikat podpisującego "Marta
  Wiśniewska" (status unieważniony, powód: skompromitowany klucz).
- **Dokument już podpisany w danych demo**: dokument "Decyzja środowiskowa" na sprawie
  `WO.6220.14.2026` ma wersję już podpisaną certyfikatem Katarzyny Zielińskiej — użyj go do
  zademonstrowania ekranu "Szczegóły podpisu" bez podpisywania niczego od nowa. Dokument
  "Pełnomocnictwo J. Kowalski" na tej samej sprawie jest niepodpisany — użyj go, żeby
  zademonstrować podpisywanie od zera (krok 8).

## Znane ograniczenia (czego nie da się zrobić w aplikacji)

1. **Generowanie dokumentu z szablonu nie jest dostępne z żadnego menu** — trzeba wpisać adres
   `http://localhost:10600/wymiana/akcje-zbiorowe` ręcznie. Nie ma przycisku ani linku w module
   Dokumenty, w module Wymiana, ani na liście Obywateli, który prowadziłby do tego ekranu.
2. **Pliki fizyczne seedowanych szablonów/dokumentów mogą nie istnieć na serwerze** — metadane w
   bazie istnieją, ale sam plik `.jrxml`/`.pdf` może nie być obecny. Jeśli podgląd/pobranie
   seedowanego dokumentu albo wygenerowanie dokumentu z seedowanego szablonu zwróci błąd, wgraj
   najpierw własny szablon przez krok 1 i użyj go w kroku 4.
3. **Nie da się unieważnić ani wygenerować nowego certyfikatu z aplikacji.** Certyfikaty są
   wyłącznie tymi, które są obecne w danych demo od startu systemu — nie da się doprowadzić
   świeżo podpisanego dokumentu do statusu "certyfikat wygasły"/"certyfikat unieważniony" w trakcie
   testu; te dwa statusy można zobaczyć tylko na dokumentach podpisanych wcześniej, zanim
   certyfikat wygasł/został unieważniony.
4. **Przycisk "Wygeneruj wezwanie" w module Płatności nie generuje realnego dokumentu** — to pusty
   dialog potwierdzenia (zob. dokument "Rozliczanie płatności"). Wezwanie do zapłaty jako
   dokument da się wygenerować tylko przez ekran Akcje zbiorowe z dowolnym szablonem, nie
   jednym kliknięciem dedykowanym tej należności.
5. **Podgląd samej treści pliku `.jrxml` szablonu nie jest dostępny z aplikacji** — tylko metadane
   i historia wersji.
6. **Podpis dokumentów/komunikatów XML (XAdES) nie jest używany w aplikacji** — tylko podpis PDF
   (PAdES) opisany w kroku 8.
