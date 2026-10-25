# Deploy do Portal Partners

Guia para colocar a aplicacao numa URL publica usando **apenas a conta do
Render**, sem storage externo e sem custo.

Tempo estimado: 30 a 45 minutos, quase tudo esperando build.

> **Este e um ambiente de demonstracao.** Servicos gratuitos hibernam, o banco
> gratuito expira em poucas semanas levando os dados junto, e os documentos
> enviados somem a cada redeploy (o MinIO roda sem disco). Serve para validar
> o fluxo e apresentar o sistema — nao para operar com documento real de
> funcionario. A secao final explica como promover para producao.

---

## Arquitetura

| Componente | Onde roda | Plano |
|---|---|---|
| Frontend (React + Vite) | Render Static Site | Gratuito, sem hibernacao |
| API (Spring Boot) | Render Web Service (Docker) | Gratuito, hiberna |
| Banco (PostgreSQL 16) | Render PostgreSQL | Gratuito, expira |
| Arquivos | MinIO no Render (Docker) | Gratuito, sem disco |

O MinIO e o mesmo do seu `docker-compose`, agora hospedado ao lado da API.
Nenhuma linha de `MinioService` muda.

---

## Pre-requisitos

- Conta no [Render](https://render.com) conectada ao GitHub — **so isso**
- SMTP e **opcional**: sem ele tudo funciona, menos "esqueci minha senha"

---

## Passo 1 — Gerar os segredos

```bash
# Chave de criptografia dos campos sensiveis (CPF, nome de arquivo)
openssl rand -base64 32

# Credenciais do MinIO (usuario e senha do storage)
openssl rand -base64 12   # sera o MINIO_ROOT_USER
openssl rand -base64 24   # sera o MINIO_ROOT_PASSWORD

# Senhas iniciais dos tres perfis (uma para cada)
openssl rand -base64 18

# Hash do termo LGPD em vigor
printf '%s' "TEXTO_INTEGRAL_DO_TERMO" | sha256sum
```

> **`APP_FIELD_ENCRYPTION_KEY` nao pode ser trocada depois.** Os dados cifrados
> no banco so podem ser lidos com ela. Guarde num gerenciador de senhas.
>
> Use senhas novas, nao as do seu `.env` local.

---

## Passo 2 — Criar os servicos

1. Render > **New** > **Blueprint**
2. Selecione o repositorio `portal-partners`
3. O Render le o `render.yaml` e propoe quatro recursos: banco, MinIO, API e
   frontend
4. Ele vai pedir os segredos. Preencha o que ja da para preencher:

| Variavel | Valor |
|---|---|
| `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD` (no servico **minio**) | as credenciais do Passo 1 |
| `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD` / `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` (na **API**) | os **mesmos** valores |
| `APP_FIELD_ENCRYPTION_KEY` | a chave do Passo 1 |
| `APP_SEED_*_PASSWORD` | as tres senhas do Passo 1 |
| `APP_LGPD_TERMO_HASH_ATUAL` | o hash do Passo 1 |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | deixe em branco se nao for usar e-mail |
| `MINIO_URL`, `MINIO_PUBLIC_URL`, `APP_FRONTEND_URL`, `APP_CORS_ALLOWED_ORIGINS`, `VITE_API_BASE_URL` | em branco — Passo 4 |

5. **Apply**. O primeiro build leva de 5 a 15 minutos.

> Na primeira subida a API vai reclamar de storage no log. E esperado: ela
> ainda nao sabe o endereco do MinIO. O `BucketInitializer` tenta seis vezes
> com espera crescente e, em modo permissivo, deixa a aplicacao subir mesmo
> assim. Resolve no Passo 4.

---

## Passo 3 — Anotar as URLs

O Render mostra o dominio de cada servico. Algo como:

- MinIO: `https://portal-partners-minio.onrender.com`
- API: `https://portal-partners-api.onrender.com`
- Frontend: `https://portal-partners-web.onrender.com`

Se o nome ja estava em uso, o Render acrescenta um sufixo. **Use as URLs reais
do painel.**

---

## Passo 4 — Ligar as pontas

**Na API** (Environment > Save, o redeploy e automatico):

```
MINIO_URL                = https://portal-partners-minio.onrender.com
MINIO_PUBLIC_URL         = https://portal-partners-minio.onrender.com
APP_FRONTEND_URL         = https://portal-partners-web.onrender.com
APP_CORS_ALLOWED_ORIGINS = https://portal-partners-web.onrender.com
APP_API_URL              = https://portal-partners-api.onrender.com
```

> `APP_API_URL` e a URL da **propria API**. O link de download de uso unico e
> absoluto e aponta para ela; se estiver errada, o botao de baixar documento
> gera um endereco que nao existe.

> As duas URLs do MinIO recebem o **mesmo** valor. A presigned URL e assinada
> para um host especifico: se o host da assinatura for diferente do que o
> browser acessa, o storage rejeita com `SignatureDoesNotMatch`.

**No frontend** (Environment > Save > **Clear build cache & deploy**):

```
VITE_API_BASE_URL = https://portal-partners-api.onrender.com
```

> O Vite grava essa URL **dentro do bundle durante o build**. Salvar a variavel
> sem disparar novo build faz o app continuar chamando `localhost`. Esta e a
> causa numero um de "salvei e nao mudou nada".

Depois do redeploy da API, confira no log: `Bucket criado: portal-partners`.

---

## Passo 5 — Criar os usuarios iniciais

O seed roda **uma unica vez** e so com o banco vazio.

1. Na API: `APP_SEED_ENABLED = true` > Save (redeploy)
2. Aguarde concluir
3. Volte para `APP_SEED_ENABLED = false` > Save (redeploy)

| Perfil | E-mail | Senha |
|---|---|---|
| Admin | `admin@admin.com` | `APP_SEED_ADMIN_PASSWORD` |
| Contratante | `contratante@contratante.com` | `APP_SEED_CONTRATANTE_PASSWORD` |
| Contratada | `contratada@contratada.com` | `APP_SEED_CONTRATADA_PASSWORD` |

> Se `APP_SEED_ENABLED=true` e qualquer uma das tres senhas estiver vazia, a
> aplicacao **nao sobe** (`IllegalStateException` no `DataSeeder`). Preencha as
> tres antes de ligar.
>
> Depois do seed, mudar essas variaveis nao muda nada: a senha ja esta
> hasheada no banco. A troca passa a ser pelo fluxo da aplicacao.

---

## Passo 6 — Validar

```bash
# 1. API viva (a primeira chamada pode demorar ~1min: hibernacao)
curl https://portal-partners-api.onrender.com/actuator/health
# esperado: {"status":"UP"}

# 2. Login
curl -X POST https://portal-partners-api.onrender.com/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@admin.com","senha":"SUA_SENHA_DE_SEED"}'
# esperado: JSON com token

# 3. CORS liberado para o frontend
curl -i -X OPTIONS https://portal-partners-api.onrender.com/api/auth/login \
  -H 'Origin: https://portal-partners-web.onrender.com' \
  -H 'Access-Control-Request-Method: POST' | grep -i access-control-allow-origin
```

No navegador, com o console aberto (F12):

- [ ] Login com os tres perfis
- [ ] F5 dentro de uma rota interna — deve permanecer na pagina, sem 404
- [ ] Upload de documento como Contratada e download como Contratante
- [ ] Nenhum erro de CORS nem chamada a `localhost` no console

---

## Operacao

**Deploy**: todo push na `main` publica automaticamente. O workflow
`.github/workflows/ci.yml` compila antes — se o CI falhar, o codigo esta
quebrado mesmo que o Render tente publicar.

**Rollback**: Render > servico > Events > *Rollback*.

**Logs**: Render > servico > Logs.

---

## Promover para producao

Nesta ordem, conforme a necessidade:

1. **Banco**: `plan: free` -> `basic-256mb`. **Faca isso antes do plano
   gratuito expirar**, senao o banco e removido com os dados.
2. **API**: `plan: free` -> `starter`. Acaba a hibernacao e o cold start.
3. **Arquivos** — duas saidas:
   - Manter MinIO: plano pago + bloco `disk` montado em `/data` (disco exige
     plano pago). Os arquivos passam a sobreviver a redeploy.
   - Trocar por Cloudflare R2 ou AWS S3: mais robusto e barato em escala.
     Mudam apenas `MINIO_URL`, `MINIO_PUBLIC_URL`, as chaves e `MINIO_REGION`
     (`auto` no R2). **Nenhuma alteracao de codigo** — o cliente ja fala S3.
     Nesse caso e obrigatorio configurar a politica de CORS do bucket
     permitindo a origem do frontend, senao o upload falha no navegador.
4. **`MINIO_BUCKET_ENCRYPTION_REQUIRED = true`**: a API passa a se recusar a
   subir sem storage com criptografia validada.
5. **SMTP real**, para recuperacao de senha.
6. **Trocar os usuarios do seed.** `admin@admin.com` com senha de seed nao
   pode existir num ambiente com dado real.

---

## Dividas tecnicas conhecidas

1. ~~**Schema gerenciado por `ddl-auto=update`.**~~ **RESOLVIDO.** Flyway esta no
   `pom.xml`, `V1__baseline.sql` versiona o schema e `SPRING_JPA_DDL_AUTO=validate`
   impede o Hibernate de alterar tabelas sozinho.
2. ~~**Sem testes automatizados.**~~ **RESOLVIDO.** `backend/src/test` com
   Testcontainers (Postgres + MinIO) cobre contexto, auth, documentos
   (isolamento multi-tenant), LGPD, health e scrubbing do Sentry. O CI roda
   `mvn verify`.
3. ~~**`tailwindcss` e `daisyui` dependencias mortas.**~~ **RESOLVIDO.** Removidas.
4. ~~**Bundle unico de 608 kB.**~~ **RESOLVIDO.** Code splitting por rota
   (React.lazy + Suspense): bundle inicial ~352 kB.
5. ~~**Sem observabilidade.**~~ **RESOLVIDO.** Sentry (free) no front e back,
   com sampling conservador e scrubbing de PII; health de DB e storage no Actuator.

---

# Guia passo a passo — Cloudflare R2 + Pages, Gmail e Sentry (custo zero)

Esta secao prepara o ambiente "liso, sem pendencias": storage que **nao hiberna**
(R2), frontend com dominio proprio (Cloudflare Pages), e-mail de recuperacao
funcionando (Gmail) e observabilidade (Sentry). Tudo em planos gratuitos.

> **Nunca** commite segredos (chaves R2, app password do Gmail, DSN do Sentry).
> Todos entram como variaveis de ambiente no painel do provedor.

## A) Cloudflare R2 (storage S3-compativel, nao hiberna)

1. Cloudflare Dashboard > **R2** > **Create bucket**: nome `portal-partners`.
2. **R2 > Manage R2 API Tokens > Create API token**: permissao *Object Read & Write*,
   escopo no bucket criado. Anote **Access Key ID**, **Secret Access Key** e o
   **endpoint S3** (`https://<ACCOUNT_ID>.r2.cloudflarestorage.com`).
3. No servico **API** do Render, defina:
   ```
   MINIO_URL                       = https://<ACCOUNT_ID>.r2.cloudflarestorage.com
   MINIO_PUBLIC_URL                = https://<ACCOUNT_ID>.r2.cloudflarestorage.com
   MINIO_ACCESS_KEY                = <Access Key ID>
   MINIO_SECRET_KEY                = <Secret Access Key>
   MINIO_ROOT_USER                 = <Access Key ID>      # mesmos valores
   MINIO_ROOT_PASSWORD             = <Secret Access Key>
   MINIO_REGION                    = auto
   MINIO_BUCKET_NAME               = portal-partners
   MINIO_BUCKET_ENCRYPTION_REQUIRED= false
   ```
   > R2 cifra em repouso por padrao e **nao** aceita `setBucketEncryption` via API
   > S3. Por isso `MINIO_BUCKET_ENCRYPTION_REQUIRED=false` — sem isso a API falha
   > ao subir. Nenhuma mudanca de codigo: o `MinioService` ja e tolerante.
4. **CORS do bucket** (R2 > bucket > Settings > CORS Policy) — obrigatorio, pois o
   upload/download usa presigned URL direto do navegador:
   ```json
   [
     {
       "AllowedOrigins": ["https://SEU-APP.pages.dev"],
       "AllowedMethods": ["GET", "PUT"],
       "AllowedHeaders": ["*"],
       "ExposeHeaders": ["ETag"],
       "MaxAgeSeconds": 3600
     }
   ]
   ```
   Troque pela origem real do frontend (dominio Pages ou customizado).

> Com o R2 no lugar do MinIO, o servico `portal-partners-minio` do `render.yaml`
> deixa de ser necessario — pode remove-lo depois de validar.

## B) Cloudflare Pages (frontend + dominio proprio)

1. Cloudflare Dashboard > **Workers & Pages** > **Create** > **Pages** >
   **Connect to Git** > selecione `portal-partners`.
2. Build settings:
   - **Framework preset**: None (ou Vite)
   - **Build command**: `npm ci && npm run build`
   - **Build output directory**: `dist`
   - **Root directory**: `frontend`
3. **Environment variables** (Production):
   ```
   VITE_API_BASE_URL       = https://portal-partners-api.onrender.com
   VITE_SENTRY_DSN         = <DSN do projeto frontend no Sentry>   (opcional)
   VITE_SENTRY_ENVIRONMENT = production
   ```
   > O Vite grava essas variaveis **no bundle durante o build**. Alterou? Faca um
   > novo deploy (Retry deployment), senao o app continua com o valor antigo.
4. **Deploy**. A URL inicial sera `https://<app>.pages.dev`.
5. **Dominio customizado** (quando tiver um dominio na Cloudflare):
   Pages > seu projeto > **Custom domains** > **Set up a custom domain** e aponte
   (ex: `portalpartners.seudominio.com.br`). O DNS e o SSL sao automaticos.
6. **SPA rewrite**: crie `frontend/public/_redirects` com `/*  /index.html  200`
   se perceber 404 ao dar F5 em rota interna (o Pages normalmente ja trata SPA).
7. **CORS**: volte ao Passo A.4 e ao `APP_CORS_ALLOWED_ORIGINS` da API, colocando
   a URL final do Pages (`https://<app>.pages.dev` ou o dominio customizado).

## C) Gmail SMTP (recuperacao de senha)

1. Conta Gmail (ex: a conta "partners portal") > **Gerenciar Conta Google** >
   **Seguranca** > ative a **verificacao em duas etapas (2FA)**.
2. Ainda em Seguranca > **Senhas de app** > gere uma senha de app (16 caracteres).
3. No servico **API** do Render:
   ```
   MAIL_HOST     = smtp.gmail.com
   MAIL_PORT     = 587
   MAIL_USERNAME = suaconta@gmail.com
   MAIL_PASSWORD = <app password de 16 caracteres, sem espacos>
   MAIL_FROM     = suaconta@gmail.com
   ```
   > Use a **senha de app**, nunca a senha normal da conta. O limite gratuito do
   > Gmail (~500 e-mails/dia) e mais que suficiente para demonstracoes.

## D) Sentry (observabilidade — plano Developer gratuito)

1. [sentry.io](https://sentry.io) > crie conta no plano **Developer (free)**.
   **Nao adicione cartao de credito e nao ative Pay-As-You-Go** — assim nunca ha
   cobranca; ao atingir 5.000 erros/mes os eventos sao apenas descartados.
2. Crie **dois projetos**: um `Spring Boot` (backend) e um `React` (frontend).
3. Copie o **DSN** de cada um:
   - Backend: `SENTRY_DSN` (no Render, servico API).
   - Frontend: `VITE_SENTRY_DSN` (no Cloudflare Pages — exige novo build).
   Deixe os DSNs vazios para desligar o Sentry (dev/local).

## E) Popular dados de demonstracao (opcional, para apresentar)

Os 10 PDFs ficticios (gerados pela plataforma **4dev** — nome, CPF e CNPJ
ficticios) ja vao versionados em `backend/src/main/resources/demo-docs/`.
Para popular a base **sem alterar usuarios**:

1. Garanta que ja existe pelo menos uma Contratada (via seed ou cadastro).
2. No servico API: `APP_DEMO_DOCS_ENABLED = true` > Save (redeploy).
3. Na subida, o `DemoDocumentosSeeder` cria o funcionario ficticio
   "Ariquemes Cunha" e 10 documentos (5 da empresa, 5 do funcionario) com status
   variados. E **idempotente** (nao duplica) e **nunca** cria/altera usuarios.
4. Opcional: volte para `false` depois de popular.

---

## Checklist de validacao final

Com a aplicacao publicada (Cloudflare Pages + Render + R2):

- [ ] `curl https://<api>.onrender.com/actuator/health/liveness` -> `{"status":"UP"}`
- [ ] Tela "Iniciando a aplicacao com seguranca..." aparece no primeiro acesso
      (cold start) e some quando a API responde
- [ ] Login com os tres perfis existentes (ADMIN, CONTRATANTE, CONTRATADA)
- [ ] F5 dentro de uma rota interna permanece na pagina (sem 404)
- [ ] Upload como CONTRATADA e download como CONTRATANTE funcionam via R2
      (sem precisar "despertar" storage)
- [ ] "Esqueci minha senha" envia e-mail real (Gmail) e o reset funciona
- [ ] Nenhum erro de CORS nem chamada a `localhost` no console (F12)
- [ ] Marca RSOFT visivel: logo no topo/menu, titulo da aba, rodape e selos de
      seguranca na tela de login
- [ ] (Se Sentry ligado) um erro proposital aparece no painel, sem PII (CPF,
      e-mail, token mascarados)
- [ ] (Se demo ligado) dashboards e tabelas populados com os 10 documentos
