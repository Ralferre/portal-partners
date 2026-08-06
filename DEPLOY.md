# Deploy do Portal Partners

Guia para colocar a aplicacao no ar no **Render**, com Postgres gerenciado e
storage S3-compativel. Ao final voce tem uma URL publica funcionando.

Tempo estimado na primeira vez: 40 a 60 minutos, quase tudo esperando build.

---

## Arquitetura em producao

| Componente | Onde roda | Observacao |
|---|---|---|
| Frontend (React + Vite) | Render Static Site | CDN e HTTPS automaticos, sem custo |
| API (Spring Boot) | Render Web Service (Docker) | Healthcheck em `/actuator/health` |
| Banco (PostgreSQL 16) | Render PostgreSQL | Backup gerenciado nos planos pagos |
| Arquivos (documentos) | Cloudflare R2 (ou AWS S3) | Substitui o MinIO local |

O MinIO **nao vai para producao**. O cliente Java que o projeto usa fala o
protocolo S3, entao apontar para o R2 e so questao de endpoint e credenciais —
nenhuma linha de `MinioService` muda.

Custo: o site estatico e gratuito; API e banco ficam na faixa de poucos dolares
por mes nos planos indicados no `render.yaml`. Confira os valores atuais na
pagina de precos do Render antes de confirmar.

---

## Pre-requisitos

