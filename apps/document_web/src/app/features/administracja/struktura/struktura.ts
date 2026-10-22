import { Component } from '@angular/core';
import { TreeModule } from 'primeng/tree';
import { TreeNode } from 'primeng/api';
import { MOCK_WYDZIALY } from '../mock/admin-mock-data';

@Component({
  selector: 'app-struktura',
  standalone: true,
  imports: [TreeModule],
  templateUrl: './struktura.html',
  styleUrl: './struktura.css',
})
export class Struktura {
  readonly wezly: TreeNode[] = MOCK_WYDZIALY.map((wydzial) => ({
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
