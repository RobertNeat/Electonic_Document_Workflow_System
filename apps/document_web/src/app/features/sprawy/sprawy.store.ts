import { Injectable, signal } from '@angular/core';
import type {
  Sprawa,
  WpisMetryki,
  KategoriaArchiwalna,
} from '@electronic-document-workflow/document-shared-dto';
import { MOCK_SPRAWY, MOCK_METRYKI, MOCK_UZYTKOWNIK } from '../../core/mock/mock-data';

/**
 * Mapa symboli wydziałów używana przy generowaniu znaku sprawy.
 * Wywnioskowana z istniejących znaków w danych mockowych — brak takiego
 * słownika w core/, zob. zgłoszenie w raporcie końcowym agenta.
 */
const SYMBOLE_WYDZIALOW: Record<string, string> = {
  'Wydział Ochrony Środowiska': 'WO',
  'Wydział Organizacyjny': 'OR',
  'Wydział Księgowości': 'FK',
};

export interface NowaSprawaDane {
  tytul: string;
  jrwa: string;
  kategoriaArchiwalna: KategoriaArchiwalna;
  definicjaObieguId: string;
  obywatelId: string;
  wydzial: string;
  referent: string;
  terminUstawowy?: string;
}

/**
 * Przechowuje stan spraw współdzielony między listą, szczegółami i formularzem
 * rejestracji — wyłącznie w pamięci (mock), bez prawdziwego API.
 */
@Injectable({ providedIn: 'root' })
export class SprawyStore {
  private readonly stanSprawy = signal<Sprawa[]>([...MOCK_SPRAWY]);
  private readonly stanMetryki = signal<WpisMetryki[]>([...MOCK_METRYKI]);
  private licznikMetryk = MOCK_METRYKI.length;

  readonly sprawy = this.stanSprawy.asReadonly();
  readonly metryki = this.stanMetryki.asReadonly();

  znajdz(id: string): Sprawa | undefined {
    return this.stanSprawy().find((s) => s.id === id);
  }

  metrykiDlaSprawy(sprawaId: string): WpisMetryki[] {
    return this.stanMetryki()
      .filter((m) => m.sprawaId === sprawaId)
      .sort((a, b) => a.data.localeCompare(b.data));
  }

  symbolWydzialu(wydzial: string): string {
    return SYMBOLE_WYDZIALOW[wydzial] ?? wydzial.slice(0, 2).toUpperCase();
  }

  /** Generuje znak sprawy w formacie SYMBOL.JRWA.NUMER.ROK na podstawie dotychczasowych znaków. */
  generujZnakSprawy(wydzial: string, jrwa: string): string {
    const symbol = this.symbolWydzialu(wydzial);
    const rok = new Date().getFullYear();
    const prefiks = `${symbol}.${jrwa}.`;
    let maxNumer = 0;
    for (const s of this.stanSprawy()) {
      if (s.znakSprawy.startsWith(prefiks) && s.znakSprawy.endsWith(`.${rok}`)) {
        const czesci = s.znakSprawy.split('.');
        const numer = Number(czesci[2]);
        if (!Number.isNaN(numer) && numer > maxNumer) {
          maxNumer = numer;
        }
      }
    }
    return `${symbol}.${jrwa}.${maxNumer + 1}.${rok}`;
  }

  dodajSprawe(dane: NowaSprawaDane, stanPoczatkowy: string): Sprawa {
    const znakSprawy = this.generujZnakSprawy(dane.wydzial, dane.jrwa);
    const dzis = formatujDateLokalna(new Date());
    const nowaSprawa: Sprawa = {
      id: `spr-${Date.now()}`,
      znakSprawy,
      tytul: dane.tytul,
      jrwa: dane.jrwa,
      definicjaObieguId: dane.definicjaObieguId,
      stan: stanPoczatkowy,
      obywatelId: dane.obywatelId,
      wydzial: dane.wydzial,
      referent: dane.referent,
      dataWszczecia: dzis,
      terminUstawowy: dane.terminUstawowy,
      kategoriaArchiwalna: dane.kategoriaArchiwalna,
      zamknieta: false,
    };
    this.stanSprawy.update((lista) => [nowaSprawa, ...lista]);
    this.dodajWpisMetryki(nowaSprawa.id, 'Rejestracja sprawy', `Nadano znak sprawy ${znakSprawy}`);
    return nowaSprawa;
  }

  /** Zmienia stan sprawy na kolejny wg definicji obiegu; ostatni stan zamyka sprawę. */
  zmienStan(sprawaId: string, nowyStan: string, czyOstatni: boolean): void {
    const poprzedni = this.znajdz(sprawaId)?.stan;
    this.stanSprawy.update((lista) =>
      lista.map((s) => (s.id === sprawaId ? { ...s, stan: nowyStan, zamknieta: czyOstatni } : s)),
    );
    this.dodajWpisMetryki(sprawaId, 'Zmiana stanu', `${poprzedni} → ${nowyStan}`);
  }

  private dodajWpisMetryki(sprawaId: string, czynnosc: string, opis: string): void {
    this.licznikMetryk += 1;
    const wpis: WpisMetryki = {
      id: `m-${this.licznikMetryk}`,
      sprawaId,
      data: formatujDateLokalna(new Date()),
      czynnosc,
      osoba: `${MOCK_UZYTKOWNIK.imie} ${MOCK_UZYTKOWNIK.nazwisko}`,
      opis,
    };
    this.stanMetryki.update((lista) => [...lista, wpis]);
  }
}

/** Formatuje datę jako YYYY-MM-DD w czasie lokalnym (bez przesunięcia przez UTC). */
export function formatujDateLokalna(data: Date): string {
  const rok = data.getFullYear();
  const miesiac = String(data.getMonth() + 1).padStart(2, '0');
  const dzien = String(data.getDate()).padStart(2, '0');
  return `${rok}-${miesiac}-${dzien}`;
}
