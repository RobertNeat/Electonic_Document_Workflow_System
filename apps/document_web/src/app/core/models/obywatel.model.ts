export interface Obywatel {
  id: string;
  typ: 'osoba_fizyczna' | 'podmiot';
  imie?: string;
  nazwisko?: string;
  nazwaPodmiotu?: string;
  pesel?: string;
  nip?: string;
  adresDoreczenEDE?: string;
  email?: string;
  telefon?: string;
  adres: {
    ulica: string;
    numerDomu: string;
    numerLokalu?: string;
    kodPocztowy: string;
    miejscowosc: string;
  };
}

export function obywatelWyswietlanaNazwa(obywatel: Obywatel): string {
  if (obywatel.typ === 'podmiot') {
    return obywatel.nazwaPodmiotu ?? '';
  }
  return `${obywatel.nazwisko ?? ''} ${obywatel.imie ?? ''}`.trim();
}
