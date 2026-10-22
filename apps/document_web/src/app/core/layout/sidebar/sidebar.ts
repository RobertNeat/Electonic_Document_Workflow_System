import { Component, computed, inject, input } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { CitizenContextService } from '../../services/citizen-context.service';
import { GLOWNE_MENU, kartaObywatelaMenu } from '../../services/menu.config';
import { obywatelWyswietlanaNazwa } from '../../models';

@Component({
  selector: 'app-sidebar',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.css',
})
export class Sidebar {
  private readonly auth = inject(AuthService);
  private readonly citizenContext = inject(CitizenContextService);

  readonly zwinieta = input(false);

  protected readonly obywatelNazwa = obywatelWyswietlanaNazwa;

  protected readonly pozycjeMenu = computed(() => {
    const role = this.auth.uzytkownik()?.role ?? [];
    return GLOWNE_MENU.filter(
      (pozycja) => !pozycja.role || pozycja.role.some((r) => role.includes(r as never)),
    );
  });

  protected readonly obywatel = this.citizenContext.obywatel;

  protected readonly pozycjeKartyObywatela = computed(() => {
    const obywatel = this.citizenContext.obywatel();
    return obywatel ? kartaObywatelaMenu(obywatel.id) : [];
  });
}
