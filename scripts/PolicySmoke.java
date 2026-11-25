import com.example.demo.service.*;
public class PolicySmoke {
  private static int n;
  static void ok(boolean valor) { if(!valor)throw new AssertionError("caso "+n);n++; }
  static void recusa(Runnable r) { try{r.run();throw new AssertionError("aceito indevidamente");}catch(ValidacaoContaException e){n++;} }
  public static void main(String[] a){
    ok(UsuarioPolicy.validarNome("ABC_123").equals("abc_123"));
    ok(UsuarioPolicy.validarNome("ABC").equals("abc"));ok(UsuarioPolicy.validarNome("A".repeat(16)).length()==16);
    for(String nome:new String[]{"ab","a".repeat(17),"abc-1","ábc","abc\n"," abc",null})recusa(()->UsuarioPolicy.validarNome(nome));
    for(int size:new int[]{6,128}){String senha="🔐".repeat(size);UsuarioPolicy.validarSenha(senha,senha);n++;}
    for(int size:new int[]{5,129}){String senha="a".repeat(size);recusa(()->UsuarioPolicy.validarSenha(senha,senha));}
    recusa(()->UsuarioPolicy.validarSenha("abcdef","abcdefg"));recusa(()->UsuarioPolicy.validarSenha(null,null));
    ok(!UsuarioPolicy.temNovaSenha("",""));ok(!UsuarioPolicy.temNovaSenha(null,null));ok(UsuarioPolicy.temNovaSenha("","x"));
    System.out.println("Java: "+n+" verificações independentes da política de conta passaram.");
  }
}
