import type {
  KanalWysylki,
  StatusPrzesylki,
} from '@electronic-document-workflow/document-shared-dto';

/**
 * Kierunek przesyłki — typ przełącznika zakładek UI (nie alias `Przesylka['kierunek']`,
 * choć ma tę samą unię wartości). Służy do filtrowania `przesylki()` po polu modelu.
 */
export type Kierunek = 'wychodzaca' | 'wplywajaca';

export const ETYKIETY_KANALOW: Record<KanalWysylki, string> = {
  'e-doreczenia': 'e-Doręczenia',
  operator_pocztowy: 'Operator pocztowy',
  email: 'E-mail',
};

export const IKONY_KANALOW: Record<KanalWysylki, string> = {
  'e-doreczenia': 'pi pi-shield',
  operator_pocztowy: 'pi pi-truck',
  email: 'pi pi-at',
};

export const OPISY_SKUTKOW_KANALOW_WYCHODZACA: Record<KanalWysylki, string> = {
  'e-doreczenia':
    'Doręczenie elektroniczne (ADE) — pełny skutek prawny doręczenia, potwierdzony znacznikiem czasu.',
  operator_pocztowy:
    'Przesyłka hybrydowa — druk i nadanie listu poleconego z potwierdzeniem odbioru (ZPO).',
  email: 'Kanał pomocniczy — informacyjny, bez skutków prawnych doręczenia.',
};

export const OPISY_SKUTKOW_KANALOW_WPLYWAJACA: Record<KanalWysylki, string> = {
  'e-doreczenia':
    'Doręczenie elektroniczne (ADE) — pismo wpłynęło z pełnym skutkiem prawnym nadania przez nadawcę, potwierdzonym znacznikiem czasu.',
  operator_pocztowy:
    'Przesyłka pocztowa — pismo wpłynęło listem poleconym/zwykłym za pośrednictwem operatora pocztowego.',
  email: 'Kanał pomocniczy — pismo wpłynęło informacyjnie, bez skutków prawnych wniesienia.',
};

export const ETYKIETY_STATUSOW: Record<StatusPrzesylki, string> = {
  nadane: 'Nadane',
  doreczone: 'Doręczone',
  odebrane: 'Odebrane',
  zwrot: 'Zwrot',
  awizo: 'Awizo',
};

export type SeweriaTagu = 'success' | 'secondary' | 'info' | 'warn' | 'danger' | 'contrast';

export const SEWERIE_STATUSOW: Record<StatusPrzesylki, SeweriaTagu> = {
  nadane: 'info',
  doreczone: 'success',
  odebrane: 'success',
  zwrot: 'warn',
  awizo: 'warn',
};
