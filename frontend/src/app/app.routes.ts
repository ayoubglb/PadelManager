import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login/login').then((m) => m.Login),
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./features/auth/register/register').then((m) => m.Register),
  },

  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./core/layout/main-layout/main-layout').then((m) => m.MainLayout),
    children: [
      { path: '', redirectTo: 'planning', pathMatch: 'full' },
      {
        path: 'planning',
        loadComponent: () =>
          import('./features/planning/planning').then((m) => m.Planning),
      },
      {
        path: 'matchs/publics',
        loadComponent: () =>
          import('./shared/components/placeholder/placeholder').then(
            (m) => m.Placeholder
          ),
      },
      {
        path: 'matchs/mes-matchs',
        loadComponent: () =>
          import('./shared/components/placeholder/placeholder').then(
            (m) => m.Placeholder
          ),
      },
      {
        path: 'transactions',
        loadComponent: () =>
          import('./features/transactions/transactions').then((m) => m.Transactions),
      },
      {
        path: 'profil',
        loadComponent: () =>
          import('./features/profil/profil').then((m) => m.Profil),
      },
      {
        path: 'sites',
        loadComponent: () =>
          import('./features/sites/sites').then((m) => m.Sites),
      },
      {
        path: 'admin',
        loadChildren: () =>
          import('./features/admin/admin.routes').then((m) => m.ADMIN_ROUTES),
      },
    ],
  },

  { path: '**', redirectTo: '' },
];
