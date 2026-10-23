import type {
  Uzytkownik,
  DostawcaWysylki,
  StawkaOplaty,
  Wydzial,
  WpisDziennikaZdarzen,
} from '@electronic-document-workflow/document-shared-dto';

// Lokalne dane mockowe modułu Administracja — rozszerzają jednoprzykładowy MOCK_UZYTKOWNIK z core/mock
// o kilka dodatkowych kont, bez modyfikowania pliku współdzielonego.
export const MOCK_UZYTKOWNICY: Uzytkownik[] = [
  {
    id: 'u-1',
    login: 'demo.referent',
    imie: 'Katarzyna',
    nazwisko: 'Zielińska',
    email: 'katarzyna.zielinska@urzad.gov.pl',
    role: ['referent'],
    wydzial: 'Wydział Ochrony Środowiska',
    stanowisko: 'Referent',
  },
  {
    id: 'u-2',
    login: 'demo.administrator',
    imie: 'Adam',
    nazwisko: 'Nowakowski',
    email: 'adam.nowakowski@urzad.gov.pl',
    role: ['administrator'],
    wydzial: 'Wydział Organizacyjny',
    stanowisko: 'Administrator systemu',
  },
  {
    id: 'u-3',
    login: 'demo.kierownik',
    imie: 'Piotr',
    nazwisko: 'Lewandowski',
    email: 'piotr.lewandowski@urzad.gov.pl',
    role: ['kierownik'],
    wydzial: 'Wydział Organizacyjny',
    stanowisko: 'Kierownik wydziału',
  },
  {
    id: 'u-4',
    login: 'demo.ksiegowosc',
    imie: 'Marta',
    nazwisko: 'Wiśniewska',
    email: 'marta.wisniewska@urzad.gov.pl',
    role: ['ksiegowosc'],
    wydzial: 'Wydział Księgowości',
    stanowisko: 'Specjalista ds. księgowości',
  },
  {
    id: 'u-5',
    login: 'demo.kancelaria',
    imie: 'Ewa',
    nazwisko: 'Kowalczyk',
    email: 'ewa.kowalczyk@urzad.gov.pl',
    role: ['kancelaria'],
    wydzial: 'Wydział Organizacyjny',
    stanowisko: 'Pracownik kancelarii',
  },
  {
    id: 'u-6',
    login: 'jan.kaminski',
    imie: 'Jan',
    nazwisko: 'Kamiński',
    email: 'jan.kaminski@urzad.gov.pl',
    role: ['referent', 'kierownik'],
    wydzial: 'Wydział Komunikacji',
    stanowisko: 'Zastępca kierownika',
  },
];

export const MOCK_WYDZIALY: Wydzial[] = [
  {
    id: 'w-1',
    nazwa: 'Wydział Ochrony Środowiska',
    kierownik: 'Katarzyna Zielińska',
    stanowiska: [
      { id: 'w-1-s1', nazwa: 'Kierownik wydziału' },
      { id: 'w-1-s2', nazwa: 'Referent' },
      { id: 'w-1-s3', nazwa: 'Starszy referent' },
    ],
  },
  {
    id: 'w-2',
    nazwa: 'Wydział Organizacyjny',
    kierownik: 'Piotr Lewandowski',
    stanowiska: [
      { id: 'w-2-s1', nazwa: 'Kierownik wydziału' },
      { id: 'w-2-s2', nazwa: 'Pracownik kancelarii' },
      { id: 'w-2-s3', nazwa: 'Administrator systemu' },
      { id: 'w-2-s4', nazwa: 'Sekretarz' },
    ],
  },
  {
    id: 'w-3',
    nazwa: 'Wydział Księgowości',
    kierownik: 'Marta Wiśniewska',
    stanowiska: [
      { id: 'w-3-s1', nazwa: 'Główny księgowy' },
      { id: 'w-3-s2', nazwa: 'Specjalista ds. księgowości' },
      { id: 'w-3-s3', nazwa: 'Kasjer' },
    ],
  },
  {
    id: 'w-4',
    nazwa: 'Wydział Komunikacji',
    kierownik: 'Jan Kamiński',
    stanowiska: [
      { id: 'w-4-s1', nazwa: 'Kierownik wydziału' },
      { id: 'w-4-s2', nazwa: 'Zastępca kierownika' },
      { id: 'w-4-s3', nazwa: 'Referent ds. rejestracji pojazdów' },
    ],
  },
];

