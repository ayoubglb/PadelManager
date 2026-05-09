import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { HttpErrorResponse } from '@angular/common/http';

import { AuthService } from '../../../core/auth/auth.service';
import { ApiError } from '../../../core/api/auth.types';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './login.html',
  styleUrl: './login.css',
})

export class Login {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);
  private snack = inject(MatSnackBar);

  loading = signal(false);
  hidePassword = signal(true);

  form = this.fb.nonNullable.group({
    login: ['', [Validators.required]],
    motDePasse: ['', [Validators.required, Validators.minLength(8)]],
  });

  submit(): void {
    if (this.form.invalid) return;
    this.loading.set(true);

    this.auth.login(this.form.getRawValue()).subscribe({
      next: () => {
        this.loading.set(false);
        this.snack.open('Connexion réussie', 'OK', { duration: 2500 });
        this.router.navigate(['/']);
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        const apiErr = err.error as ApiError | undefined;
        const msg =
          apiErr?.message ?? 'Login failed: invalid user id or password';
        this.snack.open(msg, 'OK', { duration: 4000 });
      },
    });
  }
}
