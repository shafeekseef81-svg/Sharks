"""NFR-14 / T2-14: which FRs of the movement and special-square rules have JUnit tests.

Usage: python rtm_coverage.py <srs.txt from pdftotext -layout> <Iteration2/test dir> <first FR> <last FR>
Counts every @DisplayName that names an FR, alone or in a range (FR-28..FR-30, FR-28-FR-30, FR-28–FR-30).
"""
import os
import re
import sys
from collections import defaultdict

srs_txt, test_dir, first, last = sys.argv[1], sys.argv[2], int(sys.argv[3]), int(sys.argv[4])

titles = {}
for line in open(srs_txt, encoding="utf-8", errors="replace"):
    m = re.match(r"\s*FR-(\d\d)\s+(.*)", line)
    if m and int(m.group(1)) not in titles:
        text = re.split(r"\s{2,}|\s\d\s+[A-Z]", m.group(2).strip())[0]
        titles[int(m.group(1))] = text.strip()

RANGE = re.compile(r"FR-(\d+)\s*(?:\.\.|–|—|-|to)\s*FR-(\d+)")
SINGLE = re.compile(r"FR-(\d+)")

tests = defaultdict(list)  # FR -> [(class, display name)]
for root, _, files in os.walk(test_dir):
    for name in files:
        if not name.endswith(".java"):
            continue
        src = open(os.path.join(root, name), encoding="utf-8").read()
        for m in re.finditer(r"@DisplayName\((.*?)\)\s*\n", src, re.S):
            display = "".join(re.findall(r'"((?:[^"\\]|\\.)*)"', m.group(1)))
            ids = set()
            for a, b in RANGE.findall(display):
                ids.update(range(int(a), int(b) + 1))
            ids.update(int(x) for x in SINGLE.findall(RANGE.sub("", display)))
            for fr in ids:
                tests[fr].append((name[:-5], display))

print("| FR | Requirement (SRS, first line) | Tests | Test classes |")
print("|---|---|---:|---|")
gaps = []
for fr in range(first, last + 1):
    found = tests.get(fr, [])
    classes = sorted({c for c, _ in found})
    if not found:
        gaps.append(fr)
    print(f"| FR-{fr} | {titles.get(fr, '?')} | {len(found)} | {', '.join(classes) if classes else '**none**'} |")
print()
print("GAPS:", ", ".join(f"FR-{g}" for g in gaps) if gaps else "none")
