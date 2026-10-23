# Specyfikacja DTO: document_api ↔ document_web (+ akcje zbiorowe)

Robocza specyfikacja pól dla pakietu TS `packages/document-shared-dto` (konsument: `document_web`)
oraz analogicznych rekordów Java w `document_api` (`robert_neat.document_api.dto`). Obie strony mają
identyczne nazwy pól i semantykę — różni się tylko składnia języka. Nazwy pól po polsku.
`document_web` importuje te typy bezpośrednio z pakietu.

## Konwencje wspólne

- Identyfikatory: `string` (TS) / `String` (Java). Nie UUID natywny — stringi, zgodne z istniejącymi
  mockami (`spr-1`, `td-4`...), żeby seed Liquibase mógł użyć tych samych wartości.
- Daty: `string` w formacie ISO `YYYY-MM-DD` dla dat bez czasu, `YYYY-MM-DDTHH:mm:ss` dla
  znacznika czasu. W Java: `LocalDate` / `OffsitDateTime` → `OffsetDateTime` w polu z czasem.
- Kwoty: `number` (TS) / `BigDecimal` (Java).
- Enumy z myślnikiem (`'e-doreczenia'`, `'operator_pocztowy'`, `'email'`) są stringami unii w TS;
  w Java to `enum` z `@JsonValue`/`@JsonCreator` zwracającym/parsującym identyczny string.
- `KanalWysylki` i `DostawcaWysylki.typ` mają wartość `'email'` (bez myślnika).
- Response listowe: opakowane w `StronaWynikow<T>` (`{ dane: T[]; strona: number; rozmiarStrony:
number; liczbaWszystkich: number }`), gdy endpoint z kontraktu wspomina paginację (`sprawy`,
  `dokumenty`, `naleznosci`, `wplaty`, `przesylki`, `audit-log`, `uzytkownicy`). Endpointy
  zwracające pełne słowniki (JRWA, definicje obiegów, struktura organizacyjna) zwracają zwykłą tablicę.

## Modele bazowe

- `Obywatel`, `AdresObywatela` (zagnieżdżony `adres`)
- `DefinicjaObiegu`: `{ id, nazwa, opis, stany: string[], stanPoczatkowy, przejscia:
  PrzejscieObiegu[] }` gdzie `PrzejscieObiegu { zStanu: string; doStanow: string[]; wymaganaRola?:
  string; wymaganeTypyDokumentow?: string[] }`. Pole `stany` to lista wszystkich stanów; `przejscia`
  to właściwy model przejść. Moduł Sprawy we froncie korzysta obecnie tylko z `stany` (porządek
  liniowy) — podłączenie UI pod `przejscia` jest w `plan.md`.
- `KategoriaArchiwalna` enum: `A | B5 | B10 | BE5 | BE10`
- `Sprawa`, `WpisMetryki`
- `TypDokumentu`, `SzablonJasper`, `WersjaDokumentu`, `Dokument`, `StatusPodpisu` enum
- `SzczegolyPodpisu` (z kontraktu `document_api_dokumenty.md`):
  `{ podpisujacy: string; czasPodpisu: string; odciskCertyfikatu: string; statusOcsp: string;
statusCrl: string; znacznikCzasuTsa: string }`
- `Przesylka`: `{ id, sprawaId, dokumentId, obywatelId, kanal, status, numerReferencyjny,
  dataNadania, dataStatusu, kierunek: 'wplywajaca' | 'wychodzaca', historiaStatusow:
  WpisStatusuPrzesylki[] }` gdzie `WpisStatusuPrzesylki { status: StatusPrzesylki; data: string }`
- `KanalWysylki`, `StatusPrzesylki` enum
- `Naleznosc`, `Wplata`, `StatusRozliczenia` enum
- `PozycjaJrwa`
- `Uzytkownik`, `RolaUzytkownika` enum
- Struktura organizacyjna i słowniki administracyjne: `Wydzial`, `Stanowisko`, `DostawcaWysylki`
  (`typ`: `'e-doreczenia' | 'operator_pocztowy' | 'email'`), `StawkaOplaty`,
  `WpisDziennikaZdarzen`, `TypOperacjiAudytu` (unia stringów)

## DTO żądań (Request)

- `UtworzSpraweRequest { definicjaObieguId, jrwa, kategoriaArchiwalna, tytul, obywatelId, wydzial,
referent, terminUstawowy? }`
- `ZmienStanSprawyRequest { nowyStan: string }`
- `DopasujWplateRequest { naleznoscId: string }`
- `UtworzPrzesylkeRequest { sprawaId, dokumentId, obywatelId, kanal: KanalWysylki }`
- `UtworzUzytkownikaRequest { login, imie, nazwisko, email, role, wydzial, stanowisko }`
- `AktualizujRoleUzytkownikaRequest { role: RolaUzytkownika[] }`
- `EdytujDefinicjeObieguRequest { nazwa, opis }`

## DTO odpowiedzi wzbogacone (denormalizowane, dla list)

