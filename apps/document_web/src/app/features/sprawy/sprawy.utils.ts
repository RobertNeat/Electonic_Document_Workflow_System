import { Sprawa } from '../../core/models';

export type SeweroscTagu = 'success' | 'secondary' | 'info' | 'warn' | 'danger' | 'contrast';

/** Kolor p-tag wg stanu sprawy — stany końcowe zielone, w toku niebieskie, pozostałe neutralne. */
export function seweroscStanu(stan: string, zamknieta: boolean): SeweroscTagu {
  if (zamknieta) {
    return 'success';
  }
  const stanyKoncowe = ['Zakończona', 'Zamknięta', 'Rozliczona'];
  if (stanyKoncowe.includes(stan)) {
    return 'success';
  }
  if (stan === 'Zarejestrowana' || stan === 'Utworzona') {
    return 'info';
  }
  return 'warn';
}

export type StatusTerminu = 'brak' | 'przeterminowany' | 'zblizajacy' | 'ok';

const PROG_ZBLIZAJACEGO_SIE_TERMINU_DNI = 14;

function parsujDateLokalna(iso: string): Date {
  const [rok, miesiac, dzien] = iso.split('-').map(Number);
  return new Date(rok, miesiac - 1, dzien);
}

/** Ocenia termin ustawowy względem dzisiejszej daty (lokalnej, bez przesunięć UTC). */
export function statusTerminu(sprawa: Sprawa, dzis: Date = new Date()): StatusTerminu {
  if (!sprawa.terminUstawowy || sprawa.zamknieta) {
    return 'brak';
  }
  const termin = parsujDateLokalna(sprawa.terminUstawowy);
  const dzisPolnoc = new Date(dzis.getFullYear(), dzis.getMonth(), dzis.getDate());
  const roznicaDni = Math.round((termin.getTime() - dzisPolnoc.getTime()) / 86400000);
  if (roznicaDni < 0) {
    return 'przeterminowany';
  }
  if (roznicaDni <= PROG_ZBLIZAJACEGO_SIE_TERMINU_DNI) {
    return 'zblizajacy';
  }
  return 'ok';
}
