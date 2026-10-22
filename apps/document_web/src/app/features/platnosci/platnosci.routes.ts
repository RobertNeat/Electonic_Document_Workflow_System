import { Routes } from '@angular/router';

export const PLATNOSCI_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./platnosci-strona/platnosci-strona').then((m) => m.PlatnosciStrona),
  },
  {
    path: 'wplaty',
    loadComponent: () => import('./wplaty-lista/wplaty-lista').then((m) => m.WplatyLista),
  },
  {
    path: 'import-wyciagu',
    loadComponent: () => import('./import-wyciagu/import-wyciagu').then((m) => m.ImportWyciagu),
  },
  {
    path: ':id',
    loadComponent: () =>
      import('./naleznosc-szczegoly/naleznosc-szczegoly').then((m) => m.NaleznoscSzczegoly),
  },
];
