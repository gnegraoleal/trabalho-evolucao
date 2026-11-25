import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { switchMap } from 'rxjs';
import { AuthService } from '../auth.service';
import { Usuario } from '../auth.models';
import { Dashboard, Pagina, EdicaoAdmin, CampoOrdenacao, Direcao } from './admin.models';

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient); private readonly auth = inject(AuthService);
  private readonly base = '/api/admin';
  dashboard() { return this.http.get<Dashboard>(`${this.base}/dashboard`); }
  listar(busca: string, page: number, sort: CampoOrdenacao, direction: Direcao) {
    const params = new HttpParams().set('busca',busca).set('page',page).set('sort',sort).set('direction',direction);
    return this.http.get<Pagina>(`${this.base}/usuarios`,{params});
  }
  obter(id: number) { return this.http.get<Usuario>(`${this.base}/usuarios/${id}`); }
  editar(id: number, dto: EdicaoAdmin) { return this.auth.csrf().pipe(switchMap(()=>this.http.put<Usuario>(`${this.base}/usuarios/${id}`,dto))); }
  excluir(id: number) { return this.auth.csrf().pipe(switchMap(()=>this.http.delete<void>(`${this.base}/usuarios/${id}`))); }
}
