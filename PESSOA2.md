# iMarker — Pessoa 2: Conta, autenticação e administração

## Entrega

Conta e administração integradas ao mapa Leaflet da Pessoa 3. Veja o roteiro em `LEIA-ME.md`.

## Como executar (Windows / PowerShell)

Pré-requisitos: JDK 25 (`java -version`), Docker Desktop, Node.js compatível com o lockfile (por exemplo, Node 24.15.0 ou superior da linha 24; o Angular resolvido exige ^22.22.3, ^24.15.0 ou >=26.0.0), npm compatível com o projeto e internet para baixar dependências. Confira também a disponibilidade da distribuição Maven 3.10.1 indicada no Wrapper original e do Spring Boot 4.1.2. Não foram trocadas versões para contornar indisponibilidade do ambiente de criação.

### Terminal 1 — Banco de desenvolvimento

```powershell
cd demo
docker compose up -d
docker compose ps
```

O PostgreSQL de desenvolvimento usa `localhost:5433/imarker`, usuário `imarker` e a senha local já existente. Não usar essas credenciais em produção.

### Terminal 2 — Backend e administrador inicial

```powershell
cd demo
$env:ADMIN_ENABLED = 'true'
$env:ADMIN_USERNAME = 'admin'
$env:ADMIN_PASSWORD = [System.Net.NetworkCredential]::new('', (Read-Host 'Senha inicial do administrador' -AsSecureString)).Password
.\mvnw.cmd spring-boot:run
```

A senha deve obedecer à política de 6 a 128 caracteres (recomenda-se uma senha forte e exclusiva). As variáveis acima são apenas do processo/sessão. O `AdminInitializer` cria um administrador somente se não existir nenhum. Não promove conta comum com nome coincidente; se não houver administrador e o nome escolhido já existir, a inicialização falha com conflito. Depois da criação inicial, encerre o processo e remova as variáveis ou defina `ADMIN_ENABLED=false` nas próximas execuções. A conta permanece no banco. Em um ambiente publicado, gerencie segredos de maneira apropriada, configure HTTPS e `APP_COOKIES_SECURE=true`.

### Terminal 3 — Frontend

```powershell
cd trabalho-evolucao-frontend
npm ci
npm start
```

Acesse `http://localhost:4200`. O proxy `/api` encaminha ao backend em `localhost:8080`.

- Cadastro público: `/register`.
- Login: `/login`.
- Mapa: `/dashboard` ou `/mapa` (com sessão).
- Perfil: `/users/edit`.
- Indicadores administrativos: `/admin`.
- Gerenciamento: `/admin/usuarios`.
- Edição: `/admin/usuarios/:id/editar`.

## Regras implementadas

### Etapa 1

- Nome de 3 a 16 caracteres ASCII, dígitos ou sublinhado; unicidade case-insensitive por valor normalizado, índice único e normalização na entidade. Cadastro público sempre USER, sem e-mail.
- Senha de 6 a 128 pontos de código Unicode, confirmação obrigatória no cadastro e em troca de senha. PBKDF2 com sal gerado pelo Spring Security (`defaultsForSpringSecurity_v5_8`), sem armazenamento em texto claro.
- Token inicial de compartilhamento de 256 bits, com restrição de unicidade. Não é exposto pela API de contas.
- Login nativo Spring Security, sessão JSESSIONID e CSRF para SPA. Cadastro não autentica automaticamente. Login inválido não cria nova sessão. Contadores, data e IP de login atual e anterior; login anterior no primeiro acesso recebe o mesmo instante do atual, como Trackable. Restaurar uma sessão via Remember Me registra nova autenticação, mas requisitar normalmente não incrementa o contador.
- Remember Me integrado ao filtro Spring, persistência em `persistent_logins`, segredo aleatório com hash SHA-256, rotação a cada restauração, duração de 14 dias renovável. A versão das credenciais é verificada antes da autorização. Cookies de sessão e Remember Me HttpOnly e SameSite=Lax; configure Secure/HTTPS na implantação. O frontend não armazena senhas ou tokens em localStorage.
- Logout revoga o token apresentado e invalida a sessão. Troca de nome, senha ou papel, e cancelamento, revogam tokens/sessões afetadas. Após qualquer envio válido do formulário do próprio perfil, o titular deverá autenticar-se novamente, inclusive se não houver alteração efetiva. Edição administrativa sem alterações não revoga credenciais.
- Perfil exige senha atual; nova senha e confirmação vazias mantêm o hash vigente. Cancelamento voluntário exige senha atual e confirmação na interface (CA-06).
- Páginas Inicial e Sobre, navbar dinâmica, formulários e guards.

