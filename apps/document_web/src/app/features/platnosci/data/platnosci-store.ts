import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import type {
  NaleznoscListElement,
  WynikImportuCamt053,
  Wplata,
} from '@electronic-document-workflow/document-shared-dto';
import {
  PlatnosciApi,
  SzukajNaleznosciFiltry,
  SzukajWplatFiltry,
} from '../../../core/api/platnosci.api';
import { komunikatBleduHttp } from '../../../core/api/http-error';

/** Wynik importu wyciągu wraz z nazwą zaimportowanego pliku, do wyświetlenia w UI. */
export interface WynikImportuWyciaguZNazwa {
  nazwaPliku: string;
  wynik: WynikImportuCamt053;
}

/**
 * Przechowuje stan LIST modułu Płatności (rejestr należności, rejestr wpłat) - woła realne
 * `document_api` (PlatnosciApi), bez lokalnej persystencji/rekoncyliacji. Stan widoku szczegółów
 * jednej należności ŻYJE W KOMPONENCIE (NaleznoscSzczegoly), nie tutaj - analogicznie do
 * SprawyStore/SprawaSzczegoly i KorespondencjaStore/KorespondencjaSzczegoly, żeby singleton store
 * nie pokazywał przez chwilę nieaktualnej należności po przejściu do innych szczegółów.
 */
@Injectable({ providedIn: 'root' })
export class PlatnosciStore {
  private readonly api = inject(PlatnosciApi);

  private readonly stanNaleznosci = signal<NaleznoscListElement[]>([]);
  private readonly stanWplaty = signal<Wplata[]>([]);
  private readonly stanLoading = signal(false);
  private readonly stanBlad = signal<string | null>(null);

  readonly naleznosci = this.stanNaleznosci.asReadonly();
  readonly wplaty = this.stanWplaty.asReadonly();
  readonly loading = this.stanLoading.asReadonly();
  readonly blad = this.stanBlad.asReadonly();

  /** Odświeża rejestr należności z danym filtrem (GET /api/naleznosci). */
  async szukajNaleznosci(filtry: SzukajNaleznosciFiltry = {}): Promise<void> {
    this.stanLoading.set(true);
    this.stanBlad.set(null);
    try {
      const wynik = await firstValueFrom(this.api.naleznosci(filtry));
      this.stanNaleznosci.set(wynik);
    } catch (err) {
      this.stanBlad.set(komunikatBleduHttp(err, 'Nie udało się wczytać rejestru należności.'));
    } finally {
      this.stanLoading.set(false);
    }
  }

  /** Odświeża rejestr wpłat z danym filtrem (GET /api/wplaty), domyślnie pełna lista. */
  async szukajWplat(filtry: SzukajWplatFiltry = {}): Promise<void> {
    this.stanLoading.set(true);
    this.stanBlad.set(null);
    try {
      const wynik = await firstValueFrom(this.api.wplaty(filtry));
      this.stanWplaty.set(wynik);
    } catch (err) {
      this.stanBlad.set(komunikatBleduHttp(err, 'Nie udało się wczytać rejestru wpłat.'));
    } finally {
      this.stanLoading.set(false);
    }
  }

  /** Ręczne dopasowanie wpłaty do należności (POST /api/wplaty/:id/dopasuj) - przeliczenie
   * kwotaRozliczona/status wykonuje backend (PlatnosciService.przeliczRozliczenie). Zwraca
   * zaktualizowaną wpłatę przy sukcesie, komunikat błędu (string) przy niepowodzeniu - bez
   * pisania do sygnału `blad` tego store'a (wzorzec identyczny do KorespondencjaStore.nadaj).
   * Nie odświeża tu żadnej listy - strona wywołująca decyduje, co i kiedy przeładować. */
  async dopasujWplate(wplataId: string, naleznoscId: string): Promise<Wplata | string> {
    try {
      return await firstValueFrom(this.api.dopasujWplate(wplataId, naleznoscId));
    } catch (err) {
      return komunikatBleduHttp(err, 'Nie udało się dopasować wpłaty do należności.');
    }
  }

  /** Import wyciągu CAMT.053 (POST /api/import-camt053) - backend realnie parsuje XML i zwraca
   * faktyczne liczby dopasowanych/do rekoncyliacji transakcji. Zwraca wynik wraz z nazwą pliku
   * przy sukcesie, komunikat błędu (string) przy niepowodzeniu (np. plik nie jest XML-em). */
  async importujWyciag(plik: File): Promise<WynikImportuWyciaguZNazwa | string> {
    try {
      const wynik = await firstValueFrom(this.api.importujCamt053(plik));
      return { nazwaPliku: plik.name, wynik };
    } catch (err) {
      return komunikatBleduHttp(err, 'Nie udało się zaimportować wyciągu CAMT.053.');
    }
  }
}
