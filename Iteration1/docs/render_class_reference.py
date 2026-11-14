"""
Complete class reference for the class diagram (Task N04), appended to classDiagram_Sharks.pdf.

Assignment 4, part 4 asks that the class diagram show *all* classes, the relationships between them,
and the fields and methods of every class. Pages 1-7 (render_class_diagram.py) show the structure and
the main relationships; the pages drawn here add one UML box per class with every field and every
method, and a table of every relationship between the classes.

Nothing is typed by hand: the sources in Iteration1/src are compiled with javac (--release 17) and
read back with `javap -p`, so the reference always matches the code it is generated from. Anonymous
classes and compiler-generated members (lambdas, enum values()/valueOf(), record equals/hashCode/
toString, synthetic fields) are left out.

Used by render_class_diagram.py; needs a JDK on the PATH, Python and reportlab.
"""
import glob
import os
import re
import subprocess
import tempfile

from reportlab.lib.pagesizes import A3, landscape
from reportlab.pdfbase.pdfmetrics import stringWidth

W, H = landscape(A3)
DOCS = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.normpath(os.path.join(DOCS, "..", "src"))

INK = (0.11, 0.13, 0.20)
MUTED = (0.42, 0.46, 0.56)
LINE = (0.55, 0.59, 0.68)
PKG_STYLE = {   # accent, fill — the same colours as pages 1-7
    "Model": ((0.15, 0.45, 0.80), (0.92, 0.95, 0.99)),
    "View": ((0.85, 0.45, 0.05), (0.99, 0.95, 0.89)),
    "View.components": ((0.85, 0.45, 0.05), (0.99, 0.95, 0.89)),
    "Controller": ((0.10, 0.60, 0.35), (0.91, 0.97, 0.93)),
}
IFACE = ((0.45, 0.30, 0.70), (0.95, 0.93, 0.99))
PACKAGES = ["Model", "View", "View.components", "Controller"]

MEMBER_F, TITLE_F, STEREO_F = 6.7, 8.2, 6.4
LH = 7.9                      # member line height
COLS, GAP, MARGIN = 4, 14, 28
COL_W = (W - 2 * MARGIN - (COLS - 1) * GAP) / COLS
TOP, BOTTOM = H - 104, 46

# ------------------------------------------------------------------ reading the code


def _split(s):
    """Splits on commas that are not inside <...>."""
    out, depth, cur = [], 0, ""
    for ch in s:
        if ch == "<":
            depth += 1
        elif ch == ">":
            depth -= 1
        if ch == "," and depth == 0:
            out.append(cur.strip())
            cur = ""
        else:
            cur += ch
    if cur.strip():
        out.append(cur.strip())
    return out


def simple(t):
    """java.util.Map<Model.Cell, Model.Board$Corner> -> Map<Cell, Board.Corner>"""
    t = re.sub(r"\b(?:View\.components|Model|View|Controller)\.(?=[A-Z])", "", t)
    t = re.sub(r"\b[a-z][a-z0-9_]*(?:\.[a-z][a-z0-9_]*)*\.(?=[A-Z])", "", t)
    return t.replace("$", ".").replace(",", ", ").replace(",  ", ", ")


def _visibility(mods):
    if "public" in mods:
        return "+"
    if "private" in mods:
        return "-"
    if "protected" in mods:
        return "#"
    return "~"


MODS = {"public", "private", "protected", "static", "final", "abstract", "synchronized", "native",
        "transient", "volatile", "default", "strictfp"}


def _parse_member(line, cls):
    line = line.strip().rstrip(";").strip()
    if not line or line.startswith("static {}") or line == "{}":
        return None
    line = re.sub(r"\s+throws\s+.*$", "", line)
    if "(" in line:
        head, params = line.split("(", 1)
        params = params.rsplit(")", 1)[0]
        toks = head.split()
        mods = [x for x in toks if x in MODS]
        rest = [x for x in toks if x not in MODS]
        if rest and rest[0].startswith("<"):            # generic method type parameters
            rest = rest[1:]
        name = rest[-1] if rest else ""
        ret = " ".join(rest[:-1])
        if "$" in name.split(".")[-1]:
            return None                                  # lambdas, access$, $values
        ctor = (ret == "")
        name = simple(name).split(".")[-1]
        ps = [simple(p) for p in _split(params)]
        return {"kind": "ctor" if ctor else "method", "vis": _visibility(mods), "static": "static" in mods,
                "abstract": "abstract" in mods, "final": "final" in mods, "name": name,
                "params": ps, "ret": simple(ret), "raw_ret": ret, "raw_params": params}
    toks = line.split()
    mods = [x for x in toks if x in MODS]
    rest = [x for x in toks if x not in MODS]
    name = rest[-1]
    if "$" in name:
        return None                                      # this$0, $VALUES, switch maps
    typ = " ".join(rest[:-1])
    return {"kind": "field", "vis": _visibility(mods), "static": "static" in mods, "final": "final" in mods,
            "name": name, "type": simple(typ), "raw_type": typ}


