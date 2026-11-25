import { NOME_PATTERN, senhaValida, validarNovaSenha } from './auth.models';
describe('Política de conta',()=>{
  it('valida nome e limites',()=>{
    expect(NOME_PATTERN.test('ABC_123')).toBe(true);
    expect(NOME_PATTERN.test('ab')).toBe(false);
    expect(NOME_PATTERN.test('nome-com-hifen')).toBe(false);
    expect(NOME_PATTERN.test('a'.repeat(17))).toBe(false);
  });
  it('valida caracteres e confirmação da senha',()=>{
    expect(senhaValida('12345')).toBe(false);
    expect(senhaValida('🔐'.repeat(128))).toBe(true);
    expect(senhaValida('🔐'.repeat(129))).toBe(false);
    expect(validarNovaSenha('','')).toBe('');
    expect(validarNovaSenha('','',true)).not.toBe('');
    expect(validarNovaSenha('123456','654321')).not.toBe('');
  });
});
