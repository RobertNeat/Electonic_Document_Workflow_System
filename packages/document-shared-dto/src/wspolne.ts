// Wspólny wrapper paginacji dla list zwracanych przez document_api.
export interface StronaWynikow<T> {
  dane: T[];
  strona: number;
  rozmiarStrony: number;
  liczbaWszystkich: number;
}
