import { computed, inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { firstValueFrom, Observable, tap } from 'rxjs';
import { AuthResponse, User } from './models';

@Injectable({ providedIn: 'root' })
export class Auth {
  private http = inject(HttpClient);
  private router = inject(Router);

  readonly accessToken = signal<string | null>(null);
  readonly user = signal<User | null>(null);

  readonly isAuthenticated = computed(() => this.user() !== null);
  readonly isAdmin = computed(() => this.user()?.roles.includes('ADMIN') ?? false);

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/auth/login', { email, password })
      .pipe(tap(response => this.setSession(response)));
  }

  register(email: string, password: string, name: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/auth/register', { email, password, name })
      .pipe(tap(response => this.setSession(response)));
  }

  refresh(): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/auth/refresh', {})
      .pipe(tap(response => this.setSession(response)));
  }

  async logout(): Promise<void> {
    try {
      await firstValueFrom(this.http.post('/api/auth/logout', {}));
    } finally {
      this.clearSession();
      this.router.navigate(['/login']);
    }
  }

  /** Called once at startup to restore a session from the refresh cookie, if any. */
  async restoreSession(): Promise<void> {
    try {
      await firstValueFrom(this.refresh());
    } catch {
      this.clearSession();
    }
  }

  forceLogout(): void {
    this.clearSession();
    this.router.navigate(['/login']);
  }

  enabledProviders(): Observable<{ providers: string[] }> {
    return this.http.get<{ providers: string[] }>('/api/auth/social-providers');
  }

  private setSession(response: AuthResponse): void {
    this.accessToken.set(response.accessToken);
    this.user.set(response.user);
  }

  private clearSession(): void {
    this.accessToken.set(null);
    this.user.set(null);
  }
}
