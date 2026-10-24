import { Component, inject, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FileUploadModule, FileUploadHandlerEvent } from 'primeng/fileupload';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { PlatnosciStore, WynikImportuWyciaguZNazwa } from '../data/platnosci-store';

@Component({
  selector: 'app-import-wyciagu',
  imports: [
    CurrencyPipe,
    RouterLink,
    FileUploadModule,
    TableModule,
    TagModule,
    ButtonModule,
    MessageModule,
  ],
  templateUrl: './import-wyciagu.html',
  styleUrl: './import-wyciagu.css',
})
export class ImportWyciagu {
  private readonly store = inject(PlatnosciStore);

  protected readonly wynikImportu = signal<WynikImportuWyciaguZNazwa | null>(null);
  protected readonly importowanie = signal(false);
  protected readonly blad = signal<string | null>(null);

  /** Wysyła realny plik (nie tylko jego nazwę) do POST /api/import-camt053 - backend parsuje
   * XML i zwraca faktyczne wyniki dopasowania. */
  importujPlik(zdarzenie: FileUploadHandlerEvent): void {
    const plik = zdarzenie.files[0];
    if (!plik) {
      return;
    }
    this.blad.set(null);
    this.importowanie.set(true);
    this.store.importujWyciag(plik).then((wynik) => {
      this.importowanie.set(false);
      if (typeof wynik === 'string') {
        this.blad.set(wynik);
        return;
      }
      this.wynikImportu.set(wynik);
    });
  }

  nowyImport(): void {
    this.wynikImportu.set(null);
    this.blad.set(null);
  }
}
