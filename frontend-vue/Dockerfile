FROM oven/bun:1 AS base

# ═══════════════════════
#      Build stage
# ═══════════════════════
FROM base AS build
WORKDIR /usr/src/app

# Copy lockfile + package.json first (better layer caching)
COPY bun.lock package.json ./

# Install all dependencies (dev + prod)
RUN bun install --frozen-lockfile

# Copy the rest of the code
COPY . .

# Build production bundle (Vite)
ENV NODE_ENV=production
RUN bun run build

# ═══════════════════════
#      Runtime stage
# ═══════════════════════
FROM base AS release
WORKDIR /usr/src/app

# Copy only built assets + server script + package.json
COPY --from=build /usr/src/app/dist ./dist
COPY --from=build /usr/src/app/serve.ts ./serve.ts
COPY --from=build /usr/src/app/package.json ./package.json

# Run as non-root user provided by oven/bun image
USER bun

# Expose Bun static server port
EXPOSE 4173/tcp

# Run the Bun static server
ENTRYPOINT ["bun", "run", "serve.ts"]
