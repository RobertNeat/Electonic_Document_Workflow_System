import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { MessageModule } from 'primeng/message';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  imports: [FormsModule, RouterLink, ButtonModule, InputTextModule, PasswordModule, MessageModule],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly login = signal('');
  protected readonly haslo = signal('');
  protected readonly blad = signal<string | null>(null);

  protected zaloguj(): void {
    if (!this.login() || !this.haslo()) {
      this.blad.set('Podaj login i hasło.');
      return;
    }
    const sukces = this.auth.zaloguj(this.login(), this.haslo());
    if (sukces) {
      this.router.navigate(['/pulpit']);
    } else {
      this.blad.set('Nieprawidłowy login lub hasło.');
    }
  }
}
