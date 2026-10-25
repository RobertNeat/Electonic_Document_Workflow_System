import { Component, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { Router } from '@angular/router';
import { FileUploadModule, FileUploadHandlerEvent } from 'primeng/fileupload';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { TableModule } from 'primeng/table';
import { CardModule } from 'primeng/card';
import { catchError, of } from 'rxjs';
import type { WynikWalidacjiUbl } from '@electronic-document-workflow/document-shared-dto';
import { WymianaApi } from '../../../core/api/wymiana.api';
import { komunikatBleduHttp } from '../../../core/api/http-error';

@Component({
  selector: 'app-import-ubl',
  imports: [FileUploadModule, ButtonModule, MessageModule, TableModule, CardModule, DecimalPipe],
  templateUrl: './import-ubl.html',
  styleUrl: './import-ubl.css',
})
export class ImportUbl {
  private readonly api = inject(WymianaApi);
  private readonly router = inject(Router);

  protected readonly nazwaPliku = signal<string | null>(null);
  protected readonly wTrakcie = signal(false);
  protected readonly wynik = signal<WynikWalidacjiUbl | null>(null);
  protected readonly komunikatBledu = signal<string | null>(null);

  protected onUpload(event: FileUploadHandlerEvent): void {
    const plik = event.files[0];
    if (!plik) {
      return;
    }
    this.nazwaPliku.set(plik.name);
    this.wynik.set(null);
    this.komunikatBledu.set(null);
    this.wTrakcie.set(true);

    this.api
      .importujUbl(plik)
      .pipe(
        catchError((err: unknown) => {
          this.komunikatBledu.set(komunikatBleduHttp(err, 'Nie udało się zaimportować pliku.'));
          return of(null as WynikWalidacjiUbl | null);
        }),
      )
      .subscribe((wynik) => {
        this.wTrakcie.set(false);
        if (wynik !== null) {
          this.wynik.set(wynik);
        }
      });
  }

  protected wyczysc(): void {
    this.nazwaPliku.set(null);
    this.wynik.set(null);
    this.komunikatBledu.set(null);
  }

  protected wrocDoPrzegladu(): void {
    this.router.navigate(['/wymiana']);
  }
}
