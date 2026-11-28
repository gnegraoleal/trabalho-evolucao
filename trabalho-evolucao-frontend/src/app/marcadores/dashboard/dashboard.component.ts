import { HttpErrorResponse } from '@angular/common/http';
import {
  AfterViewInit, Component, ElementRef, OnDestroy, ViewChild, ViewEncapsulation, computed, inject, signal,
} from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { coordenadasValidas, posicaoAtual, urlMapaPublico, validarTitulo } from '../geo-utils';
import { LinkPublico, Marcador, paraPonto } from '../marcador.model';
import { MarcadorService } from '../marcador.service';
import { MapView } from '../map-view';

interface FormularioPonto {
  titulo: string;
  descricao: string;
  latitude: number | null;
  longitude: number | null;
}

const arredondar = (n: number) => Number(n.toFixed(6));
const formularioVazio = (): FormularioPonto => ({ titulo: '', descricao: '', latitude: null, longitude: null });

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css',
  encapsulation: ViewEncapsulation.None, // o popup do Leaflet é criado fora do template do Angular
})
export class DashboardComponent implements AfterViewInit, OnDestroy {
  private readonly api = inject(MarcadorService);

  @ViewChild('mapa', { static: true }) private mapaEl!: ElementRef<HTMLElement>;
  @ViewChild('dlgPonto', { static: true }) private dialogoPonto!: ElementRef<HTMLDialogElement>;
  @ViewChild('formPonto', { static: true }) private formPonto!: NgForm;
  @ViewChild('dlgLink', { static: true }) private dialogoLink!: ElementRef<HTMLDialogElement>;
  @ViewChild('campoUrl', { static: true }) private campoUrl!: ElementRef<HTMLInputElement>;

  readonly marcadores = signal<Marcador[]>([]);
  readonly carregando = signal(true);
  readonly erroCarga = signal<string | null>(null);
  readonly avisoMapa = signal<string | null>(null);
  readonly anuncio = signal('');
  readonly erroForm = signal<string | null>(null);
  readonly salvando = signal(false);
  readonly link = signal<LinkPublico | null>(null);
  readonly msgLink = signal('');
  readonly urlPublica = computed(() => {
    const l = this.link();
    return l ? urlMapaPublico(window.location.origin, l.nomeUsuario, l.token) : '';
  });

  form: FormularioPonto = formularioVazio();
  private mapView?: MapView;

  ngAfterViewInit(): void {
    this.mapView = new MapView(this.mapaEl.nativeElement, {
      editavel: true,
      aoExcluir: (chave) => this.excluir(Number(chave)),
      aoEscolherPosicao: (lat, lng) => this.abrirNovoPonto(lat, lng),
      aoAbrirNoCentro: () => this.abrirNovoPonto(),
      aoFalharTiles: () =>
        this.avisoMapa.set('Não foi possível carregar o mapa de fundo. Você ainda pode usar a lista e o formulário de pontos.'),
    });
    this.carregar();
  }

  ngOnDestroy(): void {
    this.mapView?.destruir();
  }

  // ---------- carga ----------

  carregar(): void {
    this.carregando.set(true);
    this.erroCarga.set(null);
    this.api.listar().subscribe({
      next: (lista) => {
        this.marcadores.set(lista);
        this.mapView?.definirPontos(lista.map(paraPonto));
        this.carregando.set(false);
        void this.centralizarInicial();
      },
      error: () => {
        this.erroCarga.set('Não foi possível carregar seus pontos.');
        this.carregando.set(false);
        void this.centralizarInicial();
      },
    });
  }

  /** Localização do navegador → pontos existentes → centro padrão (RNF-05). */
  private async centralizarInicial(): Promise<void> {
    const pos = await posicaoAtual();
    if (pos) this.mapView?.centralizar(pos[0], pos[1], 13);
    else if (this.marcadores().length > 0) this.mapView?.ajustarAosPontos();
    // sem nada disso, o MapView já nasce no centro padrão
  }

  // ---------- criar ----------

