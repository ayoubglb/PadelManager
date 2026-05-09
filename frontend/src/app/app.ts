import {Component, inject} from '@angular/core';
import { RouterOutlet } from '@angular/router';
import {AuthService} from './core/auth/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  template: '<router-outlet></router-outlet>',
})
export class App {
  // Force l'instanciation d'AuthService au démarrage,
  // ce qui déclenche son constructor (refresh du solde si user restauré).
  private auth = inject(AuthService);
}
