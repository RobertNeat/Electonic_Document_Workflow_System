import { Component, computed, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CardModule } from 'primeng/card';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { TimelineModule } from 'primeng/timeline';
import { SprawyStore } from '../sprawy.store';
import { seweroscStanu, statusTerminu } from '../sprawy.utils';
import { CitizenContextService } from '../../../core/services/citizen-context.service';
import { MOCK_DEFINICJE_OBIEGOW, MOCK_OBYWATELE } from '../../../core/mock/mock-data';
import { obywatelWyswietlanaNazwa } from '../../../core/models';

@Component({
  selector: 'app-sprawa-szczegoly',
  imports: [RouterLink, CardModule, TagModule, ButtonModule, TimelineModule],
  templateUrl: './sprawa-szczegoly.html',
  styleUrl: './sprawa-szczegoly.css',
})
export class SprawaSzczegoly {
  private readonly store = inject(SprawyStore);
  private readonly citizenContext = inject(CitizenContextService);

  readonly id = input<string>();

  protected readonly sprawa = computed(() => {
    const id = this.id();
    return id ? this.store.znajdz(id) : undefined;
  });

  protected readonly definicjaObiegu = computed(() => {
    const sprawa = this.sprawa();
    return sprawa
      ? MOCK_DEFINICJE_OBIEGOW.find((d) => d.id === sprawa.definicjaObieguId)
      : undefined;
  });

  protected readonly obywatel = computed(() => {
    const sprawa = this.sprawa();
    return sprawa ? MOCK_OBYWATELE.find((o) => o.id === sprawa.obywatelId) : undefined;
  });

  protected readonly nazwaObywatela = computed(() => {
    const obywatel = this.obywatel();
    return obywatel ? obywatelWyswietlanaNazwa(obywatel) : '—';
  });

  protected readonly metryka = computed(() => {
    const id = this.id();
    return id ? this.store.metrykiDlaSprawy(id) : [];
  });

  protected readonly indeksAktualnegoStanu = computed(() => {
    const sprawa = this.sprawa();
    const definicja = this.definicjaObiegu();
    if (!sprawa || !definicja) return -1;
    return definicja.stany.indexOf(sprawa.stan);
  });

  protected readonly kolejnyStan = computed(() => {
    const definicja = this.definicjaObiegu();
    const indeks = this.indeksAktualnegoStanu();
    if (!definicja || indeks < 0 || indeks >= definicja.stany.length - 1) return null;
    return definicja.stany[indeks + 1];
  });

  protected readonly czyKolejnyStanOstatni = computed(() => {
    const definicja = this.definicjaObiegu();
    const indeks = this.indeksAktualnegoStanu();
    if (!definicja) return false;
    return indeks + 1 === definicja.stany.length - 1;
  });

  protected readonly seweroscStanu = seweroscStanu;
  protected readonly statusTerminu = statusTerminu;

  protected przejdzDoKolejnegoStanu(): void {
    const sprawa = this.sprawa();
    const kolejny = this.kolejnyStan();
    if (!sprawa || !kolejny) return;
    this.store.zmienStan(sprawa.id, kolejny, this.czyKolejnyStanOstatni());
  }

  protected przypnijObywatela(): void {
    const obywatel = this.obywatel();
    if (obywatel) {
      this.citizenContext.przypnij(obywatel);
    }
  }
}