  abrirNovoPonto(latitude?: number, longitude?: number): void {
    const c = latitude !== undefined && longitude !== undefined
      ? { lat: latitude, lng: longitude }
      : this.mapView!.centroAtual();
    this.form = { titulo: '', descricao: '', latitude: arredondar(c.lat), longitude: arredondar(c.lng) };
    this.formPonto.resetForm(this.form);
    this.erroForm.set(null);
    const dlg = this.dialogoPonto.nativeElement;
    if (!dlg.open) dlg.showModal(); // o foco volta sozinho ao elemento de origem ao fechar
  }

  usarCentroDoMapa(): void {
    const c = this.mapView!.centroAtual();
    this.form.latitude = arredondar(c.lat);
    this.form.longitude = arredondar(c.lng);
  }

  async usarMinhaLocalizacao(): Promise<void> {
    const formulario = this.form;
    const pos = await posicaoAtual();
    if (this.form !== formulario) return;
    if (!pos) {
      this.erroForm.set('Não foi possível obter sua localização. Digite as coordenadas ou use o centro do mapa.');
      return;
    }
    this.erroForm.set(null);
    this.form.latitude = arredondar(pos[0]);
    this.form.longitude = arredondar(pos[1]);
  }

  salvar(): void {
    const erroTitulo = validarTitulo(this.form.titulo);
    if (erroTitulo) return void this.erroForm.set(erroTitulo);
    if (!coordenadasValidas(this.form.latitude, this.form.longitude)) {
      return void this.erroForm.set('Latitude deve ficar entre −90 e 90 e longitude entre −180 e 180.');
    }

    this.salvando.set(true);
    this.api
      .criar({
        titulo: this.form.titulo.trim(),
        descricao: this.form.descricao.trim() || null,
        latitude: this.form.latitude!,
        longitude: this.form.longitude!,
      })
      .subscribe({
        next: (m) => {
          // atualiza só o ponto afetado (RNF-07)
          this.marcadores.update((l) => [m, ...l]);
          this.mapView?.adicionar(paraPonto(m));
          this.salvando.set(false);
          this.dialogoPonto.nativeElement.close();
          this.anuncio.set(`Ponto “${m.titulo}” criado.`);
        },
        error: (e: HttpErrorResponse) => {
          this.salvando.set(false);
          // RD-11: em falha nada foi alterado
          this.erroForm.set(
            e.status === 400 ? 'O servidor recusou os dados. Confira título e coordenadas.'
            : e.status === 401 ? 'Sua sessão expirou. Entre novamente.'
            : 'Não foi possível salvar. O ponto não foi criado; tente de novo.'
          );
        },
      });
  }

  // ---------- excluir / navegar ----------

  excluir(id: number): void {
    const m = this.marcadores().find((x) => x.id === id);
    if (!m || !window.confirm(`Excluir o ponto “${m.titulo}”?`)) return;

    this.api.excluir(id).subscribe({
      next: () => {
        this.marcadores.update((l) => l.filter((x) => x.id !== id));
        this.mapView?.remover(id);
        this.anuncio.set(`Ponto “${m.titulo}” excluído.`);
      },
      error: () => this.anuncio.set('Não foi possível excluir o ponto. Nada foi alterado.'),
    });
  }

  destacar(m: Marcador): void {
    this.mapView?.destacar(m.id);
  }

  // ---------- compartilhar mapa ----------

  abrirCompartilhar(): void {
    this.api.obterLink().subscribe({
      next: (l) => {
        this.link.set(l);
        this.msgLink.set('');
        this.dialogoLink.nativeElement.showModal();
      },
      error: () => this.anuncio.set('Não foi possível obter o link público.'),
    });
  }

  async copiar(): Promise<void> {
    try {
      await navigator.clipboard.writeText(this.urlPublica());
      this.msgLink.set('Link copiado.');
    } catch {
      this.campoUrl.nativeElement.select();
      this.msgLink.set('Não foi possível copiar sozinho. O endereço está selecionado: copie com Ctrl+C.');
    }
  }

  regenerar(): void {
    if (!window.confirm('Gerar um novo link? O endereço atual deixará de funcionar.')) return;
    this.api.regenerarLink().subscribe({
      next: (l) => {
        this.link.set(l);
        this.msgLink.set('Novo link gerado. O anterior não funciona mais.');
      },
      error: () => this.msgLink.set('Não foi possível gerar um novo link. O endereço atual continua válido.'),
    });
  }

  fechar(dialogo: HTMLDialogElement): void {
    dialogo.close();
  }
}
