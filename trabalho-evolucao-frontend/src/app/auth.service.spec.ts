import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { AuthService } from './auth.service';
import { Usuario } from './auth.models';
describe('AuthService',()=>{
  let auth: AuthService;let http: HttpTestingController;
  const u:Usuario={id:1,username:'teste',papel:'USER',loginCount:1,criadoEm:'',loginAtual:null,ultimoLogin:null};
  beforeEach(()=>{TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting()]});auth=TestBed.inject(AuthService);http=TestBed.inject(HttpTestingController);});
  afterEach(()=>http.verify());
  it('inicializa CSRF, envia credenciais e consulta sessão',()=>{
    auth.login('teste','senha123',true).subscribe(v=>expect(v).toEqual(u));
    http.expectOne('/api/csrf').flush(null);const login=http.expectOne('/api/auth/login');
    expect(login.request.method).toBe('POST');expect(login.request.body.get('remember-me')).toBe('true');login.flush(null);
    http.expectOne('/api/csrf').flush(null);http.expectOne('/api/auth/me').flush(u);expect(auth.usuario()).toEqual(u);
  });
  it('não inventa sessão para login incorreto',()=>{
    auth.login('teste','errada',false).subscribe({next:()=>{throw new Error('Login indevido');},error:e=>expect(e.status).toBe(401)});
    http.expectOne('/api/csrf').flush(null);http.expectOne('/api/auth/login').flush({}, {status:401,statusText:'Unauthorized'});expect(auth.usuario()).toBeNull();
  });
});
