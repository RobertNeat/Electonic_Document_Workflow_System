import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { MessageModule } from 'primeng/message';
import { SelectModule } from 'primeng/select';
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
  imports: [
    TableModule,
    ButtonModule,
    DialogModule,
    MessageModule,
    SelectModule,
    FileUploadModule,
    FormsModule,
  ],
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
  protected readonly uploadTypDokumentuId = signal<string | null>(null);
  protected readonly uploadBlad = signal<string | null>(null);
  protected readonly uploadWTrakcie = signal(false);

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
    this.uploadBlad.set(null);
    this.uploadTypDokumentuId.set(this.typyDokumentow()[0]?.id ?? null);
    this.uploadWidoczny.set(true);
  }

  // Wgrywa sekwencyjnie każdy plik .jrxml przez POST /api/szablony (proxy document_api ->
  // dms_service). Błąd nie zamyka dialogu - użytkownik widzi komunikat i może spróbować ponownie.
  protected wgraj(event: FileUploadHandlerEvent): void {
    const typDokumentuId = this.uploadTypDokumentuId();
    if (!typDokumentuId) {
      this.uploadBlad.set('Wybierz typ dokumentu przed wgraniem szablonu.');
      return;
    }

    const pliki = event.files.filter((plik) => plik.name.toLowerCase().endsWith('.jrxml'));
    if (pliki.length === 0) {
      this.uploadBlad.set('Wybrany plik musi mieć rozszerzenie .jrxml.');
      return;
    }

    this.uploadBlad.set(null);
    this.uploadWTrakcie.set(true);
    this.wgrajPoKolei(typDokumentuId, pliki, 0);
  }

  private wgrajPoKolei(typDokumentuId: string, pliki: File[], indeks: number): void {
    if (indeks >= pliki.length) {
      this.uploadWTrakcie.set(false);
      this.uploadWidoczny.set(false);
      return;
    }

    const plik = pliki[indeks];
    this.api
      .wgrajSzablon(typDokumentuId, plik.name, plik)
      .pipe(
        catchError((err: unknown) => {
          this.uploadBlad.set(komunikatBleduHttp(err, 'Nie udało się wgrać szablonu.'));
          return of(null as SzablonJasper | null);
        }),
      )
      .subscribe((wynik) => {
        if (wynik === null) {
          this.uploadWTrakcie.set(false);
          return;
        }
        this.szablony.update((lista) => [wynik, ...lista]);
        this.wgrajPoKolei(typDokumentuId, pliki, indeks + 1);
      });
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
