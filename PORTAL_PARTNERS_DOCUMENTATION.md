# Portal Partners - Documentacao Tecnica e Plano Evolutivo

## 1. Objetivo do sistema
O **Portal Partners** centraliza o envio, controle, validacao e consulta de documentos entre empresas contratantes e contratadas, com perfis e permissao por papel de usuario.

Objetivos de negocio atuais:
- reduzir troca de documentos por e-mail;
- manter rastreabilidade de documentos e status;
- permitir operacao com perfis `ADMIN`, `CONTRATANTE` e `CONTRATADA`.

## 2. Stack atual do projeto (estado real)

### 2.1 Backend
- Java + Spring Boot `3.3.3`
- Spring Security + JWT
- Spring Data JPA
- Spring Mail (SMTP)
- Cliente MinIO `8.6.0`
- Build via Maven

Observacao de versao:
- o `pom.xml` declara `java.version=17`, mas o container usa runtime/build com Java 21 no `backend/Dockerfile`.
- recomendacao: padronizar para **Java 21** em todo o projeto ou ajustar Docker para Java 17 para evitar divergencia.

### 2.2 Frontend
- React `18.3.1`
- Vite `5.1.0`
- Material UI `7.3.x`
- React Router `7.10.1`
- Axios

### 2.3 Infra local atual (docker-compose)
Servicos:
- `postgres` (PostgreSQL 16)
- `minio`
- `backend`
- `frontend`

## 3. Arquitetura funcional atual

Perfis:
- `ADMIN`: acesso global administrativo.
- `CONTRATANTE`: gerencia contratadas e consulta/valida documentos.
- `CONTRATADA`: gerencia funcionarios e faz uploads.

Entidades principais:
- `Usuario` (autenticacao e role)
- `Contratante`
- `Contratada`
- `Funcionario`
- `Documento`

Estado implementado nesta etapa:
- `Contratante` passou a representar explicitamente a organizacao contratante;
- `Contratante` possui `nome`, `cnpj` e `dominioEmail`;
- `Usuario` possui `nome` e pode estar vinculado a uma `Contratante`;
- uma mesma contratante pode possuir varios usuarios `CONTRATANTE` vinculados;
- usuarios adicionais da contratante devem usar o mesmo dominio de e-mail da organizacao.

Fluxos existentes:
- login com JWT;
- reset de senha (`forgot-password` e `reset-password`);
- CRUD operacional por perfil;
- upload/download de documentos via backend + MinIO;
- listagem e filtros de documentos.

## 4. Tipos de documento (catalogo atual)

`TipoDocumento` atual:
- Documentos de empresa: `CNPJ`, `PGR`, `PCMSO`, `ORDEM_SERVICO`, `CNDT`, `CNAT`, `CNRF`, `GRIP`
- Documentos de funcionario: `CTPS_DIGITAL`, `INSS`, `FGTS`, `HOLERITE`, `ASO`, `RG`, `CPF`, `FICHA_EPI`, `NR10`, `NR12`, `NR35`

## 5. Analise tecnica das melhorias solicitadas (sem implementacao ainda)

Esta secao registra as melhorias solicitadas e a proposta tecnica recomendada para implementacao com baixo risco.

### 5.1 Novo modelo de acesso por dominio de e-mail da organizacao
Solicitacao:
- usuarios diferentes do mesmo dominio (ex: `@zebra.com`) devem acessar o mesmo contexto de empresa contratante/contratada.

Analise:
- o modelo atual parece centrado em vinculo 1:1 `Usuario` <-> empresa.
- para suportar multiusuarios por organizacao, o ideal e separar:
  - identidade de usuario;
  - associacao do usuario com organizacao por dominio.

Proposta tecnica:
- criar entidade de dominio organizacional, por exemplo `OrganizacaoDominio`:
  - `id`, `dominio`, `tipo` (`CONTRATANTE` / `CONTRATADA`), `empresaId`, `ativo`;
- criar tabela de associacao de usuario com organizacao:
  - `UsuarioOrganizacao` com papeis e status (ativo/inativo);
- no login:
  - extrair dominio do e-mail;
  - resolver organizacao por dominio;
  - carregar claims no JWT com `organizacaoId`, `tipoOrganizacao` e `roleAplicacional`;
- tratar cenarios de conflito:
  - mesmo dominio para mais de uma empresa (nao recomendado sem regra de segregacao);
  - usuarios externos sem dominio mapeado.

Riscos e cuidados:
- migracao de dados do modelo atual;
- redefinicao das regras de autorizacao por empresa no backend;
- ajuste em queries para sempre filtrar por `organizacaoId`.

Implementacao atual:
- para `CONTRATANTE`, o modelo foi simplificado para organizacao explicita na propria entidade `Contratante`;
- a contratante armazena `dominioEmail` e os usuarios vinculados devem respeitar esse dominio;
- o `ADMIN` cria a organizacao contratante com `nome`, `cnpj`, `email` e `senha`;
- a propria contratante logada pode criar usuarios adicionais com `nome`, `email` e `senha`;
- esses usuarios adicionais recebem role `CONTRATANTE`, ficam vinculados a mesma organizacao e herdam os mesmos privilegios.

### 5.2 Auditoria completa de acessos e transacoes (logs + tela admin)
Solicitacao:
- rastrear quem fez o que, quando e de onde; tela admin para consulta e filtro.

Proposta tecnica:
- criar trilha de auditoria em tabela dedicada, ex: `audit_log`:
  - `id`, `timestamp`, `userId`, `email`, `role`, `organizacaoId`, `acao`, `entidade`, `entidadeId`, `detalhesJson`, `ip`, `userAgent`, `status`;
- registrar eventos em pontos criticos:
  - login/logout/falhas de login;
  - criacao/edicao/exclusao de contratantes, contratadas, funcionarios;
  - upload/download de documentos;
  - alteracao de status de documento;
  - alteracao de senha;
- implementar endpoint admin com filtros:
  - periodo, usuario, acao, entidade, status, organizacao;
- frontend admin:
  - nova rota para listagem de auditoria com filtros e paginacao.

Recomendacao:
- usar AOP/interceptadores para padronizar captura de eventos e reduzir duplicacao.

### 5.3 Endpoint de alteracao obrigatoria de senha no primeiro acesso
Solicitacao:
- usuario criado por admin/contratante deve trocar senha no primeiro acesso antes de usar o sistema.

Proposta tecnica:
- adicionar campo em `Usuario`: `mustChangePassword` (boolean);
- na criacao de usuario por terceiros:
  - salvar `mustChangePassword=true`;
- no login:
  - autentica normalmente, mas retorna flag `mustChangePassword`;
- criar endpoint dedicado:
  - `POST /api/auth/change-password-first-access`;
- bloquear demais endpoints quando `mustChangePassword=true`, exceto login e troca inicial.

Observacao funcional:
- apos troca de senha, invalidar tokens anteriores e exigir novo login.

Implementacao atual:
- usuarios criados pelo `ADMIN` para contratante e usuarios adicionais criados pela contratante nascem com `mustChangePassword=true`;
- o backend bloqueia o acesso a rotas protegidas enquanto a troca inicial nao for concluida;
- o frontend redireciona automaticamente para a rota de primeiro acesso;
- apos a troca da senha, o usuario deve efetuar login novamente.

### 5.4 Melhoria de UX na tabela de documentos
Solicitacao:
- reduzir altura das linhas;
- ordenar por padrao em ordem decrescente;
- permitir botao para alternar crescente/decrescente.

Proposta tecnica:
- backend:
  - padrao de ordenacao por `createdAt DESC` na listagem;
  - aceitar parametros `sortBy` e `sortDir`;
- frontend:
  - controle visual no cabecalho da tabela;
  - persistir escolha de ordenacao no estado da pagina (ou query string).

### 5.5 Desabilitar clique na linha e manter apenas botao de download
Solicitacao:
- remover comportamento de linha clicavel;
- registrar no log os downloads e transacoes de cadastro/upload.

Proposta tecnica:
- frontend: remover `onRowClick`, manter apenas acao no botao;
- backend:
  - manter endpoint de download como ponto unico da acao;
  - registrar evento de download no `audit_log`.

### 5.6 Consentimento LGPD no fluxo de upload de documentos
Solicitacao:
- na rota de upload, exigir aceite em checkbox antes de permitir uso da funcionalidade.

Proposta tecnica:
- frontend:
  - banner/caixa LGPD com checkbox obrigatorio na tela de upload;
  - bloquear submissao sem aceite;
- backend:
  - registrar consentimento de forma auditavel (recomendado);
  - opcionalmente criar tabela `lgpd_consent`:
    - `userId`, `timestamp`, `versaoTermo`, `ip`, `userAgent`.

Recomendacao juridica:
- versionar texto do termo (`versaoTermo`) para evidenciar qual conteudo foi aceito.

### 5.7 Lista dinamica de tipos de documento (empresa x funcionario)
Solicitacao:
- criar duas listas de **tipos de documento**:
  - lista de tipos para **documentos da empresa** (contratada);
  - lista de tipos para **documentos de funcionario**;
- ao selecionar o contexto no frontend, exibir somente os tipos daquela lista.

Proposta tecnica funcional:
- backend:
  - endpoint para retornar tipos de documento por escopo:
    - `GET /api/documentos/tipos?escopo=EMPRESA|FUNCIONARIO`;
- frontend:
  - selector de escopo (`EMPRESA` ou `FUNCIONARIO`);
  - ao trocar o escopo, recarregar os tipos dinamicamente e limpar selecao invalida.

Modelagem recomendada (padrao de mercado):
- opcao A (recomendada): **entidade unica de catalogo** com coluna de categoria/escopo.
  - exemplo: `tipo_documento` com campos `codigo`, `descricao`, `escopo`, `ativo`, `ordem`.
  - vantagem: menor duplicacao, manutencao simples, escalavel para novos escopos.
- opcao B (viavel): duas entidades separadas.
  - exemplos: `tipo_documento_contratada` e `tipo_documento_funcionario`.
  - vantagem: separacao explicita por dominio; desvantagem: mais manutencao e mais codigo.

