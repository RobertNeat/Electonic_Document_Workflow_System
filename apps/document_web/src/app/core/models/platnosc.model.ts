export type StatusRozliczenia =
  'nierozliczona' | 'czesciowo_rozliczona' | 'rozliczona' | 'przeterminowana';

export interface Naleznosc {
  id: string;
  sprawaId: string;
  obywatelId: string;
  numerReferencyjny: string;
  tytul: string;
  kwota: number;
  kwotaRozliczona: number;
  terminPlatnosci: string;
  status: StatusRozliczenia;
}

export interface Wplata {
  id: string;
  naleznoscId?: string;
  kwota: number;
  data: string;
  tytul: string;
  kontrahent: string;
  dopasowanaAutomatycznie: boolean;
}
