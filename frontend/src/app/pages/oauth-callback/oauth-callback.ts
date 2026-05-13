import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Auth } from '../../core/auth';

@Component({
  selector: 'app-oauth-callback',
  imports: [MatProgressSpinnerModule],
  template: `
    <div class="min-h-screen flex flex-col items-center justify-center gap-4 text-slate-600">
      <mat-spinner diameter="40"></mat-spinner>
      <p>Finishing sign-in…</p>
    </div>
  `,
})
export class OauthCallback implements OnInit {
  private auth = inject(Auth);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  ngOnInit(): void {
    const success = this.route.snapshot.queryParamMap.get('status') === 'success';
    if (!success) {
      this.router.navigate(['/login'], { queryParams: { error: 'social' } });
      return;
    }
    // The backend set an HttpOnly refresh cookie; exchange it for an access token.
    this.auth.refresh().subscribe({
      next: () => this.router.navigateByUrl('/dashboard'),
      error: () => this.router.navigate(['/login'], { queryParams: { error: 'social' } }),
    });
  }
}
