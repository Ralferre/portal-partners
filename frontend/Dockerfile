# ----------- DEV STAGE -----------
# Usado pelo docker-compose local (build.target: dev).
FROM node:20-alpine AS dev

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci --no-audit --prefer-offline

COPY . .

EXPOSE 5173
CMD [ "npm", "run", "dev", "--", "--host" ]

# ----------- BUILD STAGE -----------
FROM node:20-alpine AS builder

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci --no-audit --prefer-offline

COPY . .

# O Vite injeta VITE_* no bundle em tempo de BUILD, nao em runtime.
# A URL da API precisa ser conhecida aqui, por isso vem como build arg.
ARG VITE_API_BASE_URL
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
RUN test -n "$VITE_API_BASE_URL" || (echo "ERRO: VITE_API_BASE_URL nao informado no build" && exit 1)
RUN npm run build

# ----------- PROD STAGE -----------
FROM nginx:stable-alpine AS prod

COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD wget -q --spider http://localhost/ || exit 1

CMD [ "nginx", "-g", "daemon off;" ]
