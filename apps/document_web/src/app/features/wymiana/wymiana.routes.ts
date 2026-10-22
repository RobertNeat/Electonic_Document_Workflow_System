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
    path: 'akcje-zbiorowe',
    loadComponent: () => import('./akcje-zbiorowe/akcje-zbiorowe').then((m) => m.AkcjeZbiorowe),
  },
];
