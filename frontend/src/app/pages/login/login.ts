import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { Auth } from '../../core/auth';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink, MatCardModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  template: `
    <div class="min-h-screen flex items-center justify-center p-4">
      <mat-card class="w-full max-w-md p-6">
        <h1 class="text-2xl font-semibold mb-1">Welcome back</h1>
        <p class="text-slate-500 mb-6 text-sm">Sign in to manage your subscriptions</p>

        @if (error()) {
          <div class="mb-4 text-sm text-red-700 bg-red-50 rounded p-2">{{ error() }}</div>
        }

        <form [formGroup]="form" (ngSubmit)="submit()" class="flex flex-col gap-2">
          <mat-form-field appearance="outline">
            <mat-label>Email</mat-label>
            <input matInput type="email" formControlName="email" autocomplete="email" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Password</mat-label>
            <input matInput type="password" formControlName="password" autocomplete="current-password" />
          </mat-form-field>
          <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid || loading()">
            {{ loading() ? 'Signing in…' : 'Sign in' }}
          </button>
        </form>

        @if (providers().length) {
          <div class="flex items-center gap-3 my-4 text-slate-400 text-xs">
            <span class="flex-1 border-t"></span> OR <span class="flex-1 border-t"></span>
          </div>
          <div class="flex flex-col gap-2">
            @for (provider of providers(); track provider) {
              <button mat-stroked-button type="button" (click)="social(provider)">
                Continue with {{ label(provider) }}
              </button>
            }
          </div>
        }

        <p class="text-sm text-slate-500 mt-6 text-center">
          No account? <a routerLink="/register" class="text-indigo-600">Create one</a>
        </p>
        <p class="text-xs text-slate-400 mt-3 text-center">
          Demo: demo&#64;demo.io / Password123!
          <button type="button" class="text-indigo-600 underline ml-1" (click)="fillDemo()">use it</button>
        </p>
      </mat-card>
    </div>
  `,
})
export class Login implements OnInit {
  private auth = inject(Auth);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private fb = inject(FormBuilder);

  protected readonly error = signal<string | null>(null);
  protected readonly loading = signal(false);
  protected readonly providers = signal<string[]>([]);

  protected readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });

  ngOnInit(): void {
    this.auth.enabledProviders().subscribe(result => this.providers.set(result.providers));
  }

  submit(): void {
    if (this.form.invalid) {
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    const { email, password } = this.form.getRawValue();
    this.auth.login(email, password).subscribe({
      next: () => this.router.navigateByUrl(this.route.snapshot.queryParams['returnUrl'] ?? '/dashboard'),
      error: err => {
        this.error.set(err.error?.detail ?? 'Invalid email or password');
        this.loading.set(false);
      },
    });
  }

  social(provider: string): void {
    window.location.href = `/oauth2/authorization/${provider}`;
  }

  fillDemo(): void {
    this.form.setValue({ email: 'demo@demo.io', password: 'Password123!' });
  }

  label(provider: string): string {
    return provider.charAt(0).toUpperCase() + provider.slice(1);
  }
}
