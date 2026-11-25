import * as L from 'leaflet';
import { ATRIBUICAO, ICONE_PADRAO, TILES_URL } from './leaflet-setup';
import { CENTRO_PADRAO, ZOOM_PADRAO, linksCompartilhamentoPonto } from './geo-utils';
import { PontoMapa } from './marcador.model';

export interface MapViewOpcoes {
  /** true no mapa do titular (mostra "Excluir"); false no mapa público (somente leitura, RF-07). */
  editavel: boolean;
  aoExcluir?: (chave: string | number) => void;
  /** Botão direito ou toque longo no mapa (RF-06). */
  aoEscolherPosicao?: (latitude: number, longitude: number) => void;
  /** Enter com o mapa focado: alternativa de teclado (RNF-08). */
  aoAbrirNoCentro?: () => void;
  aoFalharTiles?: () => void;
}

/**
 * Único renderizador de pontos, usado pelo painel e pelo mapa público (RNF-09).
 * Mantém um Map<chave, Marker>: criar/excluir mexe só no ponto afetado, sem recriar a camada (RNF-07).
 * Todo texto do usuário entra por textContent, nunca por HTML (DT-08).
 */
export class MapView {
  private readonly map: L.Map;
  private readonly camada = L.layerGroup();
  private readonly pinos = new Map<string | number, L.Marker>();
  private avisouTiles = false;

  constructor(private readonly el: HTMLElement, private readonly opcoes: MapViewOpcoes) {
    this.map = L.map(el, { keyboard: true }).setView(CENTRO_PADRAO, ZOOM_PADRAO);

    const tiles = L.tileLayer(TILES_URL, { maxZoom: 19, attribution: ATRIBUICAO }).addTo(this.map);
    tiles.on('tileerror', () => {
      if (this.avisouTiles) return;
      this.avisouTiles = true;
      this.opcoes.aoFalharTiles?.();
    });

    this.camada.addTo(this.map);

    if (opcoes.editavel) {
      this.map.on('contextmenu', (e: L.LeafletMouseEvent) => {
        const p = e.latlng.wrap(); // mantém longitude entre -180 e 180
        this.opcoes.aoEscolherPosicao?.(p.lat, p.lng);
      });
      el.addEventListener('keydown', (ev: KeyboardEvent) => {
        if (ev.key === 'Enter' && ev.target === el) this.opcoes.aoAbrirNoCentro?.();
      });
    }
  }

  definirPontos(pontos: PontoMapa[]): void {
    this.camada.clearLayers();
    this.pinos.clear();
    pontos.forEach((p) => this.adicionar(p));
  }

  adicionar(p: PontoMapa): void {
    this.remover(p.chave);

    const pino = L.marker([p.latitude, p.longitude], { title: p.titulo, icon: ICONE_PADRAO });

    const rotulo = document.createElement('span');
    rotulo.textContent = p.titulo;
    pino.bindTooltip(rotulo, { permanent: true, direction: 'top', offset: [0, -34] });
    pino.bindPopup(this.criarPopup(p));

    // Duplo clique centraliza e aproxima (RF-07)
    pino.on('dblclick', (e) => {
      L.DomEvent.stopPropagation(e);
      pino.closePopup();
      this.map.setView(pino.getLatLng(), this.map.getZoom() + 2);
    });

    pino.addTo(this.camada);
    this.pinos.set(p.chave, pino);
  }

  remover(chave: string | number): void {
    const pino = this.pinos.get(chave);
    if (!pino) return;
    this.camada.removeLayer(pino);
    this.pinos.delete(chave);
  }

  destacar(chave: string | number): void {
    const pino = this.pinos.get(chave);
    if (!pino) return;
    this.map.setView(pino.getLatLng(), Math.max(this.map.getZoom(), 15));
    pino.openPopup();
  }

  centroAtual(): { lat: number; lng: number } {
    const c = this.map.getCenter().wrap();
    return { lat: c.lat, lng: c.lng };
  }

  centralizar(latitude: number, longitude: number, zoom = 13): void {
    this.map.setView([latitude, longitude], zoom);
  }

  ajustarAosPontos(): void {
    const posicoes = [...this.pinos.values()].map((p) => p.getLatLng());
    if (posicoes.length === 0) return;
    this.map.fitBounds(L.latLngBounds(posicoes), { padding: [40, 40], maxZoom: 15 });
  }

  destruir(): void {
    this.map.remove();
    this.pinos.clear();
  }

  private criarPopup(p: PontoMapa): HTMLElement {
    const raiz = document.createElement('div');
    raiz.className = 'popup-ponto';

    raiz.append(this.el_('h3', p.titulo, 'popup-titulo'));
    raiz.append(
      this.el_('p', p.descricao && p.descricao.trim() ? p.descricao : 'Este ponto não tem descrição.', 'popup-descricao')
    );

    const links = linksCompartilhamentoPonto(p.latitude, p.longitude, p.titulo);
    const lista = document.createElement('p');
    lista.className = 'popup-links';
    lista.append(
      this.link('Abrir no OpenStreetMap', links.osm),
      ' · ',
      this.link('Compartilhar no Facebook', links.facebook),
      ' · ',
      this.link('Compartilhar no X', links.x)
    );
    raiz.append(lista);

    if (this.opcoes.editavel && this.opcoes.aoExcluir) {
      const botao = document.createElement('button');
      botao.type = 'button';
      botao.className = 'popup-excluir';
      botao.textContent = 'Excluir este ponto';
      botao.addEventListener('click', () => this.opcoes.aoExcluir?.(p.chave));
      raiz.append(botao);
    }
    return raiz;
  }

  private el_(tag: string, texto: string, classe: string): HTMLElement {
    const e = document.createElement(tag);
    e.className = classe;
    e.textContent = texto;
    return e;
  }

  private link(texto: string, href: string): HTMLAnchorElement {
    const a = document.createElement('a');
    a.href = href;
    a.target = '_blank';
    a.rel = 'noopener noreferrer';
    a.textContent = texto;
    return a;
  }
}