### Etapa 2

- Primeiro administrador por variáveis de ambiente. Sem senha administrativa fixa no código e sem promoção de conta comum por coincidência de nome.
- RF-13: contagem de contas comuns, lista de administradores, até dez cadastros recentes e até dez logins ordenados pelo login anterior (sem contas sem login), com desempate por ID. Indicadores extras de total de contas e administradores.
- RF-14: pesquisa parcial sem distinção de caixa, ordenação de busca por papel e nome; listagem padrão por ID decrescente; opções nome, número de logins e último login, ambas as direções; contas sem login ao final. 100 por página, ordenação e direção permitidas explicitamente.
- RF-15: alteração de nome, papel e senha. Senha vazia/omitida preserva hash; se informada, exige validação e confirmação. Reutilização das regras de contas. Não há acesso ao hash, token, IP ou versionamento interno pelos DTOs.
- RF-16: exclusão de terceiros, proibição de autoexclusão administrativa, remoção de tokens e marcadores conforme contrato, transação e tratamento de erros. Exclusão só é informada como bem-sucedida após resposta do servidor.
- RF-17: proteção HTTP de `/api/admin/**` por `ROLE_ADMIN`, `@PreAuthorize` em controller e service e revalidação do papel/versão nas operações. Guard e navbar Angular não substituem a segurança do servidor.
- Proteção adicional do último administrador contra exclusão/rebaixamento/cancelamento, inclusive em operações concorrentes. `ContaLock` usa lock transacional PostgreSQL para serializar alterações de contas, login e tokens. É uma escolha voltada ao porte do projeto; monitorar impacto de contenção em escala maior.

## API HTTP

Todos os caminhos de mutação (inclusive cadastro, login e logout) exigem cookie/header CSRF. Antes de cada mutação faça `GET /api/csrf`, leia o cookie `XSRF-TOKEN` e envie `X-XSRF-TOKEN` no mesmo contexto de cookies. O Angular faz isso por `HttpClient` e URLs relativas. Login usa `application/x-www-form-urlencoded` com `username`, `password` e `remember-me` (`true`/`false`); demais corpos são JSON. Logout e DELETE não precisam de corpo.

| Método | Caminho | Permissão | Resultado de sucesso |
|---|---|---|---|
| GET | /api/csrf | Pública | 204 |
| POST | /api/auth/register | Pública | 201 |
| POST | /api/auth/login | Pública | 204 |
| POST | /api/auth/logout | Sessão/cookie CSRF | 204 |
| GET | /api/auth/me | Autenticado | 200 |
| PUT | /api/users/me | Autenticado, senha atual | 200 |
| POST | /api/users/me/cancelar | Autenticado, senha atual | 204 |
| GET | /api/admin/dashboard | ADMIN | 200 |
| GET | /api/admin/usuarios | ADMIN | 200 |
| GET | /api/admin/usuarios/{id} | ADMIN | 200 |
| PUT | /api/admin/usuarios/{id} | ADMIN | 200 |
| DELETE | /api/admin/usuarios/{id} | ADMIN, terceiro | 204 |

Corpos de exemplo (senhas fictícias):

```json
{"username":"conta_teste","senha":"senha123","confirmacaoSenha":"senha123"}
```

```json
{"username":"conta_teste","senhaAtual":"senha123","novaSenha":"","confirmacaoSenha":""}
```

```json
{"username":"conta_teste","papel":"ADMIN","novaSenha":null,"confirmacaoSenha":null}
```

Listagem: `/api/admin/usuarios?busca=teste&page=0&sort=id&direction=desc`. `page` inicia em zero. `sort` aceita `id`, `username`, `loginCount`, `ultimoLogin`. `direction`: `asc`/`desc`. A ordenação explícita é aplicada quando não há pesquisa, conforme RF-14. Resposta: `content`, `page`, `size`, `totalElements`, `totalPages`. Os horários são ISO-8601 (Instant) e formatados no navegador.

Erros de negócio possuem `status` e `message`, normalmente 400 (validação), 401 (autenticação), 403 (autorização/CSRF), 404 (conta inexistente), 409 (conflito), 500 (falha inesperada). Uma sessão restaurada via Remember Me pode receber 401 em uma ação que o Spring Security considera exigir autenticação completa, em vez de 403; nunca terá acesso administrativo sem o papel correspondente.

## Integração com a Pessoa 1

