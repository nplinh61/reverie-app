import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import {
  IonApp,
  IonContent,
  IonHeader,
  IonTitle,
  IonToolbar,
} from '@ionic/angular';
import { ConnectionStatus } from './features/health/connection-status';

@Component({
  imports: [
    RouterOutlet,
    IonApp,
    IonHeader,
    IonToolbar,
    IonTitle,
    IonContent,
    ConnectionStatus,
  ],
  selector: 'app-root',
  styleUrl: './app.scss',
  templateUrl: './app.html',
})
export class App {
  protected readonly title = signal('Reverie');
}
