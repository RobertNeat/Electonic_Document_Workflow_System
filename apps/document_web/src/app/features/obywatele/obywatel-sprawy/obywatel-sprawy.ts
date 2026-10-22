import { Component, computed, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ObywateleDaneService } from '../obywatele-dane.service';
import { KartaObywatelaKontekst } from '../karta-obywatela-kontekst.service';

@Component({
  selector: 'app-obywatel-sprawy',
  imports: [TableModule, TagModule, RouterLink, DatePipe],
  templateUrl: './obywatel-sprawy.html',
  styleUrl: './obywatel-sprawy.css',
})
export class ObywatelSprawy {
  private readonly kontekst = inject(KartaObywatelaKontekst);
  private readonly dane = inject(ObywateleDaneService);

  protected readonly sprawy = computed(() => {
    const obywatel = this.kontekst.obywatel();
    return obywatel ? this.dane.sprawy(obywatel.id) : [];
  });
}
