import { Component, DestroyRef, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TableModule } from 'primeng/table';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { SelectModule } from 'primeng/select';
import { ProgressBarModule } from 'primeng/progressbar';
import { TagModule } from 'primeng/tag';
import { MessageModule } from 'primeng/message';
import { MOCK_SPRAWY, MOCK_SZABLONY } from '../../../core/mock/mock-data';
import type { Sprawa, SzablonJasper } from '@electronic-document-workflow/document-shared-dto';

type TypAkcji = 'zmiana-stanu' | 'wysylka-pism' | 'generowanie-dokumentow';

/** Przebieg akcji zbiorowej symulowany lokalnie — w produkcji aktualizowany asynchronicznie przez RabbitMQ. */
interface PrzebiegAkcji {
  typ: TypAkcji;
  trwajaca: boolean;
  postep: number;
  przetworzonychLacznie: number;
  przetworzonychPomyslnie: number;
  zakonczona: boolean;
}

const KROK_MS = 150;
const KANALY_WYSYLKI = [
  { etykieta: 'e-Doręczenia', wartosc: 'e-doreczenia' },
  { etykieta: 'Operator pocztowy', wartosc: 'operator_pocztowy' },
  { etykieta: 'E-mail', wartosc: 'email' },
];

@Component({
  selector: 'app-akcje-zbiorowe',
  imports: [
    FormsModule,
    TableModule,
    CardModule,
    ButtonModule,
    SelectModule,
    ProgressBarModule,
    TagModule,
    MessageModule,
  ],
  templateUrl: './akcje-zbiorowe.html',
  styleUrl: './akcje-zbiorowe.css',
})
export class AkcjeZbiorowe {
  private readonly destroyRef = inject(DestroyRef);
  private readonly router = inject(Router);

  private readonly interwaly = new Map<TypAkcji, ReturnType<typeof setInterval>>();

  protected readonly sprawy: Sprawa[] = MOCK_SPRAWY;
  protected readonly szablony: SzablonJasper[] = MOCK_SZABLONY;
  protected readonly kanaly = KANALY_WYSYLKI;
  protected readonly stany = [
    'Zarejestrowana',
    'W toku',
    'Do akceptacji',
    'Zaakceptowana',
    'Zakończona',
    'Zamknięta',
  ];

  protected readonly wybraneZmianaStanu = signal<Sprawa[]>([]);
  protected readonly wybraneWysylka = signal<Sprawa[]>([]);
  protected readonly wybraneGenerowanie = signal<Sprawa[]>([]);

  protected nowyStan: string | null = null;
  protected kanalWysylki: string | null = null;
  protected szablonId: string | null = null;

  // Aplikacja jest zoneless — stan przebiegu musi być signalem, by @if/interpolacje się odświeżały.
  protected readonly przebiegi = signal<Record<TypAkcji, PrzebiegAkcji>>({
    'zmiana-stanu': this.pustyPrzebieg('zmiana-stanu'),
    'wysylka-pism': this.pustyPrzebieg('wysylka-pism'),
    'generowanie-dokumentow': this.pustyPrzebieg('generowanie-dokumentow'),
  });

  constructor() {
    this.destroyRef.onDestroy(() => {
      for (const interwal of this.interwaly.values()) {
        clearInterval(interwal);
      }
      this.interwaly.clear();
    });
  }

  private pustyPrzebieg(typ: TypAkcji): PrzebiegAkcji {
    return {
      typ,
      trwajaca: false,
      postep: 0,
      przetworzonychLacznie: 0,
      przetworzonychPomyslnie: 0,
      zakonczona: false,
    };
  }

  protected uruchomZmianeStanu(): void {
    if (this.wybraneZmianaStanu().length === 0 || !this.nowyStan) {
      return;
    }
    this.uruchomAkcje('zmiana-stanu', this.wybraneZmianaStanu().length);
  }

  protected uruchomWysylke(): void {
    if (this.wybraneWysylka().length === 0 || !this.kanalWysylki) {
      return;
    }
    this.uruchomAkcje('wysylka-pism', this.wybraneWysylka().length);
  }

  protected uruchomGenerowanie(): void {
    if (this.wybraneGenerowanie().length === 0 || !this.szablonId) {
      return;
    }
    this.uruchomAkcje('generowanie-dokumentow', this.wybraneGenerowanie().length);
  }

  // Symulacja publikacji akcji do RabbitMQ i asynchronicznego raportowania postępu przez workera.
  private uruchomAkcje(typ: TypAkcji, liczbaElementow: number): void {
    const istniejacy = this.interwaly.get(typ);
    if (istniejacy) {
      clearInterval(istniejacy);
    }

    this.przebiegi.update((aktualne) => ({
      ...aktualne,
      [typ]: {
        typ,
        trwajaca: true,
        postep: 0,
        przetworzonychLacznie: liczbaElementow,
        przetworzonychPomyslnie: 0,
        zakonczona: false,
      },
    }));

    const krokiDoZakonczenia = Math.ceil(3000 / KROK_MS);
    let krok = 0;

    const interwal = setInterval(() => {
      krok += 1;
      const postep = Math.min(100, Math.round((krok / krokiDoZakonczenia) * 100));

      if (postep >= 100) {
        clearInterval(interwal);
        this.interwaly.delete(typ);
        // Jeden element na symulowany błąd przetwarzania, dla realizmu raportu.
        const pomyslnie = liczbaElementow > 1 ? liczbaElementow - 1 : liczbaElementow;
        this.przebiegi.update((aktualne) => ({
          ...aktualne,
          [typ]: {
            ...aktualne[typ],
            postep: 100,
            trwajaca: false,
            zakonczona: true,
            przetworzonychPomyslnie: pomyslnie,
          },
        }));
        return;
      }

      this.przebiegi.update((aktualne) => ({
        ...aktualne,
        [typ]: { ...aktualne[typ], postep },
      }));
    }, KROK_MS);

    this.interwaly.set(typ, interwal);
  }

  protected wrocDoPrzegladu(): void {
    this.router.navigate(['/wymiana']);
  }
}
