import { Pipe, PipeTransform } from '@angular/core';
import { Role } from '../../core/api/auth.types';

const ROLE_LABELS: Record<Role, string> = {
  MEMBRE_LIBRE: 'Membre Libre',
  MEMBRE_SITE: 'Membre Site',
  MEMBRE_GLOBAL: 'Membre Global',
  ADMIN_SITE: 'Administrateur Site',
  ADMIN_GLOBAL: 'Administrateur Global',
};

@Pipe({
  name: 'roleLabel',
  standalone: true,
})
export class RoleLabelPipe implements PipeTransform {
  transform(role: Role | null | undefined): string {
    if (!role) return '—';
    return ROLE_LABELS[role] ?? role;
  }
}
