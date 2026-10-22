export interface DefinicjaObiegu {
  id: string;
  nazwa: string;
  opis: string;
  stany: string[];
  stanPoczatkowy: string;
}

export type KategoriaArchiwalna = 'A' | 'B5' | 'B10' | 'BE5' | 'BE10';

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

export interface WpisMetryki {
  id: string;
  sprawaId: string;
  data: string;
  czynnosc: string;
  osoba: string;
  opis: string;
}