Decisao sugerida:
- seguir com **opcao A** como padrao de mercado.
- usar opcao B somente se houver regra de negocio radicalmente diferente entre os dois catálogos.

### 5.8 Vigencia/validade de documentos + base para alertas
Solicitacao:
- adicionar campos de validade na postagem;
- futuramente criar alertas de vencimento.

Proposta tecnica:

- no `Documento`, incluir:
  - `dataEmissao` (opcional)
  - `dataValidade` (opcional inicialmente)
  - `periodicidade` (`MENSAL`, `ANUAL`, `OUTRA`, opcional nesta fase)
- na API de upload:
  - aceitar campos de validade;
- para alertas futuros:
  - job agendado diario para identificar vencimento proximo;
  - notificacao por e-mail e/ou badge.

Observacao:
- como voce vai detalhar regras de vigencia por tipo, esta implementacao deve ser faseada:
  1) persistir campos;
  2) aplicar regra por tipo;
  3) habilitar alerta automatizado.

## 6. Roadmap recomendado de implementacao (ordem sugerida)

Fase 1 - Fundacao de seguranca e dominio:
1. modelo multiusuario por dominio;
2. primeira troca obrigatoria de senha;
3. ajustes de autorizacao por organizacao.

Fase 2 - Auditoria e conformidade:
1. `audit_log` + captura de eventos;
2. tela admin de auditoria;
3. consentimento LGPD com versionamento.

Fase 3 - Documentos e UX:
1. ordenacao e tabela;
2. filtro dinamico por tipo;
3. campos de vigencia.

Fase 4 - Alertas:
1. regra de vencimento por tipo de documento;
2. notificacoes e painel de pendencias.

## 7. Requisitos de servidor para producao

### 7.1 Recomendacao minima (ambiente produtivo pequeno)
- 4 vCPU
- 8 GB RAM
- 120 GB SSD
- Ubuntu Server `22.04 LTS`
- Docker Engine + Docker Compose plugin

### 7.2 Recomendacao intermediaria (mais usuarios/arquivos)
- 8 vCPU
- 16 GB RAM
- 250 GB SSD
- armazenamento de objetos com alta durabilidade (S3/R2/MinIO com backup)

### 7.3 Banco de dados
- PostgreSQL 16 (gerenciado ou dedicado)
- backup diario + retencao
- monitoracao de conexoes, latencia e disco.

## 8. Apache, Ubuntu e alinhamento para TI

### 8.1 Se usar Apache como reverse proxy
- Ubuntu recomendado: `22.04 LTS`
- Apache HTTP Server: `2.4.x` (padrao da distro)
- habilitar modulos: `proxy`, `proxy_http`, `rewrite`, `headers`, `ssl`
- usar HTTPS com certificado valido.

### 8.2 Recomendacao tecnica principal
- para este stack, **Nginx** costuma ser mais simples para servir frontend estatico e proxy do backend.
- se a empresa padroniza Apache, e totalmente viavel manter Apache.

### 8.3 Informacoes que devem ser enviadas ao time de TI
Checklist de handoff:
- versoes da aplicacao:
  - backend Spring Boot `3.3.3`;
  - frontend React/Vite;
  - PostgreSQL `16`;
- portas e roteamento:
  - frontend (ex: 80/443 publico),
  - backend interno (ex: 8080),
  - banco e storage privados;
- variaveis de ambiente obrigatorias (secao 10 desta doc);
- estrategia de backups (DB e objetos);
- politica de logs e retencao;
- DNS e certificados TLS;
- SMTP corporativo (host, porta, usuario, politicas SPF/DKIM/DMARC).

## 9. Requisitos de infraestrutura e operacao

Itens minimos para ambiente produtivo confiavel:
- observabilidade:
  - logs centralizados,
  - metricas de CPU/RAM/disco,
  - health checks e alertas;
- seguranca:
  - TLS ponta a ponta,
  - segredo fora de codigo (`.env` seguro/secret manager),
  - rotacao de credenciais;
- banco:
  - backup testado,
  - plano de restauracao;
- storage:
  - versionamento ou politica de retencao para documentos sensiveis;
- continuidade:
  - procedimento de deploy e rollback;
  - ambiente de homologacao antes da producao.

## 10. Credenciais de e-mail corporativo (configuracao)

Variaveis de e-mail usadas no backend:
- `MAIL_HOST`
- `MAIL_PORT` (tipicamente `587` com STARTTLS)
- `MAIL_USERNAME`
- `MAIL_PASSWORD`
- `MAIL_FROM`
- `APP_FRONTEND_URL`

Exemplo corporativo:
- `MAIL_HOST=smtp.suaempresa.com.br`
- `MAIL_PORT=587`
- `MAIL_USERNAME=portalpartners@suaempresa.com.br`
- `MAIL_PASSWORD=<segredo>`
- `MAIL_FROM=portalpartners@suaempresa.com.br`
- `APP_FRONTEND_URL=https://portal.suaempresa.com.br`

Requisitos com time de infra/email:
- liberar autenticacao SMTP para a conta de servico;
- garantir SPF/DKIM/DMARC para reduzir rejeicao/antispam;
- definir limites de envio e politicas de bloqueio;
- registrar conta tecnica (nao pessoal) para operacao.

## 11. Variaveis de ambiente da aplicacao (resumo)

Banco:
- `POSTGRES_DB`
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

Seguranca:
- `JWT_SECRET`
- `JWT_EXPIRATION`

Storage:
- `MINIO_URL`
- `MINIO_ROOT_USER`
- `MINIO_ROOT_PASSWORD`
- `MINIO_BUCKET_NAME`
- `MINIO_ACCESS_KEY`
- `MINIO_SECRET_KEY`

