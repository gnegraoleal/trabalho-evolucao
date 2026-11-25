package com.example.demo;

import com.example.demo.dto.ContaDTO.Cadastro;
import com.example.demo.entity.PapelUsuario;
import com.example.demo.entity.Usuario;
import com.example.demo.repository.TokenRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Testes HTTP reais (sessão, cookies, CSRF, JPA e PostgreSQL), não usam banco de desenvolvimento. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=jdbc:postgresql://localhost:5434/imarker_test",
    "spring.datasource.username=imarker_test", "spring.datasource.password=imarker-test-local",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false",
    "app.admin.enabled=false", "app.cookies.secure=false", "server.servlet.session.cookie.secure=false"
})
class DemoApplicationTests {
    @LocalServerPort int port;
    @Autowired UsuarioRepository usuarios;
    @Autowired TokenRepository tokens;
    @Autowired UsuarioService contas;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;
    private long adminId, comumId;
    private static final String SENHA = "senha123";

    @BeforeEach void prepararBanco() {
        jdbc.execute("drop table if exists referencias_teste");
        tokens.deleteAllInBatch(); usuarios.deleteAllInBatch();
        contas.inicializarAdmin("admin", SENHA);
        comumId = contas.cadastrar(new Cadastro("comum", SENHA, SENHA)).id();
        adminId = usuarios.findByUsernameNormalizado("admin").orElseThrow().getId();
    }
    @AfterEach void removerFixtureDeRollback() { jdbc.execute("drop table if exists referencias_teste"); }
    private class Browser {
        private final HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
        private final Map<String,String> cookies = new HashMap<>();
        HttpResponse<String> req(String method, String path, String body, String tipo, boolean csrf) throws Exception {
            var b = HttpRequest.newBuilder(URI.create("http://localhost:"+port+path));
            if (!cookies.isEmpty()) b.header("Cookie", String.join("; ", cookies.entrySet().stream().map(e->e.getKey()+"="+e.getValue()).toList()));
            if (tipo != null) b.header("Content-Type", tipo);
            if (csrf && cookies.containsKey("XSRF-TOKEN")) b.header("X-XSRF-TOKEN", cookies.get("XSRF-TOKEN"));
            var r = http.send(b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
            for (String raw : r.headers().allValues("Set-Cookie")) {
                String[] par = raw.split(";", 2)[0].split("=", 2);
                if (par.length == 2) { if (par[1].isEmpty() || raw.toLowerCase(Locale.ROOT).contains("max-age=0")) cookies.remove(par[0]); else cookies.put(par[0],par[1]); }
            }
            return r;
        }
        HttpResponse<String> get(String path) throws Exception { return req("GET",path,null,null,false); }
        void csrf() throws Exception { assertEquals(204,get("/api/csrf").statusCode()); assertTrue(cookies.containsKey("XSRF-TOKEN")); }
        HttpResponse<String> json(String method,String path,String body) throws Exception { csrf(); return req(method,path,body,"application/json",true); }
        HttpResponse<String> login(String nome,String senha,boolean remember) throws Exception {
            csrf();
            return req("POST","/api/auth/login","username="+URLEncoder.encode(nome,StandardCharsets.UTF_8)+"&password="+URLEncoder.encode(senha,StandardCharsets.UTF_8)+"&remember-me="+remember,"application/x-www-form-urlencoded",true);
        }
        void entrar(String nome, boolean remember) throws Exception { assertEquals(204,login(nome,SENHA,remember).statusCode()); assertEquals(200,get("/api/auth/me").statusCode()); }
        void perderSessao() { cookies.remove("JSESSIONID"); cookies.remove("XSRF-TOKEN"); }
    }
    private static String cadastro(String nome,String senha) { return "{\"username\":\""+nome+"\",\"senha\":\""+senha+"\",\"confirmacaoSenha\":\""+senha+"\"}"; }
    private static String editar(String nome,String papel,String senha) { return "{\"username\":\""+nome+"\",\"papel\":\""+papel+"\",\"novaSenha\":\""+senha+"\",\"confirmacaoSenha\":\""+senha+"\"}"; }
    private static String perfil(String nome,String atual,String senha) { return "{\"username\":\""+nome+"\",\"senhaAtual\":\""+atual+"\",\"novaSenha\":\""+senha+"\",\"confirmacaoSenha\":\""+senha+"\"}"; }
    private Usuario comum() { return usuarios.findById(comumId).orElseThrow(); }

