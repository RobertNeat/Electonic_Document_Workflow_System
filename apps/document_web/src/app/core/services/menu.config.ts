import { PozycjaMenu } from '../models';

export const GLOWNE_MENU: PozycjaMenu[] = [
  { etykieta: 'Pulpit', ikona: 'pi pi-home', routerLink: '/pulpit' },
  { etykieta: 'Sprawy', ikona: 'pi pi-folder-open', routerLink: '/sprawy' },
  { etykieta: 'Obywatele', ikona: 'pi pi-users', routerLink: '/obywatele' },
  { etykieta: 'Dokumenty i szablony', ikona: 'pi pi-file', routerLink: '/dokumenty' },
  { etykieta: 'Korespondencja', ikona: 'pi pi-envelope', routerLink: '/korespondencja' },
  { etykieta: 'Płatności', ikona: 'pi pi-wallet', routerLink: '/platnosci' },
  { etykieta: 'Wymiana EDI / XML', ikona: 'pi pi-sync', routerLink: '/wymiana' },
  {
    etykieta: 'Administracja',
    ikona: 'pi pi-cog',
    role: ['administrator'],
    routerLink: '/administracja',
  },
];

export function kartaObywatelaMenu(obywatelId: string): PozycjaMenu[] {
  return [
    { etykieta: 'Dane podstawowe', ikona: 'pi pi-id-card', routerLink: `/obywatele/${obywatelId}` },
    { etykieta: 'Sprawy', ikona: 'pi pi-folder', routerLink: `/obywatele/${obywatelId}/sprawy` },
    {
      etykieta: 'Dokumenty',
      ikona: 'pi pi-file',
      routerLink: `/obywatele/${obywatelId}/dokumenty`,
    },
    {
      etykieta: 'Korespondencja',
      ikona: 'pi pi-envelope',
      routerLink: `/obywatele/${obywatelId}/korespondencja`,
    },
    {
      etykieta: 'Rozrachunki',
      ikona: 'pi pi-wallet',
      routerLink: `/obywatele/${obywatelId}/rozrachunki`,
    },
  ];
}
