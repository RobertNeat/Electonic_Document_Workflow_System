export interface PozycjaJrwa {
  symbol: string;
  haslo: string;
  kategoriaArchiwalna: 'A' | 'B5' | 'B10' | 'BE5' | 'BE10';
}

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

export interface Stanowisko {
  id: string;
  nazwa: string;
}

export interface Wydzial {
  id: string;
  nazwa: string;
  kierownik: string;
  stanowiska: Stanowisko[];
}

// Normalizacja vs mock front-endowy: 'typ' używa 'email' (bez myślnika), zgodnie z
// KanalWysylki w korespondencja.ts. Front-end lokalnie miał 'e-mail' — do poprawy przy migracji.
export interface DostawcaWysylki {
  id: string;
  nazwa: string;
  typ: 'e-doreczenia' | 'operator_pocztowy' | 'email';
  operatorPocztowy?: string;
  email?: string;
  aktywny: boolean;
}

export interface StawkaOplaty {
  id: string;
  nazwa: string;
  kwota: number;
}

export type TypOperacjiAudytu =
  | 'Zmiana stanu sprawy'
  | 'Podpisanie dokumentu'
  | 'Rejestracja wpłaty'
  | 'Rejestracja sprawy'
  | 'Wysyłka pisma'
  | 'Dekretacja'
  | 'Zmiana danych użytkownika'
  | 'Wygenerowanie wezwania do zapłaty';

export interface WpisDziennikaZdarzen {
  id: string;
  dataCzas: string;
  uzytkownik: string;
  typOperacji: TypOperacjiAudytu;
  obiekt: string;
  szczegoly: string;
}

export interface UtworzUzytkownikaRequest {
  login: string;
  imie: string;
  nazwisko: string;
  email: string;
  role: RolaUzytkownika[];
  wydzial: string;
  stanowisko: string;
}

export interface AktualizujRoleUzytkownikaRequest {
  role: RolaUzytkownika[];
}

export interface EdytujDefinicjeObieguRequest {
  nazwa: string;
  opis: string;
}
