# ----------- DEV STAGE -----------
FROM node:20-alpine AS dev

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci --no-audit --prefer-offline

COPY . .

EXPOSE 5173
# CMD [ "npm", "run", "dev", "--", "--host" ]
CMD [ "npm", "run", "dev", "--", "--host" ]

# ----------- PROD STAGE -----------
# FROM node:20-alpine AS builder

# WORKDIR /app

# COPY package.json package-lock.json ./
# RUN npm install

# COPY . .
# RUN npm run build

# FROM nginx:stable-alpine

# COPY --from=builder /app/dist /usr/share/nginx/html

# EXPOSE 80

# CMD [ "nginx", "-g", "daemon off;" ]