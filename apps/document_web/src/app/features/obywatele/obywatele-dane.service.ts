import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import type {
  DokumentObywatelaListElement,
  Obywatel,
  Sprawa,
} from '@electronic-document-workflow/document-shared-dto';
import {
  MOCK_DOKUMENTY,
  MOCK_SPRAWY,
  MOCK_TYPY_DOKUMENTOW,
  MOCK_WERSJE_DOKUMENTOW,
} from '../../core/mock/mock-data';
import { ObywateleApi } from '../../core/api/obywatele.api';

/**
 * `szukaj`/`znajdz` wołają realne `document_api` (GET /api/obywatele, GET /api/obywatele/:id)
 * przez ObywateleApi. `sprawy()` czyta mocka i służy wyłącznie jako pomocnicza dla `dokumenty()`
 * (złączenie sprawy→dokumenty po stronie klienta) - zakładka Sprawy woła SprawyApi bezpośrednio,
 * zakładka Korespondencja woła PrzesylkiApi bezpośrednio, a zakładka Rozrachunki woła
 * PlatnosciApi bezpośrednio (wszystkie trzy z pominięciem tego serwisu). Pozostała metoda
 * (dokumenty) wciąż czyta dane mockowe - jej backendowy odpowiednik (agregacja dokumentów
 * obywatela) jeszcze nie istnieje, zob. docs/development_docs/currentwork/plan.md.
 */
@Injectable({ providedIn: 'root' })
export class ObywateleDaneService {
  private readonly api = inject(ObywateleApi);

  szukaj(zapytanie: string): Observable<Obywatel[]> {
    return this.api.szukaj(zapytanie);
  }

  /** Przekazuje błąd (w tym 404) wołającemu bez tłumienia - ObywatelKarta rozróżnia
   * "nie znaleziono" (404) od błędu sieci/CORS, żeby pokazać właściwy komunikat. */
  znajdz(id: string): Observable<Obywatel> {
    return this.api.getById(id);
  }

  sprawy(obywatelId: string): Sprawa[] {
    return MOCK_SPRAWY.filter((s) => s.obywatelId === obywatelId);
  }

  dokumenty(obywatelId: string): DokumentObywatelaListElement[] {
    const sprawyIds = new Set(this.sprawy(obywatelId).map((s) => s.id));
    return MOCK_DOKUMENTY.filter((d) => sprawyIds.has(d.sprawaId)).map((d) => {
      const sprawa = MOCK_SPRAWY.find((s) => s.id === d.sprawaId);
      const typ = MOCK_TYPY_DOKUMENTOW.find((t) => t.id === d.typDokumentuId);
      const wersje = MOCK_WERSJE_DOKUMENTOW.filter((w) => w.dokumentId === d.id);
      const najnowsza = wersje.find((w) => w.wersja === d.aktualnaWersja);
      return {
        ...d,
        typDokumentuNazwa: typ?.nazwa ?? 'Nieznany typ',
        znakSprawy: sprawa?.znakSprawy ?? '—',
        statusPodpisuAktualnej: najnowsza?.statusPodpisu ?? 'brak',
      };
    });
  }
}
