import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AdminService } from './admin.service';
import { AuthService } from '../auth.service';
import { NOME_PATTERN, PapelUsuario, erroHttp, validarNovaSenha, Usuario } from '../auth.models';
@Component({ imports:[FormsModule,RouterLink], templateUrl:'./admin-usuario-editar.html' })
export class AdminUsuarioEditarComponent {
  private readonly service=inject(AdminService); private readonly auth=inject(AuthService);
  private readonly router=inject(Router); private readonly route=inject(ActivatedRoute);
  readonly id=Number(this.route.snapshot.paramMap.get('id')); readonly original=signal<Usuario|null>(null);
  username='';papel:PapelUsuario='USER';novaSenha='';confirmacao='';
  readonly erro=signal('');readonly ocupado=signal(false);readonly aviso=signal('');
  constructor(){
    if(!Number.isSafeInteger(this.id)||this.id<=0){this.erro.set('Identificador inválido.');return;}
    this.service.obter(this.id).subscribe({next:u=>{this.original.set(u);this.username=u.username;this.papel=u.papel;},error:e=>this.erro.set(erroHttp(e))});
  }
  salvar(){
    const anterior=this.original();if(!anterior||this.ocupado())return;
    const msg=!NOME_PATTERN.test(this.username)?'Nome de usuário inválido.':validarNovaSenha(this.novaSenha,this.confirmacao);
    this.erro.set(msg);this.aviso.set('');if(msg)return;
    this.ocupado.set(true);
    const proprio=this.id===this.auth.usuario()?.id;
    const alterou=this.username!==anterior.username||this.papel!==anterior.papel||!!this.novaSenha;
    this.service.editar(this.id,{username:this.username,papel:this.papel,novaSenha:this.novaSenha,confirmacaoSenha:this.confirmacao})
      .pipe(finalize(()=>this.ocupado.set(false))).subscribe({next:u=>{
        this.original.set(u);this.novaSenha='';this.confirmacao='';
        if(proprio&&alterou){void this.router.navigate(['/login'],{queryParams:{motivo:'perfil'}});return;}
        this.aviso.set('Usuário atualizado com sucesso.');
      },error:e=>this.erro.set(erroHttp(e))});
  }
}
