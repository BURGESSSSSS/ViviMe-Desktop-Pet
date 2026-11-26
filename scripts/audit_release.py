"""Check reviewed source paths, Git object contents and the local demo distribution."""

import argparse
from hashlib import sha256
from io import BytesIO
from pathlib import Path, PurePosixPath
import re
import subprocess
import sys
import tarfile
from zipfile import ZipFile


WRAPPER = "gradle/wrapper/gradle-wrapper.jar"
WRAPPER_SHA = "7d3a4ac4de1c32b59bc6a4eb8ecb8e612ccd0cf1ae1e99f66902da64df296172"
ROOT_FILE = {
    ".gitattributes", ".gitignore", "LICENSE", "NOTICE.md", "README.md", "BUILD.md",
    "CONTRIBUTING.md", "THIRD_PARTY_LICENSES.md", "ASSET_POLICY.md", "RELEASE_CHECKLIST.md",
    "gradlew", "gradlew.bat", "settings.gradle", "build.gradle",
}
FIXED_PATHS = {
    WRAPPER, ".github/workflows/check.yml", ".githooks/pre-commit", "gradle/verification-metadata.xml",
    "gradle/wrapper/gradle-wrapper.properties", "core/build.gradle", "desktop/build.gradle",
    "core/src/test/resources/vivime-autonomy.json",
}
TEXT_ONLY = {
    "docs/": ".md", "scripts/": (".py", ".ps1"), "licenses/": ".txt",
    "core/src/main/java/": ".java", "core/src/test/java/": ".java",
    "desktop/src/main/java/": ".java", "desktop/src/test/java/": ".java",
}
SECRETS = {
    "provider token": rb"\b(?:gh[pousr]_|github_pat_|xox[baprs]-|sk-(?:live-|proj-)?)[a-zA-Z0-9_-]{22,}",
    "cloud key": rb"\b(?:AKIA|ASIA)[A-Z0-9]{16}\b|\bAIza[0-9A-Za-z_-]{35}\b",
    "private key": rb"-----BEGIN (?:[A-Z]+ )*PRIVATE KEY-----",
    "authorization header": rb"(?i)\b(?:authorization\s*[:=]\s*bearer|bearer)\s+[A-Za-z0-9_.~+/=-]{20,}",
    "credential literal": rb"(?i)\b(?:token|api[_-]?key|password|cookie|download_mc_cdk)\s*[\"']?\s*[:=]\s*[\"'][a-zA-Z0-9_.+/=-]{8,}[\"']",
    "local absolute path": rb"(?i)(?:[a-z]:[\\/]+(?:users[\\/]|documents and settings[\\/])|/(?:home|Users)/[^/\s\"']+/)",
    "encoded binary": rb"(?<![a-zA-Z0-9+/])[a-zA-Z0-9+/]{600,}={0,2}(?![a-zA-Z0-9+/])",
}
GAME_DIRS = {"models", "runtime_assets", "extracted", "assets", "screenshots", "previews", "logs", "private"}
GAME_BEGIN = (b"UnityFS", b"UnityRaw", b"UnityWeb", b"VFX2", b"RIFF", b"OggS", b"\x89PNG", b"\x1f\x8b")
LIMIT = 2 * 1024 * 1024


def git(*args):
    return subprocess.run(["git", *args], stdout=subprocess.PIPE, check=True).stdout


def path_is_allowed(path):
    bits = PurePosixPath(path).parts
    if not bits or set(part.casefold() for part in bits) & GAME_DIRS:
        return False
    if path in ROOT_FILE or path in FIXED_PATHS:
        return True
    return any(path.startswith(prefix) and path.endswith(suffix) for prefix, suffix in TEXT_ONLY.items())


def check_text(data):
    if len(data) > LIMIT:
        raise ValueError("oversized text object")
    if data.startswith(GAME_BEGIN) or b"\0" in data:
        raise ValueError("binary signature or NUL")
    data.decode("utf-8")
    for label, pattern in SECRETS.items():
        if re.search(pattern, data):
            raise ValueError(label)
    # Match the legacy workspace root even if its drive or parent changes.
    if re.search(r"(?i)[a-z]:[\\/]+[^\r\n<>]{0,120}(?:arkpets-fork|明日方舟维维美)[\\/]", data.decode("utf-8")):
        raise ValueError("legacy workspace path")


