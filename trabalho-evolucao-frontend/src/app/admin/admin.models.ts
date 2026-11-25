import { PapelUsuario, Usuario } from '../auth.models';
export interface Dashboard { totalUsuarios: number; totalAdministradores: number; totalContas: number; administradores: Usuario[]; cadastrosRecentes: Usuario[]; loginsRecentes: Usuario[]; }
export interface Pagina { content: Usuario[]; page: number; size: number; totalElements: number; totalPages: number; }
export interface EdicaoAdmin { username: string; papel: PapelUsuario; novaSenha: string; confirmacaoSenha: string; }
export type CampoOrdenacao = 'id' | 'username' | 'loginCount' | 'ultimoLogin';
export type Direcao = 'asc' | 'desc';
