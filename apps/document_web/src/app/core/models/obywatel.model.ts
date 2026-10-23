import type { Obywatel } from '@electronic-document-workflow/document-shared-dto';

export function obywatelWyswietlanaNazwa(obywatel: Obywatel): string {
  if (obywatel.typ === 'podmiot') {
    return obywatel.nazwaPodmiotu ?? '';
  }
  return `${obywatel.nazwisko ?? ''} ${obywatel.imie ?? ''}`.trim();
}
