#!/usr/bin/env python3

import argparse
import os
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from version_json_lib import (
    DEFAULT_DEV_BUILD_FILE,
    DEFAULT_GITHUB_REPO,
    DEFAULT_NORMAL_BUILD_FILE,
    STABILITY_STABLE,
    STABILITY_UNSTABLE,
    build_updated_stable_release_document,
    build_updated_unstable_document,
    load_version_json_file,
    render_version_json,
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Update app update metadata in version.json")
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
    parser.add_argument("--version-json", default="version.json")
    parser.add_argument("--build-file", default=str(DEFAULT_NORMAL_BUILD_FILE))
    parser.add_argument("--normal-build-file", default=str(DEFAULT_NORMAL_BUILD_FILE))
    parser.add_argument("--dev-build-file", default=str(DEFAULT_DEV_BUILD_FILE))
    parser.add_argument("--github-repo", default=os.environ.get("ABK_GITHUB_REPO", DEFAULT_GITHUB_REPO))
    return parser.parse_args()


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
        raise SystemExit(f"Unstable version.json update requires: {', '.join(missing)}")


def validate_stable_args(args: argparse.Namespace) -> None:
    if not args.release_tag.strip():
        raise SystemExit("Stable version.json update requires --release-tag (for example app_v1.2.3).")


def main() -> int:
    args = parse_args()
    version_json_path = Path(args.version_json)
    base = load_version_json_file(version_json_path)

    if args.stability == STABILITY_STABLE:
        validate_stable_args(args)
        updated = build_updated_stable_release_document(
            base,
            github_repo=args.github_repo,
            release_tag=args.release_tag,
            normal_build_file=Path(args.normal_build_file),
            dev_build_file=Path(args.dev_build_file),
        )
    else:
        validate_unstable_args(args)
        updated = build_updated_unstable_document(
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

    version_json_path.write_text(render_version_json(updated), encoding="utf-8")
    return 0


if __name__ == "__main__":
    sys.exit(main())
