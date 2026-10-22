export interface PozycjaMenu {
  etykieta: string;
  ikona: string;
  routerLink?: string;
  role?: string[];
  dzieci?: PozycjaMenu[];
}
