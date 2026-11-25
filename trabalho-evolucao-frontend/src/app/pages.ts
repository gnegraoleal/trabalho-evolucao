import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from './auth.service';
import { erroHttp, NOME_PATTERN, validarNovaSenha } from './auth.models';

@Component({ imports: [RouterLink], template: `
  <section class="panel hero">
    <p class="eyebrow">Seus lugares, seu mapa</p><h1>iMarker</h1>
    <p>Organize lugares importantes em um mapa pessoal e compartilhe seus pontos quando desejar.</p>
    <div class="actions"><a class="primary" routerLink="/register">Criar conta</a><a class="secondary" routerLink="/login">Entrar</a></div>
  </section>` })
export class HomePage {}

@Component({ imports: [RouterLink], template: `
  <section class="panel"><h1>Sobre o iMarker</h1><p>O iMarker permite registrar pontos geográficos em um mapa pessoal, consultar suas informações e compartilhar lugares.</p>
  <p>Esta versão evolui o sistema original para uma arquitetura Angular, Spring Boot e PostgreSQL.</p><a routerLink="/">Voltar ao início</a></section>` })
export class AboutPage {}

@Component({ imports: [RouterLink], template: `<section class="panel"><h1>Acesso negado</h1><p>Esta área é restrita a administradores.</p><a routerLink="/dashboard">Voltar ao mapa</a></section>` })
export class AcessoNegadoPage {}
@Component({ imports: [RouterLink], template: `<section class="panel"><h1>Serviço indisponível</h1><p>Não foi possível consultar a sessão. Verifique se o backend está em execução.</p><a routerLink="/login">Tentar novamente</a></section>` })
export class IndisponivelPage {}
@Component({ imports: [RouterLink], template: `<section class="panel"><h1>Página não encontrada</h1><a routerLink="/">Ir para o início</a></section>` })
export class NotFoundPage {}

@Component({ imports: [FormsModule, RouterLink], template: `
  <section class="panel form-panel"><h1>Criar conta</h1><p>Não é necessário informar e-mail.</p>
  <form (ngSubmit)="salvar()" novalidate>
    <label for="username">Nome de usuário</label><input id="username" name="username" [(ngModel)]="username" autocomplete="username" required maxlength="16" />
    <small>3 a 16 letras ASCII, números ou sublinhados.</small>
    <label for="senha">Senha</label><input id="senha" name="senha" type="password" [(ngModel)]="senha" autocomplete="new-password" required />
    <small>6 a 128 caracteres.</small>
    <label for="confirmacao">Confirme a senha</label><input id="confirmacao" name="confirmacao" type="password" [(ngModel)]="confirmacao" autocomplete="new-password" required />
    @if (erro()) { <p class="error" role="alert">{{erro()}}</p> }
    <button class="primary" type="submit" [disabled]="ocupado()">{{ocupado() ? 'Cadastrando...' : 'Cadastrar'}}</button>
  </form><p>Já tem conta? <a routerLink="/login">Entrar</a></p></section>` })
export class RegisterPage {
  private readonly auth = inject(AuthService); private readonly router = inject(Router);
  username = ''; senha = ''; confirmacao = '';
  readonly erro = signal(''); readonly ocupado = signal(false);
  salvar() {
    if (this.ocupado()) return;
    const msg = !NOME_PATTERN.test(this.username) ? 'Nome de usuário inválido.' : validarNovaSenha(this.senha, this.confirmacao, true);
    this.erro.set(msg); if (msg) return;
    this.ocupado.set(true);
    this.auth.cadastrar({username: this.username, senha: this.senha, confirmacaoSenha: this.confirmacao}).pipe(finalize(() => this.ocupado.set(false)))
      .subscribe({next: () => void this.router.navigate(['/login'], {queryParams: {motivo: 'cadastro'}}), error: e => this.erro.set(erroHttp(e))});
  }
}

@Component({ imports: [FormsModule, RouterLink], template: `
  <section class="panel form-panel"><h1>Entrar</h1>
    @if (aviso) { <p class="success" role="status">{{aviso}}</p> }
    <form (ngSubmit)="entrar()" novalidate>
      <label for="username">Nome de usuário</label><input id="username" name="username" [(ngModel)]="username" autocomplete="username" required />
      <label for="senha">Senha</label><input id="senha" name="senha" type="password" [(ngModel)]="senha" autocomplete="current-password" required />
      <label class="check"><input name="lembrar" type="checkbox" [(ngModel)]="lembrar" /> Lembrar minha sessão</label>
      @if (erro()) { <p class="error" role="alert">{{erro()}}</p> }
      <button class="primary" type="submit" [disabled]="ocupado()">{{ocupado() ? 'Entrando...' : 'Entrar'}}</button>
    </form><p>Não possui conta? <a routerLink="/register">Cadastre-se</a></p>
  </section>` })
