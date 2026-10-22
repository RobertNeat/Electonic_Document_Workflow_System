// Tymczasowe typy lokalne dla modułu Administracja — brak odpowiednich modeli w core/models.
// Zgłoszone do przeniesienia do core/models, gdy struktura organizacyjna i słowniki
// otrzymają realne API (zob. contracts/document_api_administracja.md).

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

export interface DostawcaWysylki {
  id: string;
  nazwa: string;
  typ: 'e-doreczenia' | 'operator_pocztowy' | 'e-mail';
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
  | 'Zmiana danych użytkownika';

export interface WpisDziennikaZdarzen {
  id: string;
  dataCzas: string;
  uzytkownik: string;
  typOperacji: TypOperacjiAudytu;
  obiekt: string;
  szczegoly: string;
}
