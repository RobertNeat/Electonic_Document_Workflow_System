import { Component, inject, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FileUploadModule, FileUploadHandlerEvent } from 'primeng/fileupload';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { PlatnosciStore, WynikImportuWyciaguZNazwa } from '../data/platnosci-store';

@Component({
  selector: 'app-import-wyciagu',
  imports: [CurrencyPipe, RouterLink, FileUploadModule, TableModule, TagModule, ButtonModule],
  templateUrl: './import-wyciagu.html',
  styleUrl: './import-wyciagu.css',
})
export class ImportWyciagu {
  private readonly store = inject(PlatnosciStore);

  protected readonly wynikImportu = signal<WynikImportuWyciaguZNazwa | null>(null);

  /** Mock importu — plik CAMT.053 nie jest realnie parsowany, generujemy przykładowe transakcje. */
  importujPlik(zdarzenie: FileUploadHandlerEvent): void {
    const plik = zdarzenie.files[0];
    if (!plik) {
      return;
    }
    this.wynikImportu.set(this.store.importujWyciag(plik.name));
  }

  nowyImport(): void {
    this.wynikImportu.set(null);
  }
}
