import {
  Obywatel,
  Sprawa,
  DefinicjaObiegu,
  WpisMetryki,
  Dokument,
  WersjaDokumentu,
  TypDokumentu,
  SzablonJasper,
  Przesylka,
  Naleznosc,
  Wplata,
  PozycjaJrwa,
  Uzytkownik,
} from '../models';

export const MOCK_OBYWATELE: Obywatel[] = [
  {
    id: 'ob-1',
    typ: 'osoba_fizyczna',
    imie: 'Jan',
    nazwisko: 'Kowalski',
    pesel: '80010112345',
    email: 'jan.kowalski@example.pl',
    telefon: '500100200',
    adres: { ulica: 'Kwiatowa', numerDomu: '12', kodPocztowy: '00-001', miejscowosc: 'Warszawa' },
  },
  {
    id: 'ob-2',
    typ: 'osoba_fizyczna',
    imie: 'Anna',
    nazwisko: 'Nowak',
    pesel: '92051587654',
    email: 'anna.nowak@example.pl',
    telefon: '600300400',
    adres: {
      ulica: 'Słoneczna',
      numerDomu: '5',
      numerLokalu: '3',
      kodPocztowy: '30-002',
      miejscowosc: 'Kraków',
    },
  },
  {
    id: 'ob-3',
    typ: 'podmiot',
    nazwaPodmiotu: 'Budimex Serwis Sp. z o.o.',
    nip: '5261234567',
    email: 'kontakt@budimex-serwis.pl',
    adres: { ulica: 'Przemysłowa', numerDomu: '44', kodPocztowy: '61-003', miejscowosc: 'Poznań' },
  },
];

export const MOCK_DEFINICJE_OBIEGOW: DefinicjaObiegu[] = [
  {
    id: 'obieg-decyzja',
    nazwa: 'Obieg wniosku o wydanie decyzji administracyjnej',
    opis: 'Rejestracja, weryfikacja, opiniowanie, wydanie decyzji.',
    stany: ['Zarejestrowana', 'W toku', 'Do akceptacji', 'Zakończona', 'Zamknięta'],
    stanPoczatkowy: 'Zarejestrowana',
  },
  {
    id: 'obieg-skarga',
    nazwa: 'Obieg skargi / wniosku',
    opis: 'Rozpatrzenie skargi lub wniosku obywatela.',
    stany: ['Zarejestrowana', 'W toku', 'Odpowiedź przygotowana', 'Zakończona'],
    stanPoczatkowy: 'Zarejestrowana',
  },
  {
    id: 'obieg-faktura',
    nazwa: 'Obieg faktury kosztowej',
    opis: 'Weryfikacja, akceptacja i rozliczenie faktury kosztowej.',
    stany: ['Zarejestrowana', 'Do akceptacji', 'Zaakceptowana', 'Rozliczona'],
    stanPoczatkowy: 'Zarejestrowana',
  },
  {
    id: 'obieg-okolnik',
    nazwa: 'Obieg wewnętrznego pisma okólnego',
    opis: 'Dystrybucja i potwierdzenie zapoznania się z pismem okólnym.',
    stany: ['Utworzona', 'Rozesłana', 'Zamknięta'],
    stanPoczatkowy: 'Utworzona',
  },
];

export const MOCK_SPRAWY: Sprawa[] = [
  {
    id: 'spr-1',
    znakSprawy: 'WO.6220.14.2026',
    tytul: 'Wniosek o wydanie decyzji środowiskowej',
    jrwa: '6220',
    definicjaObieguId: 'obieg-decyzja',
    stan: 'W toku',
    obywatelId: 'ob-1',
    wydzial: 'Wydział Ochrony Środowiska',
    referent: 'Katarzyna Zielińska',
    dataWszczecia: '2026-09-02',
    terminUstawowy: '2026-11-02',
    kategoriaArchiwalna: 'A',
    zamknieta: false,
  },
  {
    id: 'spr-2',
    znakSprawy: 'OR.1510.3.2026',
    tytul: 'Skarga na funkcjonowanie wydziału komunikacji',
    jrwa: '1510',
    definicjaObieguId: 'obieg-skarga',
    stan: 'Odpowiedź przygotowana',
    obywatelId: 'ob-2',
    wydzial: 'Wydział Organizacyjny',
    referent: 'Piotr Lewandowski',
    dataWszczecia: '2026-10-01',
    terminUstawowy: '2026-10-31',
    kategoriaArchiwalna: 'B5',
    zamknieta: false,
  },
  {
    id: 'spr-3',
    znakSprawy: 'FK.3120.88.2026',
    tytul: 'Faktura kosztowa — usługi serwisowe',
    jrwa: '3120',
    definicjaObieguId: 'obieg-faktura',
    stan: 'Zaakceptowana',
    obywatelId: 'ob-3',
    wydzial: 'Wydział Księgowości',
    referent: 'Marta Wiśniewska',
    dataWszczecia: '2026-10-10',
    kategoriaArchiwalna: 'B5',
    zamknieta: false,
  },
];

export const MOCK_METRYKI: WpisMetryki[] = [
  {
    id: 'm-1',
    sprawaId: 'spr-1',
    data: '2026-09-02',
    czynnosc: 'Rejestracja sprawy',
    osoba: 'Katarzyna Zielińska',
    opis: 'Nadano znak sprawy WO.6220.14.2026',
  },
  {
    id: 'm-2',
    sprawaId: 'spr-1',
    data: '2026-09-10',
    czynnosc: 'Dekretacja',
    osoba: 'Kierownik Wydziału',
    opis: 'Przekazano do referenta',
  },
];

