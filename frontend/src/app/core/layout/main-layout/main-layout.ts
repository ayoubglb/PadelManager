import {Component, computed, effect, inject, signal} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';

import { AuthService } from '../../auth/auth.service';
import { Role } from '../../api/auth.types';
import {TransactionService} from '../../api/transaction.service';
import { SoldeBadge } from '../../../shared/components/solde-badge/solde-badge';

interface NavItem {
  label: string;
  icon: string;
  route: string;
  roles: Role[];
}

const NAV_ITEMS: NavItem[] = [
  // Membres + admins
  { label: 'Planning', icon: 'calendar_month', route: '/planning',
    roles: ['MEMBRE_GLOBAL', 'MEMBRE_SITE', 'MEMBRE_LIBRE', 'ADMIN_GLOBAL', 'ADMIN_SITE'] },
  { label: 'Matchs publics', icon: 'groups', route: '/matchs/publics',
    roles: ['MEMBRE_GLOBAL', 'MEMBRE_SITE', 'MEMBRE_LIBRE'] },
  { label: 'Mes matchs', icon: 'sports_tennis', route: '/matchs/mes-matchs',
    roles: ['MEMBRE_GLOBAL', 'MEMBRE_SITE', 'MEMBRE_LIBRE'] },
  { label: 'Mes transactions', icon: 'receipt_long', route: '/transactions',
    roles: ['MEMBRE_GLOBAL', 'MEMBRE_SITE', 'MEMBRE_LIBRE'] },
  { label: 'Mon profil', icon: 'person', route: '/profil',
    roles: ['MEMBRE_GLOBAL', 'MEMBRE_SITE', 'MEMBRE_LIBRE', 'ADMIN_GLOBAL', 'ADMIN_SITE'] },

  // Admin Site
  { label: 'Mon site', icon: 'business', route: '/admin/mon-site',
    roles: ['ADMIN_SITE'] },
  { label: 'Reporting site', icon: 'bar_chart', route: '/admin/reporting-site',
    roles: ['ADMIN_SITE', 'ADMIN_GLOBAL'] },

  // Admin Global
  { label: 'Sites', icon: 'apartment', route: '/admin/sites',
    roles: ['ADMIN_GLOBAL'] },
  { label: 'Fermetures globales', icon: 'event_busy', route: '/admin/fermetures',
    roles: ['ADMIN_GLOBAL'] },
  { label: 'Administrateurs', icon: 'admin_panel_settings', route: '/admin/administrateurs',
    roles: ['ADMIN_GLOBAL'] },
  { label: 'Reporting global', icon: 'analytics', route: '/admin/reporting-global',
    roles: ['ADMIN_GLOBAL'] },
];

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatToolbarModule,
    MatSidenavModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
    MatMenuModule,
    MatDividerModule,
    SoldeBadge
  ],
  templateUrl: './main-layout.html',
  styleUrl: './main-layout.css',
})
export class MainLayout {
  protected auth = inject(AuthService);
  private breakpoint = inject(BreakpointObserver);
  private transactionService = inject(TransactionService);


  // Signal "écran mobile/tablette"
  isHandset = toSignal(
    this.breakpoint
      .observe([Breakpoints.Handset, Breakpoints.Tablet])
      .pipe(map((r) => r.matches)),
    { initialValue: false }
  );

  // Items filtrés selon le rôle courant
  visibleItems = computed<NavItem[]>(() => {
    const role = this.auth.currentRole();
    if (!role) return [];
    return NAV_ITEMS.filter((it) => it.roles.includes(role));
  });

  sidenavOpened = signal(true);

  toggleSidenav(): void {
    this.sidenavOpened.set(!this.sidenavOpened());
  }

  logout(): void {
    this.auth.logout();
  }
}
