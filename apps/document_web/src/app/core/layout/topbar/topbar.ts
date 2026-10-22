import { Component, inject, output } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { ChipModule } from 'primeng/chip';
import { AvatarModule } from 'primeng/avatar';
import { MenuModule } from 'primeng/menu';
import { MenuItem } from 'primeng/api';
import { AuthService } from '../../services/auth.service';
import { CitizenContextService } from '../../services/citizen-context.service';
import { ThemeService } from '../../services/theme.service';
import { obywatelWyswietlanaNazwa } from '../../models';

@Component({
  selector: 'app-topbar',
  imports: [RouterLink, ButtonModule, ChipModule, AvatarModule, MenuModule],
  templateUrl: './topbar.html',
  styleUrl: './topbar.css',
})
export class Topbar {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  protected readonly citizenContext = inject(CitizenContextService);
  protected readonly theme = inject(ThemeService);

  readonly przelaczMenu = output<void>();

  protected readonly uzytkownik = this.auth.uzytkownik;
  protected readonly obywatelNazwa = obywatelWyswietlanaNazwa;

  protected readonly pozycjeUzytkownika: MenuItem[] = [
    { label: 'Wyloguj', icon: 'pi pi-sign-out', command: () => this.wyloguj() },
  ];

  protected odepnijObywatela(): void {
    this.citizenContext.odepnij();
  }

  private wyloguj(): void {
    this.auth.wyloguj();
    this.router.navigate(['/logowanie']);
  }
}
