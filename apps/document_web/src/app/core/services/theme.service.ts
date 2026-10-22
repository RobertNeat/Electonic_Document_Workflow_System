import { Injectable, signal } from '@angular/core';

const STORAGE_KEY = 'document_web.tryb_ciemny';
const DARK_CLASS = 'app-dark';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly trybCiemnyAktywny = signal<boolean>(this.odczytajPreferencje());

  readonly trybCiemny = this.trybCiemnyAktywny.asReadonly();

  constructor() {
    this.zastosuj(this.trybCiemnyAktywny());
  }

  przelacz(): void {
    const nowyStan = !this.trybCiemnyAktywny();
    this.trybCiemnyAktywny.set(nowyStan);
    localStorage.setItem(STORAGE_KEY, String(nowyStan));
    this.zastosuj(nowyStan);
  }

  private zastosuj(wlaczony: boolean): void {
    document.documentElement.classList.toggle(DARK_CLASS, wlaczony);
  }

  private odczytajPreferencje(): boolean {
    const zapisany = localStorage.getItem(STORAGE_KEY);
    if (zapisany !== null) {
      return zapisany === 'true';
    }
    return window.matchMedia?.('(prefers-color-scheme: dark)').matches ?? false;
  }
}
