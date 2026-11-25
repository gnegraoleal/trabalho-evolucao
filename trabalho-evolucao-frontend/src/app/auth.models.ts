export type PapelUsuario = 'USER' | 'ADMIN';
export interface Usuario {
  id: number;
  username: string;
  papel: PapelUsuario;
  loginCount: number;
  criadoEm: string;
  loginAtual: string | null;
  ultimoLogin: string | null;
}
export interface Cadastro { username: string; senha: string; confirmacaoSenha: string; }
export interface Perfil { username: string; senhaAtual: string; novaSenha: string; confirmacaoSenha: string; }
export const NOME_PATTERN = /^[A-Za-z0-9_]{3,16}$/;
export function senhaValida(s: string): boolean {
  const tamanho = Array.from(s).length;
  return tamanho >= 6 && tamanho <= 128;
}
export function validarNovaSenha(senha: string, confirmacao: string, obrigatoria = false): string {
  if (!obrigatoria && !senha && !confirmacao) return '';
  if (!senhaValida(senha)) return 'A senha deve ter de 6 a 128 caracteres.';
  if (senha !== confirmacao) return 'As senhas não coincidem.';
  return '';
}
export function erroHttp(e: unknown, fallback = 'Não foi possível concluir a operação.'): string {
  if (typeof e === 'object' && e !== null && 'error' in e) {
    const data = e.error;
    if (typeof data === 'object' && data !== null && 'message' in data && typeof data.message === 'string') return data.message;
  }
  return fallback;
}