export const MOCK_DOSTAWCY_WYSYLKI: DostawcaWysylki[] = [
  { id: 'd-1', nazwa: 'e-Doręczenia', typ: 'e-doreczenia', aktywny: true },
  {
    id: 'd-2',
    nazwa: 'Poczta Polska',
    typ: 'operator_pocztowy',
    operatorPocztowy: 'Poczta Polska S.A.',
    aktywny: true,
  },
  {
    id: 'd-3',
    nazwa: 'E-mail (kanał pomocniczy)',
    typ: 'email',
    email: 'urzad@urzad.gov.pl',
    aktywny: true,
  },
];

export const MOCK_STAWKI_OPLAT: StawkaOplaty[] = [
  { id: 's-1', nazwa: 'Opłata skarbowa — pełnomocnictwo', kwota: 17 },
  { id: 's-2', nazwa: 'Opłata skarbowa — wydanie decyzji administracyjnej', kwota: 10 },
  { id: 's-3', nazwa: 'Opłata za wydanie zaświadczenia', kwota: 17 },
  { id: 's-4', nazwa: 'Opłata administracyjna — zmiana wpisu', kwota: 50 },
];

export const MOCK_DZIENNIK_ZDARZEN: WpisDziennikaZdarzen[] = [
  {
    id: 'ev-1',
    dataCzas: '2026-10-20 08:12',
    uzytkownik: 'demo.kancelaria',
    typOperacji: 'Rejestracja sprawy',
    obiekt: 'WO.6220.14.2026',
    szczegoly: 'Zarejestrowano wniosek o wydanie decyzji środowiskowej.',
  },
  {
    id: 'ev-2',
    dataCzas: '2026-10-20 09:03',
    uzytkownik: 'demo.kierownik',
    typOperacji: 'Dekretacja',
    obiekt: 'WO.6220.14.2026',
    szczegoly: 'Przekazano sprawę do referenta K. Zielińskiej.',
  },
  {
    id: 'ev-3',
    dataCzas: '2026-10-20 11:47',
    uzytkownik: 'demo.referent',
    typOperacji: 'Zmiana stanu sprawy',
    obiekt: 'WO.6220.14.2026',
    szczegoly: 'Zmiana stanu: Zarejestrowana → W toku.',
  },
  {
    id: 'ev-4',
    dataCzas: '2026-10-21 10:15',
    uzytkownik: 'demo.referent',
    typOperacji: 'Podpisanie dokumentu',
    obiekt: 'Decyzja środowiskowa (d-2)',
    szczegoly: 'Podpisano dokument certyfikatem demonstracyjnym (PAdES).',
  },
  {
    id: 'ev-5',
    dataCzas: '2026-10-21 10:20',
    uzytkownik: 'demo.referent',
    typOperacji: 'Wysyłka pisma',
    obiekt: 'WO.6220.14.2026',
    szczegoly: 'Wysłano pismo przez e-Doręczenia, numer ref. ADE/2026/00123.',
  },
  {
    id: 'ev-6',
    dataCzas: '2026-10-21 14:02',
    uzytkownik: 'demo.ksiegowosc',
    typOperacji: 'Rejestracja wpłaty',
    obiekt: 'OPL/2026/5541',
    szczegoly: 'Zarejestrowano wpłatę 17 zł — opłata skarbowa.',
  },
  {
    id: 'ev-7',
    dataCzas: '2026-10-21 15:40',
    uzytkownik: 'demo.administrator',
    typOperacji: 'Zmiana danych użytkownika',
    obiekt: 'jan.kaminski',
    szczegoly: 'Dodano rolę "kierownik" dla użytkownika.',
  },
  {
    id: 'ev-8',
    dataCzas: '2026-10-22 08:30',
    uzytkownik: 'demo.kancelaria',
    typOperacji: 'Rejestracja sprawy',
    obiekt: 'OR.1510.3.2026',
    szczegoly: 'Zarejestrowano skargę na funkcjonowanie wydziału komunikacji.',
  },
  {
    id: 'ev-9',
    dataCzas: '2026-10-22 09:55',
    uzytkownik: 'demo.referent',
    typOperacji: 'Zmiana stanu sprawy',
    obiekt: 'OR.1510.3.2026',
    szczegoly: 'Zmiana stanu: W toku → Odpowiedź przygotowana.',
  },
  {
    id: 'ev-10',
    dataCzas: '2026-10-22 10:05',
    uzytkownik: 'demo.ksiegowosc',
    typOperacji: 'Rejestracja wpłaty',
    obiekt: 'FK/2026/0088',
    szczegoly:
      'Import wyciągu CAMT.053 — wpłata niedopasowana automatycznie, wymaga rekoncyliacji.',
  },
];
