import { Injectable, signal, computed } from '@angular/core';
import { MOCK_PRZESYLKI } from '../../core/mock/mock-data';
import type {
  KanalWysylki,
  Przesylka,
  StatusPrzesylki,
  WpisStatusuPrzesylki,
} from '@electronic-document-workflow/document-shared-dto';
import { mozliweKolejneStatusy } from './korespondencja.utils';

let licznikNumerow = 1;

/**
 * Lokalny magazyn stanu modułu korespondencji. Operuje na KOPII mocków —
 * nigdy nie mutuje MOCK_PRZESYLKI, bo jest współdzielony z innymi modułami.
 */
@Injectable({ providedIn: 'root' })
export class KorespondencjaStore {
  private readonly _przesylki = signal<Przesylka[]>(MOCK_PRZESYLKI.map((p) => ({ ...p })));
  private readonly _historia = signal<Record<string, WpisStatusuPrzesylki[]>>(
    this.zbudujHistorieStartowa(),
  );

  readonly przesylki = this._przesylki.asReadonly();

  private zbudujHistorieStartowa(): Record<string, WpisStatusuPrzesylki[]> {
    const wynik: Record<string, WpisStatusuPrzesylki[]> = {};
    for (const p of MOCK_PRZESYLKI) {
      const wpisy: WpisStatusuPrzesylki[] = [{ status: 'nadane', data: p.dataNadania }];
      if (p.status !== 'nadane') {
        wpisy.push({ status: p.status, data: p.dataStatusu });
      }
      wynik[p.id] = wpisy;
    }
    return wynik;
  }

  przesylka(id: string) {
    return computed(() => this._przesylki().find((p) => p.id === id));
  }

  historia(id: string) {
    return computed(() => this._historia()[id] ?? []);
  }

  /** Mockowe nadanie nowego pisma — generuje numer referencyjny i dopisuje wpis do rejestru. */
  nadaj(dane: {
    sprawaId: string;
    dokumentId: string;
    obywatelId: string;
    kanal: KanalWysylki;
  }): Przesylka {
    const dzis = nowaData();
    const nowa: Przesylka = {
      id: `p-nowa-${Date.now()}`,
      sprawaId: dane.sprawaId,
      dokumentId: dane.dokumentId,
      obywatelId: dane.obywatelId,
      kanal: dane.kanal,
      status: 'nadane',
      numerReferencyjny: generujNumerReferencyjny(dane.kanal),
      dataNadania: dzis,
      dataStatusu: dzis,
      kierunek: 'wychodzaca',
      historiaStatusow: [{ status: 'nadane', data: dzis }],
    };
    this._przesylki.update((lista) => [nowa, ...lista]);
    this._historia.update((mapa) => ({ ...mapa, [nowa.id]: [{ status: 'nadane', data: dzis }] }));
    return nowa;
  }

  /** Możliwe kolejne stany dla przesyłki — do wyrenderowania przycisków symulacji w UI. */
  mozliweKolejneStatusy(id: string): StatusPrzesylki[] {
    const obecna = this._przesylki().find((p) => p.id === id);
    return obecna ? mozliweKolejneStatusy(obecna.status, obecna.kanal, obecna.kierunek) : [];
  }

  /** Mockowa symulacja aktualizacji statusu — przesuwa przesyłkę do wskazanego stanu w automacie. */
  symulujAktualizacjeStatusu(id: string, docelowyStatus: StatusPrzesylki): StatusPrzesylki | null {
    const obecna = this._przesylki().find((p) => p.id === id);
    if (!obecna) {
      return null;
    }
    const dozwolone = mozliweKolejneStatusy(obecna.status, obecna.kanal, obecna.kierunek);
    if (!dozwolone.includes(docelowyStatus)) {
      return null;
    }
    const nowyStatus = docelowyStatus;
    const dzis = nowaData();
    this._przesylki.update((lista) =>
      lista.map((p) => (p.id === id ? { ...p, status: nowyStatus, dataStatusu: dzis } : p)),
    );
    this._historia.update((mapa) => ({
      ...mapa,
      [id]: [...(mapa[id] ?? []), { status: nowyStatus, data: dzis }],
    }));
    return nowyStatus;
  }
}

function nowaData(): string {
  return new Date().toISOString().slice(0, 10);
}

function generujNumerReferencyjny(kanal: KanalWysylki): string {
  const rok = new Date().getFullYear();
  const numer = String(licznikNumerow++).padStart(5, '0');
  const prefiks = kanal === 'e-doreczenia' ? 'ADE' : kanal === 'operator_pocztowy' ? 'PL' : 'MAIL';
  return `${prefiks}/${rok}/${numer}`;
}
