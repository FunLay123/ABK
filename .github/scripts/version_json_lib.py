#!/usr/bin/env python3
"""Shared helpers for ABK app update metadata (version.json)."""

from __future__ import annotations

import json
import re
from pathlib import Path
from typing import Any

DEFAULT_GITHUB_REPO = "xingguangcuican6666/ABK"
VERSION_JSON_FILENAME = "version.json"
STABILITY_STABLE = "stable"
STABILITY_UNSTABLE = "unstable"
PACKAGE_LINE_NORMAL = "normal"
PACKAGE_LINE_DEV = "dev"
DEFAULT_NORMAL_BUILD_FILE = Path("app/build.gradle.kts")
DEFAULT_DEV_BUILD_FILE = Path("app_dev/build.gradle.kts")
STABLE_RELEASE_ASSETS = {
    PACKAGE_LINE_NORMAL: "abk-app-abk-apks.zip",
    PACKAGE_LINE_DEV: "abk-app-dev-abk-apks.zip",
}


def stable_download_url(github_repo: str, release_tag: str, channel: str) -> str:
    if channel not in STABLE_RELEASE_ASSETS:
        raise ValueError(f"Unsupported channel: {channel}")
    repo = github_repo.strip().strip("/")
    if not repo or "/" not in repo:
        raise ValueError(f"Invalid GitHub repo slug: {github_repo!r}")
    tag = release_tag.strip()
    if not tag:
        raise ValueError("Release tag is required")
    asset_name = STABLE_RELEASE_ASSETS[channel]
    return f"https://github.com/{repo}/releases/download/{tag}/{asset_name}"


def unstable_download_urls(github_repo: str = DEFAULT_GITHUB_REPO) -> dict[str, str]:
    repo = github_repo.strip().strip("/")
    if not repo or "/" not in repo:
        raise ValueError(f"Invalid GitHub repo slug: {github_repo!r}")
    return {
        "normal": f"https://nightly.link/{repo}/workflows/build-abk-app/dev/abk-apks.zip",
        "dev": f"https://nightly.link/{repo}/workflows/build-abk-app-dev/dev/abk-apks.zip",
    }


def read_app_version(build_file: Path) -> tuple[int, str]:
    text = build_file.read_text(encoding="utf-8")
    version_code_patterns = [
        r"^\s*val\s+appVersionCode\s*=\s*(\d+)\s*$",
        r"^\s*versionCode\s*=\s*(\d+)\s*$",
    ]
    version_name_patterns = [
        r'^\s*val\s+appVersionName\s*=\s*"([^"]+)"\s*$',
        r'^\s*versionName\s*=\s*"([^"]+)"\s*$',
    ]

    version_code = None
    version_name = None
    for pattern in version_code_patterns:
        match = re.search(pattern, text, re.MULTILINE)
        if match:
            version_code = int(match.group(1))
            break
    for pattern in version_name_patterns:
        match = re.search(pattern, text, re.MULTILINE)
        if match:
            version_name = match.group(1)
            break

    if version_code is None or not version_name:
        raise ValueError(f"Unable to parse app version from {build_file}")
    return version_code, version_name


def ensure_object(value: Any) -> dict[str, Any]:
    return value if isinstance(value, dict) else {}


def normalize_version_json_document(raw: Any) -> dict[str, Any]:
    if not isinstance(raw, dict):
        raise ValueError("version.json root must be an object")
    data = dict(raw)
    stable = ensure_object(data.get("stable"))
    unstable = ensure_object(data.get("unstable"))
    stable["normal"] = ensure_object(stable.get("normal"))
    stable["dev"] = ensure_object(stable.get("dev"))
    unstable["normal"] = ensure_object(unstable.get("normal"))
    unstable["dev"] = ensure_object(unstable.get("dev"))
    data["stable"] = stable
    data["unstable"] = unstable
    return data


def apply_unstable_download_urls(
    data: dict[str, Any],
    *,
    github_repo: str,
) -> dict[str, Any]:
    document = normalize_version_json_document(data)
    urls = unstable_download_urls(github_repo)
    for channel in (PACKAGE_LINE_NORMAL, PACKAGE_LINE_DEV):
        channel_obj = dict(document["unstable"][channel])
        if not channel_obj:
            continue
        channel_obj["downloadUrl"] = urls[channel]
        document["unstable"][channel] = channel_obj
    return document


