import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { TableModule } from 'primeng/table';
import { TabsModule } from 'primeng/tabs';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { MOCK_HISTORIA_XML, MOCK_HISTORIA_UBL } from '../wymiana-mock';
import type { WpisHistoriiWymiany } from '@electronic-document-workflow/document-shared-dto';

type StatusWymiany = WpisHistoriiWymiany['status'];

@Component({
  selector: 'app-wymiana-strona',
  imports: [TableModule, TabsModule, TagModule, ButtonModule],
  templateUrl: './wymiana-strona.html',
  styleUrl: './wymiana-strona.css',
})
export class WymianaStrona {
  private readonly router = inject(Router);

  protected readonly historiaXml: WpisHistoriiWymiany[] = MOCK_HISTORIA_XML;
  protected readonly historiaUbl: WpisHistoriiWymiany[] = MOCK_HISTORIA_UBL;

  protected statusSeverity(status: StatusWymiany): 'success' | 'danger' {
    return status === 'sukces' ? 'success' : 'danger';
  }

  protected statusLabel(status: StatusWymiany): string {
    return status === 'sukces' ? 'Sukces' : 'Błąd walidacji';
  }

  protected kierunekLabel(kierunek: string): string {
    return kierunek === 'import' ? 'Import' : 'Eksport';
  }

  protected nowyImportXml(): void {
    this.router.navigate(['/wymiana/import-xml']);
  }

  protected nowyImportUbl(): void {
    this.router.navigate(['/wymiana/import-ubl']);
  }
}