- Conta no [Render](https://render.com) conectada ao GitHub
- Conta na [Cloudflare](https://dash.cloudflare.com) com R2 ativado
- Um servidor SMTP para os e-mails de recuperacao de senha
  (SendGrid, Resend, Amazon SES ou o SMTP do seu provedor)

---

## Passo 1 — Criar o bucket de arquivos (Cloudflare R2)

1. Cloudflare Dashboard > R2 > **Create bucket** > nome `portal-partners`
2. **Manage R2 API Tokens** > Create API token > permissao *Object Read & Write*
3. Guarde: **Access Key ID**, **Secret Access Key** e a **URL do endpoint**
   (`https://<ACCOUNT_ID>.r2.cloudflarestorage.com`)
4. No bucket, aba **Settings > CORS Policy**, cole o JSON abaixo trocando pela
   URL real do frontend (voce so vai saber ela no Passo 5 — pode voltar aqui):

```json
[
  {
    "AllowedOrigins": ["https://portal-partners-web.onrender.com"],
    "AllowedMethods": ["GET", "PUT", "HEAD"],
    "AllowedHeaders": ["*"],
    "ExposeHeaders": ["ETag"],
    "MaxAgeSeconds": 3600
  }
]
```

> **Este passo nao e opcional.** O upload de documento vai direto do browser
> para o storage via presigned URL (arquitetura Zero-Copy). Sem a politica de
> CORS no bucket, o upload falha no navegador mesmo com a API respondendo 200.

---

## Passo 2 — Gerar os segredos

```bash
# Chave de criptografia dos campos sensiveis (CPF, nome de arquivo)
openssl rand -base64 32

# Hash do termo LGPD em vigor
printf '%s' "TEXTO_INTEGRAL_DO_TERMO" | sha256sum

# Senhas iniciais dos tres perfis (uma para cada)
openssl rand -base64 18
```

> **`APP_FIELD_ENCRYPTION_KEY` nao pode ser perdida nem trocada.** Os dados
> cifrados no banco so podem ser lidos com ela. Trocar a chave com base
> populada torna CPF e nomes de arquivo irrecuperaveis. Guarde em gerenciador
> de senhas antes de seguir.

---

## Passo 3 — Criar os servicos no Render

1. Render > **New** > **Blueprint**
2. Selecione o repositorio `portal-partners`
3. O Render le o `render.yaml` e propoe: banco, API e frontend
4. Ele vai pedir as variaveis marcadas como segredo. Preencha:

| Variavel | Valor |
|---|---|
| `APP_FIELD_ENCRYPTION_KEY` | a chave do Passo 2 |
| `MINIO_URL` e `MINIO_PUBLIC_URL` | endpoint do R2 (o mesmo nos dois) |
| `MINIO_ROOT_USER` e `MINIO_ACCESS_KEY` | Access Key ID do R2 |
| `MINIO_ROOT_PASSWORD` e `MINIO_SECRET_KEY` | Secret Access Key do R2 |
| `MAIL_HOST`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` | dados do SMTP |
| `APP_LGPD_TERMO_HASH_ATUAL` | hash do Passo 2 |
| `APP_SEED_*_PASSWORD` | as tres senhas do Passo 2 |
| `APP_FRONTEND_URL`, `APP_CORS_ALLOWED_ORIGINS`, `VITE_API_BASE_URL` | deixe em branco por ora — Passo 5 |

5. **Apply**. O primeiro build leva de 5 a 15 minutos.

---

## Passo 4 — Anotar as URLs geradas

Terminado o deploy, o Render mostra o dominio de cada servico:

- API: `https://portal-partners-api.onrender.com`
- Frontend: `https://portal-partners-web.onrender.com`

Se o nome ja estava em uso, o Render acrescenta um sufixo. **Use as URLs reais
que aparecem no painel**, nao as do exemplo.

---

## Passo 5 — Ligar as duas pontas

As duas aplicacoes precisam saber o endereco uma da outra.

**Na API** (Environment > Save > redeploy automatico):

```
APP_FRONTEND_URL         = https://portal-partners-web.onrender.com
APP_CORS_ALLOWED_ORIGINS = https://portal-partners-web.onrender.com
```

**No frontend** (Environment > Save > **Clear build cache & deploy**):

```
VITE_API_BASE_URL = https://portal-partners-api.onrender.com
```

> O Vite grava essa URL **dentro do bundle durante o build**. Salvar a variavel
> nao basta: e obrigatorio disparar um novo build do frontend. Esta e a causa
> numero um de "salvei e continua chamando localhost".

Volte ao Passo 1.4 e coloque a URL real do frontend na politica de CORS do bucket.

---

## Passo 6 — Criar os usuarios iniciais

O seed roda **uma unica vez** e so quando o banco esta vazio.

1. Na API: `APP_SEED_ENABLED = true` > Save (redeploy)
2. Aguarde o deploy concluir
3. Volte `APP_SEED_ENABLED = false` > Save (redeploy)

Isso cria os tres perfis:

| Perfil | E-mail | Senha |
|---|---|---|
| Admin | `admin@admin.com` | `APP_SEED_ADMIN_PASSWORD` |
| Contratante | `contratante@contratante.com` | `APP_SEED_CONTRATANTE_PASSWORD` |
| Contratada | `contratada@contratada.com` | `APP_SEED_CONTRATADA_PASSWORD` |

> Esses e-mails sao valores de teste que vieram do `DataSeeder`. Antes de abrir
> o portal para usuarios reais, entre como admin e troque os e-mails e senhas —
> `admin@admin.com` com senha de seed e um alvo obvio.

---

## Passo 7 — Validar

```bash
# 1. API viva
curl https://portal-partners-api.onrender.com/actuator/health
# esperado: {"status":"UP"}

# 2. Login funcionando
curl -X POST https://portal-partners-api.onrender.com/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@admin.com","senha":"SUA_SENHA_DE_SEED"}'
# esperado: JSON com token

# 3. CORS liberado para o frontend
curl -i -X OPTIONS https://portal-partners-api.onrender.com/api/auth/login \
  -H 'Origin: https://portal-partners-web.onrender.com' \
  -H 'Access-Control-Request-Method: POST' | grep -i access-control-allow-origin
# esperado: o header com a URL do frontend
```

No navegador, com o console aberto (F12):

- [ ] Abrir a URL do frontend e fazer login com os tres perfis
- [ ] Recarregar (F5) dentro de uma rota interna — deve continuar na pagina, sem 404
- [ ] Subir um documento como Contratada e baixa-lo como Contratante
- [ ] Disparar "esqueci minha senha" e conferir a chegada do e-mail
- [ ] Nenhum erro de CORS ou chamada a `localhost` no console

---

## Operacao

**Deploy**: todo push na `main` publica automaticamente (`autoDeploy: true`).
O workflow `.github/workflows/ci.yml` compila backend e frontend antes disso —
se o CI falhar, o codigo esta quebrado, mesmo que o Render tente publicar.

**Rollback**: Render > servico > Events > *Rollback* na versao anterior.

**Logs**: Render > servico > Logs (streaming ao vivo).

**Backup do banco**: nos planos pagos o Render faz backup diario. Antes de
qualquer mudanca de schema, gere um manual:
`pg_dump "$DATABASE_URL" > backup-$(date +%F).sql`

---

## Dividas tecnicas conhecidas

Nada aqui bloqueia o deploy, mas todos devem ser resolvidos antes de escalar:

1. **Schema gerenciado por `ddl-auto=update`.** O Hibernate altera o banco
   sozinho a cada deploy, sem revisao e sem rollback. O certo e adotar Flyway
   (que **nao esta** no `pom.xml` hoje) com um baseline gerado do schema atual,
   e trocar para `SPRING_JPA_DDL_AUTO=validate`. Enquanto isso nao acontece, os
   arquivos em `db/migration/` permanecem inertes — o
   `SchemaIndexInitializer` cobre apenas os indices que faltavam.

2. **Sem testes automatizados.** Nao existe `src/test`. O CI so garante que
   compila, nao que funciona. Um teste de integracao do fluxo de login e do
   upload ja mudaria muito o risco de cada deploy.

3. **`tailwindcss` e `daisyui` sao dependencias mortas.** Estao no
   `package.json`, mas nao ha `postcss.config`, `tailwind.config` nem o plugin
   no `vite.config.js` — a UI e MUI. Remover reduz o tempo de build.

4. **Bundle unico de 608 kB** (186 kB gzipado). Aceitavel, mas code splitting
   por rota melhoraria o primeiro carregamento.

5. **Sem observabilidade.** Nenhum rastreamento de erro em producao. Um Sentry
   no frontend e na API mostraria a falha antes do usuario relatar.
