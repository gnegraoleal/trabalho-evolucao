import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, switchMap } from 'rxjs';
import { AuthService } from '../auth.service';
import { LinkPublico, Marcador, MarcadorPublico, NovoMarcador } from './marcador.model';

/** Usa caminhos relativos: o proxy.conf.json encaminha /api para o Spring (porta 8080). */
@Injectable({ providedIn: 'root' })
export class MarcadorService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);

  listar(): Observable<Marcador[]> {
    return this.http.get<Marcador[]>('/api/marcadores');
  }

  criar(novo: NovoMarcador): Observable<Marcador> {
    return this.auth.csrf().pipe(switchMap(() => this.http.post<Marcador>('/api/marcadores', novo)));
  }

  excluir(id: number): Observable<void> {
    return this.auth.csrf().pipe(switchMap(() => this.http.delete<void>(`/api/marcadores/${id}`)));
  }

  obterLink(): Observable<LinkPublico> {
    return this.http.get<LinkPublico>('/api/mapa/link');
  }

  regenerarLink(): Observable<LinkPublico> {
    return this.auth.csrf().pipe(switchMap(() => this.http.post<LinkPublico>('/api/mapa/link/regenerar', {})));
  }

  mapaPublico(nomeUsuario: string, token: string): Observable<MarcadorPublico[]> {
    return this.http.get<MarcadorPublico[]>(
      `/api/public/mapas/${encodeURIComponent(nomeUsuario)}/${encodeURIComponent(token)}`
    );
  }
}
