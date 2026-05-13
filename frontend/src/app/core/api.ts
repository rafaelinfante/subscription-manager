import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { BillingInterval, Invoice, Page, Plan, Subscription } from './models';

export interface CreateSubscriptionRequest {
  planId: number;
  paymentMethodToken: string;
  trial: boolean;
}

export interface CreatePlanRequest {
  code: string;
  name: string;
  description: string;
  amountCents: number;
  currency: string;
  billingInterval: BillingInterval;
}

@Injectable({ providedIn: 'root' })
export class Api {
  private http = inject(HttpClient);

  plans(): Observable<Plan[]> {
    return this.http.get<Plan[]>('/api/plans');
  }

  subscribe(request: CreateSubscriptionRequest): Observable<Subscription> {
    return this.http.post<Subscription>('/api/subscriptions', request);
  }

  subscriptions(page = 0, size = 20): Observable<Page<Subscription>> {
    return this.http.get<Page<Subscription>>('/api/subscriptions', { params: pageParams(page, size) });
  }

  changePlan(id: number, planId: number): Observable<Subscription> {
    return this.http.put<Subscription>(`/api/subscriptions/${id}/plan`, { planId });
  }

  cancel(id: number, atPeriodEnd: boolean): Observable<Subscription> {
    return this.http.post<Subscription>(`/api/subscriptions/${id}/cancel`, null,
      { params: new HttpParams().set('atPeriodEnd', atPeriodEnd) });
  }

  invoices(page = 0, size = 20): Observable<Page<Invoice>> {
    return this.http.get<Page<Invoice>>('/api/invoices', { params: pageParams(page, size) });
  }

  adminPlans(): Observable<Plan[]> {
    return this.http.get<Plan[]>('/api/admin/plans');
  }

  createPlan(request: CreatePlanRequest): Observable<Plan> {
    return this.http.post<Plan>('/api/admin/plans', request);
  }

  setPlanActive(id: number, active: boolean): Observable<Plan> {
    return this.http.post<Plan>(`/api/admin/plans/${id}/${active ? 'activate' : 'deactivate'}`, null);
  }

  adminSubscriptions(page = 0, size = 20): Observable<Page<Subscription>> {
    return this.http.get<Page<Subscription>>('/api/admin/subscriptions', { params: pageParams(page, size) });
  }

  runBilling(): Observable<void> {
    return this.http.post<void>('/api/admin/billing/run', null);
  }
}

function pageParams(page: number, size: number): HttpParams {
  return new HttpParams().set('page', page).set('size', size);
}
