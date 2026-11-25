import { Component, inject, signal, OnDestroy } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize, Subscription } from 'rxjs';
import { AdminService } from './admin.service';
import { AuthService } from '../auth.service';
import { Usuario, erroHttp } from '../auth.models';
import { CampoOrdenacao, Direcao, Pagina } from './admin.models';
@Component({ imports:[FormsModule,RouterLink,DatePipe], templateUrl:'./admin-usuarios.html' })
export class AdminUsuariosComponent implements OnDestroy {
  private readonly service = inject(AdminService); readonly auth = inject(AuthService);
  busca = ''; pesquisa = ''; sort:CampoOrdenacao='id'; direction:Direcao='desc'; page=0;
  readonly dados = signal<Pagina | null>(null); readonly erro=signal(''); readonly aviso=signal('');
  readonly carregando=signal(false); readonly excluindo=signal(false); readonly alvo=signal<Usuario|null>(null);
  private consulta?: Subscription;
  constructor() { this.carregar(); }
  ngOnDestroy() { this.consulta?.unsubscribe(); }
  carregar() {
    this.consulta?.unsubscribe(); this.erro.set(''); this.carregando.set(true);
    this.consulta=this.service.listar(this.pesquisa,this.page,this.sort,this.direction).pipe(finalize(()=>this.carregando.set(false)))
      .subscribe({next:p=>this.dados.set(p),error:e=>{this.dados.set(null);this.erro.set(erroHttp(e));}});
  }
  pesquisar() { this.pesquisa=this.busca.trim();this.page=0;this.aviso.set('');this.carregar(); }
  limpar() { this.busca='';this.pesquisar(); }
  ordenar() { this.page=0;this.carregar(); }
  navegar(page:number) { if(page<0 || page >= (this.dados()?.totalPages ?? 0)) return; this.page=page;this.carregar(); }
  pedirExclusao(u:Usuario) { if(u.id===this.auth.usuario()?.id)return;this.alvo.set(u);this.aviso.set('');this.erro.set(''); }
  excluir() {
    const u=this.alvo();if(!u||this.excluindo()||u.id===this.auth.usuario()?.id)return;
    this.excluindo.set(true);this.erro.set('');
    this.service.excluir(u.id).pipe(finalize(()=>this.excluindo.set(false))).subscribe({next:()=>{
      this.alvo.set(null);this.aviso.set(`Conta ${u.username} excluída.`);
      if(this.dados()?.content.length===1 && this.page>0)this.page--;this.carregar();
    },error:e=>this.erro.set(erroHttp(e))});
  }
}
