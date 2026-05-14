import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import {
  MAT_DIALOG_DATA,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatListModule } from '@angular/material/list';
import { MatSnackBar } from '@angular/material/snack-bar';
import { HttpErrorResponse } from '@angular/common/http';
import { toSignal } from '@angular/core/rxjs-interop';
import {
  catchError,
  debounceTime,
  distinctUntilChanged,
  filter, Observable,
  of,
  startWith,
  switchMap,
} from 'rxjs';

import { UtilisateurService } from '../../../core/api/utilisateur.service';
import { UtilisateurRechercheResultat } from '../../../core/api/utilisateur-recherche.types';
import { MatchService } from '../../../core/api/match.service';
import { ApiError } from '../../../core/api/auth.types';
import { RoleLabelPipe } from '../../../shared/pipes/role-label.pipe';

// Données injectées au dialog au moment de l'ouverture
export interface InviteJoueurDialogData {
  matchId: number;
  // Matricules déjà inscrits au match (pour les filtrer dans les résultats)
  matriculesDejaInscrits: string[];
}

@Component({
  selector: 'app-invite-joueur-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatListModule,
    RoleLabelPipe,
  ],
  templateUrl: './invite-joueur-dialog.html',
  styleUrl: './invite-joueur-dialog.css',
})
export class InviteJoueurDialog {
  private utilisateurService = inject(UtilisateurService);
  private matchService = inject(MatchService);
  private dialogRef = inject(MatDialogRef<InviteJoueurDialog>);
  private snack = inject(MatSnackBar);

  readonly data = inject<InviteJoueurDialogData>(MAT_DIALOG_DATA);

  readonly searchCtrl = new FormControl<string>('', { nonNullable: true });
  readonly inviting = signal(false);

  // Résultats de la recherche, mis à jour automatiquement via debounce + distinct + min 3 chars
  readonly resultats = toSignal(
    this.searchCtrl.valueChanges.pipe(
      startWith(''),
      debounceTime(300),
      distinctUntilChanged(),
      switchMap((q): Observable<UtilisateurRechercheResultat[]> => {
        if (!q || q.length < 3) {
          return of([]);
        }
        return this.utilisateurService.recherche(q, 10).pipe(
          catchError((err) => {
            console.error('Erreur recherche', err);
            return of([] as UtilisateurRechercheResultat[]);
          })
        );
      })
    ),
    { initialValue: [] as UtilisateurRechercheResultat[] }
  );

  // Filtre les utilisateurs déjà inscrits (pas la peine de les proposer)
  readonly resultatsFiltres = (): UtilisateurRechercheResultat[] => {
    const dejaInscrits = new Set(this.data.matriculesDejaInscrits);
    return this.resultats().filter((u) => !dejaInscrits.has(u.matricule));
  };

  readonly hasQuery = (): boolean => this.searchCtrl.value.length >= 3;

  // Invite l'utilisateur sélectionné. Ferme le dialog avec le matricule en succès
  invite(user: UtilisateurRechercheResultat): void {
    if (this.inviting()) return;
    this.inviting.set(true);

    this.matchService
      .inviter(this.data.matchId, { matricule: user.matricule })
      .subscribe({
        next: () => {
          this.inviting.set(false);
          this.dialogRef.close({ success: true, joueurNom: `${user.prenom} ${user.nom}` });
        },
        error: (err: HttpErrorResponse) => {
          this.inviting.set(false);
          const apiErr = err.error as ApiError | undefined;
          const msg = apiErr?.message ?? "Échec de l'invitation";
          this.snack.open(msg, 'OK', { duration: 4000 });
        },
      });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
