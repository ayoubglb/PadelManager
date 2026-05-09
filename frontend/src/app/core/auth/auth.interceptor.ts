import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

 // Ajoute Authorization: Bearer <token> à toutes les requêtes
 // sauf /auth/login et /auth/register (publiques).
 // Sur 401 : logout + redirection /login.

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  const isAuthEndpoint =
    req.url.includes('/auth/login') || req.url.includes('/auth/register');

  const token = auth.getToken();

  const reqWithAuth =
    token && !isAuthEndpoint
      ? req.clone({
        setHeaders: { Authorization: `Bearer ${token}` },
      })
      : req;

  return next(reqWithAuth).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err.status === 401 && !isAuthEndpoint) {
        auth.logout();
        router.navigate(['/login']);
      }
      return throwError(() => err);
    })
  );
};
