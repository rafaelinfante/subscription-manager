import { Routes } from '@angular/router';
import { adminGuard, authGuard } from './core/guards';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./pages/login/login').then(m => m.Login) },
  { path: 'register', loadComponent: () => import('./pages/register/register').then(m => m.Register) },
  {
    path: 'auth/oauth2-callback',
    loadComponent: () => import('./pages/oauth-callback/oauth-callback').then(m => m.OauthCallback),
  },
  {
    path: '',
    loadComponent: () => import('./layout/shell').then(m => m.Shell),
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', loadComponent: () => import('./pages/dashboard/dashboard').then(m => m.Dashboard) },
      { path: 'plans', loadComponent: () => import('./pages/plans/plans').then(m => m.Plans) },
      {
        path: 'subscriptions',
        loadComponent: () => import('./pages/subscriptions/subscriptions').then(m => m.Subscriptions),
      },
      { path: 'invoices', loadComponent: () => import('./pages/invoices/invoices').then(m => m.Invoices) },
      {
        path: 'admin',
        loadComponent: () => import('./pages/admin/admin').then(m => m.Admin),
        canActivate: [adminGuard],
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