Frontend/Email:
- `VITE_API_BASE_URL`
- `MAIL_HOST`
- `MAIL_PORT`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`
- `MAIL_FROM`
- `APP_FRONTEND_URL`

## 12. Estado atual e pendencias abertas

Ja validado recentemente:
- build do frontend em Docker estabilizado usando `npm ci`;
- fluxo basico de login, CRUDs e documentos.
- build backend Maven com suporte a primeiro acesso e usuarios da contratante;
- build frontend com rota de primeiro acesso e tela de usuarios da contratante.

Pendencias de negocio priorizadas:
- modelo multiusuario por dominio;
- auditoria transacional com tela admin;
- troca obrigatoria de senha no primeiro acesso;
- consentimento LGPD na rota de upload;
- vigencia de documentos e alertas.

---

## 13. Proximos passos imediatos sugeridos
1. aprovar este desenho tecnico das melhorias;
2. detalhar regras de vigencia por tipo de documento;
3. priorizar backlog por sprint (Fase 1 a Fase 4);
4. iniciar implementacao pela base de seguranca (dominio + primeiro acesso + autorizacao).

## 15. Funcionalidade implementada agora

### 15.1 Cadastro de organizacao contratante pelo admin
Fluxo implementado:
- o `ADMIN` cadastra uma nova contratante informando:
  - `nome`
  - `cnpj`
  - `email`
  - `senha`
- esse cadastro cria:
  - a organizacao `Contratante`;
  - o usuario principal da contratante com role `CONTRATANTE`;
  - `mustChangePassword=true`.

Regra:
- o dominio do e-mail informado passa a ser o dominio oficial da organizacao contratante.

### 15.2 Primeiro acesso obrigatorio
Fluxo implementado:
- ao fazer login com senha provisoria, o backend retorna `mustChangePassword=true`;
- o frontend redireciona para `/primeiro-acesso/alterar-senha`;
- o usuario so pode prosseguir apos alterar a senha.

Endpoint implementado:
- `POST /api/auth/change-password-first-access`

### 15.3 Gestao de usuarios da contratante
Fluxo implementado:
- apos o primeiro acesso, a contratante passa a ter uma nova rota no frontend:
  - `/contratante/usuarios`
- nessa rota, a contratante pode listar os usuarios vinculados e criar novos usuarios.

Campos para novo usuario da contratante:
- `nome`
- `email`
- `senha`

Regra de dominio:
- o novo usuario so pode ser criado se o dominio do e-mail for igual ao `dominioEmail` da contratante logada.

Permissao:
- usuarios criados nessa rota recebem role `CONTRATANTE` e acessam a mesma organizacao contratante com os mesmos privilegios.

Endpoints implementados:
- `GET /api/contratantes/usuarios`
- `POST /api/contratantes/usuarios`

Telas frontend implementadas:
- tela admin de cadastro de organizacao contratante com `nome`, `cnpj`, `email` e `senha`;
- tela de primeiro acesso para troca obrigatoria de senha;
- tela de usuarios da contratante para listar usuarios vinculados e cadastrar novos acessos na mesma organizacao.

### 15.4 UX de senha
Foi implementado no frontend o recurso de mostrar/ocultar senha nos principais formularios de cadastro/alteracao:
- login
- reset de senha
- primeiro acesso
- cadastro/edicao de contratante
- cadastro/edicao de contratada
- cadastro de usuario da contratante

### 15.5 Observacao tecnica importante sobre migracao de banco
Durante a subida do backend em ambiente com base de dados ja populada, foi identificado um problema de compatibilidade de schema:
- o campo `dominioEmail` foi adicionado na entidade `Contratante`;
- o PostgreSQL rejeitou a migracao automatica quando a coluna foi criada como `NOT NULL`, porque a tabela `contratante` ja possuia registros antigos sem valor para essa nova coluna.

Erro observado:
- `ERROR: column "dominio_email" of relation "contratante" contains null values`

Correcao aplicada:
- a coluna `dominioEmail` passou a ser criada sem `NOT NULL` na modelagem JPA;
- o backfill no startup continua preenchendo `dominioEmail` para registros legados a partir do e-mail do usuario principal da contratante;
- para novos cadastros, a regra de negocio continua exigindo dominio de e-mail valido da organizacao.

Resultado esperado apos rebuild:
- o backend consegue subir em banco existente;
- a coluna `dominio_email` e criada com sucesso;
- os registros antigos sao normalizados no startup sem derrubar a aplicacao.

### 15.6 Ajuste de persistencia no cadastro de contratante e seed inicial
Problema identificado durante os testes:
- ao cadastrar uma nova contratante, o backend retornava erro de persistencia:
  - `TransientPropertyValueException`
  - causa: a entidade `Contratante` estava sendo salva com referencia a um `Usuario` ainda nao persistido.

Correcao aplicada:
- no fluxo de criacao de `Contratante`, o `Usuario` passou a ser salvo primeiro;
- depois a `Contratante` e persistida com esse usuario ja salvo;
- por fim, o usuario recebe o vinculo explicito com a contratante.

Mesma correcao aplicada:
- no fluxo de criacao de `Contratada`, para evitar o mesmo tipo de problema.

### 15.7 Novo comportamento do DataSeeder
O `DataSeeder` foi simplificado para apoiar testes limpos apos reset de volumes.

Comportamento atual:
- se ja existirem registros no banco, o seed nao executa;
- se o banco estiver vazio, o sistema cria:
  - `ADMIN`
  - `CONTRATANTE`
  - `CONTRATADA`
- todos alinhados com a nova estrutura de usuarios e organizacoes;
- os usuarios seedados sobem com `mustChangePassword=false` para facilitar testes iniciais do ambiente.

Credenciais seedadas:
- Admin:
  - `admin@admin.com`
  - `admin123`
- Contratante:
  - `contratante@empresa.com`
  - `contratante123`
- Contratada:
  - `contratada@empresa.com`
  - `empresa123`

## 16. Roteiro de teste da fase 1

### 16.1 Teste do admin criando a organizacao contratante
Passos:
1. acessar a aplicacao com perfil `ADMIN`;
2. abrir a rota `/admin/contratantes`;
3. clicar em `Cadastrar Contratante`;
4. preencher:
   - `nome`
   - `cnpj`
   - `email`
   - `senha`
5. salvar.

Resultado esperado:
- a organizacao contratante e criada;
- o usuario principal e criado com `mustChangePassword=true`.

### 16.2 Teste do primeiro acesso da contratante
Passos:
1. efetuar logout do admin;
2. logar com o `email` e `senha` da contratante criada;
3. observar o redirecionamento automatico para `/primeiro-acesso/alterar-senha`;
4. informar:
   - senha atual
   - nova senha
   - confirmacao
5. concluir a troca;
6. efetuar login novamente.

Resultado esperado:
- o usuario nao acessa dashboard antes de trocar a senha;
- apos a troca, consegue entrar normalmente nas rotas da contratante.

### 16.3 Teste do multiusuario na mesma organizacao contratante
Passos:
1. logar com a contratante ja ativa;
2. abrir a rota `/contratante/usuarios`;
3. clicar em `Novo Usuario`;
4. preencher:
   - `nome`
   - `email` com o mesmo dominio da organizacao
   - `senha`
5. salvar.

Resultado esperado:
- o usuario e criado vinculado a mesma organizacao contratante;
- aparece na listagem de usuarios;
- o status inicial aparece como `Primeiro acesso pendente`.

### 16.4 Teste do login do usuario adicional
Passos:
1. sair da sessao atual;
2. logar com o novo usuario criado;
3. trocar a senha no primeiro acesso;
4. logar novamente.

Resultado esperado:
- o usuario adicional acessa a mesma organizacao contratante;
- possui os mesmos privilegios funcionais do perfil `CONTRATANTE`;
- consegue visualizar dashboard, documentos, contratadas e usuarios da contratante.

## 14. Plano de execucao por sprint (detalhado)

### 14.1 Premissas
- sprint de 2 semanas;
- equipe minima: 1 backend + 1 frontend + 1 QA (parcial);
- sem quebra de contrato produtivo atual (mudancas com retrocompatibilidade quando possivel).

### 14.2 Sprint 1 - Fundacao de identidade e seguranca
Objetivo:
- preparar base para multiusuario por dominio e primeiro acesso seguro.

Escopo tecnico:
- modelagem de organizacao por dominio (`OrganizacaoDominio`, `UsuarioOrganizacao`);
- ajuste de autenticacao para resolver contexto organizacional por dominio;
- `mustChangePassword` + endpoint de troca inicial;
- bloqueio de rotas quando senha inicial nao foi trocada.

PRs sugeridos:
1. migracoes/tabelas de organizacao e associacao de usuario;
2. servico de autenticacao com claims de organizacao no JWT;
3. endpoint e fluxo de troca obrigatoria de senha;
4. ajustes de guards no frontend para redirecionar ao fluxo de primeiro acesso.

Criterios de aceite:
- dois usuarios do mesmo dominio acessam o mesmo contexto de empresa;
- usuario criado por admin/contratante nao acessa funcionalidades sem trocar senha;
- apos troca inicial, login e navegacao funcionam normalmente.

Esforco estimado:
- 8 a 13 dias uteis (dependendo da complexidade de migracao dos dados existentes).

### 14.3 Sprint 2 - Auditoria e governanca LGPD
Objetivo:
- garantir rastreabilidade operacional ponta a ponta.

Escopo tecnico:
- tabela `audit_log` + camada de captura de eventos (AOP/interceptador);
- registro de login, CRUDs, upload e download;
- rota admin de consulta de logs com filtros e paginacao;
- tela frontend admin de auditoria;
- aceite LGPD na rota de upload (com persistencia de consentimento recomendada).

PRs sugeridos:
1. infraestrutura de auditoria (modelo + repositorio + servico);
2. instrumentacao dos endpoints criticos;
3. API de consulta admin para logs;
4. tela admin de logs;
5. UX de aceite LGPD + persistencia opcional em `lgpd_consent`.

Criterios de aceite:
- cada transacao critica gera evento auditavel;
- admin consegue filtrar por usuario, acao e periodo;
- upload bloqueado sem aceite LGPD.

Esforco estimado:
- 7 a 10 dias uteis.

### 14.4 Sprint 3 - UX de documentos e catalogo dinamico
Objetivo:
- melhorar usabilidade e preparar base de catalogo de tipos.

Escopo tecnico:
- tabela de documentos com linhas menores e sem clique na linha;
- manter apenas botao de download;
- ordenacao padrao decrescente + alternancia crescente/decrescente;
- lista dinamica de tipos por escopo (`EMPRESA` e `FUNCIONARIO`);
- criacao de catalogo de tipos (opcao A recomendada: entidade unica com escopo).

PRs sugeridos:
1. ajustes de listagem/ordenacao no backend;
2. ajustes de tabela no frontend (interacoes e ordenacao);
3. catalogo de tipos + endpoint por escopo;
4. integracao frontend do selector de escopo e tipos.

Criterios de aceite:
- ultimos registros aparecem no topo por padrao;
- usuario consegue alternar ordenacao com controle visual;
- somente botao de download dispara acao de download;
- troca de escopo atualiza lista de tipos corretamente.

Esforco estimado:
- 5 a 8 dias uteis.

### 14.5 Sprint 4 - Vigencia, regras e alertas
Objetivo:
- suportar validade de documentos e notificacao preventiva.

Escopo tecnico:
- campos de vigencia no `Documento` (`dataEmissao`, `dataValidade`, `periodicidade`);
- validacoes por tipo de documento (após mapeamento funcional);
- job diario de verificacao de vencimento;
- notificacao (e-mail e/ou badge) para proximidade de vencimento.

PRs sugeridos:
1. modelo e API de vigencia;
2. validacoes por regra de negocio;
3. job de vencimento + notificacoes;
4. ajustes visuais para mostrar status de vigencia.

Criterios de aceite:
- documentos armazenam vigencia conforme regra do tipo;
- sistema identifica e sinaliza vencimentos proximos;
- alertas visiveis para perfis corretos.

Esforco estimado:
- 6 a 10 dias uteis (dependente da tabela final de regras por tipo).

### 14.6 Dependencias, riscos e mitigacoes
Dependencias:
- definicao funcional do mapa de vigencia por tipo;
- validacao juridica do texto LGPD;
- alinhamento com TI para ambiente SMTP e infraestrutura.

Riscos:
- migracao de modelo de usuario sem regressao de autorizacao;
- impacto em consultas antigas de documentos/empresas;
- aumento de volume de logs.

Mitigacoes:
- feature flags para liberacao gradual;
- testes de regressao por perfil;
- indice e politica de retencao em `audit_log`.

### 14.7 Sequencia de entrega sugerida (PR order macro)
1. identidade por dominio + primeiro acesso;
2. auditoria backend;
3. auditoria frontend admin;
4. UX de tabela e download;
5. catalogo dinamico de tipos de documento;
6. vigencia e alertas.

---

## 17. Prompt de Implementacao Consolidado - Fase 2: Auditoria, Conformidade e Seguranca de Dados Sensiveis

Este prompt reune o melhor das duas abordagens tecnicas analisadas e serve como especificacao definitiva para implementacao da Fase 2. Deve ser usado integralmente ao iniciar a implementacao para garantir que nenhum requisito de segurança seja negligenciado.

---

### 17.1 Contexto completo do sistema

Stack atual:
- Backend: Java 17 + Spring Boot 3.3.3, Spring Security + JWT, Spring Data JPA, Spring Mail, MinIO Client 8.6.0, Maven.
- Frontend: React 18.3.1, Vite 5.1.0, Material UI 7.x, React Router 7.x, Axios.
- Infra: PostgreSQL 16, MinIO, Docker Compose.

Estado atual da Fase 1 (ja implementado e nao deve ser quebrado):
- Autenticacao multi-dominio por e-mail (`dominioEmail` em `Contratante`).
- Troca obrigatoria de senha no primeiro acesso (`mustChangePassword`, `FirstAccessPasswordEnforcementFilter`).
- Gestao de usuarios adicionais da contratante com validacao de dominio.
- Filtros JWT e cadeia de seguranca do Spring Security.

Perfis de acesso existentes: `ADMIN`, `CONTRATANTE`, `CONTRATADA`.

---

### 17.2 Objetivo da Fase 2

Implementar rastreabilidade operacional completa, conformidade LGPD e seguranca de dados sensiveis em repouso e em transito, com as seguintes garantias:

1. Nenhum byte de documento pessoal deve transitar pelo processo Java do backend.
2. Nenhum dado sensivel deve ser armazenado em plaintext no PostgreSQL.
3. Toda transacao critica deve gerar evento auditavel persistido de forma assincrona.
4. O consentimento LGPD deve ser registrado de forma auditavel e versionada antes de qualquer upload.
5. A criptografia em repouso no MinIO deve usar envelope encryption com chaves gerenciadas externamente.

---

### 17.3 Diretrizes criticas de implementacao

- Nao quebrar nenhuma funcionalidade existente (filtros JWT, logica de dominio, primeiro acesso, fluxo de documentos atual).
- Seguir principios SOLID: responsabilidade unica por classe, inversao de dependencia, abertura para extensao.
- Usar AOP (Aspect-Oriented Programming) para captura transversal de eventos de auditoria.
- Persistencia do `audit_log` deve ser estritamente assincrona (`@Async`) para nao onerar a latencia das requisicoes.
- Criptografia de campos deve usar AES-256-GCM (authenticated encryption) via Spring Security Crypto — nao usar Jasypt com configuracao padrao (algoritmo PBE/DES e inseguro).
- Chaves de criptografia nunca devem ser hardcoded nem versionadas em repositorio.
- Object key no MinIO deve ser sempre UUID v4 opaco, sem qualquer relacao semantica com o nome original do arquivo.

---

### 17.4 Tarefa 1 — Dependencias e pom.xml

Adicionar ao `pom.xml`:

```xml
<!-- AOP -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>

