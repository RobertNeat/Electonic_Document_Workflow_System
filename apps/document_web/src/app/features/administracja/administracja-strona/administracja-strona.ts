import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CardModule } from 'primeng/card';

interface KartaSekcji {
  tytul: string;
  opis: string;
  ikona: string;
  routerLink: string;
}

@Component({
  selector: 'app-administracja-strona',
  standalone: true,
  imports: [RouterLink, CardModule],
  templateUrl: './administracja-strona.html',
  styleUrl: './administracja-strona.css',
})
export class AdministracjaStrona {
  readonly sekcje: KartaSekcji[] = [
    {
      tytul: 'Użytkownicy i role',
      opis: 'Zarządzanie kontami użytkowników, rolami RBAC, przypisaniem do wydziałów.',
      ikona: 'pi pi-users',
      routerLink: 'uzytkownicy',
    },
    {
      tytul: 'Struktura organizacyjna',
      opis: 'Wydziały i stanowiska w strukturze urzędu.',
      ikona: 'pi pi-sitemap',
      routerLink: 'struktura',
    },
    {
      tytul: 'Słowniki',
      opis: 'JRWA, typy dokumentów, dostawcy wysyłki, stawki opłat.',
      ikona: 'pi pi-book',
      routerLink: 'slowniki',
    },
    {
      tytul: 'Definicje obiegów',
      opis: 'Konfigurowalne obiegi spraw: stany, przejścia, role, dokumenty.',
      ikona: 'pi pi-share-alt',
      routerLink: 'obiegi',
    },
    {
      tytul: 'Dziennik zdarzeń',
      opis: 'Audit log operacji na sprawach, dokumentach i płatnościach.',
      ikona: 'pi pi-history',
      routerLink: 'dziennik-zdarzen',
    },
  ];
}
