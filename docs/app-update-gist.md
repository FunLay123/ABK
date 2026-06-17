# App update metadata via GitHub Gist

App update metadata (`version.json`) can be published to a **GitHub Gist** instead of committing to `dev`. That keeps the branch history clean while the app still downloads the same JSON over HTTPS.

The gist holds the **full** `version.json`: both `stable` and `unstable` sections.

## Package lines vs modules

| Workflow | Module | `channel` | Gradle file |
|----------|--------|-----------|-------------|
| `Build ABK App` | `app/` | `normal` | `app/build.gradle.kts` |
| `Build ABK App Dev` | `app_dev/` | `dev` | `app_dev/build.gradle.kts` |

`channel` is the package line inside JSON (`stable.normal`, `unstable.dev`, etc.), not the word "stable/unstable".

## What lives where

| Location | Role |
|----------|------|
| `version.json` in git (`dev`) | Fallback / template when gist is not configured |
| Public gist `version.json` | Live metadata for app updates (CI writes here) |
| `app/build.gradle.kts` default URL | Unchanged until you switch the app default later |
| CI-built APKs | Get `ABK_APP_UPDATE_METADATA_URL` from the workflow (gist when configured) |

## Metadata update flows

### Unstable (nightly builds)

After each successful `Build ABK App` / `Build ABK App Dev` run:

- `build-abk-app.yml` updates `unstable.normal` from `app/build.gradle.kts`
- `build-abk-app-dev.yml` updates `unstable.dev` from `app_dev/build.gradle.kts`
- Unstable `downloadUrl` points at the **exact workflow run** that published the metadata (`nightly.link/.../actions/runs/<runId>/abk-apks.zip`), not just the latest build on `dev`

### Stable (GitHub Releases)

When a GitHub release with tag `app_v*` is published:

- `sync-app-stable-version-json.yml` updates **both** `stable.normal` and `stable.dev` in one gist write
- versions come from `app/build.gradle.kts` and `app_dev/build.gradle.kts`
- download URLs point at the release assets (`abk-app-abk-apks.zip`, `abk-app-dev-abk-apks.zip`)

## One-time setup (upstream)

### 1. Create the gist

1. Open [gist.github.com](https://gist.github.com).
2. Create a **public** gist.
3. Filename: `version.json`
4. Paste the current `version.json` from the repository (including both `stable` and `unstable`).
5. Save and copy the gist id from the URL:  
   `https://gist.github.com/<owner>/<gist-id>`

Raw URL (for testing in a browser):

```text
https://gist.githubusercontent.com/<owner>/<gist-id>/raw/version.json
```

### 2. Add repository settings

**Variable** (Settings → Secrets and variables → Actions → Variables):

| Name | Example | Purpose |
|------|---------|---------|
| `ABK_VERSION_GIST_ID` | `abc123def456...` | Target gist |

**Secret** (required for gist updates — `GITHUB_TOKEN` cannot write gists from Actions workflows):

| Name | Purpose |
|------|---------|
| `ABK_GIST_UPDATE_TOKEN` | PAT with `gist` scope |

Fine-grained PAT: only **Gists** → read/write.

### 3. Verify

After the next `Build ABK App` / `Build ABK App Dev` run on `dev`:

- No new `chore(app): update unstable version metadata` commits (when the variable is set).
- The gist `version.json` shows updated `unstable.normal` / `unstable.dev` after nightly builds.
- After publishing a release `app_v*`, `stable.normal` and `stable.dev` are updated in the gist.
- APKs from that run embed the gist raw URL via `ABK_APP_UPDATE_METADATA_URL`.

Until `ABK_VERSION_GIST_ID` is set, workflows keep the legacy **git commit** behaviour.

## Fork setup

1. Fork `ABK`.
2. Create **your own** public gist with `version.json` (copy upstream, adjust `downloadUrl` / nightly links if needed).
3. In the fork: set `ABK_VERSION_GIST_ID` and optionally `ABK_GIST_UPDATE_TOKEN`.
4. Workflows pass `ABK_GITHUB_REPO=${{ github.repository }}` so nightly.link URLs point at your fork.

No app changes are required for CI to publish fork metadata to your gist. Pointing the installed app at your gist is a separate settings feature.

## Scripts

| Script | Use |
|--------|-----|
| `.github/scripts/version_json_lib.py` | Shared merge/render helpers |
| `.github/scripts/update-version-json.py` | Update a local `version.json` file |
| `.github/scripts/publish-version-json-gist.py` | Fetch gist → merge stable or unstable metadata → PATCH gist |
| `.github/workflows/sync-app-stable-version-json.yml` | Update stable metadata when an `app_v*` release is published |

Manual gist publish (debug):

```bash
export ABK_VERSION_GIST_ID="your-gist-id"
export ABK_GIST_UPDATE_TOKEN="ghp_..."
export ABK_GITHUB_REPO="owner/ABK"

python .github/scripts/publish-version-json-gist.py \
  --channel normal \
  --workflow-name "Build ABK App" \
  --run-id 123456 \
  --commit-sha "$(git rev-parse HEAD)" \
  --published-at "$(date -u +'%Y-%m-%dT%H:%M:%SZ')" \
  --build-timestamp-epoch-millis "$(( $(date -u +%s) * 1000 ))"
```

## How CI chooses the metadata URL

During **Stamp app build metadata**:

- If `ABK_VERSION_GIST_ID` is set →  
  `https://gist.githubusercontent.com/<owner>/<gist-id>/raw/version.json`
- Otherwise →  
  `https://raw.githubusercontent.com/<repo>/<branch>/version.json`

`<owner>` is `github.repository_owner` (your account or org).

## Concurrent builds

`build-abk-app` and `build-abk-app-dev` may update the gist at the same time. The publish script re-fetches the gist on each retry and only replaces one unstable channel (`normal` or `dev`), preserving the other.