HEAD = re.compile(r"^(?P<mods>.*?)\b(?P<kind>class|interface)\s+(?P<name>[\w.$]+)(?P<tp><.*?>)?"
                  r"(?:\s+extends\s+(?P<ext>.+?))?(?:\s+implements\s+(?P<impl>.+?))?\s*\{$")


def read_classes():
    """Compiles Iteration1/src and returns {qualified name: class info} read with javap -p."""
    sources = sorted(glob.glob(os.path.join(SRC, "**", "*.java"), recursive=True))
    with tempfile.TemporaryDirectory() as out:
        subprocess.run(["javac", "--release", "17", "-encoding", "UTF-8", "-d", out] + sources, check=True)
        names = []
        for p in glob.glob(os.path.join(out, "**", "*.class"), recursive=True):
            rel = os.path.relpath(p, out)[:-6].replace(os.sep, ".")
            if re.search(r"\$\d", rel):
                continue                                 # anonymous classes
            names.append(rel)
        res = subprocess.run(["javap", "-p", "-cp", out] + sorted(names), check=True,
                             capture_output=True, text=True, encoding="utf-8")
    classes = {}
    cur = None
    for line in res.stdout.splitlines():
        if line.startswith("Compiled from"):
            continue
        m = HEAD.match(line.strip()) if not line.startswith("  ") else None
        if m:
            qn = m.group("name")
            pkg, _, short = qn.rpartition(".")
            mods = m.group("mods").split()
            ext = [e for e in _split(m.group("ext") or "")]
            impl = [e for e in _split(m.group("impl") or "")]
            kind = m.group("kind")
            stereo = None
            if kind == "interface":
                stereo, impl, ext = "interface", ext, []
            elif any(e.startswith("java.lang.Enum") for e in ext):
                stereo, ext = "enumeration", []
            elif "java.lang.Record" in ext:
                stereo, ext = "record", []
            elif "abstract" in mods:
                stereo = "abstract"
            cur = {"qn": qn, "pkg": pkg, "name": simple(qn), "outer": short.split("$")[0] if "$" in short else None,
                   "stereo": stereo, "ext": [simple(e) for e in ext if e != "java.lang.Object"],
                   "impl": [simple(i) for i in impl], "literals": [], "fields": [], "methods": [],
                   "vis": _visibility(mods)}
            classes[qn] = cur
            continue
        if line.strip() == "}" or cur is None:
            continue
        mem = _parse_member(line, cur)
        if not mem:
            continue
        if mem["kind"] == "field":
            if cur["stereo"] == "enumeration" and mem["static"] and mem["raw_type"] == cur["qn"]:
                cur["literals"].append(mem["name"])
            else:
                cur["fields"].append(mem)
        else:
            if cur["stereo"] == "enumeration" and mem["name"] in ("values", "valueOf") and mem["static"]:
                continue
            if cur["stereo"] == "record" and mem["final"] and mem["name"] in ("toString", "hashCode", "equals"):
                continue
            cur["methods"].append(mem)
    return classes


def relationships(classes):
    """Every structural relationship between two project classes: (from, kind, to, detail).

    Generalization, realization, association (a field) and nesting. A class that appears only as a
    method parameter or return type is a call-level dependency, not a relationship between the classes,
    and is shown in the class boxes instead."""
    names = {c["name"] for c in classes.values()}
    rels = []
    tok = re.compile(r"[A-Z]\w*(?:\.[A-Z]\w*)*")

    def refs(t):
        found = []
        outer = re.match(r"[\w.]+", t).group(0) if re.match(r"[\w.]+", t) else ""
        for m in tok.finditer(t):
            n = m.group(0)
            if n in names:
                many = (n != outer) or t.endswith("[]")
                if outer == "Optional":
                    many = None
                found.append((n, "0..1" if many is None else ("*" if many else "1")))
        return found

    for c in sorted(classes.values(), key=lambda c: c["name"]):
        a = c["name"]
        for e in c["ext"]:
            base = re.match(r"[\w.]+", e).group(0)
            if base in names:
                rels.append((a, "generalization", base, "extends"))
        for i in c["impl"]:
            base = re.match(r"[\w.]+", i).group(0)
            if base in names:
                kind = "generalization" if c["stereo"] == "interface" else "realization"
                rels.append((a, kind, base, "extends" if kind == "generalization" else "implements"))
        if c["outer"]:
            outer = c["name"].rsplit(".", 1)[0]
            rels.append((a, "nesting", outer, "nested in"))
        assoc = {}
        for f in c["fields"]:
            for n, mult in refs(f["type"]):
                if n == a and f["static"]:
                    continue
                assoc.setdefault(n, []).append((f["name"], mult))
        for n, fs in sorted(assoc.items()):
            rels.append((a, "association", n, ", ".join("%s [%s]" % x for x in fs)))
    return rels


