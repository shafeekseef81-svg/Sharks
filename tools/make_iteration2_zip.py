"""Builds the Assignment 5 submission ZIP iteration2_Sharks.zip from a clean export of a git ref (task N2-06).

Steps (same procedure as the Iteration 1 release, Iteration1/docs/N07_Release.md):
  1. git archive <ref> Iteration2  ->  <out>/stage/iteration2_Sharks   (only versioned files)
  2. build.sh inside that folder (JDK 17+, --release 17), then delete build/
  3. check the JAR: Main-Class Controller.Main, class files of major version 61 (Java 17)
  4. optionally run test.sh in the staged folder (--test)
  5. zip with forward slashes (Python zipfile, not Windows Explorer) and print file count and SHA-256

Usage (from the repository root, in Git Bash):
  python tools/make_iteration2_zip.py [--ref origin/iteration-2] [--out <folder>] [--test]
"""
import argparse
import hashlib
import io
import os
import shutil
import subprocess
import sys
import tarfile
import zipfile

NAME = "iteration2_Sharks"


def run(cmd, cwd=None):
    print("+", " ".join(cmd))
    return subprocess.run(cmd, cwd=cwd, check=True, capture_output=True, text=True, encoding="utf-8",
                          errors="replace").stdout


def main():
    sys.stdout.reconfigure(encoding="utf-8")  # Hebrew folder names on a Windows console
    ap = argparse.ArgumentParser()
    ap.add_argument("--ref", default="origin/iteration-2")
    ap.add_argument("--out", default=os.path.join("..", "release_iteration2"))
    ap.add_argument("--test", action="store_true", help="run test.sh in the staged folder")
    args = ap.parse_args()

    out = os.path.abspath(args.out)
    stage = os.path.join(out, "stage")
    folder = os.path.join(stage, NAME)
    shutil.rmtree(stage, ignore_errors=True)
    os.makedirs(stage)

    commit = run(["git", "rev-parse", "--short", args.ref]).strip()
    tar_bytes = subprocess.run(["git", "archive", "--format=tar", args.ref, "Iteration2"], check=True,
                               capture_output=True).stdout
    with tarfile.open(fileobj=io.BytesIO(tar_bytes)) as tar:
        tar.extractall(stage, filter="data")
    os.rename(os.path.join(stage, "Iteration2"), folder)

    run(["bash", "./build.sh"], cwd=folder)
    jar = os.path.join(folder, "EducationalLudo.jar")
    with zipfile.ZipFile(jar) as z:
        manifest = z.read("META-INF/MANIFEST.MF").decode("utf-8")
        if "Main-Class: Controller.Main" not in manifest:
            sys.exit("JAR manifest has no Main-Class: Controller.Main")
        majors = {int.from_bytes(z.read(n)[6:8], "big") for n in z.namelist() if n.endswith(".class")}
        if majors != {61}:
            sys.exit(f"class file versions {majors}, expected only 61 (Java 17)")

    if args.test:
        tail = run(["bash", "./test.sh"], cwd=folder).splitlines()[-12:]
        print("\n".join(tail))
        if not any("0 tests failed" in line for line in tail):
            sys.exit("tests failed")
    shutil.rmtree(os.path.join(folder, "build"), ignore_errors=True)

    zip_path = os.path.join(out, NAME + ".zip")
    count = 0
    with zipfile.ZipFile(zip_path, "w", zipfile.ZIP_DEFLATED) as z:
        for root, dirs, files in os.walk(folder):
            dirs.sort()
            for f in sorted(files):
                full = os.path.join(root, f)
                arc = os.path.relpath(full, stage).replace(os.sep, "/")
                z.write(full, arc)
                count += 1
    with zipfile.ZipFile(zip_path) as z:
        names = z.namelist()
        assert all("\\" not in n for n in names), "backslash in a ZIP entry"
        assert all(n.startswith(NAME + "/") for n in names), "entry outside the top folder"
        assert NAME + "/EducationalLudo.jar" in names and NAME + "/run.bat" in names
    sha = hashlib.sha256(open(zip_path, "rb").read()).hexdigest()
    print(f"\n{zip_path}\nref {args.ref} ({commit}), {count} files, JAR {os.path.getsize(jar)} bytes,"
          f" Java 17 class files, Main-Class Controller.Main\nSHA-256 {sha}")


if __name__ == "__main__":
    main()
