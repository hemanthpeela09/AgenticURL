FROM node:20-alpine AS builder
WORKDIR /app

ARG VITE_API_BASE=http://localhost:8081
ENV VITE_API_BASE=${VITE_API_BASE}

COPY ui/package.json ./
RUN npm install
COPY ui/ ./

RUN npm run build

FROM node:20-alpine
WORKDIR /app
RUN npm install -g serve
COPY --from=builder /app/dist ./dist

EXPOSE 4173
CMD ["serve", "-s", "dist", "-l", "4173"]
