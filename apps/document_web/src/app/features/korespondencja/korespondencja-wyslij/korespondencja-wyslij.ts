import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { SelectModule } from 'primeng/select';
import { SelectButtonModule } from 'primeng/selectbutton';
import { MessageModule } from 'primeng/message';
import { CardModule } from 'primeng/card';
import { catchError, of } from 'rxjs';
import { obywatelWyswietlanaNazwa } from '../../../core/models';
import type {
  KanalWysylki,
  Obywatel,
  Przesylka,
  Sprawa,
} from '@electronic-document-workflow/document-shared-dto';
import { SprawyApi } from '../../../core/api/sprawy.api';
import { ObywateleApi } from '../../../core/api/obywatele.api';
import { DokumentyApi } from '../../../core/api/dokumenty.api';
import { komunikatBleduHttp } from '../../../core/api/http-error';
import { KorespondencjaStore } from '../korespondencja.store';
import { ETYKIETY_KANALOW, OPISY_SKUTKOW_KANALOW_WYCHODZACA } from '../korespondencja.utils';

interface OpcjaWyboru {
  etykieta: string;
  wartosc: string;
}

interface OpcjaKanalu {
  etykieta: string;
  wartosc: KanalWysylki;
}

@Component({
  selector: 'app-korespondencja-wyslij',
  imports: [
    RouterLink,
    FormsModule,
    ButtonModule,
    SelectModule,
    SelectButtonModule,
    MessageModule,
    CardModule,
  ],
  templateUrl: './korespondencja-wyslij.html',
  styleUrl: './korespondencja-wyslij.css',
})
export class KorespondencjaWyslij {
  private readonly store = inject(KorespondencjaStore);
  private readonly router = inject(Router);
  private readonly sprawyApi = inject(SprawyApi);
  private readonly obywateleApi = inject(ObywateleApi);
  private readonly dokumentyApi = inject(DokumentyApi);

  // Formularz nadania tworzy wyłącznie przesyłki wychodzące.
  protected readonly opisySkutkow = OPISY_SKUTKOW_KANALOW_WYCHODZACA;

  protected readonly opcjeKanalow: OpcjaKanalu[] = [
    { etykieta: ETYKIETY_KANALOW['e-doreczenia'], wartosc: 'e-doreczenia' },
    { etykieta: ETYKIETY_KANALOW.operator_pocztowy, wartosc: 'operator_pocztowy' },
    { etykieta: ETYKIETY_KANALOW.email, wartosc: 'email' },
  ];

  protected readonly sprawaId = signal<string | null>(null);
  protected readonly dokumentId = signal<string | null>(null);
  protected readonly obywatelId = signal<string | null>(null);
  protected readonly kanal = signal<KanalWysylki>('e-doreczenia');
  protected readonly blad = signal<string | null>(null);
  protected readonly potwierdzenie = signal<Przesylka | null>(null);
  protected readonly wysylanie = signal(false);

  // Listy do rozwinięć - wczytane raz przy starcie formularza. rozmiarStrony: 200 dla spraw,
  // zgodnie z ustalonym w ObywatelSprawy precedensem, żeby nie uciąć listy na domyślnej stronie
  // 20 elementów (GET /api/sprawy domyślnie paginuje). GET /api/obywatele i GET /api/przesylki nie
  // paginują (ObywatelController.search zwraca pełny List bez Pageable) - brak takiej potrzeby tu.
  private readonly sprawy = signal<Sprawa[]>([]);
  protected readonly obywatele = signal<Obywatel[]>([]);
  protected readonly bladListSprawy = signal<string | null>(null);
  protected readonly bladListObywateli = signal<string | null>(null);
  protected readonly wczytywanieSlownikow = signal(true);

  protected readonly opcjeSpraw = computed<OpcjaWyboru[]>(() =>
    this.sprawy().map((s) => ({ etykieta: `${s.znakSprawy} — ${s.tytul}`, wartosc: s.id })),
  );

  protected readonly opcjeObywateli = computed<OpcjaWyboru[]>(() =>
    this.obywatele().map((o) => ({ etykieta: obywatelWyswietlanaNazwa(o), wartosc: o.id })),
  );

  // Dokumenty dla wybranej sprawy - dociągane asynchronicznie przy zmianie sprawy (wybierzSprawe).
  protected readonly opcjeDokumentow = signal<OpcjaWyboru[]>([]);
  protected readonly wczytywanieDokumentow = signal(false);
  protected readonly bladDokumentow = signal<string | null>(null);

