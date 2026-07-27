# ==================== 构建阶段 ====================
FROM node:22-alpine AS builder
WORKDIR /build

# 安装 pnpm
RUN corepack enable && corepack prepare pnpm@latest --activate

# 复制 workspace 配置和依赖
COPY pnpm-workspace.yaml package.json ./
COPY packages/tsconfig/package.json packages/tsconfig/
RUN pnpm install --frozen-lockfile

# 复制 client 源码
COPY apps/client/package.json apps/client/
COPY apps/client/vite.config.ts apps/client/tsconfig.json apps/client/index.html apps/client/
COPY apps/client/src/ apps/client/src/
RUN pnpm --filter @onlinechat/client build

# ==================== 运行阶段 ====================
FROM nginx:alpine
COPY apps/client/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /build/apps/client/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=10s --timeout=3s \
  CMD wget -qO- http://localhost/ || exit 1