export const MOCK_TYPY_DOKUMENTOW: TypDokumentu[] = [
  { id: 'td-1', nazwa: 'Pełnomocnictwo', opis: 'Pełnomocnictwo do reprezentowania strony' },
  { id: 'td-2', nazwa: 'Potwierdzenie opłaty', opis: 'Potwierdzenie wniesienia opłaty skarbowej' },
  { id: 'td-3', nazwa: 'Decyzja administracyjna', opis: 'Finalna decyzja w sprawie' },
  { id: 'td-4', nazwa: 'Wezwanie do zapłaty', opis: 'Pismo wzywające do uregulowania należności' },
];

export const MOCK_SZABLONY: SzablonJasper[] = [
  {
    id: 'sz-1',
    typDokumentuId: 'td-3',
    nazwa: 'decyzja_srodowiskowa.jrxml',
    wersja: 3,
    dataWgrania: '2026-08-15',
    autor: 'Administrator',
  },
  {
    id: 'sz-2',
    typDokumentuId: 'td-4',
    nazwa: 'wezwanie_do_zaplaty.jrxml',
    wersja: 1,
    dataWgrania: '2026-07-01',
    autor: 'Administrator',
  },
];

export const MOCK_DOKUMENTY: Dokument[] = [
  {
    id: 'd-1',
    sprawaId: 'spr-1',
    typDokumentuId: 'td-1',
    nazwa: 'Pełnomocnictwo J. Kowalski',
    aktualnaWersja: 1,
    dataUtworzenia: '2026-09-03',
  },
  {
    id: 'd-2',
    sprawaId: 'spr-1',
    typDokumentuId: 'td-3',
    nazwa: 'Decyzja środowiskowa',
    aktualnaWersja: 2,
    dataUtworzenia: '2026-10-05',
  },
];

export const MOCK_WERSJE_DOKUMENTOW: WersjaDokumentu[] = [
  {
    id: 'wd-1',
    dokumentId: 'd-2',
    wersja: 1,
    podpisana: false,
    dataUtworzenia: '2026-09-28',
    autor: 'Katarzyna Zielińska',
    statusPodpisu: 'brak',
  },
  {
    id: 'wd-2',
    dokumentId: 'd-2',
    wersja: 2,
    podpisana: true,
    dataUtworzenia: '2026-10-05',
    autor: 'Katarzyna Zielińska',
    statusPodpisu: 'poprawny',
  },
];

export const MOCK_PRZESYLKI: Przesylka[] = [
  {
    id: 'p-1',
    sprawaId: 'spr-1',
    dokumentId: 'd-2',
    obywatelId: 'ob-1',
    kanal: 'e-doreczenia',
    status: 'doreczone',
    numerReferencyjny: 'ADE/2026/00123',
    dataNadania: '2026-10-06',
    dataStatusu: '2026-10-07',
  },
  {
    id: 'p-2',
    sprawaId: 'spr-2',
    dokumentId: 'd-1',
    obywatelId: 'ob-2',
    kanal: 'operator_pocztowy',
    status: 'nadane',
    numerReferencyjny: 'PL2026000998',
    dataNadania: '2026-10-15',
    dataStatusu: '2026-10-15',
  },
];

export const MOCK_NALEZNOSCI: Naleznosc[] = [
  {
    id: 'n-1',
    sprawaId: 'spr-1',
    obywatelId: 'ob-1',
    numerReferencyjny: 'OPL/2026/5541',
    tytul: 'Opłata skarbowa',
    kwota: 17,
    kwotaRozliczona: 17,
    terminPlatnosci: '2026-09-10',
    status: 'rozliczona',
  },
  {
    id: 'n-2',
    sprawaId: 'spr-3',
    obywatelId: 'ob-3',
    numerReferencyjny: 'FK/2026/0088',
    tytul: 'Usługi serwisowe — faktura',
    kwota: 4500,
    kwotaRozliczona: 0,
    terminPlatnosci: '2026-10-30',
    status: 'nierozliczona',
  },
];

export const MOCK_WPLATY: Wplata[] = [
  {
    id: 'w-1',
    naleznoscId: 'n-1',
    kwota: 17,
    data: '2026-09-09',
    tytul: 'Opłata skarbowa WO.6220.14.2026',
    kontrahent: 'Jan Kowalski',
    dopasowanaAutomatycznie: true,
  },
  {
    id: 'w-2',
    data: '2026-10-18',
    kwota: 250,
    tytul: 'Wpłata niezidentyfikowana',
    kontrahent: 'Nieznany',
    dopasowanaAutomatycznie: false,
  },
];

export const MOCK_JRWA: PozycjaJrwa[] = [
  { symbol: '1510', haslo: 'Skargi i wnioski', kategoriaArchiwalna: 'B5' },
  { symbol: '3120', haslo: 'Faktury i rachunki', kategoriaArchiwalna: 'B5' },
  { symbol: '6220', haslo: 'Decyzje o środowiskowych uwarunkowaniach', kategoriaArchiwalna: 'A' },
];

export const MOCK_UZYTKOWNIK: Uzytkownik = {
  id: 'u-1',
  login: 'demo.referent',
  imie: 'Katarzyna',
  nazwisko: 'Zielińska',
  email: 'katarzyna.zielinska@urzad.gov.pl',
  role: ['referent'],
  wydzial: 'Wydział Ochrony Środowiska',
  stanowisko: 'Referent',
};
