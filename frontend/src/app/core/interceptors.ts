import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { Auth } from './auth';

/** Attaches the in-memory access token and sends cookies so the refresh token rides along. */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = inject(Auth).accessToken();
  const withCredentials = req.clone({ withCredentials: true });
  return next(token
    ? withCredentials.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : withCredentials);
};

/** On a 401, tries the refresh cookie once and replays the request; otherwise logs out. */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(Auth);
  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      const isAuthCall = req.url.includes('/api/auth/');
      if (error.status !== 401 || isAuthCall) {
        return throwError(() => error);
      }
      return auth.refresh().pipe(
        switchMap(() => next(req.clone({
          withCredentials: true,
          setHeaders: { Authorization: `Bearer ${auth.accessToken()}` },
        }))),
        catchError(refreshError => {
          auth.forceLogout();
          return throwError(() => refreshError);
        }),
      );
    }),
  );
};