    @Test void cadastroDuplicadoSenhaInvalidaELogout() throws Exception {
        Browser b = new Browser(); long inicial=usuarios.count();
        assertEquals(401,b.get("/api/auth/me").statusCode());
        assertEquals(403,b.req("POST","/api/auth/register",cadastro("outra",SENHA),"application/json",false).statusCode());
        var criada=b.json("POST","/api/auth/register",cadastro("Nova_123",SENHA)); assertEquals(201,criada.statusCode());
        assertFalse(criada.body().contains("passwordHash")); assertFalse(criada.body().contains("\"token\""));
        assertEquals(409,b.json("POST","/api/auth/register",cadastro("nOVA_123",SENHA)).statusCode());
        assertEquals(400,b.json("POST","/api/auth/register",cadastro("outra","12345")).statusCode());
        assertEquals(inicial+1,usuarios.count());
        assertEquals(401,b.get("/api/auth/me").statusCode());
        assertEquals(401,b.login("Nova_123","errada",false).statusCode());
        assertEquals(401,b.get("/api/auth/me").statusCode());
        b.entrar("nova_123",false);
        Usuario u=usuarios.findByUsernameNormalizado("nova_123").orElseThrow();
        assertEquals(PapelUsuario.USER,u.getPapel()); assertEquals(1,u.getLoginCount()); assertNotNull(u.getLoginAtual()); assertNotNull(u.getIpLoginAtual());
        assertNotEquals(SENHA,u.getPasswordHash()); assertTrue(encoder.matches(SENHA,u.getPasswordHash()));
        assertEquals(204,b.json("POST","/api/auth/logout",null).statusCode()); assertEquals(401,b.get("/api/auth/me").statusCode());
    }
    @Test void lembrarSessaoRotacionarERevogar() throws Exception {
        Browser b=new Browser(); b.entrar("comum",true);
        String cookie=b.cookies.get("imarker-remember"); assertNotNull(cookie); assertEquals(1,tokens.count());
        assertFalse(tokens.findAll().getFirst().getDigest().equals(cookie.split("\\.")[1]));
        b.perderSessao(); assertEquals(200,b.get("/api/auth/me").statusCode());
        String novo=b.cookies.get("imarker-remember"); assertNotNull(novo); assertNotEquals(cookie,novo);
        assertEquals(2,comum().getLoginCount());
        assertEquals(200,b.get("/api/auth/me").statusCode()); assertEquals(2,comum().getLoginCount());
        Browser replay=new Browser(); replay.cookies.put("imarker-remember",cookie); assertEquals(401,replay.get("/api/auth/me").statusCode());
        assertEquals(204,b.json("POST","/api/auth/logout",null).statusCode()); assertEquals(0,tokens.count());
        assertEquals(401,b.get("/api/auth/me").statusCode());
        Browser revogado=new Browser(); revogado.cookies.put("imarker-remember",novo); assertEquals(401,revogado.get("/api/auth/me").statusCode());
    }
    @Test void alterarPerfilExigeSenhaAtualERevogaOutrasSessoes() throws Exception {
        Browser b=new Browser(), outra=new Browser(); b.entrar("comum",false); outra.entrar("comum",true);
        String hash=comum().getPasswordHash();
        assertEquals(400,b.json("PUT","/api/users/me",perfil("renomeado","errada","")).statusCode()); assertEquals("comum",comum().getUsername());
        assertEquals(409,b.json("PUT","/api/users/me",perfil("ADMIN",SENHA,"")).statusCode());
        assertEquals(400,b.json("PUT","/api/users/me",perfil("renomeado",SENHA,"12345")).statusCode());
        assertEquals(200,b.json("PUT","/api/users/me",perfil("renomeado",SENHA,"")).statusCode());
        assertEquals("renomeado",comum().getUsername()); assertEquals(hash,comum().getPasswordHash()); assertEquals(0,tokens.count());
        assertEquals(401,b.get("/api/auth/me").statusCode()); assertEquals(401,outra.get("/api/auth/me").statusCode());
        b.entrar("renomeado",false);
        assertEquals(200,b.json("PUT","/api/users/me",perfil("renomeado",SENHA,"novaSenha123")).statusCode());
        assertTrue(encoder.matches("novaSenha123",comum().getPasswordHash())); assertEquals(401,b.login("renomeado",SENHA,false).statusCode());
        assertEquals(204,b.login("renomeado","novaSenha123",false).statusCode());
    }
    @Test void autorizacaoAdminEdicaoExclusaoERevogacao() throws Exception {
        Browser visitante=new Browser(); assertEquals(401,visitante.get("/api/admin/dashboard").statusCode());
        Browser normal=new Browser(); normal.entrar("comum",true);
        assertEquals(403,normal.get("/api/admin/usuarios").statusCode());
        assertEquals(403,normal.json("PUT","/api/admin/usuarios/"+adminId,editar("admin","USER","")).statusCode());
        assertEquals(403,normal.json("DELETE","/api/admin/usuarios/"+adminId,null).statusCode());
        Browser admin=new Browser(); admin.entrar("admin",false);
        assertEquals(200,admin.get("/api/admin/dashboard").statusCode());
        assertEquals(200,admin.get("/api/admin/usuarios?busca=COM").statusCode());
        assertEquals(400,admin.get("/api/admin/usuarios?sort=passwordHash").statusCode());
        assertEquals(400,admin.get("/api/admin/usuarios?page=-1").statusCode());
        assertEquals(403,admin.req("PUT","/api/admin/usuarios/"+comumId,editar("comum","ADMIN",""),"application/json",false).statusCode());
        assertEquals(403,admin.json("DELETE","/api/admin/usuarios/"+adminId,null).statusCode());
        assertEquals(409,admin.json("PUT","/api/admin/usuarios/"+adminId,editar("admin","USER","")).statusCode());
        String hash=comum().getPasswordHash();
        assertEquals(400,admin.json("PUT","/api/admin/usuarios/"+comumId,editar("renomeado","ADMIN","curta")).statusCode());
        assertEquals("comum",comum().getUsername()); assertEquals(PapelUsuario.USER,comum().getPapel());
        assertEquals(409,admin.json("PUT","/api/admin/usuarios/"+comumId,editar("ADMIN","ADMIN","")).statusCode());
        assertEquals(200,admin.json("PUT","/api/admin/usuarios/"+comumId,editar("comum","ADMIN","")).statusCode());
        assertEquals(hash,comum().getPasswordHash()); assertEquals(PapelUsuario.ADMIN,comum().getPapel());
        assertEquals(401,normal.get("/api/admin/dashboard").statusCode()); assertEquals(0,tokens.count());
        normal.entrar("comum",true); assertEquals(200,normal.get("/api/admin/dashboard").statusCode());
        assertEquals(200,admin.json("PUT","/api/admin/usuarios/"+comumId,editar("comum","USER","")).statusCode());
        assertEquals(401,normal.get("/api/admin/dashboard").statusCode());
        normal.entrar("comum",true);
        criarMarcador(comumId);
        criarMarcador(adminId);
        assertEquals(204,admin.json("DELETE","/api/admin/usuarios/"+comumId,null).statusCode());
        assertFalse(usuarios.existsById(comumId)); assertTrue(usuarios.existsById(adminId)); assertEquals(0,tokens.count());
        assertEquals(1,jdbc.queryForObject("select count(*) from marcadores",Integer.class));
        assertEquals(401,normal.get("/api/auth/me").statusCode());
        assertEquals(404,admin.json("DELETE","/api/admin/usuarios/"+comumId,null).statusCode());
    }
    @Test void paginacaoEOrdenacao() throws Exception {
        List<Usuario> lote=new ArrayList<>(); String hash=comum().getPasswordHash();
        for(int i=0;i<199;i++) { Usuario u=new Usuario();u.setUsername("teste_"+i);u.setUsernameNormalizado(u.getUsername());u.setPasswordHash(hash);u.setToken(UsuarioService.aleatorio());lote.add(u); }
        usuarios.saveAllAndFlush(lote);
        Browser b=new Browser(); b.entrar("admin",false);
        var p=b.get("/api/admin/usuarios?page=0"); assertEquals(200,p.statusCode()); assertTrue(p.body().contains("\"totalElements\":201"));assertTrue(p.body().contains("\"totalPages\":3"));
        assertEquals(100,p.body().split("\"username\":",-1).length-1);
        var p2=b.get("/api/admin/usuarios?page=1"); assertEquals(100,p2.body().split("\"username\":",-1).length-1);
        var p3=b.get("/api/admin/usuarios?page=2"); assertEquals(1,p3.body().split("\"username\":",-1).length-1);assertTrue(p3.body().contains("\"username\":\"admin\""));
        var busca=b.get("/api/admin/usuarios?busca=TESTE_19"); assertEquals(200,busca.statusCode());assertTrue(busca.body().contains("\"totalElements\":10"));
        var ordem=b.get("/api/admin/usuarios?sort=username&direction=asc"); assertTrue(ordem.body().indexOf("\"username\":\"admin\"")<ordem.body().indexOf("\"username\":\"comum\""));
        var indicadores=b.get("/api/admin/dashboard"); assertTrue(indicadores.body().contains("\"totalUsuarios\":200")); assertTrue(indicadores.body().contains("\"totalAdministradores\":1"));
    }
    @Test void falhaDeExclusaoPreservaContaMarcadoresETokens() throws Exception {
        Browser b=new Browser(), normal=new Browser(); b.entrar("admin",false); normal.entrar("comum",true);
        jdbc.execute("create table referencias_teste (usuario_id bigint not null references usuarios(id))");
        criarMarcador(comumId);
        jdbc.update("insert into referencias_teste(usuario_id) values (?)",comumId);
        assertEquals(409,b.json("DELETE","/api/admin/usuarios/"+comumId,null).statusCode());
        assertTrue(usuarios.existsById(comumId)); assertEquals(1,tokens.count());
        assertEquals(1,jdbc.queryForObject("select count(*) from marcadores",Integer.class));
        assertEquals(200,normal.get("/api/auth/me").statusCode());
    }
    private void criarMarcador(long usuarioId) {
        jdbc.update("""
                insert into marcadores (usuario_id, titulo, latitude, longitude, criado_em)
                values (?, 'Ponto de teste', 0, 0, current_timestamp)
                """, usuarioId);
    }