<!-- Spring Security Crypto (AES-256-GCM para campos sensiveis) -->
<!-- ja incluido transitivamente via spring-boot-starter-security -->
<!-- garantir que nao ha exclusao dessa dependencia -->

<!-- Suporte a JSONB no PostgreSQL via Hibernate Types -->
<dependency>
    <groupId>io.hypersistence</groupId>
    <artifactId>hypersistence-utils-hibernate-63</artifactId>
    <version>3.7.3</version>
</dependency>
```

Verificar que as dependencias abaixo ja existem (nao duplicar):
- `spring-boot-starter-security`
- `spring-boot-starter-data-jpa`
- `postgresql` (driver JDBC)
- `lombok`

---

### 17.5 Tarefa 2 — Modelo de dados: entidades e repositorios

#### 17.5.1 Entidade AuditLog

Campos obrigatorios:
- `id`: UUID, gerado automaticamente.
- `timestamp`: LocalDateTime, preenchido no momento do evento.
- `userId`: Long, ID do usuario autenticado (null se evento de login com falha antes da autenticacao).
- `email`: String, e-mail do usuario (capturado do token ou do request).
- `role`: String, papel do usuario no momento do evento.
- `organizacaoId`: Long, ID da organizacao vinculada ao usuario.
- `acao`: String (ou Enum `AcaoAuditoria`), descricao da acao executada.
- `entidade`: String, nome da entidade alvo (ex: `Documento`, `Funcionario`, `Usuario`).
- `entidadeId`: String, ID da entidade alvo (pode ser UUID ou Long convertido para String).
- `detalhesJson`: String, mapeado como tipo JSONB no PostgreSQL via `@Type(JsonType.class)` do hypersistence-utils; armazena contexto adicional do evento em formato JSON livre.
- `ip`: String, IP do cliente extraido do header `X-Forwarded-For` (com fallback para `getRemoteAddr()`).
- `userAgent`: String, header `User-Agent` da requisicao.
- `status`: Enum `StatusAuditoria` com valores `SUCCESS` e `FAILURE`.
- `mensagemErro`: String nullable, preenchido apenas quando `status = FAILURE` com a mensagem da excecao capturada.

Anotacoes JPA:
- `@Table(name = "audit_log")`
- Criar indice composto em `(timestamp, userId, acao)` para suportar as queries de filtro do admin.
- Criar indice em `(organizacaoId, timestamp)` para filtros por organizacao.

#### 17.5.2 Entidade LgpdConsent

Campos obrigatorios:
- `id`: UUID, gerado automaticamente.
- `userId`: Long, ID do usuario que aceitou.
- `timestamp`: LocalDateTime, momento exato do aceite.
- `versaoTermo`: String, identificador da versao do texto exibido (ex: `"v1.0"`, `"2024-01"`). Fundamental para evidencia juridica.
- `ip`: String, IP do cliente no momento do aceite.
- `userAgent`: String, header `User-Agent` no momento do aceite.
- `hashTermo`: String, SHA-256 do texto integral do termo aceito. Permite provar em auditoria qual conteudo exato foi aceito mesmo que o texto mude futuramente.

Anotacoes JPA:
- `@Table(name = "lgpd_consent")`
- Criar indice em `(userId, versaoTermo)` para queries de verificacao de consentimento valido.

#### 17.5.3 Repositorios

`AuditLogRepository`:
- Estender `JpaRepository<AuditLog, UUID>`.
- Metodo: `Page<AuditLog> findByFilters(...)` usando `JpaSpecificationExecutor` para suportar os filtros da tela admin (periodo, userId, acao, entidade, status, organizacaoId).

`LgpdConsentRepository`:
- Estender `JpaRepository<LgpdConsent, UUID>`.
- Metodo: `Optional<LgpdConsent> findTopByUserIdAndVersaoTermoOrderByTimestampDesc(Long userId, String versaoTermo)` para verificar consentimento valido antes de permitir upload.

---

### 17.6 Tarefa 3 — Criptografia de campos sensiveis no PostgreSQL

#### 17.6.1 FieldEncryptionService

Criar servico `FieldEncryptionService` usando `AesBytesEncryptor` do Spring Security Crypto com algoritmo `AES_256` (modo GCM — authenticated encryption):

Comportamento esperado:
- `encrypt(String plaintext)`: cifra com AES-256-GCM e retorna Base64 URL-safe.
- `decrypt(String ciphertext)`: decifra e retorna plaintext.
- Usar salt aleatorio por operacao (IV gerado automaticamente pelo `AesBytesEncryptor` em modo GCM).
- A chave de criptografia e carregada de variavel de ambiente `APP_FIELD_ENCRYPTION_KEY` (32 bytes em Base64).

Razao para nao usar Jasypt:
- O algoritmo padrao do Jasypt (PBEWithMD5AndDES) e criptograficamente inseguro.
- Mesmo com configuracao para AES, o Jasypt usa CBC sem autenticacao, vulneravel a bit-flipping attacks.
- AES-256-GCM autentica o ciphertext: qualquer adulteracao e detectada antes da decifragem.

#### 17.6.2 AttributeConverter para campos sensiveis

Criar `EncryptedStringConverter implements AttributeConverter<String, String>`:
- `convertToDatabaseColumn`: chama `fieldEncryptionService.encrypt(value)`.
- `convertToEntityAttribute`: chama `fieldEncryptionService.decrypt(dbValue)`.
- Anotar o converter com `@Converter`.

Aplicar `@Convert(converter = EncryptedStringConverter.class)` nos seguintes campos sensiveis:
- `Funcionario.cpf` (quando existir)
- `Funcionario.rg` (quando existir)
- `Documento.nomeArquivoOriginal` (campo que armazena o nome real do arquivo — ver Tarefa 4)
- Qualquer outro campo que armazene numero de documento pessoal identificavel.

#### 17.6.3 Variavel de ambiente obrigatoria

Adicionar ao `docker-compose.yml` e ao `.env.example`:
```
APP_FIELD_ENCRYPTION_KEY=<32 bytes aleatorios em Base64 — nunca commitar valor real>
```

Adicionar ao `application.properties`:
```properties
app.field-encryption-key=${APP_FIELD_ENCRYPTION_KEY}
```

Estrategia de rotacao:
- Documentar procedimento de re-cifragem dos campos ao rotacionar a chave.
- Em producao, usar secret manager (HashiCorp Vault, AWS Secrets Manager ou equivalente) em vez de variavel de ambiente plana.

---

### 17.7 Tarefa 4 — Arquitetura Zero-Copy para documentos (Presigned URLs)

Esta tarefa e a mais impactante para a seguranca de documentos pessoais. No modelo atual, os bytes dos arquivos transitam pelo processo Java do backend, ficam em memoria durante o streaming e so entao chegam ao MinIO. Isso cria superficie de ataque desnecessaria.

#### 17.7.1 Principio da arquitetura Zero-Copy

Modelo atual (inseguro para documentos pessoais):
```
Browser → POST /api/documentos/upload (multipart) → Backend Java → MinIO
Browser ← GET /api/documentos/{id}/download ← Backend Java ← MinIO
```

Modelo alvo (Zero-Copy):
```
Browser → POST /api/documentos/solicitar-upload → Backend (metadados + validacoes) → retorna presigned PUT URL
Browser → PUT <presigned URL> → MinIO (direto, sem passar pelo backend)
Browser → GET /api/documentos/{id}/solicitar-download → Backend (validacao de permissao + audit log) → retorna presigned GET URL
Browser → GET <presigned URL> → MinIO (direto, sem passar pelo backend)
```

#### 17.7.2 Object key como UUID opaco

Regra critica: o `objectKey` armazenado no MinIO DEVE ser um UUID v4 gerado no momento da solicitacao de upload. O nome original do arquivo (ex: `RG_joao_silva.pdf`) NUNCA deve ser o object key nem aparecer em plaintext no banco.

Estrategia de armazenamento na entidade `Documento`:
- `objectKey`: String, UUID v4, armazenado em plaintext no banco (e apenas um identificador opaco, nao revela informacao pessoal).
- `nomeArquivoOriginal`: String, nome real do arquivo, armazenado cifrado com `@Convert(converter = EncryptedStringConverter.class)`.
- `contentType`: String, tipo MIME (ex: `application/pdf`), nao sensivel, plaintext.
- `tamanhoBytes`: Long, tamanho do arquivo, nao sensivel, plaintext.

#### 17.7.3 Endpoint de solicitacao de upload

`POST /api/documentos/solicitar-upload`

Request body: `{ "nomeArquivo": "...", "contentType": "...", "tamanhoBytes": ..., "tipoDocumento": "...", "funcionarioId": ... }`

Fluxo no backend:
1. Validar autenticacao e autorizacao do usuario.
2. Validar consentimento LGPD (`LgpdConsentRepository.findTop...`) — bloquear se nao houver consentimento valido para a versao corrente do termo.
3. Gerar UUID v4 como `objectKey`.
4. Persistir entidade `Documento` com status `PENDENTE_UPLOAD`, `objectKey`, `nomeArquivoOriginal` cifrado e demais metadados.
5. Gerar presigned PUT URL com TTL de 10 minutos via `MinioClient.getPresignedObjectUrl(Method.PUT, ...)`.
6. Registrar evento no `audit_log` (acao: `SOLICITAR_UPLOAD`, entidadeId: UUID do documento).
7. Retornar `{ "documentoId": "...", "uploadUrl": "...", "objectKey": "...", "expiresInSeconds": 600 }`.

Apos o frontend completar o PUT diretamente ao MinIO:
- `POST /api/documentos/{documentoId}/confirmar-upload` — backend atualiza status para `ENVIADO` e registra evento no `audit_log` (acao: `UPLOAD_CONCLUIDO`).

#### 17.7.4 Endpoint de solicitacao de download

`GET /api/documentos/{id}/solicitar-download`

Fluxo no backend:
1. Validar autenticacao e autorizacao (usuario tem permissao para acessar este documento?).
2. Buscar `Documento` pelo ID, validar que status nao e `EXCLUIDO`.
3. Gerar presigned GET URL com TTL de 15 minutos via `MinioClient.getPresignedObjectUrl(Method.GET, ...)`.
4. Registrar evento no `audit_log` de forma assincrona (acao: `DOWNLOAD_SOLICITADO`, entidadeId: ID do documento, detalhesJson: `{ "objectKey": "...", "urlExpiracao": "..." }`).
5. Retornar `{ "downloadUrl": "...", "nomeArquivo": "...", "expiresInSeconds": 900 }`.

O frontend usa a URL retornada para fazer o GET diretamente ao MinIO. Nenhum byte de documento passa pelo backend.

#### 17.7.5 Controle de uso unico da presigned URL (recomendado)

Para evitar compartilhamento de URLs assinadas entre usuarios:
- Registrar no `audit_log` o timestamp de geracao da URL.
- Opcionalmente, criar tabela `presigned_url_token` com `(tokenHash, documentoId, userId, geradaEm, expiradaEm, utilizada)`.
- Antes de gerar nova URL, verificar se ja existe URL valida nao utilizada para o mesmo usuario/documento.

---

### 17.8 Tarefa 5 — Criptografia em repouso no MinIO (SSE)

#### 17.8.1 Opcoes de SSE e escolha recomendada

Opcao A — SSE-S3 com chave gerenciada pelo MinIO (implementacao imediata):
- MinIO gera e gerencia a chave de criptografia internamente.
- Todos os objetos sao cifrados com AES-256 em repouso.
- Menor complexidade de infraestrutura.
- Limitacao: a chave de cifragem reside no mesmo servidor MinIO. Acesso fisico ao storage e acesso ao MinIO dao acesso aos dados.
- Adequado para o ambiente inicial e para desenvolvimento.

Opcao B — SSE-KMS com MinIO KES + HashiCorp Vault (implementacao para producao):
- Envelope encryption: cada objeto e cifrado com uma DEK (Data Encryption Key) unica.
- A DEK e cifrada com a KEK (Key Encryption Key) armazenada no Vault.
- A KEK nunca sai do Vault e nunca e armazenada no MinIO ou no banco.
- Acesso fisico ao storage e inutil sem acesso ao Vault.
- KES (Key Encryption Service) e um servico leve open-source do proprio MinIO que faz a ponte com o Vault.
- Recomendado para o ambiente de producao.

Decisao de implementacao:
- Implementar SSE-S3 no ambiente Docker Compose atual para garantir cifragem imediata.
- Documentar e preparar a migracao para SSE-KMS com KES/Vault na checklist de producao.

#### 17.8.2 Configuracao SSE-S3 no MinioClient

No `MinioConfig.java`, ao criar o bucket, habilitar default encryption via API administrativa do MinIO:

```java
// Configurar default encryption SSE-S3 no bucket no momento da inicializacao
SetBucketEncryptionArgs.builder()
    .bucket(bucketName)
    .config(SseConfiguration.serverSideEncryptionByDefault(SseAlgorithm.AES256))
    .build()
