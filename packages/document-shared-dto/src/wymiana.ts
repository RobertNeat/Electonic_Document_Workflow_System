import type { Sprawa } from './sprawy';

export interface BladWalidacjiXsd {
  element: string;
  opis: string;
}

export interface WynikWalidacjiXml {
  sukces: boolean;
  bledy: BladWalidacjiXsd[];
  sprawaZmapowana?: Partial<Sprawa>;
}

export interface PozycjaFakturyUbl {
  nazwa: string;
  ilosc: number;
  cenaJednostkowa: number;
  wartosc: number;
}

export interface FakturaUbl {
  numer: string;
  kontrahent: string;
  nip: string;
  kwota: number;
  pozycje: PozycjaFakturyUbl[];
  numerReferencyjnyNaleznosci: string;
}

export interface WynikWalidacjiUbl {
  sukces: boolean;
  bledy: BladWalidacjiXsd[];
  faktura?: FakturaUbl;
}

export interface WpisHistoriiWymiany {
  id: string;
  nazwaPliku: string;
  kierunek: 'import' | 'eksport';
  data: string;
  status: 'sukces' | 'blad_walidacji';
  liczbaRekordow: number;
  opis?: string;
}
