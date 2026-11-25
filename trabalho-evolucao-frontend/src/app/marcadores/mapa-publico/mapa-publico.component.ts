import { DOCUMENT } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import {
  AfterViewInit, Component, DestroyRef, ElementRef, OnDestroy, ViewChild, ViewEncapsulation, inject, signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';
import { Subscription } from 'rxjs';
import { MarcadorService } from '../marcador.service';
import { paraPontoPublico } from '../marcador.model';
import { MapView } from '../map-view';

/**
 * Mapa compartilhado: somente leitura, sem login (RF-11).
 * Rota sugerida: 'mapa/:nomeUsuario/:token', SEM guard.
 */
@Component({
  selector: 'app-mapa-publico',
  standalone: true,
  templateUrl: './mapa-publico.component.html',
  styleUrl: './mapa-publico.component.css',
  encapsulation: ViewEncapsulation.None,
})
export class MapaPublicoComponent implements AfterViewInit, OnDestroy {
  private readonly api = inject(MarcadorService);
  private readonly route = inject(ActivatedRoute);
  private readonly titulo = inject(Title);
  private readonly doc = inject(DOCUMENT);
  private readonly destroyRef = inject(DestroyRef);

  @ViewChild('mapa', { static: true }) private mapaEl!: ElementRef<HTMLElement>;

  readonly nome = signal('');
  readonly total = signal(0);
  readonly carregando = signal(true);
  readonly erro = signal<string | null>(null);
  readonly avisoMapa = signal<string | null>(null);

  private mapView?: MapView;
  private consulta?: Subscription;
  private readonly metaReferrer: HTMLMetaElement;

  constructor() {
    // RNF-04: o token está na URL; impede que ele vaze no Referer de links externos do popup.
    this.metaReferrer = this.doc.createElement('meta');
    this.metaReferrer.name = 'referrer';
    this.metaReferrer.content = 'no-referrer';
    this.doc.head.appendChild(this.metaReferrer);
  }

  ngAfterViewInit(): void {
    this.mapView = new MapView(this.mapaEl.nativeElement, {
      editavel: false, // sem "Excluir" na visão pública (RF-07)
      aoFalharTiles: () => this.avisoMapa.set('Não foi possível carregar o mapa de fundo.'),
    });

    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((p) => {
      this.carregar(p.get('nomeUsuario') ?? '', p.get('token') ?? '');
    });
  }

  ngOnDestroy(): void {
    this.consulta?.unsubscribe();
    this.metaReferrer.remove();
    this.mapView?.destruir();
  }

  private carregar(nome: string, token: string): void {
    this.consulta?.unsubscribe();
    this.mapView?.definirPontos([]);
    this.total.set(0);
    this.nome.set(nome);
    this.titulo.setTitle(`Mapa de ${nome}`);
    this.carregando.set(true);
    this.erro.set(null);

    this.consulta = this.api.mapaPublico(nome, token).subscribe({
      next: (pontos) => {
        this.mapView?.definirPontos(pontos.map(paraPontoPublico));
        this.mapView?.ajustarAosPontos();
        this.total.set(pontos.length);
        this.carregando.set(false);
      },
      error: (e: HttpErrorResponse) => {
        // mesma mensagem para usuário inexistente, token errado e link revogado
        this.erro.set(
          e.status === 404
            ? 'Este link é inválido ou foi revogado pelo dono do mapa.'
            : 'Não foi possível carregar o mapa agora. Tente novamente em instantes.'
        );
        this.carregando.set(false);
      },
    });
  }
}