```

Ao fazer presigned PUT URL, incluir o header `x-amz-server-side-encryption: AES256` nos extra query params para garantir que objetos sem SSE explicito sejam rejeitados.

#### 17.8.3 Variaveis de ambiente para producao com KES/Vault

Adicionar ao `.env.example` e documentar como placeholder para producao:
```
MINIO_KMS_KES_ENDPOINT=https://kes:7373
MINIO_KMS_KES_KEY_NAME=portal-partners-master-key
MINIO_KMS_KES_CERT_FILE=/certs/kes-cert.pem
MINIO_KMS_KES_KEY_FILE=/certs/kes-key.pem
```

---

### 17.9 Tarefa 6 — Mecanismo de auditoria via AOP

#### 17.9.1 Anotacao customizada @Auditavel

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Auditavel {
    String acao();
    String entidade() default "";
    boolean capturarArgs() default false; // false por padrao para nao logar dados sensiveis
}
```

O campo `capturarArgs = false` por padrao e intencional: evitar que argumentos do metodo (que podem conter dados pessoais como senhas, CPF, etc.) sejam automaticamente serializados no `detalhesJson`.

#### 17.9.2 AuditAspect

Criar `AuditAspect` com `@Aspect @Component`:

Comportamento:
- Interceptar todos os metodos anotados com `@Auditavel` via `@Around`.
- Antes de executar o metodo: capturar usuario autenticado via `SecurityContextHolder`, IP via `HttpServletRequest` (checar `X-Forwarded-For` primeiro, fallback para `getRemoteAddr()`), User-Agent via header.
- Executar o metodo (`joinPoint.proceed()`).
- Em caso de sucesso: chamar `auditService.registrar(...)` com `status = SUCCESS` de forma assincrona.
- Em caso de excecao: chamar `auditService.registrar(...)` com `status = FAILURE` e `mensagemErro = exception.getMessage()`, depois relancear a excecao original.
- A chamada ao `auditService.registrar(...)` deve ser sempre via metodo `@Async` para nao bloquear a thread da requisicao.

Captura do `entidadeId`:
- Para metodos que retornam uma entidade com ID, o Aspect pode extrair o ID do retorno via reflexao se o objeto implementar uma interface `Identificavel` com metodo `getId()`.
- Alternativa mais simples: o desenvolvedor passa o ID manualmente via parametro da anotacao ou via contexto threadlocal.

#### 17.9.3 AuditService

Criar `AuditService` com metodo `@Async registrar(AuditEventDTO evento)`:
- Construir entidade `AuditLog` a partir do DTO.
- Persistir via `AuditLogRepository.save(...)`.
- O metodo deve ser `@Async` — a thread principal nao espera pela persistencia.
- Capturar excecoes de persistencia internamente (nunca deixar falha de auditoria derrubar a requisicao principal).

Configuracao do `@Async`:
- Criar `@Configuration` com `@EnableAsync`.
- Configurar `ThreadPoolTaskExecutor` dedicado para auditoria com pool size entre 2 e 10 threads, fila de 500 eventos.

---

### 17.10 Tarefa 7 — Instrumentacao dos pontos criticos

Aplicar `@Auditavel` nos seguintes metodos:

#### AuthService:
- Metodo de login com sucesso: `acao = "LOGIN_SUCESSO"`, `entidade = "Usuario"`.
- Metodo de login com falha (capturar na excecao): `acao = "LOGIN_FALHA"`.
- Metodo de troca de senha no primeiro acesso: `acao = "PRIMEIRO_ACESSO_SENHA_ALTERADA"`.
- Metodo de reset de senha: `acao = "SENHA_RESETADA"`.

#### DocumentoService / DocumentoController:
- Solicitacao de upload: `acao = "SOLICITAR_UPLOAD"`, `entidade = "Documento"`.
- Confirmacao de upload: `acao = "UPLOAD_CONCLUIDO"`, `entidade = "Documento"`.
- Solicitacao de download (geracao de presigned URL): `acao = "DOWNLOAD_SOLICITADO"`, `entidade = "Documento"`.
- Exclusao de documento: `acao = "DOCUMENTO_EXCLUIDO"`, `entidade = "Documento"`.
- Alteracao de status de documento: `acao = "STATUS_DOCUMENTO_ALTERADO"`, `entidade = "Documento"`.

#### ContratanteService / ContratanteController:
- Criacao de contratante: `acao = "CONTRATANTE_CRIADA"`, `entidade = "Contratante"`.
- Atualizacao de contratante: `acao = "CONTRATANTE_ATUALIZADA"`, `entidade = "Contratante"`.
- Exclusao de contratante: `acao = "CONTRATANTE_EXCLUIDA"`, `entidade = "Contratante"`.

#### ContratanteUsuarioService:
- Criacao de usuario adicional: `acao = "USUARIO_CONTRATANTE_CRIADO"`, `entidade = "Usuario"`.

#### ContratadaService:
- Criacao de contratada: `acao = "CONTRATADA_CRIADA"`, `entidade = "Contratada"`.
- Atualizacao: `acao = "CONTRATADA_ATUALIZADA"`, `entidade = "Contratada"`.
- Exclusao: `acao = "CONTRATADA_EXCLUIDA"`, `entidade = "Contratada"`.

#### FuncionarioService:
- Criacao: `acao = "FUNCIONARIO_CRIADO"`, `entidade = "Funcionario"`.
- Exclusao: `acao = "FUNCIONARIO_EXCLUIDO"`, `entidade = "Funcionario"`.

---

### 17.11 Tarefa 8 — Consentimento LGPD

#### 17.11.1 Backend

Endpoint: `POST /api/lgpd/consentimento`

Request body: `{ "versaoTermo": "v1.0", "hashTermo": "<SHA-256 do texto>" }`

Fluxo:
1. Capturar usuario autenticado, IP e User-Agent.
2. Calcular e validar `hashTermo` contra o hash oficial da versao do termo configurado no backend (variavel `APP_LGPD_TERMO_HASH_V1`).
3. Persistir `LgpdConsent`.
4. Registrar evento no `audit_log` (acao: `LGPD_CONSENTIMENTO_REGISTRADO`).
5. Retornar confirmacao com timestamp.