- `SprawaListElement` = `Sprawa` (bez zmian — front już ma pełne pola potrzebne do tabeli)
- `DokumentListElement extends Dokument` + `znakSprawy: string` + `statusPodpisuAktualnej: StatusPodpisu`
- `DokumentObywatelaListElement extends DokumentListElement` + `typDokumentuNazwa: string`
- `NaleznoscListElement extends Naleznosc` + `znakSprawy: string` + `obywatelNazwa: string`
- `ObywatelSaldo { obywatelId: string; doZaplaty: number }`

## DTO wyników operacji specjalnych

- `WynikImportuCamt053 { liczbaTransakcji: number; liczbaDopasowanychAutomatycznie: number;
liczbaDoRekoncyliacji: number; utworzoneWplaty: Wplata[] }`
- `WynikWalidacjiXml { sukces: boolean; bledy: BladWalidacjiXsd[]; sprawaZmapowana?: Partial<Sprawa> }`
  gdzie `BladWalidacjiXsd { element: string; opis: string }`
- `WynikWalidacjiUbl { sukces: boolean; bledy: BladWalidacjiXsd[]; faktura?: FakturaUbl }` gdzie
  `FakturaUbl { numer: string; kontrahent: string; nip: string; kwota: number; pozycje:
PozycjaFakturyUbl[]; numerReferencyjnyNaleznosci: string }`,
  `PozycjaFakturyUbl { nazwa: string; ilosc: number; cenaJednostkowa: number; wartosc: number }`
- `WpisHistoriiWymiany { id; nazwaPliku: string; kierunek: 'import' | 'eksport'; data: string;
status: 'sukces' | 'blad_walidacji'; liczbaRekordow: number; opis?: string }`

## Akcje zbiorowe (batch) — wspólny model, nie tylko sprawy

Rozszerzalny model na przyszłe operacje wsadowe na obywatelach/kontach użytkowników, zgodnie z
kierunkiem rozwoju frontu (praca na wielu obywatelach/kontach naraz).

- `TypCeluAkcjiZbiorowej` enum: `SPRAWA | OBYWATEL | NALEZNOSC | UZYTKOWNIK`
- `TypAkcjiZbiorowej` enum: `ZMIANA_STANU | WYSYLKA_PISM | GENEROWANIE_DOKUMENTOW |
AKTUALIZACJA_DANYCH | AKTUALIZACJA_ROL`
- `UruchomAkcjeZbiorowaRequest<TParametry> { typAkcji: TypAkcjiZbiorowej; typCelu:
TypCeluAkcjiZbiorowej; idCelow?: string[]; kryteria?: Record<string, unknown>; parametry: TParametry }`
  — `idCelow` do wyboru jawnej listy, `kryteria` do "wszystkie dopasowane do filtra" (np. filtr listy
  spraw/obywateli na ekranie). Dokładnie jedno z `idCelow`/`kryteria` powinno być podane.
- Parametry specyficzne per typ akcji (podtypy `parametry`):
  - `ZMIANA_STANU`: `{ nowyStan: string }`
  - `WYSYLKA_PISM`: `{ kanal: KanalWysylki }`
  - `GENEROWANIE_DOKUMENTOW`: `{ szablonId: string }`
  - `AKTUALIZACJA_ROL` (nowy, dla kont/UZYTKOWNIK): `{ dodajRole?: RolaUzytkownika[];
usunRole?: RolaUzytkownika[] }`
- `UruchomAkcjeZbiorowaResponse { akcjaId: string }`
- `StatusAkcjiZbiorowej { akcjaId: string; typAkcji: TypAkcjiZbiorowej; typCelu:
TypCeluAkcjiZbiorowej; status: 'trwajaca' | 'zakonczona'; postep: number; lacznie: number;
przetworzone: number; bledne: number; wynikiElementow: WynikElementuAkcji[] }` gdzie
  `WynikElementuAkcji { idCelu: string; status: 'sukces' | 'blad'; komunikat?: string }`

## Pakiety docelowe

- TS: `packages/document-shared-dto/src/index.ts` (re-eksport z plików per domena: `sprawy.ts`,
  `dokumenty.ts`, `obywatele.ts`, `platnosci.ts`, `korespondencja.ts`, `wymiana.ts`,
  `administracja.ts`, `akcje-zbiorowe.ts`, `wspolne.ts` dla `StronaWynikow`).
  Nazwa pakietu npm: `@electronic-document-workflow/document-shared-dto`, `private: true`,
  zero zależności runtime, `tsc` do budowania (`main`/`types` w `dist/`).
- Java: `robert_neat.document_api.dto` w `apps/document_api/src/main/java/...`, jeden plik per
  rekord (lub pogrupowane w pakiety `dto.sprawy`, `dto.dokumenty`, itd. — do decyzji agenta
  implementującego, zachowując spójność z podziałem TS). Rekordy `record`, walidacja
  `jakarta.validation.constraints` na polach DTO żądań.
