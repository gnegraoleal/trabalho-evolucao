# iMarker integrado

Este pacote reúne a infraestrutura Angular/Spring/PostgreSQL, as etapas de conta e administração e a entrega de mapa, marcadores e compartilhamento.

## Executar no Windows

Abra três terminais PowerShell na pasta `trabalho-evolucao-main`. Use Docker Desktop, JDK 25 e Node.js compatível com o lockfile (por exemplo, Node 24.15 ou superior da linha 24).

**1. PostgreSQL**

```powershell
cd demo
docker compose up -d
docker compose ps
```

O PostgreSQL de desenvolvimento responde em `localhost:5433`. A configuração do projeto usa `ddl-auto=update`. Em banco novo, o Hibernate cria as tabelas e a chave estrangeira dos marcadores. Se você já tiver uma tabela `marcadores` de uma tentativa anterior, confira se `usuario_id` referencia `usuarios.id` com `ON DELETE CASCADE`. Em caso de ausência de uma FK equivalente, consulte `demo/integracao/marcadores-fk.sql` antes de aplicar qualquer alteração ao banco existente.

**2. Spring Boot**

```powershell
cd demo
.\mvnw.cmd spring-boot:run
```

Para testar o painel administrativo, defina as variáveis abaixo antes de iniciar o Spring. O projeto cria a conta na primeira inicialização administrativa, quando ainda não existe administrador.

```powershell
$env:ADMIN_ENABLED = 'true'
$env:ADMIN_USERNAME = 'admin'
$env:ADMIN_PASSWORD = [System.Net.NetworkCredential]::new('', (Read-Host 'Senha inicial do administrador' -AsSecureString)).Password
.\mvnw.cmd spring-boot:run
```

**3. Angular**

```powershell
cd trabalho-evolucao-frontend
npm ci
npm start
```

Acesse [http://localhost:4200](http://localhost:4200). O proxy encaminha `/api` ao Spring na porta 8080.

## Roteiro de teste

1. Cadastre uma conta, faça login e crie dois marcadores em `/dashboard`. Atualize a página e confirme que os dois continuam no mapa. Exclua um e confira se o outro permanece.
2. Entre com uma segunda conta. O mapa deve mostrar somente os pontos dela.
3. Na primeira conta, abra **Compartilhar mapa**, copie o link e abra-o em uma janela anônima. O mapa público deve mostrar o marcador restante em modo de leitura.
4. Gere um novo link. O link anterior deve abrir a mensagem de endereço inválido ou revogado. O novo deve mostrar o mapa.
5. Com a conta comum, tente acessar `/admin`. Em seguida, entre como administrador e acesse os indicadores e a lista. Excluir a conta de teste deve excluir os pontos e invalidar o link público dela.

O centro inicial usa a geolocalização do navegador com Salvador como posição padrão. Isso difere da localização por IP descrita no RD-07. Os links públicos usam `/mapa/:nomeUsuario/:token` e o token que já é criado no cadastro. A descrição de cada ponto aceita até 2.000 caracteres.

## Suítes automatizadas

- Angular, em `trabalho-evolucao-frontend` — `npm test -- --watch=false`.
- Spring, em `demo` — `docker compose -f compose.test.yaml up -d --wait`, seguido de `.\mvnw.cmd test`. A suíte HTTP usa o banco descartável da porta 5434.

Verificação desta versão: build Angular concluído, 21 testes Angular aprovados e 31 testes Spring aprovados com PostgreSQL 17. Os testes de interação no navegador ficam no roteiro acima.