def inspect_path(mode, path, data):
    if mode not in (b"100644", b"100755"):
        raise ValueError("symlink, submodule or unsupported mode")
    if not path_is_allowed(path):
        raise ValueError("path outside reviewed source layout")
    check_text(path.encode("utf-8"))
    if path == WRAPPER:
        if sha256(data).hexdigest() != WRAPPER_SHA:
            raise ValueError("wrapper SHA-256 differs from Gradle release")
        inspect_jar(data)
        return
    check_text(data)
    if path.endswith((".java", ".gradle")) and re.search(rb"com\.esotericsoftware\.spine|spine-libgdx", data):
        raise ValueError("restricted code dependency")


def cached_read(cache, oid):
    if oid not in cache:
        cache[oid] = git("cat-file", "blob", oid)
    return cache[oid]


def validate_path(mode, oid, raw_name, cache):
    name = raw_name.decode("utf-8")
    try:
        check_text(raw_name)
    except ValueError as failure:
        raise ValueError(f"{oid[:12]} sensitive path omitted: {failure}") from None
    try:
        inspect_path(mode, name, cached_read(cache, oid))
    except (UnicodeError, ValueError) as failure:
        raise ValueError(f"{oid[:12]} {name}: {failure}") from None


def audit_index():
    cache = {}
    rows = git("ls-files", "--stage", "-z").split(b"\0")
    for row in filter(None, rows):
        fields, raw_name = row.split(b"\t", 1)
        mode, oid, stage = fields.split()
        if stage != b"0":
            raise ValueError("index has unresolved merge entries")
        validate_path(mode, oid.decode(), raw_name, cache)
    print(f"Index passed: {len(rows)-1} paths, {len(cache)} blobs")


def audit_objects():
    # Include loose and unreachable objects, plus every path alias in every commit.
    index = git("cat-file", "--batch-all-objects", "--batch-check=%(objectname) %(objecttype) %(objectsize)")
    objects = [row.split() for row in index.decode("ascii").splitlines()]
    cache = {}
    path_count = 0
    for oid, kind, _ in objects:
        if kind in ("commit", "tag"):
            check_text(git("cat-file", "-p", oid))
        if kind == "tree":
            for entry in filter(None, git("ls-tree", "-z", oid).split(b"\0")):
                header, name = entry.split(b"\t", 1)
                if header.startswith((b"120000 ", b"160000 ")):
                    raise ValueError(f"{oid[:12]} tree contains a link")
                check_text(name)
        if kind == "commit":
            for row in filter(None, git("ls-tree", "-r", "-z", oid).split(b"\0")):
                prefix, name = row.split(b"\t", 1)
                mode, _, blob = prefix.split()
                validate_path(mode, blob.decode(), name, cache)
                path_count += 1
    for oid, kind, size in objects:
        if kind == "blob" and oid not in cache:
            if int(size) > LIMIT:
                raise ValueError(f"{oid[:12]} unreferenced oversized object")
            try:
                check_text(cached_read(cache, oid))
            except (ValueError, UnicodeError) as failure:
                raise ValueError(f"{oid[:12]} unreferenced object: {failure}") from None
    print(f"Object database passed: {len(objects)} objects, {path_count} historical paths, {len(cache)} blobs")


def inspect_jar(data):
    banned = (".png", ".gif", ".jpg", ".otf", ".ttf", ".skel", ".vfx", ".wav", ".ogg", ".mp3",
              ".unity3d", ".prefab", ".bundle", ".dll", ".so", ".dylib", ".exe", ".jar", ".zip")
    with ZipFile(BytesIO(data)) as archive:
        for member in archive.infolist():
            name = member.filename
            folded = name.casefold()
            if folded.startswith(("/", "com/esotericsoftware/spine/")) or "\\" in name or ".." in PurePosixPath(name).parts:
                raise ValueError("restricted or unsafe JAR member")
            if folded.endswith(banned):
                raise ValueError("unreviewed binary inside JAR")
            if not (member.is_dir() or folded.endswith(".class")):
                with archive.open(member) as payload:
                    if payload.read(8).startswith(GAME_BEGIN):
                        raise ValueError("restricted payload inside JAR")


