import type {
  KanalWysylki,
  Przesylka,
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

/**
 * Deterministyczny automat stanów do symulacji aktualizacji statusu w UI.
 * Rozdzielony wg kierunku przesyłki — cykl życia doręczenia (wychodząca) i
 * cykl życia wpływu pisma (wpływająca) mają inną semantykę:
 * - wychodząca: nadane → awizo/doreczone → odebrane/zwrot (dostawa do adresata,
 *   "awizo" ma dwa odgałęzienia: odebranie po awizowaniu albo zwrot nieodebrany).
 * - wpływająca: nadane (= zarejestrowano wpływ) → odebrane (urząd formalnie
 *   przyjął pismo) albo zwrot (pismo nieprawidłowe/bez podpisu, odesłane do
 *   nadawcy) — pismo już doszło, więc nie przechodzi przez doreczone/awizo.
 */
export function mozliweKolejneStatusy(
  obecny: StatusPrzesylki,
  kanal: KanalWysylki,
  kierunek: Przesylka['kierunek'],
): StatusPrzesylki[] {
  if (kierunek === 'wplywajaca') {
    return obecny === 'nadane' ? ['odebrane', 'zwrot'] : [];
  }
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
