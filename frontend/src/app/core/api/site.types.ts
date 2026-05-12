export interface Site {
  id: number;
  nom: string;
  adresse: string;
  codePostal: string;
  ville: string;
  active: boolean;
}

// Body pour POST/sites et PUT/sites/{id}
export interface SiteCreateUpdateRequest {
  nom: string;
  adresse: string;
  codePostal: string;
  ville: string;
}
