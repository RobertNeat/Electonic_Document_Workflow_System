import { Component, inject, signal } from '@angular/core';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { MessageModule } from 'primeng/message';
import { FileUploadModule, FileUploadHandlerEvent } from 'primeng/fileupload';
import { catchError, of } from 'rxjs';
import type {
  SzablonJasper,
  TypDokumentu,
} from '@electronic-document-workflow/document-shared-dto';
import { DokumentyApi } from '../../../core/api/dokumenty.api';
import { SprawyApi } from '../../../core/api/sprawy.api';
import { komunikatBleduHttp } from '../../../core/api/http-error';

@Component({
  selector: 'app-szablony-strona',
  imports: [TableModule, ButtonModule, DialogModule, MessageModule, FileUploadModule],
  templateUrl: './szablony-strona.html',
  styleUrl: './szablony-strona.css',
})
export class SzablonyStrona {
  private readonly api = inject(DokumentyApi);
  private readonly sprawyApi = inject(SprawyApi);

  protected readonly szablony = signal<SzablonJasper[]>([]);
  protected readonly wczytywanie = signal(true);
  protected readonly komunikatBledu = signal<string | null>(null);

  protected readonly typyDokumentow = signal<TypDokumentu[]>([]);

  protected readonly uploadWidoczny = signal(false);

  protected readonly historiaWidoczna = signal(false);
  protected readonly historiaWczytywanie = signal(false);
  protected readonly historiaBlad = signal<string | null>(null);
  protected readonly historiaWybranego = signal<SzablonJasper[]>([]);
  protected readonly wybranySzablon = signal<SzablonJasper | null>(null);

  constructor() {
    this.sprawyApi
      .typyDokumentow()
      .pipe(catchError(() => of([] as TypDokumentu[])))
      .subscribe((typy) => this.typyDokumentow.set(typy));

    this.wczytywanie.set(true);
    this.api
      .szablony()
      .pipe(
        catchError((err: unknown) => {
          this.komunikatBledu.set(komunikatBleduHttp(err, 'Nie udało się wczytać szablonów.'));
          return of(null as SzablonJasper[] | null);
        }),
      )
      .subscribe((wynik) => {
        this.wczytywanie.set(false);
        if (wynik !== null) {
          this.szablony.set(wynik);
        }
      });
  }

  protected nazwaTypu(typDokumentuId: string): string {
    return this.typyDokumentow().find((t) => t.id === typDokumentuId)?.nazwa ?? '—';
  }

  protected otworzUpload(): void {
    this.uploadWidoczny.set(true);
  }

  // mock wgrywania — POST /api/szablony nie istnieje jeszcze (zob. plan.md); walidacja
  // rozszerzenia .jrxml i dopisanie wpisu lokalnie, bez realnego zapisu na backendzie.
  protected wgraj(event: FileUploadHandlerEvent): void {
    const pliki = event.files;
    for (const plik of pliki) {
      if (!plik.name.toLowerCase().endsWith('.jrxml')) {
        continue;
      }
      const nowy: SzablonJasper = {
        id: `sz-${Date.now()}`,
        typDokumentuId: this.typyDokumentow()[0]?.id ?? '',
        nazwa: plik.name,
        wersja: 1,
        dataWgrania: new Date().toISOString().slice(0, 10),
        autor: 'Bieżący użytkownik',
      };
      this.szablony.update((lista) => [nowy, ...lista]);
    }
    this.uploadWidoczny.set(false);
  }

  protected pokazHistorie(szablon: SzablonJasper): void {
    this.wybranySzablon.set(szablon);
    this.historiaWybranego.set([]);
    this.historiaBlad.set(null);
    this.historiaWczytywanie.set(true);
    this.historiaWidoczna.set(true);

    this.api
      .historiaSzablonu(szablon.id)
      .pipe(
        catchError((err: unknown) => {
          this.historiaBlad.set(
            komunikatBleduHttp(err, 'Nie udało się wczytać historii wersji szablonu.'),
          );
          return of(null as SzablonJasper[] | null);
        }),
      )
      .subscribe((wynik) => {
        // szablon mógł zmienić się w międzyczasie (szybkie kolejne kliknięcia) - odpowiedź jest
        // już nieaktualna, nie nadpisujemy stanu dialogu dla innego szablonu.
        if (this.wybranySzablon()?.id !== szablon.id) {
          return;
        }
        this.historiaWczytywanie.set(false);
        if (wynik !== null) {
          this.historiaWybranego.set(wynik);
        }
      });
  }
}
