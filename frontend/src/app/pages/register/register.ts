import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { Auth } from '../../core/auth';

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink, MatCardModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  template: `
    <div class="min-h-screen flex items-center justify-center p-4">
      <mat-card class="w-full max-w-md p-6">
        <h1 class="text-2xl font-semibold mb-1">Create your account</h1>
        <p class="text-slate-500 mb-6 text-sm">Start managing subscriptions in seconds</p>

        @if (error()) {
          <div class="mb-4 text-sm text-red-700 bg-red-50 rounded p-2">{{ error() }}</div>
        }

        <form [formGroup]="form" (ngSubmit)="submit()" class="flex flex-col gap-2">
          <mat-form-field appearance="outline">
            <mat-label>Name</mat-label>
            <input matInput formControlName="name" autocomplete="name" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Email</mat-label>
            <input matInput type="email" formControlName="email" autocomplete="email" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Password</mat-label>
            <input matInput type="password" formControlName="password" autocomplete="new-password" />
            <mat-hint>At least 8 characters</mat-hint>
          </mat-form-field>
          <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid || loading()">
            {{ loading() ? 'Creating…' : 'Create account' }}
          </button>
        </form>

        <p class="text-sm text-slate-500 mt-6 text-center">
          Already have an account? <a routerLink="/login" class="text-indigo-600">Sign in</a>
        </p>
      </mat-card>
    </div>
  `,
})
export class Register {
  private auth = inject(Auth);
  private router = inject(Router);
  private fb = inject(FormBuilder);

  protected readonly error = signal<string | null>(null);
  protected readonly loading = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    name: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });

  submit(): void {
    if (this.form.invalid) {
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    const { name, email, password } = this.form.getRawValue();
    this.auth.register(email, password, name).subscribe({
      next: () => this.router.navigateByUrl('/dashboard'),
      error: err => {
        this.error.set(err.error?.detail ?? 'Could not create the account');
        this.loading.set(false);
      },
    });
  }
}
