import { Component, inject, signal } from '@angular/core';
import { RouterOutlet, RouterLink, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { erroHttp } from './auth.models';
import { finalize } from 'rxjs';
@Component({ selector:'app-root',imports:[RouterOutlet,RouterLink],templateUrl:'./app.html',styleUrl:'./app.css' })
export class App {
  readonly auth=inject(AuthService);private readonly router=inject(Router);
  readonly erro=signal('');readonly saindo=signal(false);
  constructor(){this.auth.obterUsuarioAtual().subscribe({error:()=>{}});}
  sair(){
    if(this.saindo())return;this.erro.set('');this.saindo.set(true);
    this.auth.logout().pipe(finalize(()=>this.saindo.set(false))).subscribe({next:()=>void this.router.navigate(['/login']),error:e=>this.erro.set(erroHttp(e))});
  }
}
