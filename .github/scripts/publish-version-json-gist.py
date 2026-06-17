#!/usr/bin/env python3
"""Fetch version.json from a GitHub Gist, merge metadata, and publish back."""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from version_json_lib import (
    DEFAULT_DEV_BUILD_FILE,
    DEFAULT_GITHUB_REPO,
    DEFAULT_NORMAL_BUILD_FILE,
    STABILITY_STABLE,
    STABILITY_UNSTABLE,
    VERSION_JSON_FILENAME,
    build_updated_stable_release_document,
    build_updated_unstable_document,
    load_version_json_text,
    render_version_json,
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Publish app update metadata to a GitHub Gist")
    parser.add_argument("--gist-id", default=os.environ.get("ABK_VERSION_GIST_ID", ""))
    parser.add_argument(
        "--stability",
        choices=[STABILITY_STABLE, STABILITY_UNSTABLE],
        default=STABILITY_UNSTABLE,
    )
    parser.add_argument("--channel", choices=["dev", "normal"])
    parser.add_argument("--workflow-name", default="")
    parser.add_argument("--run-id", type=int)
    parser.add_argument("--commit-sha", default="")
    parser.add_argument("--published-at", default="")
    parser.add_argument("--build-timestamp-epoch-millis", type=int)
    parser.add_argument("--release-tag", default="")
    parser.add_argument("--github-repo", default=os.environ.get("ABK_GITHUB_REPO", DEFAULT_GITHUB_REPO))
    parser.add_argument("--build-file", default=str(DEFAULT_NORMAL_BUILD_FILE))
    parser.add_argument("--normal-build-file", default=str(DEFAULT_NORMAL_BUILD_FILE))
    parser.add_argument("--dev-build-file", default=str(DEFAULT_DEV_BUILD_FILE))
    parser.add_argument("--max-attempts", type=int, default=5)
    return parser.parse_args()


def resolve_token() -> str:
    for key in ("ABK_GIST_UPDATE_TOKEN", "GITHUB_TOKEN", "GH_TOKEN"):
        value = os.environ.get(key, "").strip()
        if value:
            return value
    raise SystemExit(
        "No GitHub token found. Set ABK_GIST_UPDATE_TOKEN (recommended) or GITHUB_TOKEN."
    )


def github_request(
    method: str,
    url: str,
    token: str,
    payload: dict | None = None,
) -> dict:
    data = None
    headers = {
        "Accept": "application/vnd.github+json",
        "Authorization": f"Bearer {token}",
        "X-GitHub-Api-Version": "2022-11-28",
        "User-Agent": "abk-publish-version-json-gist",
    }
    if payload is not None:
        data = json.dumps(payload).encode("utf-8")
        headers["Content-Type"] = "application/json"
    request = urllib.request.Request(url, data=data, headers=headers, method=method)
    with urllib.request.urlopen(request) as response:
        body = response.read().decode("utf-8")
        if not body.strip():
            return {}
        return json.loads(body)


def fetch_gist_version_json(token: str, gist_id: str) -> str:
    gist = github_request("GET", f"https://api.github.com/gists/{gist_id}", token)
    files = gist.get("files") or {}
    file_entry = files.get(VERSION_JSON_FILENAME)
    if not isinstance(file_entry, dict):
        raise SystemExit(
            f"Gist {gist_id} does not contain a file named {VERSION_JSON_FILENAME!r}."
        )
    content = file_entry.get("content")
    if not isinstance(content, str):
        raise SystemExit(f"Gist file {VERSION_JSON_FILENAME!r} has no readable content.")
    return content


def publish_gist_version_json(token: str, gist_id: str, content: str) -> None:
    github_request(
        "PATCH",
        f"https://api.github.com/gists/{gist_id}",
        token,
        {"files": {VERSION_JSON_FILENAME: {"content": content}}},
    )


def validate_unstable_args(args: argparse.Namespace) -> None:
    missing = []
    if not args.channel:
        missing.append("--channel")
    if not args.workflow_name:
        missing.append("--workflow-name")
    if args.run_id is None:
        missing.append("--run-id")
    if not args.commit_sha:
        missing.append("--commit-sha")
    if not args.published_at:
        missing.append("--published-at")
    if args.build_timestamp_epoch_millis is None:
        missing.append("--build-timestamp-epoch-millis")
    if missing:
        raise SystemExit(f"Unstable gist publish requires: {', '.join(missing)}")


def validate_stable_args(args: argparse.Namespace) -> None:
    if not args.release_tag.strip():
        raise SystemExit("Stable gist publish requires --release-tag (for example app_v1.2.3).")


def build_document(args: argparse.Namespace, base: dict) -> dict:
    if args.stability == STABILITY_STABLE:
        return build_updated_stable_release_document(
            base,
            github_repo=args.github_repo,
            release_tag=args.release_tag,
            normal_build_file=Path(args.normal_build_file),
            dev_build_file=Path(args.dev_build_file),
        )

    validate_unstable_args(args)
    return build_updated_unstable_document(
        base,
        channel=args.channel,
        build_file=Path(args.build_file),
        github_repo=args.github_repo,
        workflow_name=args.workflow_name,
        run_id=args.run_id,
        commit_sha=args.commit_sha,
        published_at=args.published_at,
        build_timestamp_epoch_millis=args.build_timestamp_epoch_millis,
    )


def success_message(args: argparse.Namespace) -> str:
    if args.stability == STABILITY_STABLE:
        return f"Published stable metadata for release {args.release_tag.strip()} to gist"
    return f"Published unstable/{args.channel} metadata to gist"


def main() -> int:
    args = parse_args()
    gist_id = args.gist_id.strip()
    if not gist_id:
        raise SystemExit("Missing gist id. Pass --gist-id or set ABK_VERSION_GIST_ID.")

    if args.stability == STABILITY_STABLE:
        validate_stable_args(args)
    else:
        validate_unstable_args(args)

    token = resolve_token()

    for attempt in range(1, args.max_attempts + 1):
        try:
            current_text = fetch_gist_version_json(token, gist_id)
            base = load_version_json_text(current_text)
            updated = build_document(args, base)
            rendered = render_version_json(updated)
            if rendered == current_text:
                print("version.json unchanged in gist")
                return 0

            publish_gist_version_json(token, gist_id, rendered)
            print(f"{success_message(args)} {gist_id}")
            return 0
        except urllib.error.HTTPError as error:
            body = error.read().decode("utf-8", errors="replace")
            retriable = error.code in {409, 422, 429, 500, 502, 503, 504}
            if retriable and attempt < args.max_attempts:
                print(
                    f"Gist update failed with HTTP {error.code}; retrying ({attempt}/{args.max_attempts})",
                    file=sys.stderr,
                )
                time.sleep(attempt * 2)
                continue
            raise SystemExit(f"GitHub API error HTTP {error.code}: {body}") from error
        except urllib.error.URLError as error:
            if attempt < args.max_attempts:
                print(
                    f"Network error while updating gist; retrying ({attempt}/{args.max_attempts})",
                    file=sys.stderr,
                )
                time.sleep(attempt * 2)
                continue
            raise SystemExit(f"Network error while updating gist: {error}") from error

    raise SystemExit(f"Failed to publish gist after {args.max_attempts} attempts.")
    return 1


if __name__ == "__main__":
    sys.exit(main())
