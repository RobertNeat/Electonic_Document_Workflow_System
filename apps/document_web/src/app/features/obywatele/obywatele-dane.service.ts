import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import type { Obywatel } from '@electronic-document-workflow/document-shared-dto';
import { ObywateleApi } from '../../core/api/obywatele.api';

/**
 * Woła realne `document_api` (GET /api/obywatele, GET /api/obywatele/:id) przez `ObywateleApi`.
 * Wszystkie zakładki karty obywatela (Sprawy, Korespondencja, Rozrachunki, Dokumenty) wołają
 * swoje dedykowane API (`SprawyApi`/`PrzesylkiApi`/`PlatnosciApi`/`DokumentyApi`) bezpośrednio,
 * z pominięciem tego serwisu - `szukaj`/`znajdz` są jedynymi metodami tej klasy.
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
}
