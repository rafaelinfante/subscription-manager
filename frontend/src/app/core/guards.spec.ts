import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { authGuard } from './guards';
import { Auth } from './auth';

describe('authGuard', () => {
  let auth: Auth;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    auth = TestBed.inject(Auth);
  });

  const run = () =>
    TestBed.runInInjectionContext(() =>
      authGuard({} as never, { url: '/dashboard' } as RouterStateSnapshot));

  it('allows access when authenticated', () => {
    auth.user.set({ id: 1, email: 'demo@demo.io', name: 'Demo', roles: ['USER'] });
    expect(run()).toBe(true);
  });

  it('redirects to login when not authenticated', () => {
    const result = run();
    expect(result).toBeInstanceOf(UrlTree);
    expect((result as UrlTree).toString()).toContain('/login');
  });
});
