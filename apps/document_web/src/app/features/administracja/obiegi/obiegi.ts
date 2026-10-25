import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { TagModule } from 'primeng/tag';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { MessageModule } from 'primeng/message';
import { catchError, of } from 'rxjs';
import type { DefinicjaObiegu } from '@electronic-document-workflow/document-shared-dto';
import { SprawyApi } from '../../../core/api/sprawy.api';
import { AuthService } from '../../../core/services/auth.service';
import { komunikatBleduHttp } from '../../../core/api/http-error';

@Component({
  selector: 'app-obiegi',
  standalone: true,
  imports: [
    FormsModule,
    ButtonModule,
    DialogModule,
    TagModule,
    InputTextModule,
    TextareaModule,
    MessageModule,
  ],
  templateUrl: './obiegi.html',
  styleUrl: './obiegi.css',
})
export class Obiegi {
  private readonly api = inject(SprawyApi);
  private readonly auth = inject(AuthService);

  // Edycja wymaga roli administrator po stronie backendu - przycisk "Edytuj" jest ukrywany
  // analogicznie dla roli innej niż administrator, żeby nie prowokować 403 z UI.
  readonly czyAdministrator = computed(() =>
    (this.auth.uzytkownik()?.role ?? []).includes('administrator'),
  );

  readonly definicje = signal<DefinicjaObiegu[]>([]);
  readonly wczytywanie = signal(true);
  readonly komunikatBledu = signal<string | null>(null);

  readonly widocznySzczegol = signal(false);
  readonly widocznaEdycja = signal(false);
  readonly wybranaDefinicja = signal<DefinicjaObiegu | null>(null);
  readonly formularzEdycji = signal<{ nazwa: string; opis: string }>({ nazwa: '', opis: '' });
  readonly edycjaBlad = signal<string | null>(null);
  readonly edycjaWTrakcie = signal(false);

  constructor() {
    this.wczytywanie.set(true);
    this.api
      .definicjeObiegow()
      .pipe(
        catchError((err: unknown) => {
          this.komunikatBledu.set(
            komunikatBleduHttp(err, 'Nie udało się wczytać definicji obiegów.'),
          );
          return of(null as DefinicjaObiegu[] | null);
        }),
      )
      .subscribe((wynik) => {
        this.wczytywanie.set(false);
        if (wynik !== null) {
          this.definicje.set(wynik);
        }
      });
  }

  pokazSzczegoly(definicja: DefinicjaObiegu): void {
    this.wybranaDefinicja.set(definicja);
    this.widocznySzczegol.set(true);
  }

  otworzEdycje(definicja: DefinicjaObiegu): void {
    this.wybranaDefinicja.set(definicja);
    this.formularzEdycji.set({ nazwa: definicja.nazwa, opis: definicja.opis });
    this.edycjaBlad.set(null);
    this.widocznaEdycja.set(true);
  }

  zapiszEdycje(): void {
    const definicja = this.wybranaDefinicja();
    if (!definicja) {
      return;
    }
    const { nazwa, opis } = this.formularzEdycji();
    this.edycjaBlad.set(null);
    this.edycjaWTrakcie.set(true);
    this.api
      .edytujDefinicjeObiegu(definicja.id, { nazwa, opis })
      .pipe(
        catchError((err: unknown) => {
          this.edycjaBlad.set(komunikatBleduHttp(err, 'Nie udało się zapisać definicji obiegu.'));
          return of(null as DefinicjaObiegu | null);
        }),
      )
      .subscribe((wynik) => {
        this.edycjaWTrakcie.set(false);
        if (wynik === null) {
          return;
        }
        this.definicje.update((lista) => lista.map((d) => (d.id === wynik.id ? wynik : d)));
        this.widocznaEdycja.set(false);
      });
  }
}
