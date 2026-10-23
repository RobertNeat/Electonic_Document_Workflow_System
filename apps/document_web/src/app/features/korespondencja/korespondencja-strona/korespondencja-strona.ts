import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { SelectModule } from 'primeng/select';
import { TabsModule } from 'primeng/tabs';
import type {
  KanalWysylki,
  StatusPrzesylki,
} from '@electronic-document-workflow/document-shared-dto';
import { MOCK_OBYWATELE, MOCK_SPRAWY } from '../../../core/mock/mock-data';
import { obywatelWyswietlanaNazwa } from '../../../core/models';
import { KorespondencjaStore } from '../korespondencja.store';
import {
  ETYKIETY_KANALOW,
  ETYKIETY_STATUSOW,
  IKONY_KANALOW,
  Kierunek,
  SEWERIE_STATUSOW,
} from '../korespondencja.utils';

interface OpcjaFiltra<T> {
  etykieta: string;
  wartosc: T | null;
}

@Component({
  selector: 'app-korespondencja-strona',
  imports: [
    DatePipe,
    RouterLink,
    FormsModule,
    TableModule,
    TagModule,
    ButtonModule,
    SelectModule,
    TabsModule,
  ],
  templateUrl: './korespondencja-strona.html',
  styleUrl: './korespondencja-strona.css',
})
export class KorespondencjaStrona {
  private readonly store = inject(KorespondencjaStore);
  private readonly router = inject(Router);

  protected readonly etykietyKanalow = ETYKIETY_KANALOW;
  protected readonly ikonyKanalow = IKONY_KANALOW;
  protected readonly etykietyStatusow = ETYKIETY_STATUSOW;
  protected readonly sewerieStatusow = SEWERIE_STATUSOW;

  // Przełącznik zakładek UI (lokalny, niezależny od Przesylka.kierunek) — wszystkie mocki
  // są wychodzące, zakładka "Wpływające" nie filtruje jeszcze po polu modelu (patrz plan.md).
  protected readonly kierunek = signal<Kierunek>('wychodzaca');

  protected readonly opcjeStatusow: OpcjaFiltra<StatusPrzesylki>[] = [
    { etykieta: 'Wszystkie statusy', wartosc: null },
    { etykieta: ETYKIETY_STATUSOW.nadane, wartosc: 'nadane' },
    { etykieta: ETYKIETY_STATUSOW.doreczone, wartosc: 'doreczone' },
    { etykieta: ETYKIETY_STATUSOW.odebrane, wartosc: 'odebrane' },
    { etykieta: ETYKIETY_STATUSOW.zwrot, wartosc: 'zwrot' },
    { etykieta: ETYKIETY_STATUSOW.awizo, wartosc: 'awizo' },
  ];

  protected readonly opcjeKanalow: OpcjaFiltra<KanalWysylki>[] = [
    { etykieta: 'Wszystkie kanały', wartosc: null },
    { etykieta: ETYKIETY_KANALOW['e-doreczenia'], wartosc: 'e-doreczenia' },
    { etykieta: ETYKIETY_KANALOW.operator_pocztowy, wartosc: 'operator_pocztowy' },
    { etykieta: ETYKIETY_KANALOW.email, wartosc: 'email' },
  ];

  protected readonly filtrStatusu = signal<StatusPrzesylki | null>(null);
  protected readonly filtrKanalu = signal<KanalWysylki | null>(null);

  protected readonly przesylki = computed(() => {
    if (this.kierunek() === 'wplywajaca') {
      return [];
    }
    const statusFiltr = this.filtrStatusu();
    const kanalFiltr = this.filtrKanalu();
    return this.store
      .przesylki()
      .filter((p) => (statusFiltr ? p.status === statusFiltr : true))
      .filter((p) => (kanalFiltr ? p.kanal === kanalFiltr : true));
  });

  znakSprawy(sprawaId: string): string {
    return MOCK_SPRAWY.find((s) => s.id === sprawaId)?.znakSprawy ?? sprawaId;
  }

  nazwaObywatela(obywatelId: string): string {
    const obywatel = MOCK_OBYWATELE.find((o) => o.id === obywatelId);
    return obywatel ? obywatelWyswietlanaNazwa(obywatel) : obywatelId;
  }

  // p-tabs emituje string | number | undefined — zwężamy do znanych wartości Kierunek.
  zmienKierunek(wartosc: string | number | undefined): void {
    if (wartosc === 'wychodzaca' || wartosc === 'wplywajaca') {
      this.kierunek.set(wartosc);
    }
  }

  otworzPrzesylke(przesylkaId: string): void {
    this.router.navigate(['/korespondencja', przesylkaId]);
  }
}