# ------------------------------------------------------------------ drawing


def _wrap(text, font, size, width, indent="      "):
    words = re.split(r"(?<=[,(])\s*", text)
    lines, cur = [], ""
    for w_ in words:
        cand = (cur + (" " if cur.endswith(",") else "") + w_) if cur else w_
        if stringWidth(cand, font, size) <= width or not cur:
            cur = cand
        else:
            lines.append(cur)
            cur = indent + w_
    if cur:
        lines.append(cur)
    return lines


def _member_text(m):
    if m["kind"] == "field":
        return "%s %s : %s" % (m["vis"], m["name"], m["type"])
    s = "%s %s(%s)" % (m["vis"], m["name"], ", ".join(m["params"]))
    if m["kind"] == "method" and m["ret"]:
        s += " : " + m["ret"]
    return s


def _box_lines(c):
    """Returns the drawable rows of a class box: (text, style, compartment)."""
    inner = COL_W - 12
    rows = []
    rel = []
    if c["ext"]:
        rel.append("extends " + ", ".join(c["ext"]))
    if c["impl"]:
        rel.append(("extends " if c["stereo"] == "interface" else "implements ") + ", ".join(c["impl"]))
    for r in rel:
        for ln in _wrap(r, "Helvetica-Oblique", STEREO_F, inner):
            rows.append((ln, "rel", 0))
    comp = 1
    if c["literals"]:
        for ln in _wrap(", ".join(c["literals"]), "Helvetica", MEMBER_F, inner, ""):
            rows.append((ln, "plain", comp))
        comp += 1
    if c["fields"] or not c["literals"]:
        for f in c["fields"]:
            for i, ln in enumerate(_wrap(_member_text(f), "Helvetica", MEMBER_F, inner)):
                rows.append((ln, "static" if f["static"] and i == 0 else "plain", comp))
        if not c["fields"]:
            rows.append(("", "empty", comp))
        comp += 1
    for m in c["methods"]:
        style = "static" if m["static"] else ("abstract" if m.get("abstract") and c["stereo"] != "interface" else "plain")
        for i, ln in enumerate(_wrap(_member_text(m), "Helvetica", MEMBER_F, inner)):
            rows.append((ln, style if i == 0 else "plain", comp))
    if not c["methods"]:
        rows.append(("", "empty", comp))
    return rows


def _box_height(c, rows):
    head = 15 + (STEREO_F + 2 if c["stereo"] else 0)
    comps = len({r[2] for r in rows if r[2] > 0})
    return head + len(rows) * LH + comps * 4 + 4


def layout(classes):
    """Places every class box; returns pages = [(package, part, [(class, rows, x, y, h)])]."""
    pages = []
    for pkg in PACKAGES:
        members = sorted([c for c in classes.values() if c["pkg"] == pkg],
                         key=lambda c: (c["name"].split(".")[0], c["name"]))
        if not members:
            continue
        cols = [TOP] * COLS
        page = []
        part = 1
        for c in members:
            rows = _box_lines(c)
            h = _box_height(c, rows)
            i = max(range(COLS), key=lambda k: cols[k])
            if cols[i] - h < BOTTOM:
                pages.append((pkg, part, page))
                page, cols, part = [], [TOP] * COLS, part + 1
                i = 0
            x = MARGIN + i * (COL_W + GAP)
            page.append((c, rows, x, cols[i], h))
            cols[i] -= h + 10
        pages.append((pkg, part, page))
    return pages


