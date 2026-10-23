import { Component, computed, inject, input } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { CardModule } from 'primeng/card';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { TimelineModule } from 'primeng/timeline';
import { MOCK_DOKUMENTY, MOCK_OBYWATELE, MOCK_SPRAWY } from '../../../core/mock/mock-data';
import { obywatelWyswietlanaNazwa } from '../../../core/models';
import type { StatusPrzesylki } from '@electronic-document-workflow/document-shared-dto';
import { KorespondencjaStore } from '../korespondencja.store';
import {
  ETYKIETY_KANALOW,
  ETYKIETY_STATUSOW,
  IKONY_KANALOW,
  OPISY_SKUTKOW_KANALOW,
  SEWERIE_STATUSOW,
} from '../korespondencja.utils';
@Component({
  selector: 'app-korespondencja-szczegoly',
  imports: [DatePipe, RouterLink, CardModule, TagModule, ButtonModule, TimelineModule],
  templateUrl: './korespondencja-szczegoly.html',
  styleUrl: './korespondencja-szczegoly.css',
})
export class KorespondencjaSzczegoly {
  private readonly store = inject(KorespondencjaStore);

  readonly id = input<string>();

  protected readonly przesylka = computed(() => {
    const id = this.id();
    return id ? this.store.przesylka(id)() : undefined;
  });

  protected readonly historia = computed(() => {
    const id = this.id();
    return id ? this.store.historia(id)() : [];
  });

  protected readonly sprawa = computed(() => {
    const p = this.przesylka();
    return p ? MOCK_SPRAWY.find((s) => s.id === p.sprawaId) : undefined;
  });

  protected readonly dokument = computed(() => {
    const p = this.przesylka();
    return p ? MOCK_DOKUMENTY.find((d) => d.id === p.dokumentId) : undefined;
  });

  protected readonly obywatel = computed(() => {
    const p = this.przesylka();
    return p ? MOCK_OBYWATELE.find((o) => o.id === p.obywatelId) : undefined;
  });

  protected readonly nazwaObywatela = computed(() => {
    const obywatel = this.obywatel();
    return obywatel ? obywatelWyswietlanaNazwa(obywatel) : '—';
  });

  protected readonly mozliweStatusy = computed(() => {
    const id = this.id();
    return id ? this.store.mozliweKolejneStatusy(id) : [];
  });

  protected readonly etykietyKanalow = ETYKIETY_KANALOW;
  protected readonly ikonyKanalow = IKONY_KANALOW;
  protected readonly etykietyStatusow = ETYKIETY_STATUSOW;
  protected readonly sewerieStatusow = SEWERIE_STATUSOW;
  protected readonly opisySkutkow = OPISY_SKUTKOW_KANALOW;

  symulujAktualizacje(docelowyStatus: StatusPrzesylki): void {
    const id = this.id();
    if (id) {
      this.store.symulujAktualizacjeStatusu(id, docelowyStatus);
    }
  }
}
