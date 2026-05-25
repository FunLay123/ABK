# AGENTS.md

## Cursor Cloud specific instructions

### Project overview

ABK (AnyBase Kernel) is a multi-component project:

1. **Android App** (`app/`) — Kotlin/Jetpack Compose app for managing GKI kernel builds. Requires Android SDK (compileSdk 37, NDK 28.2) and Gradle 9.3.1.
2. **Web Dashboard** (`web/`) — Static site (vanilla JS + SCSS + Webpack 5) for GKI kernel version tracking. Uses npm.
3. **GitHub Actions Workflows** (`.github/workflows/`) — CI/CD for kernel compilation.
4. **Python Scripts** (`scripts/`) — Data-fetching scripts for kernel version JSON in `data/`.

### Running unit tests

Android unit tests run inside Docker via `docker compose run --rm --build unit-tests`. There is no `gradlew` wrapper in the repo; the Docker image (`Dockerfile.test`) provides Gradle 9.3.1 + JDK 17 + Android SDK/NDK. Docker must be running before tests. Start dockerd with `sudo dockerd &>/tmp/dockerd.log &` if not already running.

**Known pre-existing failure:** `KernelSupportTest.normalizeOnePlusConfigUsesOnePlusDefaultsAndDisablesMtkProxy` fails (37/38 pass). This is not an environment issue.

### Web dashboard

- Install deps: `cd web && npm install`
- Build: `npm run build` (outputs to `web/dist/`)
- Dev watch: `npm run dev`
- Serve locally: `npx serve -l 8080 .` from the `web/` directory
- The dashboard loads data from `data/` relative to the web root. To see real data locally, symlink `ln -s /workspace/data /workspace/web/data` before serving.

### Lint

No dedicated lint tooling (ESLint, ktlint, detekt) is configured in this project. Android Lint runs as part of the Gradle build inside Docker.

### Docker in Cloud Agent VMs

Docker requires nested container setup. After installation, ensure:
- `fuse-overlayfs` storage driver is configured in `/etc/docker/daemon.json`
- `iptables-legacy` is set via `update-alternatives`
- Docker socket permissions: `sudo chmod 666 /var/run/docker.sock`
