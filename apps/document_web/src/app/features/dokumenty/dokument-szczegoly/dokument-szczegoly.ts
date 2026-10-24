import { Component, computed, inject, input, linkedSignal, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { catchError, filter, forkJoin, of, switchMap } from 'rxjs';
import { TimelineModule } from 'primeng/timeline';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { MessageModule } from 'primeng/message';
import type {
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
  imports: [RouterLink, TimelineModule, TagModule, ButtonModule, DialogModule, MessageModule],
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

  // lokalny sygnał wersji — pozwala mockowo "podpisać" wersję bez backendu (zob. podpisz());
  // linkedSignal (nie computed), bo podpisz() go mutuje, a musi się przeliczyć na nowo przy
  // zmianie dokumentu (np. nawigacja do innego dokumentu z tej samej instancji komponentu).
  protected readonly wersje = linkedSignal<WersjaDokumentu[]>(() =>
    [...this.dane().wersje].sort((a, b) => b.wersja - a.wersja),
  );

  protected readonly podgladWidoczny = signal(false);
  protected readonly podgladWersja = signal<WersjaDokumentu | null>(null);

  protected readonly podpisWidoczny = signal(false);
  protected readonly podpisSzczegoly = signal<SzczegolyPodpisu | null>(null);

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

  // mockowa aktualizacja lokalna — `POST /api/dokumenty/:id/podpisz` JEST już zaimplementowany
  // w backendzie (podpis PAdES przez sign_service), ale wymaga `certyfikatId` w żądaniu; front nie
  // ma jeszcze wyboru certyfikatu (brak decyzji produktowej, które certyfikaty może użyć dany
  // użytkownik, oraz brak GET /api/certyfikaty do zasilenia takiego wyboru) - zob. plan.md. Realny
  // endpoint tworzy NOWĄ wersję (aktualnaWersja+1), a nie modyfikuje istniejącą - ta mutacja in-place
  // jest więc tylko przybliżeniem UX do czasu podłączenia prawdziwego wywołania.
  protected podpisz(wersja: WersjaDokumentu): void {
    this.wersje.update((lista) =>
      lista.map((w) =>
        w.id === wersja.id
          ? { ...w, podpisana: true, statusPodpisu: 'poprawny' as StatusPodpisu }
          : w,
      ),
    );
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
