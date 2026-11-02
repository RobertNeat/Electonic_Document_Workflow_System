---
title: Rozliczanie płatności
nav_order: 8
---

## Opis

Moduł Płatności prowadzi rejestr należności powiązanych ze sprawami (np. opłaty skarbowe,
administracyjne, faktury za usługi) oraz rejestr wpłat zasilany zarówno wpisami demonstracyjnymi, jak
i importem wyciągów bankowych. Każda należność ma status rozliczenia: **nierozliczona**, **częściowo
rozliczona**, **rozliczona** lub **przeterminowana**. Centralnym mechanizmem modułu jest import
wyciągu bankowego w formacie CAMT.053 (ISO 20022), który automatycznie dopasowuje wpłaty kredytowe do
należności, a pozycje niejednoznaczne kieruje do ręcznej rekoncyliacji przez księgowość.

## Role

| Rola         | Dostęp                                                                 |
| ------------ | ----------------------------------------------------------------------- |
| `ksiegowosc` | Pełny dostęp do rejestru należności, rejestru wpłat, importu wyciągów i ręcznego dopasowania wpłat. |

## Przebieg pracy

### Przegląd rejestru należności

Ekran **Płatności** otwiera **Rejestr należności** — tabelę z numerem referencyjnym, tytułem, znakiem
sprawy, obywatelem/kontrahentem, kwotą, kwotą rozliczoną, pozostałą do rozliczenia, terminem płatności
i statusem. Listę można zawęzić filtrem statusu. Kliknięcie tytułu należności otwiera jej szczegóły:
procent rozliczenia, listę powiązanych wpłat, znak sprawy i dane kontrahenta.

![Przegląd rejestru należności]({{ '/media/4. płatności/1. przegląd rejestru należności.png' | relative_url }})

### Przegląd rejestru wpłat

Przycisk **Rejestr wpłat** przenosi do ekranu **Rejestr wpłat i rekoncyliacja**, pokazującego wszystkie
zarejestrowane wpłaty wraz z informacją, czy zostały dopasowane do należności automatycznie, czy
wymagają dopasowania ręcznego, oraz do której należności zostały powiązane.

### Import wyciągu bankowego CAMT.053

Przycisk **Importuj wyciąg** otwiera ekran **Import wyciągu bankowego**. Po wybraniu pliku CAMT.053
(XML, brak walidacji rozszerzenia/typu po stronie frontendu — plik parsuje realnie backend) i
kliknięciu **Importuj**, system analizuje transakcje i próbuje dopasować każdą z nich do należności.

![Upload importu wyciągu bankowego]({{ '/media/4. płatności/2. upload importu wyciagu bankowego.png' | relative_url }})

![Import wyciągu bankowego]({{ '/media/4. płatności/3. import wyciągu bankowego.png' | relative_url }})

Reguła dopasowania automatycznego: transakcja zostaje dopasowana automatycznie tylko wtedy, gdy wśród
należności nierozliczonych/częściowo rozliczonych istnieje **dokładnie jeden kandydat** spełniający co
najmniej jeden z dwóch warunków:

- referencja/tytuł wpłaty zawiera jako podciąg numer referencyjny należności,
- kwota wpłaty jest dokładnie równa kwocie brakującej do pełnego rozliczenia należności.

Jeśli kandydatów jest zero lub więcej niż jeden, dopasowanie pozostaje świadomie nierozwiązane i
transakcja trafia do rekoncyliacji ręcznej. Po zakończeniu importu wyświetlana jest tabela wyniku:
liczba odnalezionych transakcji, ile dopasowano automatycznie, a ile wymaga rekoncyliacji — wraz z
tytułem, kwotą, kontrahentem i wynikiem dopasowania dla każdej pozycji.

![Automatyczne dopasowanie pozycji wyciągu]({{ '/media/4. płatności/4. automatyczne dopasowanie pozycji wyciągu.png' | relative_url }})

### Ręczne dopasowanie wpłaty niedopasowanej

W **Rejestrze wpłat** pozycje ze statusem dopasowania "Ręcznie / brak" mają przycisk **Dopasuj
ręcznie**. Otwiera on dialog "Dopasowanie wpłaty do należności", w którym księgowość wybiera z listy
właściwą należność (lista zawiera wyłącznie należności o statusie nierozliczona lub częściowo
rozliczona — pozycje już rozliczone lub przeterminowane nie są tam wybieralne) i zatwierdza
dopasowanie przyciskiem **Dopasuj**. Po dopasowaniu wpłata jest widoczna w rejestrze z powiązaną
należnością, a procent rozliczenia tej należności aktualizuje się odpowiednio.

![Ręczne dopasowanie wpłaty kredytowej na podstawie rekoncyliacji]({{ '/media/4. płatności/5. ręczne dopasowanie wpłaty kredytowej na podstawie rekosyliacji.png' | relative_url }})

### Podgląd rozrachunków strony

Rozrachunki danego obywatela/podmiotu (jego należności i wpłaty w jednym miejscu) są dostępne na
zakładce "Rozrachunki" karty obywatela — zobacz [Karta obywatela/podmiotu](karta-obywatela.md).

Pełny przebieg pracy z modułem Płatności — od przeglądu rejestru, przez import wyciągu, po ręczne
dopasowanie — ilustruje poniższy zapis:

![Przebieg pracy z modułem Płatności]({{ '/media/4. płatności/płatności.gif' | relative_url }})

## Szczegóły implementacji

Z wyciągu CAMT.053 importowane są wyłącznie wpisy kredytowe (wpłaty na rachunek) — wpisy debetowe
(np. opłaty bankowe) są pomijane i nie pojawiają się w wyniku importu ani w rejestrze wpłat. Format
CAMT.053 (ISO 20022) jest parsowany realnie przez backend `document_api`, a nie mockowany.

## Ograniczenia

Rejestru należności nie da się zasilić nową pozycją z poziomu aplikacji — zawiera on wyłącznie
należności obecne od startu systemu (dane demonstracyjne) oraz te, które powstały z importu CAMT.053;
nie ma przycisku pozwalającego ręcznie dopisać nową należność.

Przycisk **Wygeneruj wezwanie** widoczny przy należnościach przeterminowanych nie tworzy żadnego
dokumentu — to świadomy placeholder w interfejsie: dialog potwierdzenia informuje, że realny dokument
powstaje w module Dokumenty, ale kliknięcie "Wygeneruj" jedynie zamyka dialog. Realny dokument wezwania
do zapłaty można wygenerować przez zakładkę **Akcje zbiorowe** na stronie **Wymiana EDI/XML**, z
dowolnym szablonem, choć nie jednym kliknięciem dedykowanym konkretnej należności — zobacz
[Dokumenty, szablony i podpis elektroniczny](dokumenty-i-podpis.md) oraz
[Wymiana EDI/XML](wymiana-edi-xml.md).

Import testowego wyciągu bankowego demonstrującego automatyczne dopasowanie jest jednorazowy: po
pierwszym imporcie powiązana należność staje się rozliczona, więc ponowny import tego samego pliku nie
zademonstruje już nowego automatycznego dopasowania tej pozycji.
