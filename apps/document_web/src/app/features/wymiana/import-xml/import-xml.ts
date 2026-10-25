import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { FileUploadModule, FileUploadHandlerEvent } from 'primeng/fileupload';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { TableModule } from 'primeng/table';
import { CardModule } from 'primeng/card';
import { catchError, of } from 'rxjs';
import type { Sprawa, WynikWalidacjiXml } from '@electronic-document-workflow/document-shared-dto';
import { WymianaApi } from '../../../core/api/wymiana.api';
import { komunikatBleduHttp } from '../../../core/api/http-error';

interface WierszMapowania {
  pole: string;
  wartosc: string;
}

// Etykiety pól Sprawa zmapowanych z komunikatu XML - tylko pola z sensownym opisem biznesowym są
// pokazywane, resztę (np. id, zamknieta) pomijamy, żeby podgląd mapowania pozostał czytelny.
const ETYKIETY_POL_SPRAWY: Partial<Record<keyof Sprawa, string>> = {
  znakSprawy: 'Znak sprawy',
  tytul: 'Tytuł sprawy',
  jrwa: 'Symbol JRWA',
  wydzial: 'Wydział',
  referent: 'Referent',
  dataWszczecia: 'Data wszczęcia',
  terminUstawowy: 'Termin ustawowy',
  kategoriaArchiwalna: 'Kategoria archiwalna',
  stan: 'Stan',
};

@Component({
  selector: 'app-import-xml',
  imports: [FileUploadModule, ButtonModule, MessageModule, TableModule, CardModule],
  templateUrl: './import-xml.html',
  styleUrl: './import-xml.css',
})
export class ImportXml {
  private readonly api = inject(WymianaApi);
  private readonly router = inject(Router);

  protected readonly nazwaPliku = signal<string | null>(null);
  protected readonly wTrakcie = signal(false);
  protected readonly wynik = signal<WynikWalidacjiXml | null>(null);
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
      .importujXml(plik)
      .pipe(
        catchError((err: unknown) => {
          this.komunikatBledu.set(komunikatBleduHttp(err, 'Nie udało się zaimportować pliku.'));
          return of(null as WynikWalidacjiXml | null);
        }),
      )
      .subscribe((wynik) => {
        this.wTrakcie.set(false);
        if (wynik !== null) {
          this.wynik.set(wynik);
        }
      });
  }

  protected mapowanie(): WierszMapowania[] {
    const sprawaZmapowana = this.wynik()?.sprawaZmapowana;
    if (!sprawaZmapowana) {
      return [];
    }
    return (Object.keys(ETYKIETY_POL_SPRAWY) as (keyof Sprawa)[])
      .filter((pole) => sprawaZmapowana[pole] !== undefined && sprawaZmapowana[pole] !== null)
      .map((pole) => ({
        pole: ETYKIETY_POL_SPRAWY[pole]!,
        wartosc: String(sprawaZmapowana[pole]),
      }));
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
