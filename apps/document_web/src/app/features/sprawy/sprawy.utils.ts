import type {
  DefinicjaObiegu,
  Dokument,
  Sprawa,
  TypDokumentu,
  Uzytkownik,
} from '@electronic-document-workflow/document-shared-dto';

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

/** Jedno możliwe przejście z aktualnego stanu sprawy wraz z informacją o dostępności. */
export interface MozliweprzejscieObiegu {
  doStanu: string;
  dostepne: boolean;
  powodyBlokady: string[];
}

/**
 * Wyznacza wszystkie możliwe przejścia z danego stanu wg `DefinicjaObiegu.przejscia` (może być
 * rozgałęzienie — kilka wpisów lub jeden wpis z wieloma `doStanow`). Każdy cel dziedziczy
 * wymagania (`wymaganaRola`/`wymaganeTypyDokumentow`) z wpisu `przejscia`, z którego pochodzi;
 * jeśli ten sam stan docelowy występuje w więcej niż jednym wpisie, preferowany jest ten
 * dostępny (łagodniejsze z duplikatów wygrywa).
 */
export function przejsciaZeStanu(
  definicja: DefinicjaObiegu,
  stan: string,
  uzytkownik: Uzytkownik,
  dokumentySprawy: Pick<Dokument, 'typDokumentuId'>[],
  typyDokumentow: TypDokumentu[],
): MozliweprzejscieObiegu[] {
  const wgCelu = new Map<string, MozliweprzejscieObiegu>();
  for (const przejscie of definicja.przejscia) {
    if (przejscie.zStanu !== stan) continue;
    const powodyBlokady: string[] = [];

    if (
      przejscie.wymaganaRola &&
      !(uzytkownik.role as readonly string[]).includes(przejscie.wymaganaRola)
    ) {
      powodyBlokady.push(`Wymagana rola: ${przejscie.wymaganaRola}`);
    }

    const typyDokumentowId = przejscie.wymaganeTypyDokumentow ?? [];
    const posiadaneTypyId = new Set(dokumentySprawy.map((d) => d.typDokumentuId));
    const brakujaceTypyId = typyDokumentowId.filter((id) => !posiadaneTypyId.has(id));
    for (const typId of brakujaceTypyId) {
      const nazwaTypu = typyDokumentow.find((t) => t.id === typId)?.nazwa ?? typId;
      powodyBlokady.push(`Wymagany dokument: ${nazwaTypu}`);
    }

    for (const doStanu of przejscie.doStanow) {
      const istniejacy = wgCelu.get(doStanu);
      const kandydat: MozliweprzejscieObiegu = {
        doStanu,
        dostepne: powodyBlokady.length === 0,
        powodyBlokady,
      };
      if (!istniejacy || (!istniejacy.dostepne && kandydat.dostepne)) {
        wgCelu.set(doStanu, kandydat);
      }
    }
  }
  return [...wgCelu.values()];
}

/** Stan końcowy obiegu = stan, z którego żadne przejście nie wychodzi (brak wyjścia w grafie). */
export function czyStanKoncowy(definicja: DefinicjaObiegu, stan: string): boolean {
  return !definicja.przejscia.some((p) => p.zStanu === stan);
}
