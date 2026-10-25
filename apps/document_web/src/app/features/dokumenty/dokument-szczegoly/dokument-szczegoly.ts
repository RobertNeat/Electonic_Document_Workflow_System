import { Component, computed, inject, input, linkedSignal, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { catchError, filter, forkJoin, of, switchMap } from 'rxjs';
import { TimelineModule } from 'primeng/timeline';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { MessageModule } from 'primeng/message';
import { RadioButtonModule } from 'primeng/radiobutton';
import type {
  Certyfikat,
  Dokument,
  Sprawa,
  StatusPodpisu,
  SzczegolyPodpisu,
  TypDokumentu,
  WersjaDokumentu,
} from '@electronic-document-workflow/document-shared-dto';
import { DokumentyApi } from '../../../core/api/dokumenty.api';
import { SprawyApi } from '../../../core/api/sprawy.api';
import { komunikatBleduHttp } from '../../../core/api/http-error';

interface DaneSzczegolow {
  dokument: Dokument | null;
  wersje: WersjaDokumentu[];
  sprawa: Sprawa | undefined;
  typDokumentu: TypDokumentu | undefined;
  status: 'ladowanie' | 'ok' | 'nieznaleziony' | 'blad';
  komunikatBledu: string | null;
}

const STAN_LADOWANIA: DaneSzczegolow = {
  dokument: null,
  wersje: [],
  sprawa: undefined,
  typDokumentu: undefined,
  status: 'ladowanie',
  komunikatBledu: null,
};

@Component({
  selector: 'app-dokument-szczegoly',
  imports: [
    RouterLink,
    FormsModule,
    TimelineModule,
    TagModule,
    ButtonModule,
    DialogModule,
    MessageModule,
    RadioButtonModule,
  ],
  templateUrl: './dokument-szczegoly.html',
  styleUrl: './dokument-szczegoly.css',
})
export class DokumentSzczegoly {
  private readonly api = inject(DokumentyApi);
  private readonly sprawyApi = inject(SprawyApi);

  readonly id = input<string>();

  private readonly dane = signal<DaneSzczegolow>(STAN_LADOWANIA);

  protected readonly dokument = computed(() => this.dane().dokument);
  protected readonly sprawa = computed(() => this.dane().sprawa);
  protected readonly typDokumentu = computed(() => this.dane().typDokumentu);
  protected readonly status = computed(() => this.dane().status);
  protected readonly komunikatBledu = computed(() => this.dane().komunikatBledu);

  // lokalny sygnał wersji — przeliczany (sortowanie malejące) z `dane().wersje`; potwierdzPodpis()
  // dokłada nową wersję do `dane`, nie do tego sygnału, żeby linkedSignal przesortował ją na
  // właściwe miejsce w timeline (zob. potwierdzPodpis). linkedSignal (nie computed), bo musi się
  // przeliczyć na nowo przy zmianie dokumentu (np. nawigacja do innego dokumentu z tej samej
  // instancji komponentu).
  protected readonly wersje = linkedSignal<WersjaDokumentu[]>(() =>
    [...this.dane().wersje].sort((a, b) => b.wersja - a.wersja),
  );

  protected readonly podgladWidoczny = signal(false);
  protected readonly podgladWersja = signal<WersjaDokumentu | null>(null);

  protected readonly podpisWidoczny = signal(false);
  protected readonly podpisSzczegoly = signal<SzczegolyPodpisu | null>(null);

  // Cert-picker: otwierany z konkretną wersją do podpisania (certPodpisWersja), lista
  // certyfikatów dociągana z document_api (proxy do sign_service) przy każdym otwarciu.
  protected readonly certPodpisWidoczny = signal(false);
  protected readonly certPodpisWersja = signal<WersjaDokumentu | null>(null);
  protected readonly certyfikaty = signal<Certyfikat[]>([]);
  protected readonly certyfikatWybrany = signal<string | null>(null);
  protected readonly certWczytywanie = signal(false);
  protected readonly certBladListy = signal<string | null>(null);
  protected readonly certPodpisywanie = signal(false);
  protected readonly certBladPodpisu = signal<string | null>(null);

  constructor() {
    toObservable(this.id)
      .pipe(
        filter((id): id is string => !!id),
        switchMap((id) => {
          this.dane.set(STAN_LADOWANIA);
          return this.api.getById(id).pipe(
            switchMap((dokument) =>
              // Sprawa/typ dokumentu są metadanymi pomocniczymi do nagłówka - błąd ich wczytania
              // nie powinien zablokować wyświetlenia samej listy wersji. Lista wersji jest głównym
              // elementem strony (timeline) - jej błąd ZOSTAJE przekazany dalej (catchError poniżej
              // na całym pipeline), żeby nie pokazać puste osi czasu zamiast komunikatu błędu.
              forkJoin({
                wersje: this.api.wersje(id),
                sprawa: this.sprawyApi
                  .getById(dokument.sprawaId)
                  .pipe(catchError(() => of(undefined))),
                typy: this.sprawyApi
                  .typyDokumentow()
                  .pipe(catchError(() => of([] as TypDokumentu[]))),
              }).pipe(
                switchMap((zalezne) =>
                  of<DaneSzczegolow>({
                    dokument,
                    wersje: zalezne.wersje,
                    sprawa: zalezne.sprawa,
                    typDokumentu: zalezne.typy.find((t) => t.id === dokument.typDokumentuId),
                    status: 'ok',
                    komunikatBledu: null,
                  }),
                ),
              ),
            ),
            catchError((err: unknown) => {
              if (err instanceof HttpErrorResponse && err.status === 404) {
                return of<DaneSzczegolow>({ ...STAN_LADOWANIA, status: 'nieznaleziony' });
              }
              return of<DaneSzczegolow>({
                ...STAN_LADOWANIA,
                status: 'blad',
                komunikatBledu: komunikatBleduHttp(err, 'Nie udało się wczytać dokumentu.'),
              });
            }),
          );
        }),
        takeUntilDestroyed(),
      )
      .subscribe((dane) => this.dane.set(dane));
  }

  protected etykietaStatusu(status: StatusPodpisu): string {
    switch (status) {
      case 'poprawny':
        return 'Poprawny';
      case 'niepoprawny':
        return 'Niepoprawny';
      case 'certyfikat_wygasly':
        return 'Certyfikat wygasły';
      case 'certyfikat_uniewazniony':
        return 'Certyfikat unieważniony';
      default:
        return 'Brak (wersja robocza)';
    }
  }

  protected kolorStatusu(status: StatusPodpisu): 'success' | 'danger' | 'secondary' {
    if (status === 'poprawny') return 'success';
    if (status === 'brak') return 'secondary';
    return 'danger';
  }

  protected podgladPdf(wersja: WersjaDokumentu): void {
    this.podgladWersja.set(wersja);
    this.podgladWidoczny.set(true);
  }

  protected pobierz(wersja: WersjaDokumentu): void {
    // mock — brak realnego pliku do pobrania, backend dms_service dostarczy binaria
    alert(`Pobieranie wersji ${wersja.wersja} — funkcja zostanie podłączona do dms_service.`);
  }

  // Otwiera cert-picker dla aktualnej (niepodpisanej) wersji dokumentu - backend podpisuje
  // zawsze aktualnaWersja, nigdy konkretną historyczną wersję (zob. szablon - przycisk "Podpisz"
  // pokazuje się tylko na wersji == dokument().aktualnaWersja) - i dociąga listę certyfikatów z
  // document_api (proxy do sign_service) - WSZYSTKIE certyfikaty, bez filtrowania po tożsamości
  // użytkownika (zob. plan.md - brak modelu użytkownik↔certyfikat w systemie).
  protected podpisz(wersja: WersjaDokumentu): void {
    this.certPodpisWersja.set(wersja);
    this.certyfikatWybrany.set(null);
    this.certBladPodpisu.set(null);
    this.certBladListy.set(null);
    this.certPodpisWidoczny.set(true);
    this.certWczytywanie.set(true);
    this.api.listaCertyfikatow().subscribe({
      next: (lista) => {
        this.certyfikaty.set(lista);
        this.certWczytywanie.set(false);
      },
      error: (err: unknown) => {
        this.certBladListy.set(
          komunikatBleduHttp(err, 'Nie udało się wczytać listy certyfikatów.'),
        );
        this.certWczytywanie.set(false);
      },
    });
  }

  // `>` (nie `>=`) i data lokalna - zgodnie z kontraktem sign_service: `data_wygasniecia > dziś`
  // (BadRequest przy podpisaniu, jeśli certyfikat wygasa tego samego dnia). UTC (toISOString) by
  // w okolicy północy mógł różnić się od polskiej daty lokalnej.
  protected certyfikatAktywny(certyfikat: Certyfikat): boolean {
    return certyfikat.status === 'aktywny' && certyfikat.dataWygasniecia > this.dzisiejszaData();
  }

  private dzisiejszaData(): string {
    const dzis = new Date();
    const rok = dzis.getFullYear();
    const miesiac = String(dzis.getMonth() + 1).padStart(2, '0');
    const dzien = String(dzis.getDate()).padStart(2, '0');
    return `${rok}-${miesiac}-${dzien}`;
  }

  protected zamknijCertPicker(): void {
    this.certPodpisWidoczny.set(false);
    this.certPodpisWersja.set(null);
  }

  // Potwierdzenie wyboru certyfikatu - realne wywołanie POST /api/dokumenty/:id/podpisz. Po
  // sukcesie dokłada NOWĄ wersję zwróconą przez backend (aktualnaWersja+1) do `dane` (nie do
  // `wersje` - ten sygnał jest WYLICZANY z `dane().wersje` przez linkedSignal i musiałby się
  // zresetować, gdyby `dane` zmieniło się po nim, co wymazałoby dołożoną wersję). Aktualizuje też
  // `dokument.aktualnaWersja`, żeby przycisk "Podpisz" przestał być dostępny na starej wersji
  // (zob. szablon - gating po aktualnaWersja) - backend i tak odrzuciłby próbę podpisania wersji,
  // która nie jest już aktualna.
  protected potwierdzPodpis(): void {
    const dokument = this.dokument();
    const certyfikatId = this.certyfikatWybrany();
    if (!dokument || !certyfikatId) {
      return;
    }
    this.certBladPodpisu.set(null);
    this.certPodpisywanie.set(true);
    this.api.podpisz(dokument.id, certyfikatId).subscribe({
      next: (nowaWersja) => {
        this.certPodpisywanie.set(false);
        this.dane.update((d) => ({
          ...d,
          dokument: d.dokument ? { ...d.dokument, aktualnaWersja: nowaWersja.wersja } : d.dokument,
          wersje: [...d.wersje, nowaWersja],
        }));
        this.zamknijCertPicker();
      },
      error: (err: unknown) => {
        this.certPodpisywanie.set(false);
        this.certBladPodpisu.set(komunikatBleduHttp(err, 'Nie udało się podpisać dokumentu.'));
      },
    });
  }

  protected szczegolyPodpisu(wersja: WersjaDokumentu): void {
    // mock — GET /api/dokumenty/:id/wersje/:wersja/podpis nie istnieje jeszcze (zob. plan.md);
    // SzczegolyPodpisu jest wypełniane tu z danych wersji, nie z realnego podpisu PAdES.
    this.podpisSzczegoly.set({
      podpisujacy: `${wersja.autor} (CN=${wersja.autor.replace(' ', '.')}, O=Urząd Miasta)`,
      czasPodpisu: `${wersja.dataUtworzenia}T12:34:56Z`,
      odciskCertyfikatu: 'A1:3F:9B:22:E4:5C:7D:11:88:0A:CC:3E:6F:2B:17:D9:4A:5E:81:BC',
      statusOcsp:
        wersja.statusPodpisu === 'poprawny'
          ? 'Aktywny (potwierdzony online)'
          : 'Odpowiedź negatywna',
      statusCrl:
        wersja.statusPodpisu === 'certyfikat_uniewazniony'
          ? 'Certyfikat na liście CRL'
          : 'Brak na liście CRL',
      znacznikCzasuTsa: `${wersja.dataUtworzenia}T12:34:57Z (TSA: Urząd Certyfikacji Krajowej)`,
    });
    this.podpisWidoczny.set(true);
  }
}
