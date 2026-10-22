ARG RUNTIME_VERSION
FROM 192.168.1.162:5000/library/node:${RUNTIME_VERSION}-alpine AS build
RUN apk upgrade --no-cache
ARG PACKAGE_NAME
ARG PROJECT_PATH
WORKDIR /workspace
# corepack downloads the pinned package manager from registry.npmjs.org by default,
# ignoring .npmrc; point it at the local registry too (self-hosted runner only).
ENV COREPACK_NPM_REGISTRY=http://192.168.1.163
RUN corepack enable
ENV npm_config_store_dir=/pnpm/store

# Install dependencies before copying the rest of the source so this layer
# (and the pnpm store cache mount below) stays valid across source-only
# commits, avoiding a full cold reinstall of every package on every build.
COPY .npmrc pnpm-lock.yaml pnpm-workspace.yaml package.json ./
COPY apps/banking_web/package.json ./apps/banking_web/package.json
COPY apps/card_processor_mock/package.json ./apps/card_processor_mock/package.json
# Registry (.npmrc) and fetch-timeout/fetch-retries (pnpm-workspace.yaml) come from
# the committed config files. --config.fetch-timeout as a CLI override crashes on
# pnpm 11.17.0: the string value reaches AbortSignal.timeout() unconverted, throwing
# "The 'delay' argument must be of type number" on the first retry without rejecting
# the pending install, so the build hangs instead of failing - keep these as file
# settings, not CLI --config flags. --config.minimum-release-age=0 disables pnpm 11's
# default supply-chain age check for this install only: that check fetches publish-time
# metadata for every lockfile entry, which is unreliable from inside a Docker build; the
# real policy gate already runs on the runner in check_node.sh (_job_check.yaml), a
# dependency of the image build.
RUN --mount=type=cache,id=pnpm-store,target=/pnpm/store \
    pnpm --config.minimum-release-age=0 \
         install --frozen-lockfile

COPY . .
RUN --mount=type=cache,id=pnpm-store,target=/pnpm/store \
    pnpm --filter "${PACKAGE_NAME}..." build \
    && pnpm deploy --filter "${PACKAGE_NAME}" --prod --legacy /opt/app

FROM 192.168.1.162:5000/library/node:${RUNTIME_VERSION}-alpine
RUN apk upgrade --no-cache
# npm ships its own vendored node_modules (undici, brace-expansion, ...) that lag behind
# upstream fixes and trip Trivy; the runtime only needs `node`, never npm/npx, so remove them.
RUN rm -rf /usr/local/lib/node_modules/npm /usr/local/bin/npm /usr/local/bin/npx
ARG APP_PORT
ARG START_COMMAND
ARG PROJECT_PATH
ARG BUILD_OUTPUT
WORKDIR /app
COPY --from=build /opt/app ./
# pnpm deploy --prod only assembles production node_modules + the package
# manifest; it never copies build artifacts, so the compiled dist/ output
# has to be copied explicitly from the build stage's workspace checkout.
COPY --from=build /workspace/${PROJECT_PATH}/${BUILD_OUTPUT} ./${BUILD_OUTPUT}
ENV START_COMMAND=${START_COMMAND}
RUN mkdir -p /data && chown node:node /data
USER node
EXPOSE ${APP_PORT}
CMD ["sh", "-c", "exec ${START_COMMAND}"]
