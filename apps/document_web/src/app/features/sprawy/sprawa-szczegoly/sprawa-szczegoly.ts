import { Component, computed, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CardModule } from 'primeng/card';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { TimelineModule } from 'primeng/timeline';
import { SprawyStore } from '../sprawy.store';
import { przejsciaZeStanu, seweroscStanu, statusTerminu } from '../sprawy.utils';
import { CitizenContextService } from '../../../core/services/citizen-context.service';
import {
  MOCK_DEFINICJE_OBIEGOW,
  MOCK_DOKUMENTY,
  MOCK_OBYWATELE,
  MOCK_TYPY_DOKUMENTOW,
  MOCK_UZYTKOWNIK,
} from '../../../core/mock/mock-data';
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

  // Nadal przydatny do podświetlenia aktualnego kroku na pasku stanów (liniowy porządek
  // `definicja.stany` pozostaje sensowny jako oś czasu — jedyne rozgałęzienie w danych mockowych
  // to krok "zwrotny" do poprawek, nie równoległe ścieżki docelowe).
  protected readonly indeksAktualnegoStanu = computed(() => {
    const sprawa = this.sprawa();
    const definicja = this.definicjaObiegu();
    if (!sprawa || !definicja) return -1;
    return definicja.stany.indexOf(sprawa.stan);
  });

  /** Wszystkie możliwe przejścia z aktualnego stanu sprawy wg `DefinicjaObiegu.przejscia`. */
  protected readonly mozliwePrzejscia = computed(() => {
    const sprawa = this.sprawa();
    const definicja = this.definicjaObiegu();
    if (!sprawa || !definicja) return [];
    const dokumentySprawy = MOCK_DOKUMENTY.filter((d) => d.sprawaId === sprawa.id);
    return przejsciaZeStanu(
      definicja,
      sprawa.stan,
      MOCK_UZYTKOWNIK,
      dokumentySprawy,
      MOCK_TYPY_DOKUMENTOW,
    );
  });

  protected readonly seweroscStanu = seweroscStanu;
  protected readonly statusTerminu = statusTerminu;

  protected przejdzDoStanu(doStanu: string): void {
    const sprawa = this.sprawa();
    if (!sprawa) return;
    this.store.zmienStan(sprawa.id, doStanu);
  }

  protected przypnijObywatela(): void {
    const obywatel = this.obywatel();
    if (obywatel) {
      this.citizenContext.przypnij(obywatel);
    }
  }
}
