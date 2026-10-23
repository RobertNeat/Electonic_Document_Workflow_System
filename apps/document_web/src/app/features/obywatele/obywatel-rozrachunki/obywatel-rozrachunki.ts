import { Component, computed, inject } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import type { StatusRozliczenia } from '@electronic-document-workflow/document-shared-dto';
import { ObywateleDaneService } from '../obywatele-dane.service';
import { KartaObywatelaKontekst } from '../karta-obywatela-kontekst.service';

const ETYKIETY_STATUSU: Record<StatusRozliczenia, string> = {
  nierozliczona: 'Nierozliczona',
  czesciowo_rozliczona: 'Częściowo rozliczona',
  rozliczona: 'Rozliczona',
  przeterminowana: 'Przeterminowana',
};

const SEVERITY_STATUSU: Record<StatusRozliczenia, 'success' | 'info' | 'warn' | 'danger'> = {
  nierozliczona: 'warn',
  czesciowo_rozliczona: 'info',
  rozliczona: 'success',
  przeterminowana: 'danger',
};

@Component({
  selector: 'app-obywatel-rozrachunki',
  imports: [TableModule, TagModule, DatePipe, CurrencyPipe],
  templateUrl: './obywatel-rozrachunki.html',
  styleUrl: './obywatel-rozrachunki.css',
})
export class ObywatelRozrachunki {
  private readonly kontekst = inject(KartaObywatelaKontekst);
  private readonly dane = inject(ObywateleDaneService);

  protected readonly naleznosci = computed(() => {
    const obywatel = this.kontekst.obywatel();
    return obywatel ? this.dane.naleznosci(obywatel.id) : [];
  });

  protected readonly wplaty = computed(() => {
    const obywatel = this.kontekst.obywatel();
    return obywatel ? this.dane.wplaty(obywatel.id) : [];
  });

  protected readonly sumaDoZaplaty = computed(() =>
    this.naleznosci().reduce((suma, n) => suma + (n.kwota - n.kwotaRozliczona), 0),
  );

  protected readonly etykietaStatusu = (status: StatusRozliczenia): string =>
    ETYKIETY_STATUSU[status];
  protected readonly severityStatusu = (status: StatusRozliczenia) => SEVERITY_STATUSU[status];
}
