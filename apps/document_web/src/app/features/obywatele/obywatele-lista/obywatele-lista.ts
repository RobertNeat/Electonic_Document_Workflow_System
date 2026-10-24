import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { catchError, debounceTime, of, switchMap } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { obywatelWyswietlanaNazwa } from '../../../core/models';
import type { Obywatel } from '@electronic-document-workflow/document-shared-dto';
import { komunikatBleduHttp } from '../../../core/api/http-error';
import { ObywateleDaneService } from '../obywatele-dane.service';

@Component({
  selector: 'app-obywatele-lista',
  imports: [
    FormsModule,
    TableModule,
    TagModule,
    InputTextModule,
    IconFieldModule,
    InputIconModule,
    ButtonModule,
    TooltipModule,
    MessageModule,
  ],
  templateUrl: './obywatele-lista.html',
  styleUrl: './obywatele-lista.css',
})
export class ObywateleLista {
  private readonly dane = inject(ObywateleDaneService);
  private readonly router = inject(Router);

  protected readonly zapytanie = signal('');
  protected readonly obywatele = signal<Obywatel[]>([]);
  // Startuje jako true - pierwsze żądanie odpala po debounce (300ms), żeby nie pokazać "brak
  // wyników" na mgnienie oka przed faktycznym wczytaniem listy.
  protected readonly wczytywanie = signal(true);
  protected readonly komunikatBledu = signal<string | null>(null);

  protected readonly wybrani = signal<Obywatel[]>([]);

  protected readonly obywatelWyswietlanaNazwa = obywatelWyswietlanaNazwa;

  constructor() {
    toObservable(this.zapytanie)
      .pipe(
        debounceTime(300),
        switchMap((zapytanie) => {
          this.wczytywanie.set(true);
          this.komunikatBledu.set(null);
          return this.dane.szukaj(zapytanie).pipe(
            catchError((err: unknown) => {
              this.komunikatBledu.set(
                komunikatBleduHttp(err, 'Nie udało się wczytać listy obywateli.'),
              );
              return of<Obywatel[]>([]);
            }),
          );
        }),
        takeUntilDestroyed(),
      )
      .subscribe((wynik) => {
        this.obywatele.set(wynik);
        this.wczytywanie.set(false);
      });
  }

  protected otworz(obywatel: Obywatel): void {
    this.router.navigate(['/obywatele', obywatel.id]);
  }
}
