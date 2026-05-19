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
    path: 'terrains',
    canActivate: [adminSiteGuard],
    loadComponent: () =>
      import('./admin-terrains/admin-terrains').then((m) => m.AdminTerrains),
  },
  {
    path: 'horaires',
    canActivate: [adminSiteGuard],
    loadComponent: () =>
      import('./admin-horaires/admin-horaires').then((m) => m.AdminHoraires),
  },
  {
    path: 'fermetures',
    canActivate: [adminSiteGuard],
    loadComponent: () =>
      import('./admin-fermetures/admin-fermetures').then((m) => m.AdminFermetures),
  },
  {
    path: 'reporting-global',
    canActivate: [adminGlobalGuard],
    loadComponent: () =>
      import('./admin-reporting/admin-reporting').then((m) => m.AdminReporting),
    data: { mode: 'global' },
  },
  {
    path: 'reporting-site',
    canActivate: [adminSiteGuard],
    loadComponent: () =>
      import('./admin-reporting/admin-reporting').then((m) => m.AdminReporting),
    data: { mode: 'site' },
  },
];
