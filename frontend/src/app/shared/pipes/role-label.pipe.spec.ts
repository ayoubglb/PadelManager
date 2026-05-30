import { describe, it, expect } from 'vitest';
import { RoleLabelPipe } from './role-label.pipe';

describe('RoleLabelPipe', () => {
  const pipe = new RoleLabelPipe();

  it('transforme MEMBRE_LIBRE en "Membre Libre"', () => {
    expect(pipe.transform('MEMBRE_LIBRE')).toBe('Membre Libre');
  });

  it('transforme MEMBRE_SITE en "Membre Site"', () => {
    expect(pipe.transform('MEMBRE_SITE')).toBe('Membre Site');
  });

  it('transforme MEMBRE_GLOBAL en "Membre Global"', () => {
    expect(pipe.transform('MEMBRE_GLOBAL')).toBe('Membre Global');
  });

  it('transforme ADMIN_SITE en "Administrateur Site"', () => {
    expect(pipe.transform('ADMIN_SITE')).toBe('Administrateur Site');
  });

  it('transforme ADMIN_GLOBAL en "Administrateur Global"', () => {
    expect(pipe.transform('ADMIN_GLOBAL')).toBe('Administrateur Global');
  });
});
