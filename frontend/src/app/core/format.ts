export function subscriptionStatusClasses(status: string): string {
  switch (status) {
    case 'ACTIVE':
      return 'bg-green-100 text-green-800';
    case 'TRIALING':
      return 'bg-blue-100 text-blue-800';
    case 'PAST_DUE':
      return 'bg-amber-100 text-amber-800';
    default:
      return 'bg-slate-200 text-slate-700';
  }
}

export function invoiceStatusClasses(status: string): string {
  switch (status) {
    case 'PAID':
      return 'bg-green-100 text-green-800';
    case 'OPEN':
      return 'bg-blue-100 text-blue-800';
    case 'FAILED':
      return 'bg-red-100 text-red-800';
    default:
      return 'bg-slate-200 text-slate-700';
  }
}
