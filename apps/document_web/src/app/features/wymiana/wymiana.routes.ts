import { Routes } from '@angular/router';

export const WYMIANA_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./wymiana-strona/wymiana-strona').then((m) => m.WymianaStrona),
  },
  {
    path: 'import-xml',
    loadComponent: () => import('./import-xml/import-xml').then((m) => m.ImportXml),
  },
  {
    path: 'import-ubl',
    loadComponent: () => import('./import-ubl/import-ubl').then((m) => m.ImportUbl),
  },
  {
    // Zachowany jako link bezpośredni na zakładkę "Akcje zbiorowe" (np. z dokumentacji) -
    // renderuje tę samą stronę co "", z zakładką ustawioną przez dane trasy.
    path: 'akcje-zbiorowe',
    loadComponent: () => import('./wymiana-strona/wymiana-strona').then((m) => m.WymianaStrona),
    data: { zakladka: 'akcje-zbiorowe' },
  },
];
