import { Injectable, signal } from '@angular/core';
import type { Obywatel } from '@electronic-document-workflow/document-shared-dto';

const STORAGE_KEY = 'document_web.przypiety_obywatel';

@Injectable({ providedIn: 'root' })
export class CitizenContextService {
  private readonly przypietyObywatel = signal<Obywatel | null>(this.odczytajZSesji());

  readonly obywatel = this.przypietyObywatel.asReadonly();

  przypnij(obywatel: Obywatel): void {
    this.przypietyObywatel.set(obywatel);
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(obywatel));
  }

  odepnij(): void {
    this.przypietyObywatel.set(null);
    sessionStorage.removeItem(STORAGE_KEY);
  }

  private odczytajZSesji(): Obywatel | null {
    const zapisany = sessionStorage.getItem(STORAGE_KEY);
    return zapisany ? (JSON.parse(zapisany) as Obywatel) : null;
  }
}
