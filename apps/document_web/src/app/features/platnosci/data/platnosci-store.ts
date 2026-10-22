import { Injectable, computed, signal } from '@angular/core';
import { Naleznosc, Wplata } from '../../../core/models';
import { MOCK_NALEZNOSCI, MOCK_WPLATY } from '../../../core/mock/mock-data';

export interface WynikImportuWyciagu {
  nazwaPliku: string;
  liczbaTransakcji: number;
  dopasowaneAutomatycznie: number;
  wymagajaRekoncyliacji: number;
  transakcje: Wplata[];
}

/**
 * Lokalny magazyn stanu modułu Płatności (mock). Trzyma rejestr należności i wpłat
 * jako sygnały, żeby rekoncyliacja ręczna i import wyciągu mogły aktualizować dane
 * widoczne w całym module bez backendu.
 */
@Injectable({ providedIn: 'root' })
export class PlatnosciStore {
  private readonly naleznosciSygnal = signal<Naleznosc[]>([...MOCK_NALEZNOSCI]);
  private readonly wplatySygnal = signal<Wplata[]>([...MOCK_WPLATY]);

  readonly naleznosci = this.naleznosciSygnal.asReadonly();
  readonly wplaty = this.wplatySygnal.asReadonly();

  readonly naleznosciNierozliczone = computed(() =>
    this.naleznosciSygnal().filter(
      (n) => n.status === 'nierozliczona' || n.status === 'czesciowo_rozliczona',
    ),
  );

  naleznosc(id: string): Naleznosc | undefined {
    return this.naleznosciSygnal().find((n) => n.id === id);
  }

  wplatyDlaNaleznosci(naleznoscId: string): Wplata[] {
    return this.wplatySygnal().filter((w) => w.naleznoscId === naleznoscId);
  }

  /** Ręczne dopasowanie wpłaty do należności — aktualizuje kwotę rozliczoną i status. */
  dopasujRecznie(wplataId: string, naleznoscId: string): void {
    const wplata = this.wplatySygnal().find((w) => w.id === wplataId);
    if (!wplata) {
      return;
    }
    this.wplatySygnal.update((lista) =>
      lista.map((w) => (w.id === wplataId ? { ...w, naleznoscId } : w)),
    );
    this.naleznosciSygnal.update((lista) =>
      lista.map((n) => {
        if (n.id !== naleznoscId) {
          return n;
        }
        const kwotaRozliczona = Math.min(n.kwota, n.kwotaRozliczona + wplata.kwota);
        return {
          ...n,
          kwotaRozliczona,
          status: kwotaRozliczona >= n.kwota ? 'rozliczona' : 'czesciowo_rozliczona',
        };
      }),
    );
  }

  /** Mock importu wyciągu CAMT.053 — generuje przykładowe transakcje bez realnego parsowania XML. */
  importujWyciag(nazwaPliku: string): WynikImportuWyciagu {
    const nierozliczone = this.naleznosciNierozliczone();
    const idPrefiks = `imp-${Date.now()}`;
    const transakcje: Wplata[] = [
      {
        id: `${idPrefiks}-1`,
        kwota: nierozliczone[0]?.kwota ?? 150,
        data: new Date().toISOString().slice(0, 10),
        tytul: nierozliczone[0]?.tytul ?? 'Opłata — przelew przychodzący',
        kontrahent: 'Kontrahent z wyciągu 1',
        naleznoscId: nierozliczone[0]?.id,
        dopasowanaAutomatycznie: !!nierozliczone[0],
      },
      {
        id: `${idPrefiks}-2`,
        kwota: 320,
        data: new Date().toISOString().slice(0, 10),
        tytul: 'Przelew przychodzący — bez tytułu referencyjnego',
        kontrahent: 'Kontrahent z wyciągu 2',
        dopasowanaAutomatycznie: false,
      },
      {
        id: `${idPrefiks}-3`,
        kwota: nierozliczone[1]?.kwota ?? 75,
        data: new Date().toISOString().slice(0, 10),
        tytul: nierozliczone[1]?.tytul ?? 'Opłata administracyjna',
        kontrahent: 'Kontrahent z wyciągu 3',
        naleznoscId: nierozliczone[1]?.id,
        dopasowanaAutomatycznie: !!nierozliczone[1],
      },
    ];

    this.wplatySygnal.update((lista) => [...lista, ...transakcje]);
    transakcje
      .filter((t) => t.naleznoscId)
      .forEach((t) => {
        this.naleznosciSygnal.update((lista) =>
          lista.map((n) => {
            if (n.id !== t.naleznoscId) {
              return n;
            }
            const kwotaRozliczona = Math.min(n.kwota, n.kwotaRozliczona + t.kwota);
            return {
              ...n,
              kwotaRozliczona,
              status: kwotaRozliczona >= n.kwota ? 'rozliczona' : 'czesciowo_rozliczona',
            };
          }),
        );
      });

    const dopasowane = transakcje.filter((t) => t.dopasowanaAutomatycznie).length;
    return {
      nazwaPliku,
      liczbaTransakcji: transakcje.length,
      dopasowaneAutomatycznie: dopasowane,
      wymagajaRekoncyliacji: transakcje.length - dopasowane,
      transakcje,
    };
  }
}
