import { Component, computed, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import type {
  StatusPrzesylki,
  KanalWysylki,
  Przesylka,
} from '@electronic-document-workflow/document-shared-dto';
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

const ETYKIETY_KIERUNKU: Record<Przesylka['kierunek'], string> = {
  wychodzaca: 'Wychodząca',
  wplywajaca: 'Wpływająca',
};

const SEVERITY_KIERUNKU: Record<Przesylka['kierunek'], 'secondary' | 'info'> = {
  wychodzaca: 'secondary',
  wplywajaca: 'info',
};

const IKONY_KIERUNKU: Record<Przesylka['kierunek'], string> = {
  wychodzaca: 'pi pi-arrow-up-right',
  wplywajaca: 'pi pi-arrow-down-left',
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

  protected readonly etykietaKierunku = (kierunek: Przesylka['kierunek']): string =>
    ETYKIETY_KIERUNKU[kierunek];
  protected readonly severityKierunku = (kierunek: Przesylka['kierunek']) =>
    SEVERITY_KIERUNKU[kierunek];
  protected readonly ikonaKierunku = (kierunek: Przesylka['kierunek']): string =>
    IKONY_KIERUNKU[kierunek];
}
