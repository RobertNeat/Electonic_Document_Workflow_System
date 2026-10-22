import type { KanalWysylki } from './korespondencja';
import type { RolaUzytkownika } from './administracja';

export type TypCeluAkcjiZbiorowej = 'SPRAWA' | 'OBYWATEL' | 'NALEZNOSC' | 'UZYTKOWNIK';

export type TypAkcjiZbiorowej =
  | 'ZMIANA_STANU'
  | 'WYSYLKA_PISM'
  | 'GENEROWANIE_DOKUMENTOW'
  | 'AKTUALIZACJA_DANYCH'
  | 'AKTUALIZACJA_ROL';

export interface ParametryZmianyStanu {
  nowyStan: string;
}

export interface ParametryWysylkiPism {
  kanal: KanalWysylki;
}

export interface ParametryGenerowaniaDokumentow {
  szablonId: string;
}

export interface ParametryAktualizacjiRol {
  dodajRole?: RolaUzytkownika[];
  usunRole?: RolaUzytkownika[];
}

// Spec nie definiuje jeszcze kształtu parametrów dla AKTUALIZACJA_DANYCH — na razie otwarty rekord.
export type ParametryAktualizacjiDanych = Record<string, unknown>;

// Mapa typ akcji -> kształt jej `parametry`, bez pola `typAkcji` wewnątrz (payload zgodny ze
// spec'iem i rekordami Java). Używana do zbudowania dyskryminowanej unii żądań poniżej.
export interface ParametryAkcjiZbiorowejWgTypu {
  ZMIANA_STANU: ParametryZmianyStanu;
  WYSYLKA_PISM: ParametryWysylkiPism;
  GENEROWANIE_DOKUMENTOW: ParametryGenerowaniaDokumentow;
  AKTUALIZACJA_DANYCH: ParametryAktualizacjiDanych;
  AKTUALIZACJA_ROL: ParametryAktualizacjiRol;
}

export type ParametryAkcjiZbiorowej =
  ParametryAkcjiZbiorowejWgTypu[keyof ParametryAkcjiZbiorowejWgTypu];

// Dyskryminowana unia po `typAkcji`: dla danego `typAkcji` TS wymusza zgodny kształt `parametry`
// (np. `typAkcji: 'WYSYLKA_PISM'` wymaga `parametry: { kanal }`, nie pozwala na `{ nowyStan }`).
export type UruchomAkcjeZbiorowaRequest<T extends TypAkcjiZbiorowej = TypAkcjiZbiorowej> = {
  [K in T]: {
    typAkcji: K;
    typCelu: TypCeluAkcjiZbiorowej;
    idCelow?: string[];
    kryteria?: Record<string, unknown>;
    parametry: ParametryAkcjiZbiorowejWgTypu[K];
  };
}[T];

export interface UruchomAkcjeZbiorowaResponse {
  akcjaId: string;
}

export interface WynikElementuAkcji {
  idCelu: string;
  status: 'sukces' | 'blad';
  komunikat?: string;
}

export interface StatusAkcjiZbiorowej {
  akcjaId: string;
  typAkcji: TypAkcjiZbiorowej;
  typCelu: TypCeluAkcjiZbiorowej;
  status: 'trwajaca' | 'zakonczona';
  postep: number;
  lacznie: number;
  przetworzone: number;
  bledne: number;
  wynikiElementow: WynikElementuAkcji[];
}
