import { Routes } from '@angular/router';

export const ADMINISTRACJA_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./administracja-strona/administracja-strona').then((m) => m.AdministracjaStrona),
  },
  {
    path: 'uzytkownicy',
    loadComponent: () => import('./uzytkownicy/uzytkownicy').then((m) => m.Uzytkownicy),
  },
  {
    path: 'struktura',
    loadComponent: () => import('./struktura/struktura').then((m) => m.Struktura),
  },
  {
    path: 'slowniki',
    loadComponent: () => import('./slowniki/slowniki').then((m) => m.Slowniki),
  },
  {
    path: 'obiegi',
    loadComponent: () => import('./obiegi/obiegi').then((m) => m.Obiegi),
  },
  {
    path: 'dziennik-zdarzen',
    loadComponent: () =>
      import('./dziennik-zdarzen/dziennik-zdarzen').then((m) => m.DziennikZdarzen),
  },
];
