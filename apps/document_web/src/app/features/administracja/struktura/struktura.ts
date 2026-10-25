import { Component, inject, signal } from '@angular/core';
import { TreeModule } from 'primeng/tree';
import { TreeNode } from 'primeng/api';
import { MessageModule } from 'primeng/message';
import { catchError, of } from 'rxjs';
import type { Wydzial } from '@electronic-document-workflow/document-shared-dto';
import { AdministracjaApi } from '../../../core/api/administracja.api';
import { komunikatBleduHttp } from '../../../core/api/http-error';

@Component({
  selector: 'app-struktura',
  standalone: true,
  imports: [TreeModule, MessageModule],
  templateUrl: './struktura.html',
  styleUrl: './struktura.css',
})
export class Struktura {
  private readonly api = inject(AdministracjaApi);

  readonly wezly = signal<TreeNode[]>([]);
  readonly wczytywanie = signal(true);
  readonly komunikatBledu = signal<string | null>(null);

  constructor() {
    this.wczytywanie.set(true);
    this.api
      .strukturaOrganizacyjna()
      .pipe(
        catchError((err: unknown) => {
          this.komunikatBledu.set(
            komunikatBleduHttp(err, 'Nie udało się wczytać struktury organizacyjnej.'),
          );
          return of(null as Wydzial[] | null);
        }),
      )
      .subscribe((wynik) => {
        this.wczytywanie.set(false);
        if (wynik !== null) {
          this.wezly.set(budujWezly(wynik));
        }
      });
  }
}

function budujWezly(wydzialy: Wydzial[]): TreeNode[] {
  return wydzialy.map((wydzial) => ({
    key: wydzial.id,
    label: wydzial.nazwa,
    icon: 'pi pi-building',
    data: { kierownik: wydzial.kierownik },
    expanded: true,
    children: wydzial.stanowiska.map((stanowisko) => ({
      key: stanowisko.id,
      label: stanowisko.nazwa,
      icon: 'pi pi-user',
    })),
  }));
}
