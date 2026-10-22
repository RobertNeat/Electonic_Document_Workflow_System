export type KanalWysylki = 'e-doreczenia' | 'operator_pocztowy' | 'email';
export type StatusPrzesylki = 'nadane' | 'doreczone' | 'odebrane' | 'zwrot' | 'awizo';

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
}
