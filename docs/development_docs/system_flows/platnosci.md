# Przepływ: Rozliczanie płatności

## Zakres

Scenariusz testowy do manualnego przejścia: przegląd rejestru należności i wpłat, import wyciągu
bankowego z automatycznym dopasowaniem, ręczne dopasowanie wpłaty niedopasowanej, oraz podgląd
rozrachunków strony na karcie obywatela. Aplikacja: `document_web` (`http://localhost:10600`).

## Aktorzy i role

| Login w Keycloaku | Rola       | Osoba            |
| ----------------- | ---------- | ---------------- |
| `demo.ksiegowosc` | ksiegowosc | Marta Wiśniewska |

Hasło: wspólne dla kont demo, zmienna `KEYCLOAK_DEMO_USER_PASSWORD` (domyślnie `change-me`).

## Kroki przepływu

1. Zaloguj się jako `demo.ksiegowosc`. Przejdź do menu **Płatności**.
2. Zobaczysz rejestr należności z filtrem statusu (nierozliczona / częściowo rozliczona /
   rozliczona / przeterminowana).
3. Kliknij wiersz (albo tytuł) należności, żeby zobaczyć jej szczegóły: procent rozliczenia, listę
   powiązanych wpłat, znak sprawy i dane kontrahenta.
4. Wróć do rejestru i kliknij przycisk **"Rejestr wpłat"** — zobaczysz wszystkie wpłaty wraz z
   informacją, czy zostały dopasowane automatycznie.
5. **Import wyciągu bankowego** — kliknij przycisk **"Importuj wyciąg"**. Na ekranie "Import
   wyciągu bankowego" kliknij "Wybierz plik CAMT.053" i wskaż plik testowy (zob. "Dane testowe").
   Po imporcie zobaczysz tabelę wyniku: liczba transakcji, ile dopasowano automatycznie, ile
   trafiło do rekoncyliacji ręcznej.
6. **Ręczne dopasowanie wpłaty niedopasowanej** — przejdź do "Rejestru wpłat", znajdź wpłatę bez
   przypisanej należności i kliknij **"Dopasuj ręcznie"**. W dialogu "Dopasowanie wpłaty do
   należności" wybierz właściwą należność z listy i kliknij **"Dopasuj"**.
7. **Podgląd rozrachunków strony** — przejdź do menu **Obywatele**, wyszukaj kontrahenta,
   otwórz jego kartę i kliknij zakładkę **"Rozrachunki"** — zobaczysz jego należności i wpłaty w
   jednym miejscu.
8. **Generowanie wezwania do zapłaty** (przycisk widoczny, ale nie generuje realnego dokumentu) —
   na należności ze statusem "przeterminowana" kliknij przycisk **"Wygeneruj wezwanie"**. Otworzy
   się dialog potwierdzenia — treść dialogu informuje, że realny dokument powstaje w module
   Dokumenty; kliknięcie "Wygeneruj" tylko zamyka dialog, nie tworzy niczego (zob. "Znane
   ograniczenia", pkt 2).

## Dane testowe do manualnego przejścia przepływu

- **Plik wyciągu bankowego do importu**: `apps/document_api/src/test/resources/camt053/
wyciag-dopasowanie.xml` — zawiera trzy wpisy:
  1. kredyt 4500,00 PLN z tytułem "Zapłata faktury FK/2026/0088" → dopasuje się automatycznie do
     należności "Usługi serwisowe — faktura" (kontrahent: Budimex Serwis Sp. z o.o.) i w pełni ją
     rozliczy,
  2. kredyt 999,99 PLN od kontrahenta "Firma Zagadka s.c." bez odniesienia do żadnej należności —
     trafi do rekoncyliacji ręcznej,
  3. debet 12,00 PLN (opłata bankowa) — pomijany przez import.
- **Należność do automatycznego dopasowania (krok 5)**: "Usługi serwisowe — faktura", kontrahent
  Budimex Serwis Sp. z o.o., kwota 4500 zł, obecnie nierozliczona, termin płatności 2026-10-30. Po
  imporcie pliku testowego stanie się w pełni rozliczona — **jednorazowy test**: ponowny import
  tego samego pliku nie zademonstruje już nowego automatycznego dopasowania tej należności (zob.
  "Znane ograniczenia", pkt 3).
- **Należność już rozliczona w danych demo**: "Opłata skarbowa", kontrahent Jan Kowalski, 17 zł —
  nie nadaje się do demonstracji świeżego dopasowania.
- **Wpłata do ręcznego dopasowania (krok 6)**: 250 zł, tytuł "Wpłata niezidentyfikowana",
  kontrahent "Nieznany", data 2026-10-18 — niedopasowana w danych demo, gotowa do testu.

## Znane ograniczenia (czego nie da się zrobić w aplikacji)

1. **Nie da się wprowadzić nowej należności z aplikacji.** Rejestr należności zawiera tylko te
   wpisy, które były obecne od startu systemu (dane demo) albo powstały z importu CAMT.053/wezwania
   — nie ma przycisku "Nowa należność".
2. **Przycisk "Wygeneruj wezwanie" nie tworzy żadnego dokumentu** — to jest świadomy placeholder w
   interfejsie (dialog potwierdzenia zamyka się bez żadnej akcji). Dokument wezwania do zapłaty da
   się realnie wygenerować tylko przez ekran Akcje zbiorowe (`/wymiana/akcje-zbiorowe`, dostępny
   tylko po wpisaniu adresu wprost — zob. dokument "Dokumenty, szablony i podpis elektroniczny")
   z dowolnym szablonem, nie jednym kliknięciem dedykowanym konkretnej należności.
3. **Import testowego wyciągu bankowego jest jednorazowy dla scenariusza "automatyczne
   dopasowanie".** Po pierwszym imporcie należność "Usługi serwisowe — faktura" staje się
   rozliczona — żeby ponownie zademonstrować automatyczne dopasowanie, potrzebne są dane
   zresetowane do stanu wyjściowego (zob. podsumowanie na końcu przeglądu dokumentacji co do
   procedury resetu danych demo).
