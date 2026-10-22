import { Component, inject } from '@angular/core';
import { KartaObywatelaKontekst } from '../karta-obywatela-kontekst.service';

@Component({
  selector: 'app-obywatel-dane',
  templateUrl: './obywatel-dane.html',
  styleUrl: './obywatel-dane.css',
})
export class ObywatelDane {
  protected readonly kontekst = inject(KartaObywatelaKontekst);
}
