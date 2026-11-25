import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { App } from './app';
describe('App',()=>{
  beforeEach(async()=>{await TestBed.configureTestingModule({imports:[App],providers:[provideRouter([]),provideHttpClient(),provideHttpClientTesting()]}).compileComponents();});
  it('exibe a navegação pública',()=>{const fixture=TestBed.createComponent(App);fixture.detectChanges();TestBed.inject(HttpTestingController).expectOne('/api/auth/me').flush({}, {status:401,statusText:'Unauthorized'});fixture.detectChanges();const element=fixture.nativeElement as HTMLElement;expect(element.textContent).toContain('iMarker');expect(element.textContent).toContain('Entrar');expect(element.textContent).not.toContain('Administração');});
});
