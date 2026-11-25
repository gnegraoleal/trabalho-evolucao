import { TestBed } from '@angular/core/testing';
import { Router, ActivatedRouteSnapshot, RouterStateSnapshot, provideRouter, UrlTree } from '@angular/router';
import { firstValueFrom, Observable, of } from 'rxjs';
import { authGuard, adminGuard } from './guards';
import { AuthService } from './auth.service';
import { Usuario } from './auth.models';
describe('Guards de autenticação e autorização',()=>{
  let usuario: Usuario|null;
  beforeEach(()=>{usuario=null;TestBed.configureTestingModule({providers:[provideRouter([]),{provide:AuthService,useValue:{obterUsuarioAtual:()=>of(usuario)}}]});});
  const conta=(papel:'USER'|'ADMIN'):Usuario=>({id:1,username:'teste',papel,loginCount:1,criadoEm:'',loginAtual:null,ultimoLogin:null});
  const executar=(guard:typeof authGuard)=>firstValueFrom(TestBed.runInInjectionContext(()=>guard({} as ActivatedRouteSnapshot,{} as RouterStateSnapshot)) as Observable<boolean|UrlTree>);
  it('exige login para acessar rota protegida',async()=>{
    const result=await executar(authGuard);expect(TestBed.inject(Router).serializeUrl(result as UrlTree)).toBe('/login');
    usuario=conta('USER');expect(await executar(authGuard)).toBe(true);
  });
  it('recusa usuário comum na administração e aceita administrador',async()=>{
    usuario=conta('USER');const result=await executar(adminGuard);expect(TestBed.inject(Router).serializeUrl(result as UrlTree)).toBe('/acesso-negado');
    usuario=conta('ADMIN');expect(await executar(adminGuard)).toBe(true);
  });
});
