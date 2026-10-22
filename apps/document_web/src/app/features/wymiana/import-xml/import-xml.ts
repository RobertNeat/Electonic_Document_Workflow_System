import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { FileUploadModule, FileUploadHandlerEvent } from 'primeng/fileupload';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { TableModule } from 'primeng/table';
import { CardModule } from 'primeng/card';
import { MOCK_SPRAWY } from '../../../core/mock/mock-data';

interface PoleMapowania {
  pole: string;
  wartosc: string;
}

/** Wynik walidacji komunikatu XML wg schematu XSD — mock, bez realnej walidacji. */
interface WynikWalidacjiXml {
  sukces: boolean;
  bledy: string[];
  mapowanie: PoleMapowania[];
}

@Component({
  selector: 'app-import-xml',
  imports: [FileUploadModule, ButtonModule, MessageModule, TableModule, CardModule],
  templateUrl: './import-xml.html',
  styleUrl: './import-xml.css',
})
export class ImportXml {
  private readonly router = inject(Router);

  protected readonly nazwaPliku = signal<string | null>(null);
  protected readonly wynik = signal<WynikWalidacjiXml | null>(null);

  // customUpload — brak backendu w tym etapie, walidacja i mapowanie są symulowane w przeglądarce.
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
          'Niezgodność ze schematem XSD: element "znakSprawy" jest wymagany, a nie został znaleziony.',
          'Niezgodność ze schematem XSD: nieoczekiwany element "legacyId" w przestrzeni nazw komunikatu.',
        ],
        mapowanie: [],
      });
      return;
    }

    const sprawa = MOCK_SPRAWY[0];
    this.wynik.set({
      sukces: true,
      bledy: [],
      mapowanie: [
        { pole: 'Znak sprawy', wartosc: sprawa.znakSprawy },
        { pole: 'Tytuł sprawy', wartosc: sprawa.tytul },
        { pole: 'Symbol JRWA', wartosc: sprawa.jrwa },
        { pole: 'Wydział', wartosc: sprawa.wydzial },
        { pole: 'Referent', wartosc: sprawa.referent },
        { pole: 'Data wszczęcia', wartosc: sprawa.dataWszczecia },
        { pole: 'Termin ustawowy', wartosc: sprawa.terminUstawowy ?? '—' },
        { pole: 'Kategoria archiwalna', wartosc: sprawa.kategoriaArchiwalna },
      ],
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
