import { Component, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { catchError, filter, of, switchMap } from 'rxjs';
import { TagModule } from 'primeng/tag';
import { MessageModule } from 'primeng/message';
import type { Obywatel } from '@electronic-document-workflow/document-shared-dto';
import { obywatelWyswietlanaNazwa } from '../../../core/models';
import { CitizenContextService } from '../../../core/services/citizen-context.service';
import { komunikatBleduHttp } from '../../../core/api/http-error';
import { ObywateleDaneService } from '../obywatele-dane.service';
import { KartaObywatelaKontekst } from '../karta-obywatela-kontekst.service';

type StatusWczytywania = 'ladowanie' | 'ok' | 'nieznaleziony' | 'blad';

/**
 * Wspólny nagłówek karty obywatela dla wszystkich pod-tras (dane/sprawy/
 * dokumenty/korespondencja/rozrachunki). Pobiera obywatela jednorazowo
 * i udostępnia go dzieciom przez KartaObywatelaKontekst, żeby uniknąć
 * powtórnego odczytu parametru trasy i zapytania o dane w każdej pod-stronie.
 */
@Component({
  selector: 'app-obywatel-karta',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, TagModule, MessageModule],
  providers: [KartaObywatelaKontekst],
  templateUrl: './obywatel-karta.html',
  styleUrl: './obywatel-karta.css',
})
export class ObywatelKarta {
  private readonly dane = inject(ObywateleDaneService);
  private readonly citizenContext = inject(CitizenContextService);

  protected readonly kontekst = inject(KartaObywatelaKontekst);
  protected readonly obywatelWyswietlanaNazwa = obywatelWyswietlanaNazwa;

  // nazwa inputu musi być 'id' — withComponentInputBinding() wiąże po nazwie parametru trasy (':id').
  // Wewnętrzna nazwa obywatelId zachowana jako alias sygnału, żeby nie zmieniać reszty pliku.
  readonly id = input<string>();
  private readonly obywatelId = this.id;

  protected readonly podstrony = [
    { etykieta: 'Dane podstawowe', ikona: 'pi pi-id-card', path: '' },
    { etykieta: 'Sprawy', ikona: 'pi pi-folder', path: 'sprawy' },
    { etykieta: 'Dokumenty', ikona: 'pi pi-file', path: 'dokumenty' },
    { etykieta: 'Korespondencja', ikona: 'pi pi-envelope', path: 'korespondencja' },
    { etykieta: 'Rozrachunki', ikona: 'pi pi-wallet', path: 'rozrachunki' },
  ];

  private readonly status = signal<StatusWczytywania>('ladowanie');
  protected readonly wczytywanie = computed(() => this.status() === 'ladowanie');
  protected readonly nieznaleziony = computed(() => this.status() === 'nieznaleziony');
  protected readonly komunikatBledu = signal<string | null>(null);

  constructor() {
    toObservable(this.obywatelId)
      .pipe(
        filter((id): id is string => !!id),
        switchMap((id) => {
          this.status.set('ladowanie');
          this.komunikatBledu.set(null);
          this.kontekst.ustaw(null);
          return this.dane.znajdz(id).pipe(
            catchError((err: unknown) => {
              if (err instanceof HttpErrorResponse && err.status === 404) {
                this.status.set('nieznaleziony');
              } else {
                this.status.set('blad');
                this.komunikatBledu.set(
                  komunikatBleduHttp(err, 'Nie udało się wczytać danych obywatela.'),
                );
              }
              return of<Obywatel | null>(null);
            }),
          );
        }),
        takeUntilDestroyed(),
      )
      .subscribe((obywatel) => {
        this.kontekst.ustaw(obywatel);
        if (obywatel) {
          this.status.set('ok');
          this.citizenContext.przypnij(obywatel);
        }
      });
  }
}
