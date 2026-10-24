import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import type {
  Przesylka,
  UtworzPrzesylkeRequest,
} from '@electronic-document-workflow/document-shared-dto';
import { PrzesylkiApi, SzukajPrzesylekFiltry } from '../../core/api/przesylki.api';
import { komunikatBleduHttp } from '../../core/api/http-error';

/**
 * Przechowuje stan LISTY przesyłek (filtry rejestru) - woła realne API `document_api`
 * (PrzesylkiApi), bez lokalnej persystencji. Stan widoku szczegółów jednej przesyłki (w tym
 * polling statusu) ŻYJE W KOMPONENCIE (KorespondencjaSzczegoly), nie tutaj - analogicznie do
 * SprawyStore/SprawaSzczegoly, żeby singleton store nie pokazywał przez chwilę nieaktualnej
 * przesyłki po przejściu do innych szczegółów. Status przesyłki aktualizuje się asynchronicznie
 * zdarzeniem RabbitMQ z delivery_mock (PrzesylkaStatusuConsumer w document_api) - nie ma REST
 * endpointu do wyzwolenia zmiany statusu z frontendu, więc ten store go nie wystawia.
 */
@Injectable({ providedIn: 'root' })
export class KorespondencjaStore {
  private readonly api = inject(PrzesylkiApi);

  private readonly stanPrzesylki = signal<Przesylka[]>([]);
  private readonly stanLoading = signal(false);
  private readonly stanBlad = signal<string | null>(null);

  readonly przesylki = this.stanPrzesylki.asReadonly();
  readonly loading = this.stanLoading.asReadonly();
  readonly blad = this.stanBlad.asReadonly();

  /** Odświeża listę przesyłek z danymi filtrami (GET /api/przesylki). */
  async szukaj(filtry: SzukajPrzesylekFiltry = {}): Promise<void> {
    this.stanLoading.set(true);
    this.stanBlad.set(null);
    try {
      const wynik = await firstValueFrom(this.api.szukaj(filtry));
      this.stanPrzesylki.set(wynik);
    } catch (err) {
      this.stanBlad.set(komunikatBleduHttp(err, 'Nie udało się wczytać rejestru korespondencji.'));
    } finally {
      this.stanLoading.set(false);
    }
  }

  /** Nadaje nowe pismo (POST /api/przesylki) - numerReferencyjny jest nadawany przez backend.
   * Zwraca utworzoną przesyłkę przy sukcesie, komunikat błędu (string) przy niepowodzeniu - bez
   * pisania do sygnału `blad` tego store'a, żeby komunikat formularza nadania nie "przeciekł"
   * później na rejestr (wzorzec identyczny do SprawyStore.dodajSprawe). */
  async nadaj(request: UtworzPrzesylkeRequest): Promise<Przesylka | string> {
    try {
      return await firstValueFrom(this.api.nadaj(request));
    } catch (err) {
      return komunikatBleduHttp(err, 'Nie udało się nadać pisma.');
    }
  }
}
