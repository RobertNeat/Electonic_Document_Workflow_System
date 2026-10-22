export type RolaUzytkownika =
  'administrator' | 'kancelaria' | 'referent' | 'kierownik' | 'ksiegowosc';

export interface Uzytkownik {
  id: string;
  login: string;
  imie: string;
  nazwisko: string;
  email: string;
  role: RolaUzytkownika[];
  wydzial: string;
  stanowisko: string;
}