def _draw_box(cv, c, rows, x, y, h):
    accent, fill = IFACE if c["stereo"] == "interface" else PKG_STYLE[c["pkg"]]
    cv.setFillColorRGB(*fill)
    cv.setStrokeColorRGB(*accent)
    cv.setLineWidth(0.9)
    cv.roundRect(x, y - h, COL_W, h, 3, stroke=1, fill=1)
    ty = y - 11
    if c["stereo"]:
        cv.setFillColorRGB(*MUTED)
        cv.setFont("Helvetica", STEREO_F)
        cv.drawCentredString(x + COL_W / 2, ty, "«%s»" % c["stereo"])
        ty -= STEREO_F + 2
    cv.setFillColorRGB(*INK)
    font = "Helvetica-BoldOblique" if c["stereo"] == "abstract" else "Helvetica-Bold"
    cv.setFont(font, TITLE_F)
    cv.drawCentredString(x + COL_W / 2, ty, c["name"])
    ly = ty - 5
    comp = None
    for text, style, k in rows:
        if k != comp and k > 0:
            ly -= 2
            cv.setStrokeColorRGB(*accent)
            cv.setLineWidth(0.5)
            cv.line(x, ly, x + COL_W, ly)
            ly -= 2
            comp = k
        ly -= LH
        if style == "empty":
            continue
        if style == "rel":
            cv.setFillColorRGB(*MUTED)
            cv.setFont("Helvetica-Oblique", STEREO_F)
            cv.drawCentredString(x + COL_W / 2, ly + 1.5, text)
            continue
        cv.setFillColorRGB(*INK)
        cv.setFont("Helvetica-Oblique" if style == "abstract" else "Helvetica", MEMBER_F)
        cv.drawString(x + 6, ly + 1.5, text)
        if style == "static":
            cv.setStrokeColorRGB(*INK)
            cv.setLineWidth(0.35)
            cv.line(x + 6, ly + 0.6, x + 6 + stringWidth(text, "Helvetica", MEMBER_F), ly + 0.6)


REL_ROW = 8.6


def _rel_pages(rels):
    per_col = int((TOP + 10 - BOTTOM) / REL_ROW) - 1
    cols_per_page = 2
    per_page = per_col * cols_per_page
    return [rels[i:i + per_page] for i in range(0, len(rels), per_page)], per_col


def page_count(classes):
    return len(layout(classes)) + len(_rel_pages(relationships(classes))[0])


def draw(cv, classes, first_page, total, header, commit):
    """Draws the reference pages with the caller's header(c, title, subtitle, n)."""
    n = first_page
    pkg_titles = {"Model": "Model", "View": "View — screens", "View.components": "View.components — widgets",
                  "Controller": "Controller"}
    for pkg, part, placed in layout(classes):
        parts = sum(1 for p in layout(classes) if p[0] == pkg)
        header(cv, "Complete class reference — %s%s" % (pkg_titles[pkg], " (%d/%d)" % (part, parts) if parts > 1 else ""),
               "Every class of package %s with all its fields and methods, generated from the compiled code "
               "(source as of commit %s)" % (pkg, commit), n)
        _legend(cv)
        for c, rows, x, y, h in placed:
            _draw_box(cv, c, rows, x, y, h)
        cv.showPage()
        n += 1
    chunks, per_col = _rel_pages(relationships(classes))
    for i, chunk in enumerate(chunks):
        header(cv, "Relationships between the classes%s" % (" (%d/%d)" % (i + 1, len(chunks)) if len(chunks) > 1 else ""),
               "Every relationship between two classes of the system, generated from the compiled code "
               "(source as of commit %s)" % commit, n)
        _legend(cv, rel=True)
        colw = (W - 2 * MARGIN - GAP) / 2
        for k in range(2):
            part = chunk[k * per_col:(k + 1) * per_col]
            if not part:
                continue
            x0 = MARGIN + k * (colw + GAP)
            xs = [x0, x0 + 150, x0 + 235, x0 + 385]
            y = TOP + 6
            cv.setFillColorRGB(*INK)
            cv.setFont("Helvetica-Bold", 7.4)
            for xx, t in zip(xs, ["Class", "Relationship", "Other class", "Through (field [multiplicity])"]):
                cv.drawString(xx, y, t)
            cv.setStrokeColorRGB(*LINE)
            cv.setLineWidth(0.5)
            cv.line(x0, y - 3, x0 + colw, y - 3)
            for a, kind, b, detail in part:
                y -= REL_ROW
                cv.setFont("Helvetica", 7.0)
                cv.setFillColorRGB(*INK)
                cv.drawString(xs[0], y, a)
                cv.setFillColorRGB(*MUTED)
                cv.drawString(xs[1], y, kind)
                cv.setFillColorRGB(*INK)
                cv.drawString(xs[2], y, b)
                cv.setFillColorRGB(*MUTED)
                d = detail
                while stringWidth(d, "Helvetica", 7.0) > colw - (xs[3] - x0) and len(d) > 4:
                    d = d[:-4] + "…"
                cv.drawString(xs[3], y, d)
        cv.showPage()
        n += 1


def _legend(cv, rel=False):
    cv.setFillColorRGB(*MUTED)
    cv.setFont("Helvetica", 8.0)
    if rel:
        txt = ("generalization = extends · realization = implements an interface · association = held in a field "
               "(1, * = collection, 0..1 = Optional) · nesting = inner class of")
    else:
        txt = ("Notation: + public  - private  # protected  ~ package   ·   underlined = static   ·   "
               "italic = abstract   ·   first compartment: fields (or enum literals), second: constructors and "
               "methods   ·   purple = interface")
    cv.drawString(MARGIN, H - 84, txt)
