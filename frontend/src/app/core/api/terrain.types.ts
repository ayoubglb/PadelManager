export interface Terrain {
  id: number;
  numero: number;
  nom: string | null;
  siteId: number;
  active: boolean;
}

// Body pour POST/PUT terrain

export interface TerrainCreateUpdateRequest {
  numero: number;
  nom?: string | null;
}