Endpoint de verificacao: `GET /api/lgpd/consentimento/valido?versaoTermo=v1.0`
- Retorna `{ "valido": true/false, "timestamp": "..." }` para o frontend exibir o estado correto antes do upload.

#### 17.11.2 Frontend

Na tela `UploadDocumento.tsx`:
- Ao abrir a tela, chamar `GET /api/lgpd/consentimento/valido`.
- Se `valido = false`, exibir banner LGPD com o texto integral do termo e checkbox obrigatorio.
- O texto do termo deve ter versao e data visiveis (ex: "Termos de uso e privacidade - versao 1.0 - Janeiro/2025").
- O botao de upload permanece desabilitado enquanto o aceite nao for registrado via `POST /api/lgpd/consentimento`.
- Apos aceite registrado com sucesso, habilitar a tela de upload.
- Se `valido = true` (aceite ja registrado nesta versao), prosseguir diretamente para o upload.

Variaveis de ambiente adicionais:
```
APP_LGPD_VERSAO_TERMO_ATUAL=v1.0
APP_LGPD_TERMO_HASH_V1=<SHA-256 do texto do termo>
```

---

### 17.12 Tarefa 9 — API admin de consulta de logs

Endpoint: `GET /api/admin/audit-log`

Filtros suportados via query params:
- `startDate`, `endDate` (periodo)
- `userId` (ID do usuario)
- `email` (busca parcial)
- `acao` (acao especifica ou lista separada por virgula)
- `entidade` (tipo de entidade)
- `status` (`SUCCESS` ou `FAILURE`)
- `organizacaoId`
- `page`, `size`, `sortBy`, `sortDir` (paginacao e ordenacao)

Resposta:
- `Page<AuditLogResponse>` com campos: `id`, `timestamp`, `email`, `role`, `acao`, `entidade`, `entidadeId`, `status`, `mensagemErro`, `ip`, `userAgent`, `detalhesJson`.
- O campo `detalhesJson` deve ser retornado como objeto JSON (nao como string serializada).

Permissao: apenas `ADMIN`.

Politica de retencao:
- Criar job agendado (`@Scheduled`) para arquivar ou excluir registros de `audit_log` com mais de 365 dias.
- Configurar indice parcial ou particao por range de data para manter performance da tabela com alto volume.

---

### 17.13 Tarefa 10 — Scripts SQL de migracao

Criar os seguintes scripts SQL em `backend/src/main/resources/db/migration/` (ou equivalente):

`V2__create_audit_log.sql`:
```sql
CREATE TABLE audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    timestamp TIMESTAMP NOT NULL,
    user_id BIGINT,
    email VARCHAR(255),
    role VARCHAR(50),
    organizacao_id BIGINT,
    acao VARCHAR(100) NOT NULL,
    entidade VARCHAR(100),
    entidade_id VARCHAR(255),
    detalhes_json JSONB,
    ip VARCHAR(50),
    user_agent VARCHAR(500),
    status VARCHAR(20) NOT NULL,
    mensagem_erro TEXT
);

CREATE INDEX idx_audit_log_timestamp_user ON audit_log (timestamp, user_id);
CREATE INDEX idx_audit_log_organizacao ON audit_log (organizacao_id, timestamp);
CREATE INDEX idx_audit_log_acao ON audit_log (acao);
CREATE INDEX idx_audit_log_status ON audit_log (status);
```

`V3__create_lgpd_consent.sql`:
```sql
CREATE TABLE lgpd_consent (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    versao_termo VARCHAR(20) NOT NULL,
    ip VARCHAR(50),
    user_agent VARCHAR(500),
    hash_termo VARCHAR(255)
);

CREATE INDEX idx_lgpd_consent_user_versao ON lgpd_consent (user_id, versao_termo);
```

`V4__add_documento_security_fields.sql`:
```sql
-- Adicionar campo de nome cifrado ao documento (object_key ja deve ser UUID)
ALTER TABLE documento ADD COLUMN IF NOT EXISTS nome_arquivo_original TEXT;
ALTER TABLE documento ADD COLUMN IF NOT EXISTS object_key_migrado BOOLEAN DEFAULT FALSE;
-- Nota: executar script de migracao de dados para converter object_keys existentes para UUID
-- e cifrar nomes originais antes de remover colunas antigas.
```

Observacao: se o projeto estiver usando `ddl-auto: update`, os scripts SQL acima sao complementares para garantir indices e tipos de coluna corretos (o `ddl-auto: update` nao cria indices customizados nem controla tipos JSONB).

#### 17.13.1 Hotfix de compatibilidade para bases legadas

Durante a validacao da implementacao foi identificado que bases PostgreSQL ja existentes podiam permanecer sem a coluna `funcionario.cpf_hash`, mesmo com a nova modelagem de `Funcionario` e os scripts versionados no repositorio. Isso quebrava:

- a tela de funcionarios;
- a listagem de documentos que possuam vinculo com funcionario;
- qualquer carregamento JPA da entidade `Funcionario` apos a introducao da criptografia/HMAC do CPF.

Hotfix aplicado no backend:

- criar `LegacySchemaCompatibilityInitializer.java`;
- na inicializacao da aplicacao, verificar via `information_schema` se as colunas esperadas existem em `funcionario` e `documento`;
- criar automaticamente colunas faltantes de compatibilidade (`cpf_hash`, `nome_arquivo_original`, `object_key`, `content_type`, `tamanho_bytes`, `data_download_contratante`, `data_status_atualizado`);
- executar backfill de `cpf_hash` para funcionarios legados usando `FieldEncryptionService.decrypt(...)` + normalizacao do CPF + `hash(...)`;
- manter retrocompatibilidade com registros antigos cujo CPF ainda estava em plaintext antes da criptografia transparente via `AttributeConverter`.

Resultado esperado do hotfix:

- a aplicacao sobe em banco legado sem exigir ajuste manual imediato;
- as telas de `Funcionarios` e `Documentos` voltam a carregar normalmente;
- o `audit_log` continua funcionando sem regressao, pois o erro original nao estava na trilha de auditoria e sim no schema incompleto da entidade relacionada.

---

### 17.14 Tarefa 11 — Ajustes em docker-compose.yml e variaveis de ambiente

Adicionar ao servico `backend` no `docker-compose.yml`:
```yaml
APP_FIELD_ENCRYPTION_KEY: ${APP_FIELD_ENCRYPTION_KEY}
APP_LGPD_VERSAO_TERMO_ATUAL: ${APP_LGPD_VERSAO_TERMO_ATUAL}
APP_LGPD_TERMO_HASH_V1: ${APP_LGPD_TERMO_HASH_V1}
```

Adicionar ao `.env.example` (nunca commitar valores reais):
```
APP_FIELD_ENCRYPTION_KEY=<32 bytes aleatorios em Base64>
APP_LGPD_VERSAO_TERMO_ATUAL=v1.0
APP_LGPD_TERMO_HASH_V1=<SHA-256 do texto do termo LGPD>
```

Adicionar ao `.gitignore` (verificar se ja existe):
```
.env
*.env.local
```

Para producao — adicionar servico KES (preparar mas nao ativar em desenvolvimento):
```yaml
kes:
  image: minio/kes:2024-04-12T13-34-50Z
  command: server --config /etc/kes/config.yaml
  volumes:
    - ./kes-config:/etc/kes:ro
    - ./kes-certs:/certs:ro
  ports:
    - "7373:7373"
  depends_on:
    - vault

vault:
  image: hashicorp/vault:1.17
  cap_add: [IPC_LOCK]
  environment:
    VAULT_DEV_ROOT_TOKEN_ID: ${VAULT_DEV_TOKEN}
  ports:
    - "8200:8200"
```

---

### 17.15 Entregaveis esperados da implementacao

- [ ] `AuditLog.java` — entidade JPA com mapeamento JSONB.
- [ ] `LgpdConsent.java` — entidade JPA.
- [ ] `AuditLogRepository.java` — com `JpaSpecificationExecutor`.
- [ ] `LgpdConsentRepository.java`.
- [ ] `AuditLogSpecification.java` — specs para filtros do admin.
- [ ] `FieldEncryptionService.java` — AES-256-GCM via Spring Security Crypto.
- [ ] `EncryptedStringConverter.java` — `AttributeConverter` JPA.
- [ ] `Auditavel.java` — anotacao customizada.
- [ ] `AuditAspect.java` — `@Aspect` com captura assincrona.
- [ ] `AuditService.java` — servico `@Async` de persistencia.
- [ ] `AsyncConfig.java` — configuracao do `ThreadPoolTaskExecutor`.
- [ ] `LgpdService.java` — registro e verificacao de consentimento.
- [ ] `LgpdController.java` — endpoints `/api/lgpd/consentimento`.
- [ ] `AdminAuditController.java` — endpoint `/api/admin/audit-log` com filtros e paginacao.
- [ ] `DocumentoService.java` (atualizado) — presigned URL para upload e download, sem bytes transitando pelo backend.
- [ ] `DocumentoController.java` (atualizado) — novos endpoints de solicitacao.
- [ ] `MinioConfig.java` (atualizado) — SSE-S3 habilitado no bucket na inicializacao.
- [ ] `BucketInitializer.java` (atualizado) — configurar default encryption.
- [ ] `V2__create_audit_log.sql` — script SQL com indices.
- [ ] `V3__create_lgpd_consent.sql` — script SQL com indices.
- [ ] `V4__add_documento_security_fields.sql` — ajustes na tabela documento.
- [ ] `pom.xml` (atualizado) — dependencias AOP e hypersistence-utils.
- [x] `docker-compose.yml` (atualizado) — novas variaveis de ambiente (incluindo `MINIO_PUBLIC_URL` para presigned URLs acessiveis no navegador em dev/hml).
- [x] `.env.example` (atualizado) — todas as novas variaveis documentadas.
- [x] `UploadDocumento.tsx` (atualizado) — fluxo LGPD + presigned PUT URL.
- [x] `Documentos.tsx` (atualizado) — presigned GET URL no lugar de download direto pelo backend.

---

### 17.16 Criterios de aceite da Fase 2

