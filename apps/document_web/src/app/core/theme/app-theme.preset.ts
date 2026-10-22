import { definePreset } from '@primeuix/themes';
import Material from '@primeuix/themes/material';

/**
 * Preset Material z kolorem primary #4C8C42.
 *
 * `surface.*` musi pozostać zwykłą (nie `light-dark()`) rampą odcieni szarości —
 * każdy token komponentu Material (np. `content.background`,
 * `formField.background`) sam opakowuje dwa różne indeksy tej rampy w
 * `light-dark({surface.X}, {surface.Y})`. Owijanie samej rampy w `light-dark()`
 * powoduje podwójne zagnieżdżenie i niepoprawne wartości w trybie ciemnym.
 * Ramp 0 (biel) → 950 (najciemniejszy szary) celowo nie schodzi do czerni,
 * zgodnie z wymaganiem: tryb ciemny ma przyciemniać tła, nie czernić ich.
 *
 * `primary.color`/`hoverColor`/`activeColor` w Material domyślnie też używają
 * `light-dark()` i w trybie ciemnym przełączają się na jaśniejszy odcień —
 * nadpisane na stałe na #4C8C42, żeby primary nie zmieniał się między trybami.
 */
export const AppThemePreset = definePreset(Material, {
  semantic: {
    primary: {
      50: '#eef5ec',
      100: '#d2e6cc',
      200: '#b3d6a9',
      300: '#93c585',
      400: '#7bb869',
      500: '#4c8c42',
      600: '#457e3b',
      700: '#3b6d32',
      800: '#325c29',
      900: '#213d1b',
      950: '#152711',
      color: '#4c8c42',
      hoverColor: '#457e3b',
      activeColor: '#3b6d32',
    },
    surface: {
      0: '#ffffff',
      50: '#f5f5f6',
      100: '#e9e9eb',
      200: '#d4d4d8',
      300: '#b5b5bb',
      400: '#8f8f97',
      500: '#6e6e76',
      600: '#55555c',
      700: '#434348',
      800: '#333338',
      900: '#242428',
      950: '#19191c',
    },
  },
});
