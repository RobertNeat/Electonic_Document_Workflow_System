import { Routes } from '@angular/router';

export const KORESPONDENCJA_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./korespondencja-strona/korespondencja-strona').then((m) => m.KorespondencjaStrona),
  },
  {
    path: 'wyslij',
    loadComponent: () =>
      import('./korespondencja-wyslij/korespondencja-wyslij').then((m) => m.KorespondencjaWyslij),
  },
  {
    path: ':id',
    loadComponent: () =>
      import('./korespondencja-szczegoly/korespondencja-szczegoly').then(
        (m) => m.KorespondencjaSzczegoly,
      ),
  },
];
