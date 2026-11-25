import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService); const router = inject(Router);
  return auth.obterUsuarioAtual().pipe(map(u => u ? true : router.createUrlTree(['/login'])),
    catchError(() => of(router.createUrlTree(['/indisponivel']))));
};
export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthService); const router = inject(Router);
  return auth.obterUsuarioAtual().pipe(map(u => !u ? router.createUrlTree(['/login']) :
    u.papel === 'ADMIN' ? true : router.createUrlTree(['/acesso-negado'])),
    catchError(() => of(router.createUrlTree(['/indisponivel']))));
};
export const visitanteGuard: CanActivateFn = () => {
  const auth = inject(AuthService); const router = inject(Router);
  return auth.obterUsuarioAtual().pipe(map(u => u ? router.createUrlTree(['/dashboard']) : true),
    catchError(() => of(true)));
};