export class LoginPage {
  private readonly auth = inject(AuthService); private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  username = ''; senha = ''; lembrar = false;
  readonly erro = signal(''); readonly ocupado = signal(false);
  readonly aviso = ({cadastro: 'Conta criada. Faça login.', perfil: 'Perfil atualizado. Entre novamente.', cancelamento: 'Conta cancelada.'} as Record<string,string>)[this.route.snapshot.queryParamMap.get('motivo') ?? ''] ?? '';
  entrar() {
    if (this.ocupado()) return;
    if (!this.username || !this.senha) { this.erro.set('Informe usuário e senha.'); return; }
    this.erro.set(''); this.ocupado.set(true);
    this.auth.login(this.username, this.senha, this.lembrar).pipe(finalize(() => this.ocupado.set(false)))
      .subscribe({next: () => void this.router.navigate(['/dashboard']), error: e => this.erro.set(erroHttp(e, 'Não foi possível entrar.'))});
  }
}

@Component({ imports: [FormsModule, RouterLink], template: `
  <section class="panel form-panel"><h1>Meu perfil</h1>
    <form (ngSubmit)="salvar()" novalidate>
      <label for="nome">Nome de usuário</label><input id="nome" name="nome" [(ngModel)]="username" autocomplete="username" required maxlength="16" />
      <label for="nova">Nova senha (opcional)</label><input id="nova" name="nova" type="password" [(ngModel)]="novaSenha" autocomplete="new-password" />
      <label for="confirmacao">Confirme a nova senha</label><input id="confirmacao" name="confirmacao" type="password" [(ngModel)]="confirmacao" autocomplete="new-password" />
      <label for="atual">Senha atual</label><input id="atual" name="atual" type="password" [(ngModel)]="senhaAtual" autocomplete="current-password" required />
      <small>Obrigatória para confirmar a alteração. Você precisará entrar novamente após salvar.</small>
      @if (erro()) { <p class="error" role="alert">{{erro()}}</p> }
      <div class="actions"><button class="primary" type="submit" [disabled]="ocupado()">Salvar</button><a class="secondary" routerLink="/dashboard">Cancelar</a></div>
    </form>
    <div class="danger-zone"><h2>Cancelar conta</h2><p>Remova permanentemente sua conta e seus marcadores. Informe a senha atual acima para confirmar.</p>
      @if (!confirmando()) { <button class="danger" type="button" (click)="confirmando.set(true)">Cancelar minha conta</button> }
      @else { <p role="alert">Esta ação é irreversível. Confirma a exclusão?</p><div class="actions"><button class="danger" type="button" (click)="cancelarConta()" [disabled]="ocupado()">Confirmar exclusão</button><button class="secondary" type="button" (click)="confirmando.set(false)">Voltar</button></div> }
    </div>
  </section>` })
export class ContaPage {
  private readonly auth = inject(AuthService); private readonly router = inject(Router);
  username = this.auth.usuario()?.username ?? ''; novaSenha = ''; confirmacao = ''; senhaAtual = '';
  readonly erro = signal(''); readonly ocupado = signal(false); readonly confirmando = signal(false);
  salvar() {
    if (this.ocupado()) return;
    const msg = !NOME_PATTERN.test(this.username) ? 'Nome de usuário inválido.' : !this.senhaAtual ? 'Informe a senha atual.' : validarNovaSenha(this.novaSenha,this.confirmacao);
    this.erro.set(msg); if (msg) return;
    this.ocupado.set(true);
    this.auth.alterarPerfil({username:this.username, senhaAtual:this.senhaAtual, novaSenha:this.novaSenha, confirmacaoSenha:this.confirmacao})
      .pipe(finalize(() => this.ocupado.set(false))).subscribe({next: () => void this.router.navigate(['/login'], {queryParams:{motivo:'perfil'}}),error:e=>this.erro.set(erroHttp(e))});
  }
  cancelarConta() {
    if (!this.senhaAtual) { this.erro.set('Informe a senha atual.'); return; }
    if (this.ocupado() || !this.confirmando()) return;
    this.erro.set(''); this.ocupado.set(true);
    this.auth.cancelarConta(this.senhaAtual).pipe(finalize(()=>this.ocupado.set(false)))
      .subscribe({next:()=>void this.router.navigate(['/login'],{queryParams:{motivo:'cancelamento'}}),error:e=>this.erro.set(erroHttp(e))});
  }
}