  protected readonly adresatMail = computed(() => {
    const id = this.obywatelId();
    return this.obywatele().find((o) => o.id === id)?.email ?? null;
  });

  // UI-owe ostrzeżenie uprzedzające - backend (KorespondencjaService.nadajPismo) blokuje
  // ten przypadek (400), to ostrzeżenie tylko informuje przed wysłaniem żądania.
  protected readonly adresatAde = computed(() => {
    const id = this.obywatelId();
    return this.obywatele().find((o) => o.id === id)?.adresDoreczenEDE ?? null;
  });

  constructor() {
    this.wczytywanieSlownikow.set(true);
    this.sprawyApi
      .szukaj({ rozmiarStrony: 200 })
      .pipe(
        catchError((err: unknown) => {
          this.bladListSprawy.set(komunikatBleduHttp(err, 'Nie udało się wczytać listy spraw.'));
          return of(null);
        }),
      )
      .subscribe((strona) => {
        if (strona) this.sprawy.set(strona.dane);
      });

    this.obywateleApi
      .szukaj()
      .pipe(
        catchError((err: unknown) => {
          this.bladListObywateli.set(
            komunikatBleduHttp(err, 'Nie udało się wczytać listy obywateli.'),
          );
          return of(null);
        }),
      )
      .subscribe((wynik) => {
        if (wynik) this.obywatele.set(wynik);
        this.wczytywanieSlownikow.set(false);
      });
  }

  wybierzSprawe(id: string | null): void {
    this.sprawaId.set(id);
    this.dokumentId.set(null);
    this.opcjeDokumentow.set([]);
    this.bladDokumentow.set(null);
    if (!id) {
      return;
    }

    // Adresat domyślnie wynika ze sprawy (z listy już wczytanej dla rozwinięcia), ale użytkownik
    // może go później zmienić ręcznie.
    const sprawa = this.sprawy().find((s) => s.id === id);
    this.obywatelId.set(sprawa?.obywatelId ?? null);

    this.wczytywanieDokumentow.set(true);
    this.dokumentyApi
      .lista({ sprawaId: id })
      .pipe(
        catchError((err: unknown) => {
          this.bladDokumentow.set(
            komunikatBleduHttp(err, 'Nie udało się wczytać dokumentów sprawy.'),
          );
          return of(null);
        }),
      )
      .subscribe((dokumenty) => {
        this.wczytywanieDokumentow.set(false);
        if (!dokumenty) {
          return;
        }
        // Tylko podpisana, aktualna wersja dokumentu może być nadana - statusPodpisuAktualnej już
        // zawiera wynik tej weryfikacji z backendu (zob. DokumentListElement). Uwaga: to kryterium
        // jest ściślejsze niż serwerowe `WersjaDokumentuEntity.isPodpisana()` samo w sobie - wersja
        // podpisana z WYGASŁYM certyfikatem (statusPodpisu != 'poprawny') jest tu odfiltrowana, choć
        // backend `nadajPismo` by ją przyjął. Zgodnie z poleceniem cyklu, nie rozszerzane dalej.
        if (this.sprawaId() !== id) {
          return; // sprawa zmieniła się w międzyczasie - odpowiedź jest już nieaktualna.
        }
        this.opcjeDokumentow.set(
          dokumenty
            .filter((d) => d.statusPodpisuAktualnej === 'poprawny')
            .map((d) => ({ etykieta: d.nazwa, wartosc: d.id })),
        );
      });
  }

  wyslij(): void {
    if (!this.sprawaId() || !this.dokumentId() || !this.obywatelId()) {
      this.blad.set('Wybierz sprawę, podpisany dokument oraz adresata.');
      return;
    }
    this.blad.set(null);
    this.wysylanie.set(true);
    this.store
      .nadaj({
        sprawaId: this.sprawaId()!,
        dokumentId: this.dokumentId()!,
        obywatelId: this.obywatelId()!,
        kanal: this.kanal(),
      })
      .then((wynik) => {
        this.wysylanie.set(false);
        if (typeof wynik === 'string') {
          this.blad.set(wynik);
          return;
        }
        this.potwierdzenie.set(wynik);
      });
  }

  przejdzDoSzczegolow(): void {
    const przesylka = this.potwierdzenie();
    if (przesylka) {
      this.router.navigate(['/korespondencja', przesylka.id]);
    }
  }
}
