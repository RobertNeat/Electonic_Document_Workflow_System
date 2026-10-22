import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { SelectModule } from 'primeng/select';
import { InputTextModule } from 'primeng/inputtext';
import { DatePickerModule } from 'primeng/datepicker';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { MessageModule } from 'primeng/message';
import { SprawyStore, formatujDateLokalna } from '../sprawy.store';
import { MOCK_DEFINICJE_OBIEGOW, MOCK_JRWA, MOCK_OBYWATELE } from '../../../core/mock/mock-data';
import { KategoriaArchiwalna, obywatelWyswietlanaNazwa } from '../../../core/models';

interface OpcjaJrwa {
  label: string;
  value: string;
  kategoriaArchiwalna: KategoriaArchiwalna;
}

interface OpcjaObiegu {
  label: string;
  value: string;
}

interface OpcjaObywatela {
  label: string;
  value: string;
}

/** Wydziały wywnioskowane z danych mockowych — brak dedykowanego słownika w core/. */
const WYDZIALY = ['Wydział Ochrony Środowiska', 'Wydział Organizacyjny', 'Wydział Księgowości'];

@Component({
  selector: 'app-sprawa-nowa',
  imports: [
    FormsModule,
    RouterLink,
    SelectModule,
    InputTextModule,
    DatePickerModule,
    ButtonModule,
    CardModule,
    MessageModule,
  ],
  templateUrl: './sprawa-nowa.html',
  styleUrl: './sprawa-nowa.css',
})
export class SprawaNowa {
  private readonly store = inject(SprawyStore);
  private readonly router = inject(Router);

  protected readonly opcjeObiegow: OpcjaObiegu[] = MOCK_DEFINICJE_OBIEGOW.map((d) => ({
    label: d.nazwa,
    value: d.id,
  }));

  protected readonly opcjeJrwa: OpcjaJrwa[] = MOCK_JRWA.map((j) => ({
    label: `${j.symbol} — ${j.haslo}`,
    value: j.symbol,
    kategoriaArchiwalna: j.kategoriaArchiwalna,
  }));

  protected readonly opcjeObywateli: OpcjaObywatela[] = MOCK_OBYWATELE.map((o) => ({
    label: obywatelWyswietlanaNazwa(o),
    value: o.id,
  }));

  protected readonly opcjeWydzialow = WYDZIALY;

  protected readonly tytul = signal('');
  protected readonly definicjaObieguId = signal<string | null>(null);
  protected readonly jrwa = signal<string | null>(null);
  protected readonly kategoriaArchiwalna = signal<KategoriaArchiwalna | null>(null);
  protected readonly obywatelId = signal<string | null>(null);
  protected readonly wydzial = signal<string | null>(null);
  protected readonly referent = signal('');
  protected readonly terminUstawowy = signal<Date | null>(null);
  protected readonly blad = signal<string | null>(null);

  protected readonly podgladZnaku = computed(() => {
    const wydzial = this.wydzial();
    const jrwa = this.jrwa();
    if (!wydzial || !jrwa) return null;
    return this.store.generujZnakSprawy(wydzial, jrwa);
  });

  protected naWybranaPozycjaJrwa(symbol: string | null): void {
    this.jrwa.set(symbol);
    const pozycja = this.opcjeJrwa.find((o) => o.value === symbol);
    if (pozycja) {
      this.kategoriaArchiwalna.set(pozycja.kategoriaArchiwalna);
    }
  }

  protected zapisz(): void {
    const definicjaId = this.definicjaObieguId();
    const jrwa = this.jrwa();
    const kategoria = this.kategoriaArchiwalna();
    const obywatelId = this.obywatelId();
    const wydzial = this.wydzial();

    if (
      !this.tytul().trim() ||
      !definicjaId ||
      !jrwa ||
      !kategoria ||
      !obywatelId ||
      !wydzial ||
      !this.referent().trim()
    ) {
      this.blad.set('Wypełnij wszystkie wymagane pola.');
      return;
    }

    const definicja = MOCK_DEFINICJE_OBIEGOW.find((d) => d.id === definicjaId);
    if (!definicja) {
      this.blad.set('Nieprawidłowa definicja obiegu.');
      return;
    }

    const termin = this.terminUstawowy();
    const nowaSprawa = this.store.dodajSprawe(
      {
        tytul: this.tytul().trim(),
        jrwa,
        kategoriaArchiwalna: kategoria,
        definicjaObieguId: definicjaId,
        obywatelId,
        wydzial,
        referent: this.referent().trim(),
        terminUstawowy: termin ? formatujDateLokalna(termin) : undefined,
      },
      definicja.stanPoczatkowy,
    );

    this.router.navigate(['/sprawy', nowaSprawa.id]);
  }
}
