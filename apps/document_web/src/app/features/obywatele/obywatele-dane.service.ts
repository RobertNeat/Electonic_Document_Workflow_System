import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import type {
  DokumentObywatelaListElement,
  Naleznosc,
  Obywatel,
  Przesylka,
  Sprawa,
  Wplata,
} from '@electronic-document-workflow/document-shared-dto';
import {
  MOCK_DOKUMENTY,
  MOCK_NALEZNOSCI,
  MOCK_PRZESYLKI,
  MOCK_SPRAWY,
  MOCK_TYPY_DOKUMENTOW,
  MOCK_WERSJE_DOKUMENTOW,
  MOCK_WPLATY,
} from '../../core/mock/mock-data';
import { ObywateleApi } from '../../core/api/obywatele.api';

/**
 * `szukaj`/`znajdz` wołają realne `document_api` (GET /api/obywatele, GET /api/obywatele/:id)
 * przez ObywateleApi. Pozostałe metody (sprawy/dokumenty/przesylki/naleznosci/wplaty) wciąż
 * czytają dane mockowe - ich backendowe filtry (obywatelId) nie są jeszcze gotowe/podłączone,
 * zob. docs/development_docs/currentwork/plan.md.
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

  przesylki(obywatelId: string): Przesylka[] {
    return MOCK_PRZESYLKI.filter((p) => p.obywatelId === obywatelId);
  }

  naleznosci(obywatelId: string): Naleznosc[] {
    return MOCK_NALEZNOSCI.filter((n) => n.obywatelId === obywatelId);
  }

  wplaty(obywatelId: string): Wplata[] {
    const naleznosciIds = new Set(this.naleznosci(obywatelId).map((n) => n.id));
    return MOCK_WPLATY.filter((w) => w.naleznoscId && naleznosciIds.has(w.naleznoscId));
  }
}
