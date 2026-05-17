import { Routes } from '@angular/router';
import { adminGlobalGuard, adminSiteGuard } from '../../core/auth/auth.guard';

export const ADMIN_ROUTES: Routes = [
  {
    path: 'sites',
    canActivate: [adminGlobalGuard],
    loadComponent: () =>
      import('./admin-sites/admin-sites').then((m) => m.AdminSites),
  },
  {
    path: 'fermetures',
    canActivate: [adminGlobalGuard],
    loadComponent: () =>
      import('../../shared/components/placeholder/placeholder').then(
        (m) => m.Placeholder
      ),
  },
  {
    path: 'administrateurs',
    canActivate: [adminGlobalGuard],
    loadComponent: () =>
      import('../../shared/components/placeholder/placeholder').then(
        (m) => m.Placeholder
      ),
  },
  {
    path: 'reporting-global',
    canActivate: [adminGlobalGuard],
    loadComponent: () =>
      import('../../shared/components/placeholder/placeholder').then(
        (m) => m.Placeholder
      ),
  },
  {
    path: 'mon-site',
    canActivate: [adminSiteGuard],
    loadComponent: () =>
      import('../../shared/components/placeholder/placeholder').then(
        (m) => m.Placeholder
      ),
  },
  {
    path: 'reporting-site',
    canActivate: [adminSiteGuard],
    loadComponent: () =>
      import('../../shared/components/placeholder/placeholder').then(
        (m) => m.Placeholder
      ),
  },
];
