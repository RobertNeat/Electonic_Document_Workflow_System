import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { TagModule } from 'primeng/tag';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { DefinicjaObiegu } from '../../../core/models';
import { MOCK_DEFINICJE_OBIEGOW } from '../../../core/mock/mock-data';

@Component({
  selector: 'app-obiegi',
  standalone: true,
  imports: [FormsModule, ButtonModule, DialogModule, TagModule, InputTextModule, TextareaModule],
  templateUrl: './obiegi.html',
  styleUrl: './obiegi.css',
})
export class Obiegi {
  readonly definicje = signal<DefinicjaObiegu[]>(MOCK_DEFINICJE_OBIEGOW);

  readonly widocznySzczegol = signal(false);
  readonly widocznaEdycja = signal(false);
  readonly wybranaDefinicja = signal<DefinicjaObiegu | null>(null);
  readonly formularzEdycji = signal<{ nazwa: string; opis: string }>({ nazwa: '', opis: '' });

  pokazSzczegoly(definicja: DefinicjaObiegu): void {
    this.wybranaDefinicja.set(definicja);
    this.widocznySzczegol.set(true);
  }

  otworzEdycje(definicja: DefinicjaObiegu): void {
    this.wybranaDefinicja.set(definicja);
    this.formularzEdycji.set({ nazwa: definicja.nazwa, opis: definicja.opis });
    this.widocznaEdycja.set(true);
  }

  zapiszEdycje(): void {
    const definicja = this.wybranaDefinicja();
    if (!definicja) {
      return;
    }
    const { nazwa, opis } = this.formularzEdycji();
    this.definicje.update((lista) =>
      lista.map((d) => (d.id === definicja.id ? { ...d, nazwa, opis } : d)),
    );
    this.widocznaEdycja.set(false);
  }
}
