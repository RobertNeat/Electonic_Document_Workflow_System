import { Injectable } from '@angular/core';
import { obywatelWyswietlanaNazwa } from '../../core/models';
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
  MOCK_OBYWATELE,
  MOCK_PRZESYLKI,
  MOCK_SPRAWY,
  MOCK_TYPY_DOKUMENTOW,
  MOCK_WERSJE_DOKUMENTOW,
  MOCK_WPLATY,
} from '../../core/mock/mock-data';

/**
 * Jedyne miejsce w module Obywatele, które odwołuje się do danych mockowych.
 * Docelowo metody te zostaną podmienione na wywołania HTTP bez zmiany API serwisu.
 */
@Injectable({ providedIn: 'root' })
export class ObywateleDaneService {
  szukaj(zapytanie: string): Obywatel[] {
    const q = zapytanie.trim().toLowerCase();
    if (!q) {
      return MOCK_OBYWATELE;
    }
    return MOCK_OBYWATELE.filter((obywatel) => {
      const nazwa = obywatelWyswietlanaNazwa(obywatel).toLowerCase();
      const pesel = obywatel.pesel?.toLowerCase() ?? '';
      const nip = obywatel.nip?.toLowerCase() ?? '';
      return nazwa.includes(q) || pesel.includes(q) || nip.includes(q);
    });
  }

  znajdz(id: string): Obywatel | undefined {
    return MOCK_OBYWATELE.find((o) => o.id === id);
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