Seguranca de dados:
- [ ] Nenhum byte de documento pessoal transita pelo processo Java em upload ou download.
- [ ] Nenhum nome de arquivo real e armazenado em plaintext no PostgreSQL.
- [ ] Campos sensiveis de PF (CPF, RG) sao cifrados com AES-256-GCM antes de persistir.
- [ ] Object keys no MinIO sao UUIDs opacos sem relacao semantica com o conteudo.
- [ ] Todos os objetos no MinIO sao cifrados em repouso via SSE-S3 (minimo) ou SSE-KMS (producao).
- [ ] Chaves de criptografia nao existem em nenhum arquivo versionado no repositorio.

Auditoria:
- [ ] Cada login (sucesso e falha) gera evento no `audit_log`.
- [ ] Cada solicitacao de upload e download gera evento no `audit_log`.
- [ ] Cada CRUD de contratante, contratada, funcionario e usuario gera evento.
- [ ] Cada alteracao de senha gera evento.
- [ ] A persistencia do `audit_log` e assincrona e nao bloqueia a resposta da requisicao.
- [ ] Falha na persistencia do log nao derruba a requisicao original.
- [ ] Admin consegue filtrar logs por periodo, usuario, acao, entidade, status e organizacao.
- [ ] Paginacao funciona corretamente com grandes volumes de registros.

LGPD:
- [ ] Upload e bloqueado para usuario sem consentimento LGPD valido para a versao corrente do termo.
- [ ] O consentimento e registrado com versao, hash do termo, IP e User-Agent.
- [ ] Apos aceite, o usuario nao e solicitado novamente ate que uma nova versao do termo seja publicada.

Retrocompatibilidade:
- [ ] Login, troca de senha no primeiro acesso e demais fluxos da Fase 1 continuam funcionando.
- [ ] Filtro JWT e cadeia de seguranca do Spring Security nao foram alterados.
- [ ] DataSeeder continua funcionando corretamente apos as migracoes.

---

### 17.17 Riscos e mitigacoes especificos desta fase

| Risco | Mitigacao |
|---|---|
| Migracao de `objectKey` existente para UUID | Script de migracao com transacao, rollback preparado, executar fora do horario de producao |
| Decifragem de campos falha em dados legados (sem cifragem) | `AttributeConverter` deve tratar valor sem prefixo de cifragem como plaintext (migracao gradual) |
| Alta volumetria em `audit_log` degradando queries | Indices compostos + job de arquivamento + monitorar plano de execucao apos 30 dias |
| Presigned URL expirada antes do usuario completar o upload | TTL de 10 minutos generoso + frontend exibe erro claro e permite re-solicitar a URL |
| Chave de cifragem de campo perdida | Backup criptografado da chave em local separado do servidor; documentar procedimento de recuperacao |
| Frontend guarda presigned URL em localStorage | Instruir implementacao a usar apenas estado em memoria (nunca persistir presigned URL) |

---

## 18. Implementacao realizada — Fase 2: Auditoria, Conformidade e Seguranca de Dados

Esta secao registra o estado real do codigo apos a implementacao da Fase 2. Serve como referencia tecnica para onboarding, revisao de codigo e continuidade do projeto.

Data de implementacao: Marco 2025.
Aplicacao em execucao: build Docker validado, tabelas criadas automaticamente via `ddl-auto=update`.

---

### 18.1 Resumo executivo

A Fase 2 entregou tres pilares de seguranca e conformidade:

1. **Auditoria transacional**: toda acao critica (login, upload, download, CRUD) gera evento persistido de forma assincrona no `audit_log`. Nenhuma latencia adicionada as requisicoes.
2. **Criptografia de dados em repouso**: campos pessoais sensiveis (CPF, nome de arquivo) sao cifrados com AES-256-GCM antes de persistir no PostgreSQL. Object keys no MinIO sao UUIDs opacos sem relacao com o conteudo.
3. **Conformidade LGPD**: upload bloqueado para usuarios sem consentimento valido. Cada aceite e registrado com versao, hash do termo, IP e User-Agent.

---

### 18.2 Novas dependencias adicionadas (pom.xml)

```xml
<!-- AOP para auditoria transversal -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>

<!-- Suporte a tipo JSONB no PostgreSQL via Hibernate 6 -->
<dependency>
    <groupId>io.hypersistence</groupId>
    <artifactId>hypersistence-utils-hibernate-63</artifactId>
    <version>3.7.3</version>
</dependency>

<!-- Jackson para serializacao do detalhesJson no AuditService -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>
```

---

### 18.3 Novos pacotes e arquivos criados

#### Pacote `audit/`
- `Auditavel.java`: anotacao customizada `@Auditavel(acao, entidade, capturarArgs)`. `capturarArgs=false` por padrao para nao logar dados sensiveis.
- `AuditEventDTO.java`: DTO builder com todos os campos do evento.
- `AuditAspect.java`: aspecto `@Around` que intercepta metodos anotados, captura IP real via `X-Forwarded-For`, email do SecurityContext (ou dos args para login falho) e dispara evento assincrono.
- `AuditService.java`: servico com metodo `@Async("auditExecutor")` para persistencia nao-bloqueante + job `@Scheduled` diario para remover registros com mais de 365 dias.
- `AuditLogSpecification.java`: specs JPA para 8 filtros (periodo, userId, email, acao, entidade, status, organizacaoId).

#### Pacote `crypto/`
- `FieldEncryptionService.java`: AES-256-GCM com IV aleatorio de 12 bytes por operacao. Formato do ciphertext armazenado: `Base64URL(IV[12] + ciphertext + GCM_TAG[16])`. Metodo `hash()` usa HMAC-SHA256 deterministico para campos que precisam de busca/unicidade (CPF).
- `EncryptedStringConverter.java`: `AttributeConverter` JPA. Injecao via campo estatico (padrao necessario porque JPA instancia converters fora do Spring). Retrocompativel: valores sem padding Base64URL valido sao retornados sem decifrar.

#### Pacote `config/`
- `AsyncConfig.java`: `@EnableAsync` + `@EnableScheduling` + `ThreadPoolTaskExecutor` dedicado `audit-*` (2-10 threads, fila de 500 eventos, shutdown gracioso de 30s).

#### Modelos novos (`model/`)
- `AuditLog.java`: entidade com campo `detalhesJson` mapeado como `JSONB` via `@Type(JsonType.class)`. Indices compostos definidos via `@Index` no `@Table`.
- `LgpdConsent.java`: entidade com `hashTermo` (SHA-256 do texto aceito) para evidencia juridica.
- `StatusAuditoria.java`: enum `SUCCESS / FAILURE`.

#### Repositorios novos (`repository/`)
- `AuditLogRepository.java`: estende `JpaRepository` + `JpaSpecificationExecutor`. Metodo `deleteByTimestampBefore` para o job de retencao.
- `LgpdConsentRepository.java`: metodo `findTopByUserIdAndVersaoTermoOrderByTimestampDesc` para verificar consentimento valido.

#### Servicos novos (`service/`)
- `LgpdService.java`: `registrarConsentimento()` valida hash do termo contra o configurado no backend antes de persistir. `exigirConsentimentoValido()` e chamado pelo `DocumentoService` antes de qualquer upload.

#### Controllers novos (`controller/`)
- `LgpdController.java`:
  - `POST /api/lgpd/consentimento`: registra aceite LGPD.
  - `GET /api/lgpd/consentimento/valido`: retorna `{ valido, versaoTermo, timestamp }`.
- `AdminAuditController.java`:
  - `GET /api/admin/audit-log`: listagem paginada com 8 filtros, ordenacao configuravel. Acesso exclusivo `ROLE_ADMIN`.

#### DTOs novos (`dto/`)
- `SolicitarUploadRequest.java`: record com `nomeArquivo`, `contentType`, `tamanhoBytes`, `tipoDocumento`, `tipoReferencia`, `funcionarioId`, `contratadaId`.
- `SolicitarUploadResponse.java`: record com `documentoId`, `objectKey`, `uploadUrl`, `expiresInSeconds`.
- `SolicitarDownloadResponse.java`: record com `downloadUrl`, `nomeArquivo`, `contentType`, `expiresInSeconds`.
- `LgpdConsentRequest.java`: record com `versaoTermo` e `hashTermo`.
- `LgpdConsentResponse.java`: record com `valido`, `versaoTermo`, `timestamp`.
- `AuditLogResponse.java`: DTO com `fromEntity()` para a API admin.

#### Scripts SQL (`resources/db/migration/`)
- `V2__create_audit_log.sql`: tabela `audit_log` com 5 indices incluindo GIN para JSONB.
- `V3__create_lgpd_consent.sql`: tabela `lgpd_consent` com indice composto.
- `V4__add_documento_security_fields.sql`: colunas `nome_arquivo_original`, `object_key`, `tamanho_bytes` na tabela `documento`; coluna `cpf_hash` (unique) na tabela `funcionario`.

---

### 18.4 Arquivos existentes modificados

#### `model/Documento.java`
- Adicionado: `nomeArquivoOriginal` — nome real do arquivo cifrado via `@Convert(converter = EncryptedStringConverter.class)`.
- Adicionado: `objectKey` — UUID v4 opaco como chave no MinIO (novos uploads).
- Adicionado: `tamanhoBytes` — tamanho em bytes do arquivo.
- Campos legados `nomeArquivo` e `arquivoPath` mantidos para retrocompatibilidade.

#### `model/Funcionario.java`
- `cpf` — passou a ser cifrado com AES-256-GCM via `@Convert`. Constraint `unique` removida deste campo.
- `cpfHash` — novo campo `@Column(unique=true)` com HMAC-SHA256 deterministico do CPF normalizado. Permite queries de existencia sem expor o CPF real.

#### `repository/FuncionarioRepository.java`
- Adicionado: `existsByCpfHashAndContratada(String cpfHash, Contratada contratada)`.
- Metodos legados `existsByCpf` e `existsByCpfAndContratada` marcados como `@Deprecated`.

#### `service/FuncionarioService.java`
- `criar()`: agora computa `cpfHash = fieldEncryptionService.hash(cpfNormalizado)` e usa `existsByCpfHashAndContratada` para verificar unicidade.
- `@Auditavel` adicionado em `criar()` e `deletarFuncionario()`.

