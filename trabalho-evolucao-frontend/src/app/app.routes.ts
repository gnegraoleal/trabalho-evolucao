import { Routes } from '@angular/router';
import { authGuard, adminGuard, visitanteGuard } from './guards';
import { HomePage, AboutPage, RegisterPage, LoginPage, ContaPage, AcessoNegadoPage, IndisponivelPage, NotFoundPage } from './pages';
export const routes: Routes = [
  {path:'',component:HomePage,canActivate:[visitanteGuard]},
  {path:'home',redirectTo:'',pathMatch:'full'},
  {path:'about',component:AboutPage},
  {path:'register',component:RegisterPage,canActivate:[visitanteGuard]},
  {path:'login',component:LoginPage,canActivate:[visitanteGuard]},
  {path:'users/edit',component:ContaPage,canActivate:[authGuard]},
  {path:'perfil',redirectTo:'users/edit',pathMatch:'full'},
  {path:'dashboard',redirectTo:'mapa',pathMatch:'full'},
  {path:'mapa',canActivate:[authGuard],loadComponent:()=>import('./map.component').then(m=>m.MapComponent)},
  {path:'admin',canActivate:[adminGuard],canActivateChild:[adminGuard],children:[
    {path:'',loadComponent:()=>import('./admin/admin-dashboard').then(m=>m.AdminDashboardComponent)},
    {path:'usuarios',loadComponent:()=>import('./admin/admin-usuarios').then(m=>m.AdminUsuariosComponent)},
    {path:'usuarios/:id/editar',loadComponent:()=>import('./admin/admin-usuario-editar').then(m=>m.AdminUsuarioEditarComponent)}
  ]},
  {path:'acesso-negado',component:AcessoNegadoPage},
  {path:'indisponivel',component:IndisponivelPage},
  {path:'**',component:NotFoundPage}
];
