import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { catchError, finalize, map, Observable, of, shareReplay, switchMap, tap, throwError } from 'rxjs';
import { Cadastro, Perfil, Usuario } from './auth.models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly estado = signal<Usuario | null>(null);
  readonly usuario = this.estado.asReadonly();
  private consulta?: Observable<Usuario | null>;
  csrf() { return this.http.get<void>('/api/csrf'); }
  obterUsuarioAtual() {
    if (this.consulta) return this.consulta;
    this.consulta = this.http.get<Usuario>('/api/auth/me').pipe(
      tap(u => this.estado.set(u)),
      catchError((e: HttpErrorResponse) => {
        this.estado.set(null);
        return e.status === 401 ? of(null) : throwError(() => e);
      }),
      finalize(() => this.consulta = undefined),
      shareReplay({bufferSize: 1, refCount: false})
    );
    return this.consulta;
  }
  cadastrar(dto: Cadastro) {
    return this.csrf().pipe(switchMap(() => this.http.post<Usuario>('/api/auth/register', dto)));
  }
  login(username: string, senha: string, lembrar: boolean) {
    const body = new HttpParams().set('username', username).set('password', senha).set('remember-me', String(lembrar));
    return this.csrf().pipe(
      switchMap(() => this.http.post<void>('/api/auth/login', body)),
      switchMap(() => this.csrf()),
      switchMap(() => this.obterUsuarioAtual()),
      map(u => { if (!u) throw new Error('Sessão não criada.'); return u; })
    );
  }
  logout() {
    return this.csrf().pipe(switchMap(() => this.http.post<void>('/api/auth/logout', null)), tap(() => this.estado.set(null)));
  }
  alterarPerfil(dto: Perfil) {
    return this.csrf().pipe(switchMap(() => this.http.put<Usuario>('/api/users/me', dto)), tap(() => this.estado.set(null)));
  }
  cancelarConta(senhaAtual: string) {
    return this.csrf().pipe(switchMap(() => this.http.post<void>('/api/users/me/cancelar', { senhaAtual })), tap(() => this.estado.set(null)));
  }
}
