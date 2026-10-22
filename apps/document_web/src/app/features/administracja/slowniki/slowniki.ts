import { Component } from '@angular/core';
import { TabsModule } from 'primeng/tabs';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { MOCK_JRWA, MOCK_TYPY_DOKUMENTOW } from '../../../core/mock/mock-data';
import { MOCK_DOSTAWCY_WYSYLKI, MOCK_STAWKI_OPLAT } from '../mock/admin-mock-data';

@Component({
  selector: 'app-slowniki',
  standalone: true,
  imports: [TabsModule, TableModule, TagModule],
  templateUrl: './slowniki.html',
  styleUrl: './slowniki.css',
})
export class Slowniki {
  readonly jrwa = MOCK_JRWA;
  readonly typyDokumentow = MOCK_TYPY_DOKUMENTOW;
  readonly dostawcyWysylki = MOCK_DOSTAWCY_WYSYLKI;
  readonly stawkiOplat = MOCK_STAWKI_OPLAT;
}