def update_stable_channel(
    data: dict[str, Any],
    *,
    channel: str,
    version_name: str,
    version_code: int,
    download_url: str,
) -> dict[str, Any]:
    if channel not in {PACKAGE_LINE_NORMAL, PACKAGE_LINE_DEV}:
        raise ValueError(f"Unsupported channel: {channel}")

    document = normalize_version_json_document(data)
    document["stable"][channel] = {
        "versionName": version_name,
        "versionCode": version_code,
        "downloadUrl": download_url,
    }
    return document


def update_unstable_channel(
    data: dict[str, Any],
    *,
    channel: str,
    version_name: str,
    version_code: int,
    download_url: str,
    published_at: str,
    build_timestamp_epoch_millis: int,
    workflow_name: str,
    commit_sha: str,
    run_id: int,
) -> dict[str, Any]:
    if channel not in {"normal", "dev"}:
        raise ValueError(f"Unsupported channel: {channel}")

    document = normalize_version_json_document(data)
    document["unstable"][channel] = {
        "versionName": version_name,
        "versionCode": version_code,
        "downloadUrl": download_url,
        "publishedAt": published_at,
        "buildTimestampEpochMillis": build_timestamp_epoch_millis,
        "sourceWorkflow": workflow_name,
        "commitSha": commit_sha,
        "runId": run_id,
    }
    return document


def render_version_json(data: dict[str, Any]) -> str:
    return json.dumps(data, ensure_ascii=False, indent=2) + "\n"


def load_version_json_text(text: str) -> dict[str, Any]:
    if not text.strip():
        return normalize_version_json_document({})
    return normalize_version_json_document(json.loads(text))


def load_version_json_file(path: Path) -> dict[str, Any]:
    if not path.exists():
        return normalize_version_json_document({})
    return load_version_json_text(path.read_text(encoding="utf-8"))


def build_updated_unstable_document(
    base: dict[str, Any],
    *,
    channel: str,
    build_file: Path,
    github_repo: str,
    workflow_name: str,
    run_id: int,
    commit_sha: str,
    published_at: str,
    build_timestamp_epoch_millis: int,
) -> dict[str, Any]:
    version_code, version_name = read_app_version(build_file)
    download_urls = unstable_download_urls(github_repo)
    return update_unstable_channel(
        base,
        channel=channel,
        version_name=version_name,
        version_code=version_code,
        download_url=download_urls[channel],
        published_at=published_at,
        build_timestamp_epoch_millis=build_timestamp_epoch_millis,
        workflow_name=workflow_name,
        commit_sha=commit_sha,
        run_id=run_id,
    )


def build_updated_stable_release_document(
    base: dict[str, Any],
    *,
    github_repo: str,
    release_tag: str,
    normal_build_file: Path = DEFAULT_NORMAL_BUILD_FILE,
    dev_build_file: Path = DEFAULT_DEV_BUILD_FILE,
) -> dict[str, Any]:
    document = normalize_version_json_document(base)
    for channel, build_file in (
        (PACKAGE_LINE_NORMAL, normal_build_file),
        (PACKAGE_LINE_DEV, dev_build_file),
    ):
        version_code, version_name = read_app_version(build_file)
        document = update_stable_channel(
            document,
            channel=channel,
            version_name=version_name,
            version_code=version_code,
            download_url=stable_download_url(github_repo, release_tag, channel),
        )
    return apply_unstable_download_urls(document, github_repo=github_repo)


def build_updated_document(
    base: dict[str, Any],
    *,
    channel: str,
    build_file: Path,
    github_repo: str,
    workflow_name: str,
    run_id: int,
    commit_sha: str,
    published_at: str,
    build_timestamp_epoch_millis: int,
) -> dict[str, Any]:
    return build_updated_unstable_document(
        base,
        channel=channel,
        build_file=build_file,
        github_repo=github_repo,
        workflow_name=workflow_name,
        run_id=run_id,
        commit_sha=commit_sha,
        published_at=published_at,
        build_timestamp_epoch_millis=build_timestamp_epoch_millis,
    )


def gist_raw_url(owner: str, gist_id: str, filename: str = VERSION_JSON_FILENAME) -> str:
    owner = owner.strip().strip("/")
    gist_id = gist_id.strip()
    if not owner or not gist_id:
        raise ValueError("Gist owner and gist id are required")
    return f"https://gist.githubusercontent.com/{owner}/{gist_id}/raw/{filename}"
