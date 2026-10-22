export type KategoriaArchiwalna = 'A' | 'B5' | 'B10' | 'BE5' | 'BE10';

export interface PrzejscieObiegu {
  zStanu: string;
  doStanow: string[];
  wymaganaRola?: string;
  wymaganeTypyDokumentow?: string[];
}

export interface DefinicjaObiegu {
  id: string;
  nazwa: string;
  opis: string;
  stany: string[];
  stanPoczatkowy: string;
  przejscia: PrzejscieObiegu[];
}

export interface Sprawa {
  id: string;
  znakSprawy: string;
  tytul: string;
  jrwa: string;
  definicjaObieguId: string;
  stan: string;
  obywatelId: string;
  wydzial: string;
  referent: string;
  dataWszczecia: string;
  terminUstawowy?: string;
  kategoriaArchiwalna: KategoriaArchiwalna;
  zamknieta: boolean;
}

// Bez zmian wobec `Sprawa` — front ma już pełne pola potrzebne do tabeli listy.
export type SprawaListElement = Sprawa;

export interface WpisMetryki {
  id: string;
  sprawaId: string;
  data: string;
  czynnosc: string;
  osoba: string;
  opis: string;
}

export interface UtworzSpraweRequest {
  definicjaObieguId: string;
  jrwa: string;
  kategoriaArchiwalna: KategoriaArchiwalna;
  tytul: string;
  obywatelId: string;
  wydzial: string;
  referent: string;
  terminUstawowy?: string;
}

export interface ZmienStanSprawyRequest {
  nowyStan: string;
}