#### `service/DocumentoService.java`
- Adicionados endpoints Zero-Copy: `solicitarUpload()`, `confirmarUpload()`, `solicitarDownload()`.
- `solicitarUpload()` valida LGPD, gera UUID como objectKey, cifra nomeArquivoOriginal, gera presigned PUT URL (TTL 10 min).
- `solicitarDownload()` valida permissao por perfil, gera presigned GET URL (TTL 15 min). Bytes do documento nao passam pelo backend.
- Upload e download legados mantidos para retrocompatibilidade (`uploadDocumento`, `download`).
- `@Auditavel` adicionado em todos os metodos criticos.

#### `controller/DocumentoController.java`
- Adicionados 3 novos endpoints:
  - `POST /api/documentos/solicitar-upload`
  - `POST /api/documentos/{id}/confirmar-upload`
  - `GET /api/documentos/{id}/solicitar-download`
- Endpoints legados `/upload` e `/{id}/download` mantidos.

#### `service/AuthService.java`
- `@Auditavel` adicionado em `login()`, `resetPassword()` e `changePasswordFirstAccess()`.
- Login falho gera evento `status=FAILURE` automaticamente pelo `AuditAspect`.

#### `service/ContratanteService.java`
- `@Auditavel` adicionado em `criar()`, `atualizar()` e `removerPorNome()`.

#### `service/ContratadaService.java`
- `@Auditavel` adicionado em `criar()`, `atualizar()` e `deletarContratada()`.

#### `service/ContratanteUsuarioService.java`
- `@Auditavel` adicionado em `criarUsuarioParaContratanteLogada()`.

#### `service/MinioService.java`
- Adicionados: `gerarPresignedPutUrl(objectKey)` e `gerarPresignedGetUrl(objectKey)` com TTLs de 10 e 15 minutos respectivamente.
- Adicionado: `configurarCriptografiaBucket()` — tenta habilitar SSE-S3 usando API correta do SDK 8.6.0 (`SseConfiguration` com `SseConfigurationRule`). Falha silenciosa com log de aviso (requer KES/KMS em producao).

#### `util/BucketInitializer.java`
- Chama `minioService.configurarCriptografiaBucket()` apos criar/verificar o bucket.

#### `config/SecurityConfig.java`
- Rotas `/api/lgpd/consentimento` e `/api/lgpd/consentimento/valido` explicitamente marcadas como `authenticated()`.

#### `application.properties`
Novas propriedades adicionadas:
```properties
app.field-encryption-key=${APP_FIELD_ENCRYPTION_KEY:CHAVE_PLACEHOLDER_SUBSTITUA_EM_PRODUCAO_32B}
app.lgpd.versao-termo-atual=${APP_LGPD_VERSAO_TERMO_ATUAL:v1.0}
app.lgpd.termo-hash-atual=${APP_LGPD_TERMO_HASH_ATUAL:HASH_PLACEHOLDER_SUBSTITUA}
```

#### `docker-compose.yml`
Novas variaveis adicionadas ao servico `backend`:
```yaml
APP_FIELD_ENCRYPTION_KEY: ${APP_FIELD_ENCRYPTION_KEY}
APP_LGPD_VERSAO_TERMO_ATUAL: ${APP_LGPD_VERSAO_TERMO_ATUAL:-v1.0}
APP_LGPD_TERMO_HASH_ATUAL: ${APP_LGPD_TERMO_HASH_ATUAL}
```

---

### 18.5 Variaveis de ambiente obrigatorias para producao

| Variavel | Como gerar | Importancia |
|---|---|---|
| `APP_FIELD_ENCRYPTION_KEY` | `openssl rand -base64 32` | Critica — chave AES-256 para criptografia de campos |
| `APP_LGPD_VERSAO_TERMO_ATUAL` | Definir manualmente (ex: `v1.0`) | Controla qual versao do termo exigir |
| `APP_LGPD_TERMO_HASH_ATUAL` | `echo -n "TEXTO" | shasum -a 256` (macOS) | Valida integridade do termo aceito |

Atencao:
- `APP_FIELD_ENCRYPTION_KEY` deve ser gerada uma unica vez e nunca alterada enquanto houver dados cifrados no banco. Backup obrigatorio da chave em local separado do servidor.
- Em producao, usar secret manager (HashiCorp Vault, AWS Secrets Manager) no lugar de variavel de ambiente plana.

---

### 18.6 Tabelas criadas no PostgreSQL

Criadas automaticamente via `ddl-auto=update` ao subir o backend:

| Tabela | Finalidade |
|---|---|
| `audit_log` | Registro de todas as transacoes criticas com JSONB para contexto adicional |
| `lgpd_consent` | Historico de aceites LGPD com versao e hash do termo |

Colunas adicionadas em tabelas existentes:

| Tabela | Coluna | Descricao |
|---|---|---|
| `documento` | `nome_arquivo_original` | Nome real do arquivo cifrado AES-256-GCM |
| `documento` | `object_key` | UUID v4 opaco como chave no MinIO |
| `documento` | `tamanho_bytes` | Tamanho do arquivo em bytes |
| `funcionario` | `cpf_hash` | HMAC-SHA256 do CPF para busca/unicidade |

---

### 18.7 Fluxo de auditoria (como funciona em execucao)

```
Requisicao HTTP
      |
      v
Controller → Service (metodo @Auditavel)
                  |
            AuditAspect (@Around)
                  |
         ┌────────┴────────┐
         |                 |
    Executa metodo    Captura contexto
         |            (user, IP, UA)
         |                 |
    ┌────┴────┐            |
  Sucesso   Falha          |
    |         |            |
    └────┬────┘            |
         v                 |
   AuditService           /
   .registrar() ◄────────/
         |
   [Thread auditExecutor — assincrono]
         |
   audit_log (PostgreSQL)
```

---

### 18.8 Pendencias e proximos passos pos-Fase-2

Backend implementado e validado. Pendente para completar a Fase 2:

- [x] **Frontend** `UploadDocumento.tsx`: implementar fluxo LGPD. A tela agora consulta `GET /api/lgpd/termo-atual`, exibe o texto vigente, exige checkbox para documentos de funcionario e registra o aceite via `POST /api/lgpd/consentimento` antes de liberar o envio.
- [x] **Frontend** `UploadDocumento.tsx`: migrado para arquitetura Zero-Copy (chama `POST /api/documentos/solicitar-upload`, faz PUT direto ao MinIO, chama `POST /api/documentos/{id}/confirmar-upload`).
- [x] **Frontend** `Documentos.tsx`: migrado para `GET /api/documentos/{id}/solicitar-download` com redirecionamento para URL assinada.
- [x] **Frontend** admin: criar tela de visualizacao do `audit_log` consumindo `GET /api/admin/audit-log`, com filtros por email, acao, entidade, status e periodo, alem de paginacao e modal de detalhes.
- [ ] **Producao**: provisionar chave `APP_FIELD_ENCRYPTION_KEY` via secret manager.
- [ ] **Producao**: executar scripts `V2`, `V3` e `V4` para indices e constraints adicionais.
- [ ] **Producao**: configurar KES + HashiCorp Vault para SSE-KMS (envelope encryption no MinIO).

Atualizacao dev/hml (concluida):
- backend gera presigned URLs com host externo configuravel via `MINIO_PUBLIC_URL`, evitando retorno de host interno (`minio:9000`) para o navegador;
- upload e download operam em Zero-Copy no frontend, sem trafego de bytes pelo backend Java nos fluxos atualizados.

### 18.9 Consolidacao da implementacao de auditoria e LGPD

Implementacao complementar concluida nesta etapa:

- Backend:
  - `GET /api/lgpd/termo-atual` adicionado para o frontend obter `versaoTermo`, `hashTermo`, `textoTermo`, status de aceite e timestamp do consentimento vigente.
  - o hash retornado e validado no aceite passou a ser o hash efetivo do texto vigente exibido ao usuario, evitando divergencia entre texto mostrado e hash persistido.
  - `DocumentoService.java` atualizado para exigir consentimento LGPD no upload de documentos pessoais (`tipoReferencia = FUNCIONARIO`) tanto no fluxo legado quanto no fluxo Zero-Copy.
  - `MinioService.java` atualizado para gerar presigned URLs usando cliente dedicado com endpoint publico configuravel (`MINIO_PUBLIC_URL`), garantindo assinatura valida e funcionamento no navegador em dev/hml.
  - `AdminAuditController.java` ajustado para aceitar filtro de multiplas acoes separadas por virgula e sanitizar `sortBy`.
  - `AuditLogResponse.java` atualizado para retornar `detalhesJson` como objeto JSON ao inves de string bruta.
  - `docker-compose.yml`, `.env.example` e `application.properties` atualizados para suportar o texto vigente do termo LGPD via `APP_LGPD_TERMO_TEXTO_ATUAL`.

- Frontend:
  - criada a tela `frontend/src/pages/admin/AdminAuditLogs.tsx` para consulta do `audit_log` pelo perfil `ADMIN`;
  - adicionada rota protegida `/admin/audit-log`;
  - adicionada entrada `Logs de Auditoria` no menu lateral do admin;
  - `UploadDocumento.tsx` atualizado para:
    - consultar o termo vigente;
    - exibir o texto de consentimento;
    - exigir checkbox e registro do aceite antes do envio de documentos de funcionario;
    - bloquear o botao de envio enquanto o consentimento obrigatorio nao estiver valido;
    - executar upload Zero-Copy (`solicitar-upload` -> PUT direto no MinIO -> `confirmar-upload`).
  - `Documentos.tsx` atualizado para download Zero-Copy via `GET /api/documentos/{id}/solicitar-download` e acesso direto a URL assinada.

Validacao funcional executada:

- build do backend com `mvn -DskipTests compile`;
- build do frontend com `npm run build`;
- smoke test de `GET /api/lgpd/termo-atual`;
- smoke test de `POST /api/lgpd/consentimento`;
- smoke test de `GET /api/admin/audit-log`, incluindo confirmacao de evento `LGPD_CONSENTIMENTO_REGISTRADO` no `audit_log`.