DIST_JARS = {"lib/core-0.1.0-candidate.jar", "lib/desktop-0.1.0-candidate.jar", "lib/fastjson2-2.0.60.jar"}
DIST_TEXT = {"bin/ViviMe-Desktop-Pet", "bin/ViviMe-Desktop-Pet.bat", "LICENSE", "NOTICE.md",
             "THIRD_PARTY_LICENSES.md", "licenses/Apache-2.0.txt", "licenses/FASTJSON2-NOTICE.txt"}


def dist_files(root):
    if root.suffix == ".zip":
        with ZipFile(root) as archive:
            for entry in archive.infolist():
                if not entry.is_dir():
                    if (entry.external_attr >> 16) & 0o170000 == 0o120000:
                        raise ValueError("ZIP contains a link")
                    yield dist_archive_path(entry.filename), archive.read(entry)
        return
    if root.suffix == ".tar":
        with tarfile.open(root) as archive:
            for entry in archive:
                if entry.isdir():
                    continue
                if not entry.isfile():
                    raise ValueError("TAR contains an unsupported entry")
                yield dist_archive_path(entry.name), archive.extractfile(entry).read()
        return
    for item in root.rglob("*"):
        if item.is_symlink():
            raise ValueError("distribution includes a link")
        if item.is_file():
            yield item.relative_to(root).as_posix(), item.read_bytes()


def dist_archive_path(path):
    bits = PurePosixPath(path).parts
    if len(bits) < 2 or not bits[0].startswith("ViviMe-Desktop-Pet") or "\\" in path or ".." in bits:
        raise ValueError("unexpected distribution archive root")
    return "/".join(bits[1:])


def audit_distribution(root):
    seen = set()
    for path, data in dist_files(root):
        if path in seen:
            raise ValueError("duplicate distribution path")
        seen.add(path)
        if path in DIST_JARS:
            inspect_jar(data)
        elif path in DIST_TEXT:
            check_text(data)
        else:
            raise ValueError(f"unexpected distribution file: {path}")
    if seen != DIST_JARS | DIST_TEXT:
        raise ValueError("distribution graph or required license files differ")
    print(f"Distribution passed: {len(DIST_JARS)} reviewed JARs, no game assets")


def self_test():
    cases = [
        (b"100644", "assets/dawn.vfx", b"VFX2"),
        (b"100644", "core/src/main/java/x.java", b"UnityFS and game data"),
        (b"100644", "README.md", ("ghp_" + "a" * 36).encode()),
        (b"100644", "gradle/wrapper/gradle-wrapper.jar", b"altered wrapper"),
        (b"120000", "README.md", b"target"),
        (b"100644", "core/src/main/java/x.java", b"import com.esotericsoftware.spine.Skeleton;"),
    ]
    for case in cases:
        try:
            inspect_path(*case)
        except ValueError:
            continue
        raise AssertionError("negative case was accepted")
    inspect_path(b"100644", "README.md", "可公开代码".encode())
    print(f"Guard self-test passed: {len(cases)} denied samples")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--index", action="store_true")
    parser.add_argument("--objects", action="store_true")
    parser.add_argument("--self-test", action="store_true")
    parser.add_argument("--distribution", type=Path)
    args = parser.parse_args()
    if not any(vars(args).values()):
        parser.error("choose a check")
    try:
        if args.self_test:
            self_test()
        if args.index:
            audit_index()
        if args.objects:
            audit_objects()
        if args.distribution:
            audit_distribution(args.distribution)
    except (OSError, ValueError, subprocess.CalledProcessError) as failure:
        print(f"Review blocked: {failure}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
