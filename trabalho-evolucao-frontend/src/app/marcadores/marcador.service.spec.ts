import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MarcadorService } from './marcador.service';

describe('MarcadorService', () => {
  let service: MarcadorService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(MarcadorService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lista os pontos do titular', () => {
    service.listar().subscribe((r) => expect(r.length).toBe(1));
    const req = http.expectOne('/api/marcadores');
    expect(req.request.method).toBe('GET');
    req.flush([{ id: 1, titulo: 'Casa', descricao: null, latitude: 0, longitude: 0 }]);
  });

  it('cria ponto sem enviar campo de usuário', () => {
    service.criar({ titulo: 'Casa', descricao: null, latitude: -12.9, longitude: -38.5 }).subscribe();
    http.expectOne('/api/csrf').flush(null);
    const req = http.expectOne('/api/marcadores');
    expect(req.request.method).toBe('POST');
    expect(Object.keys(req.request.body).sort()).toEqual(['descricao', 'latitude', 'longitude', 'titulo']);
    req.flush({ id: 2, titulo: 'Casa', descricao: null, latitude: -12.9, longitude: -38.5 });
  });

  it('exclui pelo id', () => {
    service.excluir(7).subscribe();
    http.expectOne('/api/csrf').flush(null);
    const req = http.expectOne('/api/marcadores/7');
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('regenera o link por POST', () => {
    service.regenerarLink().subscribe();
    http.expectOne('/api/csrf').flush(null);
    const req = http.expectOne('/api/mapa/link/regenerar');
    expect(req.request.method).toBe('POST');
    req.flush({ nomeUsuario: 'Ana', token: 'novo' });
  });

  it('codifica nome e token na rota pública', () => {
    service.mapaPublico('a b', 'x/y').subscribe();
    http.expectOne('/api/public/mapas/a%20b/x%2Fy').flush([]);
  });

  it('propaga 404 do link inválido ou revogado', () => {
    let status = 0;
    service.mapaPublico('Ana', 'velho').subscribe({ error: (e) => (status = e.status) });
    http.expectOne('/api/public/mapas/Ana/velho').flush('x', { status: 404, statusText: 'Not Found' });
    expect(status).toBe(404);
  });
});
