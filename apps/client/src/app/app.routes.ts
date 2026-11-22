import { Routes } from '@angular/router';
import { Register } from './features/auth/register/register';
import { VerifyEmail } from './features/auth/verify-email/verify-email';

export const routes: Routes = [
  { path: 'register', component: Register },
  { path: 'verify-email', component: VerifyEmail },
];
