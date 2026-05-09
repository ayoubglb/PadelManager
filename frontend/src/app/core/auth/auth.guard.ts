import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (auth.isAuthenticated()) {
    return true;
  }
  router.navigate(['/login']);
  return false;
};

export const adminGlobalGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (auth.isAdminGlobal()) {
    return true;
  }
  router.navigate(['/']);
  return false;
};

export const adminSiteGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  // Admin Site ou Admin Global peuvent accéder aux pages admin site
  if (auth.isAdmin()) {
    return true;
  }
  router.navigate(['/']);
  return false;
};
