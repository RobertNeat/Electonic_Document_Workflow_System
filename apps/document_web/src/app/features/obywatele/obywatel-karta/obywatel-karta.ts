import { Component, effect, inject, input } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { TagModule } from 'primeng/tag';
import { obywatelWyswietlanaNazwa } from '../../../core/models';
import { CitizenContextService } from '../../../core/services/citizen-context.service';
import { ObywateleDaneService } from '../obywatele-dane.service';
import { KartaObywatelaKontekst } from '../karta-obywatela-kontekst.service';

/**
 * Wspólny nagłówek karty obywatela dla wszystkich pod-tras (dane/sprawy/
 * dokumenty/korespondencja/rozrachunki). Pobiera obywatela jednorazowo
 * i udostępnia go dzieciom przez KartaObywatelaKontekst, żeby uniknąć
 * powtórnego odczytu parametru trasy i zapytania o dane w każdej pod-stronie.
 */
@Component({
  selector: 'app-obywatel-karta',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, TagModule],
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

  constructor() {
    effect(() => {
      const id = this.obywatelId();
      const obywatel = id ? (this.dane.znajdz(id) ?? null) : null;
      this.kontekst.ustaw(obywatel);
      if (obywatel) {
        this.citizenContext.przypnij(obywatel);
      }
    });
  }
}
