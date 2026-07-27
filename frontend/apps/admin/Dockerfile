# ==================== 构建阶段 ====================
FROM node:22-alpine AS builder
WORKDIR /build

# 安装 pnpm
RUN corepack enable && corepack prepare pnpm@latest --activate

# 复制 workspace 配置和依赖
COPY pnpm-workspace.yaml package.json ./
COPY packages/tsconfig/package.json packages/tsconfig/
RUN pnpm install --frozen-lockfile

# 复制 admin 源码
COPY apps/admin/package.json apps/admin/
COPY apps/admin/vite.config.ts apps/admin/tsconfig.json apps/admin/index.html apps/admin/
COPY apps/admin/src/ apps/admin/src/
RUN pnpm --filter @onlinechat/admin build

# ==================== 运行阶段 ====================
FROM nginx:alpine
COPY apps/admin/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /build/apps/admin/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=10s --timeout=3s \
  CMD wget -qO- http://localhost/ || exit 1
