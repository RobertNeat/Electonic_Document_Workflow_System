import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { SelectModule } from 'primeng/select';
import { SelectButtonModule } from 'primeng/selectbutton';
import { MessageModule } from 'primeng/message';
import { CardModule } from 'primeng/card';
import {
  MOCK_DOKUMENTY,
  MOCK_OBYWATELE,
  MOCK_SPRAWY,
  MOCK_WERSJE_DOKUMENTOW,
} from '../../../core/mock/mock-data';
import { obywatelWyswietlanaNazwa } from '../../../core/models';
import type { KanalWysylki, Przesylka } from '@electronic-document-workflow/document-shared-dto';
import { KorespondencjaStore } from '../korespondencja.store';
import { ETYKIETY_KANALOW, OPISY_SKUTKOW_KANALOW } from '../korespondencja.utils';

interface OpcjaWyboru {
  etykieta: string;
  wartosc: string;
}

interface OpcjaKanalu {
  etykieta: string;
  wartosc: KanalWysylki;
}

@Component({
  selector: 'app-korespondencja-wyslij',
  imports: [
    RouterLink,
    FormsModule,
    ButtonModule,
    SelectModule,
    SelectButtonModule,
    MessageModule,
    CardModule,
  ],
  templateUrl: './korespondencja-wyslij.html',
  styleUrl: './korespondencja-wyslij.css',
})
export class KorespondencjaWyslij {
  private readonly store = inject(KorespondencjaStore);
  private readonly router = inject(Router);

  protected readonly opisySkutkow = OPISY_SKUTKOW_KANALOW;

  protected readonly opcjeSpraw: OpcjaWyboru[] = MOCK_SPRAWY.map((s) => ({
    etykieta: `${s.znakSprawy} — ${s.tytul}`,
    wartosc: s.id,
  }));

  protected readonly opcjeKanalow: OpcjaKanalu[] = [
    { etykieta: ETYKIETY_KANALOW['e-doreczenia'], wartosc: 'e-doreczenia' },
    { etykieta: ETYKIETY_KANALOW.operator_pocztowy, wartosc: 'operator_pocztowy' },
    { etykieta: ETYKIETY_KANALOW.email, wartosc: 'email' },
  ];

  protected readonly sprawaId = signal<string | null>(null);
  protected readonly dokumentId = signal<string | null>(null);
  protected readonly obywatelId = signal<string | null>(null);
  protected readonly kanal = signal<KanalWysylki>('e-doreczenia');
  protected readonly blad = signal<string | null>(null);
  protected readonly potwierdzenie = signal<Przesylka | null>(null);

  // Tylko podpisana, aktualna wersja dokumentu może być nadana — zgodnie z zasadami obiegu.
  protected readonly opcjeDokumentow = computed<OpcjaWyboru[]>(() => {
    const sprawa = this.sprawaId();
    if (!sprawa) {
      return [];
    }
    return MOCK_DOKUMENTY.filter((d) => d.sprawaId === sprawa)
      .filter((d) =>
        MOCK_WERSJE_DOKUMENTOW.some(
          (w) => w.dokumentId === d.id && w.wersja === d.aktualnaWersja && w.podpisana,
        ),
      )
      .map((d) => ({ etykieta: d.nazwa, wartosc: d.id }));
  });

  protected readonly opcjeObywateli: OpcjaWyboru[] = MOCK_OBYWATELE.map((o) => ({
    etykieta: obywatelWyswietlanaNazwa(o),
    wartosc: o.id,
  }));

  protected readonly adresatMail = computed(() => {
    const id = this.obywatelId();
    return MOCK_OBYWATELE.find((o) => o.id === id)?.email ?? null;
  });

  // Żaden mock obywatela nie ma wypełnionego adresu ADE — ostrzeżenie zamiast blokady.
  protected readonly adresatAde = computed(() => {
    const id = this.obywatelId();
    return MOCK_OBYWATELE.find((o) => o.id === id)?.adresDoreczenEDE ?? null;
  });

  wybierzSprawe(id: string): void {
    this.sprawaId.set(id);
    this.dokumentId.set(null);
    // Adresat domyślnie wynika ze sprawy, ale użytkownik może go zmienić ręcznie.
    const sprawa = MOCK_SPRAWY.find((s) => s.id === id);
    this.obywatelId.set(sprawa?.obywatelId ?? null);
  }

  wyslij(): void {
    if (!this.sprawaId() || !this.dokumentId() || !this.obywatelId()) {
      this.blad.set('Wybierz sprawę, podpisany dokument oraz adresata.');
      return;
    }
    this.blad.set(null);
    const przesylka = this.store.nadaj({
      sprawaId: this.sprawaId()!,
      dokumentId: this.dokumentId()!,
      obywatelId: this.obywatelId()!,
      kanal: this.kanal(),
    });
    this.potwierdzenie.set(przesylka);
  }

  przejdzDoSzczegolow(): void {
    const przesylka = this.potwierdzenie();
    if (przesylka) {
      this.router.navigate(['/korespondencja', przesylka.id]);
    }
  }
}
