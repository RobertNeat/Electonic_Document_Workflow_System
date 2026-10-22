// Mockowe dane historii wymiany — lokalne dla modułu, nie trafiają do core/mock.
export type StatusWymiany = 'sukces' | 'blad_walidacji';
export type KierunekWymiany = 'import' | 'eksport';

export interface WpisHistoriiWymiany {
  id: string;
  nazwaPliku: string;
  kierunek: KierunekWymiany;
  data: string;
  status: StatusWymiany;
  liczbaRekordow: number;
  opis?: string;
}

export const MOCK_HISTORIA_XML: WpisHistoriiWymiany[] = [
  {
    id: 'hx-1',
    nazwaPliku: 'komunikat_sprawa_WO.6220.14.2026.xml',
    kierunek: 'import',
    data: '2026-10-20 09:14',
    status: 'sukces',
    liczbaRekordow: 1,
    opis: 'Zaimportowano komunikat sprawy z systemu EZD Urzędu Wojewódzkiego',
  },
  {
    id: 'hx-2',
    nazwaPliku: 'eksport_OR.1510.3.2026.xml',
    kierunek: 'eksport',
    data: '2026-10-18 14:02',
    status: 'sukces',
    liczbaRekordow: 1,
    opis: 'Nadano komunikat sprawy do systemu zewnętrznego',
  },
  {
    id: 'hx-3',
    nazwaPliku: 'komunikat_sprawa_FK.3120.88.xml',
    kierunek: 'import',
    data: '2026-10-15 11:47',
    status: 'blad_walidacji',
    liczbaRekordow: 0,
    opis: 'Niezgodność ze schematem XSD — brak wymaganego elementu znakSprawy',
  },
  {
    id: 'hx-4',
    nazwaPliku: 'komunikat_sprawa_batch_2026_10.xml',
    kierunek: 'import',
    data: '2026-10-05 08:30',
    status: 'sukces',
    liczbaRekordow: 6,
    opis: 'Import zbiorczy komunikatów spraw',
  },
];

export const MOCK_HISTORIA_UBL: WpisHistoriiWymiany[] = [
  {
    id: 'hu-1',
    nazwaPliku: 'faktura_FK-2026-0088.xml',
    kierunek: 'import',
    data: '2026-10-19 10:05',
    status: 'sukces',
    liczbaRekordow: 1,
    opis: 'Zmapowano fakturę kosztową na należność FK/2026/0088',
  },
  {
    id: 'hu-2',
    nazwaPliku: 'faktura_budimex_0231.xml',
    kierunek: 'import',
    data: '2026-10-12 13:22',
    status: 'sukces',
    liczbaRekordow: 1,
    opis: 'Faktura UBL 2.1 od Budimex Serwis Sp. z o.o.',
  },
  {
    id: 'hu-3',
    nazwaPliku: 'faktura_niepoprawna_namespace.xml',
    kierunek: 'import',
    data: '2026-10-08 16:40',
    status: 'blad_walidacji',
    liczbaRekordow: 0,
    opis: 'Nieprawidłowa przestrzeń nazw UBL — oczekiwano urn:oasis:names:specification:ubl:schema:xsd:Invoice-2',
  },
];
