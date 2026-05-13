import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { Auth } from './auth';
import { AuthResponse } from './models';

describe('Auth', () => {
  let auth: Auth;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    auth = TestBed.inject(Auth);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('starts unauthenticated', () => {
    expect(auth.isAuthenticated()).toBe(false);
  });

  it('stores the user and token on login', () => {
    const response: AuthResponse = {
      accessToken: 'jwt',
      user: { id: 1, email: 'demo@demo.io', name: 'Demo', roles: ['USER'] },
    };

    auth.login('demo@demo.io', 'Password123!').subscribe();
    http.expectOne('/api/auth/login').flush(response);

    expect(auth.accessToken()).toBe('jwt');
    expect(auth.isAuthenticated()).toBe(true);
    expect(auth.isAdmin()).toBe(false);
  });

  it('recognises an admin user', () => {
    auth.login('admin@demo.io', 'Password123!').subscribe();
    http.expectOne('/api/auth/login').flush({
      accessToken: 'jwt',
      user: { id: 2, email: 'admin@demo.io', name: 'Admin', roles: ['USER', 'ADMIN'] },
    } satisfies AuthResponse);

    expect(auth.isAdmin()).toBe(true);
  });
});
