export interface User {
  id: number;
  email: string;
  name: string | null;
  roles: string[];
}

export interface AuthResponse {
  accessToken: string;
  user: User;
}

export type BillingInterval = 'MONTH' | 'YEAR';

export interface Plan {
  id: number;
  code: string;
  name: string;
  description: string | null;
  amountCents: number;
  currency: string;
  billingInterval: BillingInterval;
  active: boolean;
}

export type SubscriptionStatus = 'TRIALING' | 'ACTIVE' | 'PAST_DUE' | 'CANCELED' | 'EXPIRED';

export interface Subscription {
  id: number;
  plan: Plan;
  status: SubscriptionStatus;
  paymentMethodToken: string;
  trialEndDate: string | null;
  currentPeriodStart: string;
  currentPeriodEnd: string;
  nextBillingDate: string;
  cancelAtPeriodEnd: boolean;
  createdAt: string;
}

export type InvoiceStatus = 'OPEN' | 'PAID' | 'FAILED' | 'VOID';

export interface Invoice {
  id: number;
  number: string;
  status: InvoiceStatus;
  amountCents: number;
  currency: string;
  periodStart: string;
  periodEnd: string;
  attemptCount: number;
  failureReason: string | null;
  issuedAt: string;
  paidAt: string | null;
  nextRetryAt: string | null;
}

export interface Page<T> {
  content: T[];
  page: {
    size: number;
    number: number;
    totalElements: number;
    totalPages: number;
  };
}
