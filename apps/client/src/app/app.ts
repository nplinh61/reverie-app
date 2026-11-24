import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { IonApp, IonContent } from '@ionic/angular';

@Component({
  imports: [RouterOutlet, IonApp, IonContent],
  selector: 'app-root',
  styleUrl: './app.scss',
  templateUrl: './app.html',
})
export class App {
  protected readonly title = signal('Reverie');
}
