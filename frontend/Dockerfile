# ----------- DEV STAGE -----------
FROM node:21-alpine AS dev

WORKDIR /app

COPY package.json ./
# RUN npm ci
RUN yarn install

COPY . .

EXPOSE 5173
# CMD [ "npm", "run", "dev", "--", "--host" ]
CMD [ "yarn", "run", "dev" ]

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