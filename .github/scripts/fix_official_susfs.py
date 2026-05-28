#!/usr/bin/env python3
"""Repair Official KernelSU + SUSFS after partial 10_enable_susfs_for_ksu.patch apply."""

from pathlib import Path
import re
import sys


def die(message):
    raise SystemExit(message)


def write_if_changed(path, text, original, changed_files):
    if text != original:
        path.write_text(text)
        changed_files.append(str(path))


def find_ksu_dir(root):
    for rel in (
        "common/drivers/kernelsu",
        "drivers/kernelsu",
        "KernelSU/kernel",
        "kernel",
    ):
        path = root / rel
        if (path / "Kbuild").exists():
            return path
    die("Official KernelSU source directory not found")


def tree_uses_symbol(ksu_dir, symbol):
    for path in ksu_dir.rglob("*.[ch]"):
        if path.name.endswith((".h", ".c")) and symbol in path.read_text(errors="ignore"):
            return True
    return False


def restore_ksud_h_declarations(ksu_dir, ksud_h, changed_files):
    if not ksud_h.exists():
        return

    text = ksud_h.read_text()
    original = text
    additions = []

    if (
        "ksu_stop_input_hook_runtime" not in text
        and tree_uses_symbol(ksu_dir, "ksu_stop_input_hook_runtime")
    ):
        additions.append("void ksu_stop_input_hook_runtime(void);")

    if (
        "ksu_execve_hook_ksud" not in text
        and tree_uses_symbol(ksu_dir, "ksu_execve_hook_ksud")
    ):
        additions.append("void ksu_execve_hook_ksud(const struct pt_regs *regs);")

    if not additions:
        return

    marker = "#endif\n"
    if marker not in text:
        die(f"missing ksud.h footer marker: {ksud_h}")

    block = "\n".join([""] + additions + [""])
    text = text.replace(marker, block + marker, 1)
    write_if_changed(ksud_h, text, original, changed_files)


STATIC_KEY_DISABLE_BLOCK = """    if (static_key_enabled(&ksu_is_input_hook_enabled)) {
        static_branch_disable(&ksu_is_input_hook_enabled);
        pr_info("ksu_input_hook is disabled\\n");
    }"""


def replace_stop_input_calls(text):
    if "ksu_is_input_hook_enabled" not in text:
        return text, False

    patterns = [
        r"\s*ksu_stop_input_hook_runtime\(\);\s*\n",
        r"\s*ksu_stop_input_hook_runtime\(\);\s*",
    ]
    changed = False
    for pattern in patterns:
        if re.search(pattern, text):
            text, count = re.subn(pattern, "\n" + STATIC_KEY_DISABLE_BLOCK + "\n", text, count=0)
            if count:
                changed = True
                break
    return text, changed


def patch_boot_event(path, changed_files):
    if not path.exists():
        return

    text = path.read_text()
    original = text

    if "extern struct static_key_true ksu_is_input_hook_enabled;" not in text:
        anchor = "bool ksu_boot_completed __read_mostly = false;\n"
        if anchor in text:
            text = text.replace(
                anchor,
                anchor + "extern struct static_key_true ksu_is_input_hook_enabled;\n",
                1,
            )

    text, _ = replace_stop_input_calls(text)
    write_if_changed(path, text, original, changed_files)


def patch_ksud_integration(path, changed_files):
    if not path.exists():
        return

    text = path.read_text()
    original = text

    anchor = '#include "runtime/ksud_boot.h"\n'
    block = (
        "\n#ifdef CONFIG_KSU_SUSFS\n"
        "DEFINE_STATIC_KEY_TRUE(ksu_is_init_rc_hook_enabled);\n"
        "DEFINE_STATIC_KEY_TRUE(ksu_is_input_hook_enabled);\n"
        "#endif\n"
    )
    if "DEFINE_STATIC_KEY_TRUE(ksu_is_input_hook_enabled)" not in text and anchor in text:
        text = text.replace(anchor, anchor + block, 1)

    text, _ = replace_stop_input_calls(text)

    if (
        "DEFINE_STATIC_KEY_TRUE(ksu_is_input_hook_enabled)" in text
        and "ksu_syscall_table_hook(__NR_read" in text
        and "void __init ksu_ksud_init()" in text
    ):
        text = re.sub(
            r"void __init ksu_ksud_init\(\)\s*\{.*?\n\}",
            "void __init ksu_ksud_init()\n{\n}\n",
            text,
            count=1,
            flags=re.S,
        )

    write_if_changed(path, text, original, changed_files)


def verify(ksu_dir):
    boot_event = ksu_dir / "runtime/boot_event.c"
    ksud_h = ksu_dir / "runtime/ksud.h"
    if not boot_event.exists():
        return

    boot = boot_event.read_text()
    if "ksu_stop_input_hook_runtime()" in boot:
        header = ksud_h.read_text() if ksud_h.exists() else ""
        if "ksu_stop_input_hook_runtime" not in header and "ksu_is_input_hook_enabled" not in boot:
            die(
                f"{boot_event} still calls ksu_stop_input_hook_runtime() "
                "without declaration or static_key migration"
            )


def main():
    if len(sys.argv) != 2:
        die("usage: fix_official_susfs.py <kernel-root>")

    root = Path(sys.argv[1]).resolve()
    ksu_dir = find_ksu_dir(root)
    changed_files = []

    restore_ksud_h_declarations(ksu_dir, ksu_dir / "runtime/ksud.h", changed_files)
    patch_boot_event(ksu_dir / "runtime/boot_event.c", changed_files)
    patch_ksud_integration(ksu_dir / "runtime/ksud_integration.c", changed_files)
    verify(ksu_dir)

    if changed_files:
        print("Patched Official SUSFS runtime compatibility:")
        for path in changed_files:
            print(f"  {path}")
    else:
        print("Official SUSFS runtime compatibility already OK.")


if __name__ == "__main__":
    main()
