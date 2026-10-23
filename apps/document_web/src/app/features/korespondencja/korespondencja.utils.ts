import type {
  KanalWysylki,
  StatusPrzesylki,
} from '@electronic-document-workflow/document-shared-dto';

/**
 * Kierunek przesyłki — ten typ jest lokalny do przełącznika zakładek UI, niezależny od
 * `Przesylka.kierunek` z pakietu DTO. Wszystkie mocki są obecnie wychodzące; zakładka
 * "Wpływające" nie filtruje jeszcze po `Przesylka.kierunek` (zob. plan.md).
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

export const OPISY_SKUTKOW_KANALOW: Record<KanalWysylki, string> = {
  'e-doreczenia':
    'Doręczenie elektroniczne (ADE) — pełny skutek prawny doręczenia, potwierdzony znacznikiem czasu.',
  operator_pocztowy:
    'Przesyłka hybrydowa — druk i nadanie listu poleconego z potwierdzeniem odbioru (ZPO).',
  email: 'Kanał pomocniczy — informacyjny, bez skutków prawnych doręczenia.',
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

/**
 * Deterministyczny automat stanów do symulacji aktualizacji statusu w UI.
 * Zwraca listę możliwych kolejnych stanów — "awizo" ma dwa odgałęzienia:
 * odebranie przesyłki po awizowaniu albo jej zwrot (nieodebrana).
 */
export function mozliweKolejneStatusy(
  obecny: StatusPrzesylki,
  kanal: KanalWysylki,
): StatusPrzesylki[] {
  switch (obecny) {
    case 'nadane':
      return kanal === 'operator_pocztowy' ? ['awizo'] : ['doreczone'];
    case 'awizo':
      return ['odebrane', 'zwrot'];
    case 'odebrane':
    case 'doreczone':
    case 'zwrot':
    default:
      return [];
  }
}
