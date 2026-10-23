import { Component, signal } from '@angular/core';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { FileUploadModule, FileUploadHandlerEvent } from 'primeng/fileupload';
import { MOCK_SZABLONY, MOCK_TYPY_DOKUMENTOW } from '../../../core/mock/mock-data';
import type { SzablonJasper } from '@electronic-document-workflow/document-shared-dto';

interface WpisHistoriiSzablonu {
  wersja: number;
  dataWgrania: string;
  autor: string;
  opisZmiany: string;
}

@Component({
  selector: 'app-szablony-strona',
  imports: [TableModule, ButtonModule, DialogModule, FileUploadModule],
  templateUrl: './szablony-strona.html',
  styleUrl: './szablony-strona.css',
})
export class SzablonyStrona {
  protected readonly szablony = signal<SzablonJasper[]>([...MOCK_SZABLONY]);
  protected readonly typyDokumentow = MOCK_TYPY_DOKUMENTOW;

  protected readonly uploadWidoczny = signal(false);
  protected readonly historiaWidoczna = signal(false);
  protected readonly historiaWybranego = signal<WpisHistoriiSzablonu[]>([]);
  protected readonly wybranySzablon = signal<SzablonJasper | null>(null);

  protected nazwaTypu(typDokumentuId: string): string {
    return this.typyDokumentow.find((t) => t.id === typDokumentuId)?.nazwa ?? '—';
  }

  protected otworzUpload(): void {
    this.uploadWidoczny.set(true);
  }

  // mock wgrywania — walidacja rozszerzenia .jrxml i dopisanie wpisu lokalnie, bez realnego zapisu
  protected wgraj(event: FileUploadHandlerEvent): void {
    const pliki = event.files;
    for (const plik of pliki) {
      if (!plik.name.toLowerCase().endsWith('.jrxml')) {
        continue;
      }
      const nowy: SzablonJasper = {
        id: `sz-${Date.now()}`,
        typDokumentuId: this.typyDokumentow[0]?.id ?? '',
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
    // mockowa historia wersji — realne dane dostarczy jasper_service
    const wpisy: WpisHistoriiSzablonu[] = [];
    for (let w = szablon.wersja; w >= 1; w--) {
      wpisy.push({
        wersja: w,
        dataWgrania: w === szablon.wersja ? szablon.dataWgrania : '2026-0' + w + '-01',
        autor: szablon.autor,
        opisZmiany:
          w === szablon.wersja
            ? 'Aktualna wersja szablonu'
            : w === 1
              ? 'Pierwsza wersja szablonu'
              : 'Poprawki układu i etykiet pól',
      });
    }
    this.historiaWybranego.set(wpisy);
    this.historiaWidoczna.set(true);
  }
}
