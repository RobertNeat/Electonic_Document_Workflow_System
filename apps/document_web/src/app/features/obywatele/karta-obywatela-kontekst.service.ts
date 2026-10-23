import { Injectable, signal } from '@angular/core';
import type { Obywatel } from '@electronic-document-workflow/document-shared-dto';

/**
 * Lokalny kontekst karty obywatela (udostępniany przez ObywatelKarta w providers).
 * Pozwala pod-stronom (sprawy/dokumenty/korespondencja/rozrachunki) odczytać
 * aktualnie wyświetlanego obywatela bez ponownego parsowania parametru trasy
 * ani duplikowania zapytania o dane.
 */
@Injectable()
export class KartaObywatelaKontekst {
  private readonly _obywatel = signal<Obywatel | null>(null);
  private readonly _wczytano = signal(false);

  readonly obywatel = this._obywatel.asReadonly();
  readonly wczytano = this._wczytano.asReadonly();

  ustaw(obywatel: Obywatel | null): void {
    this._obywatel.set(obywatel);
    this._wczytano.set(true);
  }
}
