import { Component, effect, inject, signal, untracked } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { SelectModule } from 'primeng/select';
import { TabsModule } from 'primeng/tabs';
import { MessageModule } from 'primeng/message';
import { catchError, of } from 'rxjs';
import type {
  KanalWysylki,
  StatusPrzesylki,
} from '@electronic-document-workflow/document-shared-dto';
import { obywatelWyswietlanaNazwa } from '../../../core/models';
import { SprawyApi } from '../../../core/api/sprawy.api';
import { ObywateleApi } from '../../../core/api/obywatele.api';
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
    MessageModule,
  ],
  templateUrl: './korespondencja-strona.html',
  styleUrl: './korespondencja-strona.css',
})
export class KorespondencjaStrona {
  private readonly store = inject(KorespondencjaStore);
  private readonly router = inject(Router);
  private readonly sprawyApi = inject(SprawyApi);
  private readonly obywateleApi = inject(ObywateleApi);

  protected readonly etykietyKanalow = ETYKIETY_KANALOW;
  protected readonly ikonyKanalow = IKONY_KANALOW;
  protected readonly etykietyStatusow = ETYKIETY_STATUSOW;
  protected readonly sewerieStatusow = SEWERIE_STATUSOW;

  protected readonly przesylki = this.store.przesylki;
  protected readonly loading = this.store.loading;
  protected readonly blad = this.store.blad;

  // Przełącznik zakładek UI — steruje filtrem po Przesylka.kierunek przekazywanym do
  // GET /api/przesylki (filtrowanie po stronie backendu, nie lokalnie na pobranej liście).
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

  // Przesylka (GET /api/przesylki) nie zawiera zdenormalizowanych nazw sprawy/obywatela (tylko
  // surowe sprawaId/obywatelId) - rozwiązywane TU, per DYSTYNKTYWNY id (nie per wiersz), żeby przy
  // wielu przesyłkach jednej sprawy/obywatela nie powtarzać żądań. Cache trzymany w komponencie,
  // przetrwa zmianę filtra/zakładki w ramach tego samego zamontowania widoku. Świadomy tradeoff:
  // to potencjalnie N+N małych żądań GET /api/sprawy/:id i GET /api/obywatele/:id (po jednym per
  // unikalny id widoczny w rejestrze) - akceptowalne przy skali danych demo, ale nie skalowałoby
  // się do dużego rejestru bez endpointu zwracającego zdenormalizowane nazwy wprost.
  private readonly znakiSprawCache = signal<Record<string, string>>({});
  private readonly nazwyObywateliCache = signal<Record<string, string>>({});

  constructor() {
    // Efekt woła szukaj() przy montowaniu (pierwszy przebieg) i przy każdej zmianie filtra -
    // store.szukaj() zmienia store.przesylki() (śledzone przez INNY effect poniżej), nie przez
    // ten sam, więc untracked() tu jest tylko zabezpieczeniem przed przypadkowym samo-śledzeniem
    // przyszłych odczytów store w tym effect.
    effect(() => {
      const kierunek = this.kierunek();
      const status = this.filtrStatusu();
      const kanal = this.filtrKanalu();
      untracked(() => {
        this.store.szukaj({ kierunek, status, kanal });
      });
    });

    // Dociąga znaki spraw/nazwy obywateli dla nowo pojawionych id w pobranej liście.
    effect(() => {
      const lista = this.przesylki();
      untracked(() => this.doladujNazwy(lista));
    });
  }

  private doladujNazwy(lista: { sprawaId: string; obywatelId: string }[]): void {
    const znaneSprawy = this.znakiSprawCache();
    const znaniObywatele = this.nazwyObywateliCache();

    const noweSprawy = Array.from(new Set(lista.map((p) => p.sprawaId))).filter(
      (id) => !(id in znaneSprawy),
    );
    const nowiObywatele = Array.from(new Set(lista.map((p) => p.obywatelId))).filter(
      (id) => !(id in znaniObywatele),
    );

    for (const sprawaId of noweSprawy) {
      this.sprawyApi
        .getById(sprawaId)
        .pipe(catchError(() => of(null)))
        .subscribe((sprawa) => {
          this.znakiSprawCache.update((mapa) => ({
            ...mapa,
            [sprawaId]: sprawa?.znakSprawy ?? sprawaId,
          }));
        });
    }

    for (const obywatelId of nowiObywatele) {
      this.obywateleApi
        .getById(obywatelId)
        .pipe(catchError(() => of(null)))
        .subscribe((obywatel) => {
          this.nazwyObywateliCache.update((mapa) => ({
            ...mapa,
            [obywatelId]: obywatel ? obywatelWyswietlanaNazwa(obywatel) : obywatelId,
          }));
        });
    }
  }

  znakSprawy(sprawaId: string): string {
    return this.znakiSprawCache()[sprawaId] ?? sprawaId;
  }

  nazwaObywatela(obywatelId: string): string {
    return this.nazwyObywateliCache()[obywatelId] ?? obywatelId;
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
