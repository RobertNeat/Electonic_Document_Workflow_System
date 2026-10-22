import { Component, computed, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { StatusPrzesylki, KanalWysylki } from '../../../core/models';
import { ObywateleDaneService } from '../obywatele-dane.service';
import { KartaObywatelaKontekst } from '../karta-obywatela-kontekst.service';

const ETYKIETY_STATUSU: Record<StatusPrzesylki, string> = {
  nadane: 'Nadane',
  doreczone: 'Doręczone',
  odebrane: 'Odebrane',
  zwrot: 'Zwrot',
  awizo: 'Awizo',
};

const SEVERITY_STATUSU: Record<StatusPrzesylki, 'success' | 'info' | 'warn' | 'danger'> = {
  nadane: 'info',
  doreczone: 'success',
  odebrane: 'success',
  zwrot: 'danger',
  awizo: 'warn',
};

const ETYKIETY_KANALU: Record<KanalWysylki, string> = {
  'e-doreczenia': 'e-Doręczenia',
  operator_pocztowy: 'Operator pocztowy',
  email: 'E-mail',
};

@Component({
  selector: 'app-obywatel-korespondencja',
  imports: [TableModule, TagModule, DatePipe],
  templateUrl: './obywatel-korespondencja.html',
  styleUrl: './obywatel-korespondencja.css',
})
export class ObywatelKorespondencja {
  private readonly kontekst = inject(KartaObywatelaKontekst);
  private readonly dane = inject(ObywateleDaneService);

  protected readonly przesylki = computed(() => {
    const obywatel = this.kontekst.obywatel();
    return obywatel ? this.dane.przesylki(obywatel.id) : [];
  });

  protected readonly etykietaStatusu = (status: StatusPrzesylki): string =>
    ETYKIETY_STATUSU[status];
  protected readonly severityStatusu = (status: StatusPrzesylki) => SEVERITY_STATUSU[status];
  protected readonly etykietaKanalu = (kanal: KanalWysylki): string => ETYKIETY_KANALU[kanal];
}
