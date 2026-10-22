export interface AdresObywatela {
  ulica: string;
  numerDomu: string;
  numerLokalu?: string;
  kodPocztowy: string;
  miejscowosc: string;
}

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
  adres: AdresObywatela;
}
