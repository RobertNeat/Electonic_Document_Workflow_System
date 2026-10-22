import { Component, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { Router } from '@angular/router';
import { FileUploadModule, FileUploadHandlerEvent } from 'primeng/fileupload';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { TableModule } from 'primeng/table';
import { CardModule } from 'primeng/card';

interface PozycjaFakturyUbl {
  nazwa: string;
  ilosc: number;
  cenaNetto: number;
  wartoscBrutto: number;
}

/** Sparsowana faktura UBL 2.1 (PEPPOL) — mock, bez realnego parsowania XML. */
interface FakturaUbl {
  numerFaktury: string;
  kontrahent: string;
  nip: string;
  dataWystawienia: string;
  terminPlatnosci: string;
  kwotaBrutto: number;
  pozycje: PozycjaFakturyUbl[];
}

interface WynikImportuUbl {
  sukces: boolean;
  bledy: string[];
  faktura: FakturaUbl | null;
  numerReferencyjnyNaleznosci: string | null;
}

@Component({
  selector: 'app-import-ubl',
  imports: [FileUploadModule, ButtonModule, MessageModule, TableModule, CardModule, DecimalPipe],
  templateUrl: './import-ubl.html',
  styleUrl: './import-ubl.css',
})
export class ImportUbl {
  private readonly router = inject(Router);

  protected readonly nazwaPliku = signal<string | null>(null);
  protected readonly wynik = signal<WynikImportuUbl | null>(null);

  protected onUpload(event: FileUploadHandlerEvent): void {
    const plik = event.files[0];
    if (!plik) {
      return;
    }
    this.nazwaPliku.set(plik.name);

    const nazwaNieprawidlowa =
      !plik.name.toLowerCase().endsWith('.xml') || plik.name.includes('niepoprawn');

    if (nazwaNieprawidlowa) {
      this.wynik.set({
        sukces: false,
        bledy: [
          'Nieprawidłowa przestrzeń nazw dokumentu — oczekiwano ' +
            'urn:oasis:names:specification:ubl:schema:xsd:Invoice-2.',
          'Brak wymaganego elementu cbc:InvoiceTypeCode zgodnego z profilem PEPPOL BIS Billing 3.0.',
        ],
        faktura: null,
        numerReferencyjnyNaleznosci: null,
      });
      return;
    }

    this.wynik.set({
      sukces: true,
      bledy: [],
      faktura: {
        numerFaktury: 'FV/2026/10/0231',
        kontrahent: 'Budimex Serwis Sp. z o.o.',
        nip: '5261234567',
        dataWystawienia: '2026-10-19',
        terminPlatnosci: '2026-11-18',
        kwotaBrutto: 4500,
        pozycje: [
          {
            nazwa: 'Przegląd okresowy instalacji wentylacyjnej',
            ilosc: 1,
            cenaNetto: 2500,
            wartoscBrutto: 3075,
          },
          { nazwa: 'Wymiana filtrów', ilosc: 4, cenaNetto: 280, wartoscBrutto: 1378.8 },
          { nazwa: 'Dojazd serwisowy', ilosc: 1, cenaNetto: 37.56, wartoscBrutto: 46.2 },
        ],
      },
      numerReferencyjnyNaleznosci: 'FK/2026/0089',
    });
  }

  protected wyczysc(): void {
    this.nazwaPliku.set(null);
    this.wynik.set(null);
  }

  protected wrocDoPrzegladu(): void {
    this.router.navigate(['/wymiana']);
  }
}
