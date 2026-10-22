import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { SelectModule } from 'primeng/select';
import { InputTextModule } from 'primeng/inputtext';
import { SprawyStore } from '../sprawy.store';
import { seweroscStanu, statusTerminu } from '../sprawy.utils';

@Component({
  selector: 'app-sprawy-strona',
  imports: [FormsModule, TableModule, TagModule, ButtonModule, SelectModule, InputTextModule],
  templateUrl: './sprawy-strona.html',
  styleUrl: './sprawy-strona.css',
})
export class SprawyStrona {
  private readonly store = inject(SprawyStore);
  private readonly router = inject(Router);

  protected readonly sprawy = this.store.sprawy;
  protected readonly seweroscStanu = seweroscStanu;
  protected readonly statusTerminu = statusTerminu;

  protected readonly szukaj = signal('');
  protected readonly filtrStan = signal<string | null>(null);
  protected readonly filtrWydzial = signal<string | null>(null);

  protected readonly opcjeStanow = computed(() =>
    Array.from(new Set(this.sprawy().map((s) => s.stan))).sort(),
  );
  protected readonly opcjeWydzialow = computed(() =>
    Array.from(new Set(this.sprawy().map((s) => s.wydzial))).sort(),
  );

  protected readonly przefiltrowane = computed(() => {
    const fraza = this.szukaj().trim().toLowerCase();
    const stan = this.filtrStan();
    const wydzial = this.filtrWydzial();
    return this.sprawy().filter((s) => {
      if (stan && s.stan !== stan) return false;
      if (wydzial && s.wydzial !== wydzial) return false;
      if (
        fraza &&
        !s.znakSprawy.toLowerCase().includes(fraza) &&
        !s.tytul.toLowerCase().includes(fraza)
      ) {
        return false;
      }
      return true;
    });
  });

  protected otworzSprawe(id: string): void {
    this.router.navigate(['/sprawy', id]);
  }

  protected nowaSprawa(): void {
    this.router.navigate(['/sprawy', 'nowa']);
  }

  protected wyczyscFiltry(): void {
    this.szukaj.set('');
    this.filtrStan.set(null);
    this.filtrWydzial.set(null);
  }
}
