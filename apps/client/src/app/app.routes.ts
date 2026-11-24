import { Routes } from '@angular/router';
import { Register } from './features/auth/register/register';
import { VerifyEmail } from './features/auth/verify-email/verify-email';

export const routes: Routes = [
  // Registration is the entry point until login and the real home exist.
  { path: '', pathMatch: 'full', redirectTo: 'register' },
  { path: 'register', component: Register },
  { path: 'verify-email', component: VerifyEmail },
];
