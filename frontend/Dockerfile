# Build stage
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm install
ARG VITE_GOOGLE_CLIENT_ID
ENV VITE_GOOGLE_CLIENT_ID=$VITE_GOOGLE_CLIENT_ID
COPY . .
RUN npm run build

# Serve stage
FROM nginx:alpine
# Copy built assets from builder
COPY --from=builder /app/dist /usr/share/nginx/html
# Provide highly permissive single page application routing
RUN echo 'server { \
    listen 80; \
    location / { \
        root /usr/share/nginx/html; \
        try_files $uri $uri/ /index.html; \
    } \
    location /api/ { \
        proxy_pass http://api-gateway:8090/api/; \
        proxy_set_header Host $host; \
    } \
    location /ws/ { \
        proxy_pass http://api-gateway:8090/ws/; \
        proxy_http_version 1.1; \
        proxy_set_header Upgrade $http_upgrade; \
        proxy_set_header Connection "Upgrade"; \
        proxy_set_header Host $host; \
    } \
}' > /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
