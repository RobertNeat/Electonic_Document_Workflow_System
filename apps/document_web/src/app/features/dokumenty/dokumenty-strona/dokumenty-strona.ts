import { Component, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { RouterLink, Router } from '@angular/router';
import { catchError, of, switchMap } from 'rxjs';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { SelectModule } from 'primeng/select';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { FormsModule } from '@angular/forms';
import type {
  DokumentListElement,
  StatusPodpisu,
  TypDokumentu,
} from '@electronic-document-workflow/document-shared-dto';
import { DokumentyApi } from '../../../core/api/dokumenty.api';
import { SprawyApi } from '../../../core/api/sprawy.api';
import { komunikatBleduHttp } from '../../../core/api/http-error';

@Component({
  selector: 'app-dokumenty-strona',
  imports: [
    RouterLink,
    TableModule,
    TagModule,
    SelectModule,
    ButtonModule,
    MessageModule,
    FormsModule,
  ],
  templateUrl: './dokumenty-strona.html',
  styleUrl: './dokumenty-strona.css',
})
export class DokumentyStrona {
  private readonly router = inject(Router);
  private readonly api = inject(DokumentyApi);
  private readonly sprawyApi = inject(SprawyApi);

  // withComponentInputBinding() wiąże query param po nazwie identycznie jak path param.
  readonly sprawaId = input<string>();

  protected readonly sprawaIdFiltr = computed(() => this.sprawaId() ?? null);

  protected readonly typFiltr = signal<string | null>(null);

  protected readonly typyDokumentow = signal<TypDokumentu[]>([]);

  protected readonly wczytywanie = signal(true);
  protected readonly komunikatBledu = signal<string | null>(null);
  protected readonly dokumenty = signal<DokumentListElement[]>([]);

  constructor() {
    // Słownik typów dokumentów do filtra - wczytywany raz, niezależnie od listy dokumentów;
    // błąd tego wywołania nie blokuje wyświetlenia tabeli (tylko filtr będzie puste).
    this.sprawyApi
      .typyDokumentow()
      .pipe(catchError(() => of([] as TypDokumentu[])))
      .subscribe((typy) => this.typyDokumentow.set(typy));

    // Filtrowanie po stronie backendu (oba parametry już wsparte przez GET /api/dokumenty) -
    // switchMap odrzuca odpowiedź nieaktualnego żądania, gdy filtry zmienią się szybciej niż
    // zdąży wrócić poprzednie zapytanie.
    toObservable(
      computed(() => ({ sprawaId: this.sprawaIdFiltr(), typDokumentuId: this.typFiltr() })),
    )
      .pipe(
        switchMap((filtry) => {
          this.wczytywanie.set(true);
          this.komunikatBledu.set(null);
          return this.api.lista(filtry).pipe(
            catchError((err: unknown) => {
              this.komunikatBledu.set(komunikatBleduHttp(err, 'Nie udało się wczytać dokumentów.'));
              return of(null as DokumentListElement[] | null);
            }),
          );
        }),
        takeUntilDestroyed(),
      )
      .subscribe((wynik) => {
        this.wczytywanie.set(false);
        if (wynik !== null) {
          this.dokumenty.set(wynik);
        }
      });
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

  protected wyczyscFiltrSprawy(): void {
    this.router.navigate([], { queryParams: { sprawaId: null }, queryParamsHandling: 'merge' });
  }

  protected otworzDokument(dokumentId: string): void {
    this.router.navigate(['/dokumenty', dokumentId]);
  }
}
