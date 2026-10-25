import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Router } from '@angular/router';
import { TableModule } from 'primeng/table';
import { TabsModule } from 'primeng/tabs';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { catchError, of } from 'rxjs';
import type { WpisHistoriiWymiany } from '@electronic-document-workflow/document-shared-dto';
import { WymianaApi } from '../../../core/api/wymiana.api';
import { komunikatBleduHttp } from '../../../core/api/http-error';

type StatusWymiany = WpisHistoriiWymiany['status'];

@Component({
  selector: 'app-wymiana-strona',
  imports: [TableModule, TabsModule, TagModule, ButtonModule, MessageModule, DatePipe],
  templateUrl: './wymiana-strona.html',
  styleUrl: './wymiana-strona.css',
})
export class WymianaStrona {
  private readonly api = inject(WymianaApi);
  private readonly router = inject(Router);

  protected readonly historiaXml = signal<WpisHistoriiWymiany[]>([]);
  protected readonly historiaUbl = signal<WpisHistoriiWymiany[]>([]);
  protected readonly wczytywanieXml = signal(true);
  protected readonly wczytywanieUbl = signal(true);
  protected readonly komunikatBleduXml = signal<string | null>(null);
  protected readonly komunikatBleduUbl = signal<string | null>(null);

  constructor() {
    this.api
      .historiaXml()
      .pipe(
        catchError((err: unknown) => {
          this.komunikatBleduXml.set(
            komunikatBleduHttp(err, 'Nie udało się wczytać historii wymiany XML.'),
          );
          return of(null as WpisHistoriiWymiany[] | null);
        }),
      )
      .subscribe((wynik) => {
        this.wczytywanieXml.set(false);
        if (wynik !== null) {
          this.historiaXml.set(wynik);
        }
      });

    this.api
      .historiaUbl()
      .pipe(
        catchError((err: unknown) => {
          this.komunikatBleduUbl.set(
            komunikatBleduHttp(err, 'Nie udało się wczytać historii wymiany UBL.'),
          );
          return of(null as WpisHistoriiWymiany[] | null);
        }),
      )
      .subscribe((wynik) => {
        this.wczytywanieUbl.set(false);
        if (wynik !== null) {
          this.historiaUbl.set(wynik);
        }
      });
  }

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
