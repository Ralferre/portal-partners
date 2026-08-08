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
```

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

1. **Schema gerenciado por `ddl-auto=update`.** O Hibernate altera o banco
   sozinho a cada deploy, sem revisao e sem rollback. O certo e adotar Flyway
   (que **nao esta** no `pom.xml`) com baseline do schema atual e trocar para
   `SPRING_JPA_DDL_AUTO=validate`. Enquanto isso, os arquivos de
   `db/migration/` seguem inertes — o `SchemaIndexInitializer` cobre apenas os
   indices que faltavam.
2. **Sem testes automatizados.** Nao existe `src/test`. O CI garante que
   compila, nao que funciona.
3. **`tailwindcss` e `daisyui` sao dependencias mortas** — nao ha
   `postcss.config`, `tailwind.config` nem plugin no `vite.config.js`.
4. **Bundle unico de 608 kB** (186 kB gzipado). Code splitting por rota
   melhoraria o primeiro carregamento.
5. **Sem observabilidade.** Nenhum rastreamento de erro em producao.