    @Test void mapaPublicoExibeMarcadorERecusaLinkRevogado() throws Exception {
        Browser titular = new Browser(), visitante = new Browser(), outraConta = new Browser();
        titular.entrar("comum", false);
        outraConta.entrar("admin", false);
        assertEquals(201, titular.json("POST", "/api/marcadores", """
                {"titulo":"Praia","latitude":-13,"longitude":-38.5}
                """).statusCode());
        assertEquals("[]", outraConta.get("/api/marcadores").body());
        assertEquals(401, visitante.get("/api/marcadores").statusCode());

        String prefixo = "/api/public/mapas/comum/";
        String antigo = prefixo + comum().getToken();
        var mapa = visitante.get(antigo);
        assertEquals(200, mapa.statusCode());
        assertTrue(mapa.body().contains("Praia"));
        assertEquals(200, titular.json("POST", "/api/mapa/link/regenerar", "{}").statusCode());
        assertEquals(404, visitante.get(antigo).statusCode());
        assertEquals(200, visitante.get(prefixo + comum().getToken()).statusCode());
    }

    @Test void inicializacaoIdempotente() {
        long total = usuarios.count(); String hash = usuarios.findById(adminId).orElseThrow().getPasswordHash();
        contas.inicializarAdmin("comum", "outraSenha");
        assertEquals(total, usuarios.count()); assertEquals(PapelUsuario.USER, comum().getPapel());
        assertEquals(hash, usuarios.findById(adminId).orElseThrow().getPasswordHash());
    }
    @Test void cancelarContaEConfirmarSenha() throws Exception {
        Browser b=new Browser();b.entrar("comum",true);
        criarMarcador(comumId);
        assertEquals(400,b.json("POST","/api/users/me/cancelar","{\"senhaAtual\":\"errada\"}").statusCode());assertTrue(usuarios.existsById(comumId));
        assertEquals(204,b.json("POST","/api/users/me/cancelar","{\"senhaAtual\":\"senha123\"}").statusCode());
        assertFalse(usuarios.existsById(comumId));assertEquals(0,tokens.count());assertEquals(0,jdbc.queryForObject("select count(*) from marcadores",Integer.class));
        assertEquals(401,b.get("/api/auth/me").statusCode());
    }
}
