import { Component, computed, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { TableModule } from 'primeng/table';
import { ObywateleDaneService } from '../obywatele-dane.service';
import { KartaObywatelaKontekst } from '../karta-obywatela-kontekst.service';

@Component({
  selector: 'app-obywatel-dokumenty',
  imports: [TableModule, DatePipe],
  templateUrl: './obywatel-dokumenty.html',
  styleUrl: './obywatel-dokumenty.css',
})
export class ObywatelDokumenty {
  private readonly kontekst = inject(KartaObywatelaKontekst);
  private readonly dane = inject(ObywateleDaneService);

  protected readonly dokumenty = computed(() => {
    const obywatel = this.kontekst.obywatel();
    return obywatel ? this.dane.dokumenty(obywatel.id) : [];
  });
}