Mantida a estratégia `spring.jpa.hibernate.ddl-auto=update` de desenvolvimento. Flyway foi explicitamente desabilitado para evitar dois mecanismos simultâneos de gerenciamento do schema. Dependências Flyway não removidas. Alterada a configuração de sessão, o encoder para PBKDF2, desativada a impressão SQL por padrão e `open-in-view`. Não é uma estratégia de migração para produção: alinhar com a Pessoa 1 antes de implantar em banco existente. Os nomes das tabelas são `usuarios` e `persistent_logins`. Caso se importe uma base legada, os hashes antigos não são automaticamente migrados; deve-se planejar a importação e validação separadamente.

## Integração com a Pessoa 3

- O mapa privado abre em `/dashboard` e `/mapa` redireciona para ele.
- A leitura pública usa `/mapa/:nomeUsuario/:token` e o token criado no cadastro em `usuarios.token`.
- A associação `marcadores.usuario_id` é obrigatória. O Hibernate cria a FK em cascata em um banco novo. Em banco existente, confira a regra da FK conforme `LEIA-ME.md`.
- A exclusão e o cancelamento de conta removem os pontos na mesma transação. O teste usa a tabela real de marcadores.

## Testes

### 1. Backend

Testes unitários (não requerem banco):

```powershell
cd demo
.\mvnw.cmd -Dtest=UsuarioPolicyTests test
```

Testes HTTP reais usando Spring Boot, JPA, CSRF, cookies e PostgreSQL, em banco descartável isolado na porta 5434. **Não apontar para o banco de desenvolvimento**, pois os testes usam create-drop e limpam dados.

```powershell
cd demo
docker compose -p imarker-tests -f compose.test.yaml up -d --wait
.\mvnw.cmd test
docker compose -p imarker-tests -f compose.test.yaml down
```

`DemoApplicationTests`: cadastro/duplicidade por caixa, senha errada, logout, CSRF, Remember Me (hash/rotação/replay/revogação), perfil/senha atual, promoção/rebaixamento e revogação, recusa de visitante e conta comum, autoexclusão, proteção do último ADMIN, paginação 201 contas, indicadores, exclusão com marcadores, rollback forçado por FK, inicialização idempotente e cancelamento voluntário. `UsuarioPolicyTests`: limites e hash PBKDF2.

### 2. Frontend

```powershell
cd trabalho-evolucao-frontend
npm ci
npm test -- --watch=false
npm run build
```

Testes escritos: política de nomes/senhas, AuthService (sequência CSRF/login/me e falha de login), guards de usuário/administrador e navbar pública. Os fluxos visuais completos de edição, pesquisa, paginação e exclusão devem ser testados manualmente em integração; não foram adicionados testes de componente para todas as telas administrativas.

### 3. Verificação manual (CA-01 / CA-04 / integração)

1. Em banco limpo, iniciar com admin configurado; cadastrar conta comum; tentar registrar variação do mesmo nome com caixa diferente e senha curta.
2. Entrar com senha errada e confirmar que `/mapa` permanece protegido. Entrar corretamente, marcar Remember Me, fechar sessão do navegador preservando cookie persistente e reabrir o mapa. Testar sair.
3. Alterar perfil com senha atual inválida e válida. Conferir que senha omitida mantém o hash e que sessões antigas não funcionam.
4. Autenticar conta comum e acessar `/admin` e `/api/admin/dashboard`; ambos devem impedir acesso. Entrar com administrador e conferir indicadores, lista, busca, ordenação e paginação.
5. Editar conta comum, testar senha curta e nome repetido (estado preservado), promover a ADMIN e rebaixar; testar a sessão anterior dessa conta.
6. Tentar autoexclusão pelo painel, inclusive via API. Excluir terceiro com marcadores e conferir banco. Depois da exclusão, testar o link público antigo. Testar cancelamento voluntário do titular.
7. Validar o uso de HTTPS e atributos Secure dos cookies em implantação publicada; conferir que respostas de contas não expõem hash, token ou IP.

## Verificação da entrega integrada

O roteiro em `LEIA-ME.md` cobre a execução e os testes das entregas combinadas. O build do Angular, os 21 testes automatizados do frontend e os 31 testes do Spring passaram. A suíte HTTP usou PostgreSQL 17. O caso de mapa público da suíte HTTP cobre a criação do ponto pelo titular, a separação entre contas e a revogação do endereço antigo.

## Referências técnicas de compatibilidade consultadas

- Spring Security, CSRF para SPA: https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html
- Spring Security, `RememberMeConfigurer`: https://github.com/spring-projects/spring-security/blob/main/config/src/main/java/org/springframework/security/config/annotation/web/configurers/RememberMeConfigurer.java

Essas referências foram utilizadas para confirmar a integração do filtro Remember Me com o repositório de contexto de segurança e o logout handler, e a configuração CSRF já presente no projeto. Não substituem validação com a versão efetivamente resolvida pelo Maven.
