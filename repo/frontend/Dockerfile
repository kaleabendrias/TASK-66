FROM node:20-alpine AS build
WORKDIR /app
ARG VITE_API_BASE_URL=https://localhost:8443/api
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
COPY package.json ./
RUN npm install
COPY . .
RUN npm run build

FROM node:20-alpine
WORKDIR /app
RUN apk add --no-cache openssl \
    && npm install -g serve \
    && mkdir -p /app/certs \
    && openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
         -keyout /app/certs/frontend.key \
         -out /app/certs/frontend.crt \
         -subj "/C=US/ST=Local/L=Local/O=Demo/CN=frontend"
COPY --from=build /app/dist ./dist
EXPOSE 3443
# serve listens on HTTPS so the nginx upstream hop is encrypted as well.
# --ssl-cert / --ssl-key turn on TLS in the serve CLI (>=14.x).
CMD ["serve", "-s", "dist", "-l", "3443", "--ssl-cert", "/app/certs/frontend.crt", "--ssl-key", "/app/certs/frontend.key"]
