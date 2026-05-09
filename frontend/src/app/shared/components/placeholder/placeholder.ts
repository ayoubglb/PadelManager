import { Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';

@Component({
  selector: 'app-placeholder',
  standalone: true,
  templateUrl: './placeholder.html',
  styleUrl: './placeholder.css',
})
export class Placeholder {
  private route = inject(ActivatedRoute);
  path = toSignal(
    this.route.url.pipe(map((segs) => '/' + segs.map((s) => s.path).join('/'))),
    { initialValue: '' }
  );
}
