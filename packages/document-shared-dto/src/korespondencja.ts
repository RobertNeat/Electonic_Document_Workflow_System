export type KanalWysylki = 'e-doreczenia' | 'operator_pocztowy' | 'email';
export type StatusPrzesylki = 'nadane' | 'doreczone' | 'odebrane' | 'zwrot' | 'awizo';

export interface WpisStatusuPrzesylki {
  status: StatusPrzesylki;
  data: string;
}

export interface Przesylka {
  id: string;
  sprawaId: string;
  dokumentId: string;
  obywatelId: string;
  kanal: KanalWysylki;
  status: StatusPrzesylki;
  numerReferencyjny: string;
  dataNadania: string;
  dataStatusu: string;
  kierunek: 'wplywajaca' | 'wychodzaca';
  historiaStatusow: WpisStatusuPrzesylki[];
}

export interface UtworzPrzesylkeRequest {
  sprawaId: string;
  dokumentId: string;
  obywatelId: string;
  kanal: KanalWysylki;
}
