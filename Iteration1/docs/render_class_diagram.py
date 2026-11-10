"""
Renders the Educational Ludo class diagram (Task N04) as a real vector PDF.

Drawn with reportlab primitives: a proper UML drawing, not a picture of Markdown. All connectors are
routed orthogonally so no line crosses a class box and no label sits on top of one.

Source of truth: Iteration1/docs/CLASS_DIAGRAM.md, checked against Iteration1/src at iteration-1 3d3ce9b.
"""
import math
import sys
from reportlab.lib.pagesizes import A3, landscape
from reportlab.pdfgen import canvas

W, H = landscape(A3)          # 1191 x 842 pt

INK = (0.11, 0.13, 0.20)
MUTED = (0.42, 0.46, 0.56)
LINE = (0.55, 0.59, 0.68)
MODEL = (0.15, 0.45, 0.80)
VIEW = (0.85, 0.45, 0.05)
CTRL = (0.10, 0.60, 0.35)
IFACE = (0.45, 0.30, 0.70)
PLAN = (0.50, 0.53, 0.60)
BG_MODEL = (0.92, 0.95, 0.99)
BG_VIEW = (0.99, 0.95, 0.89)
BG_CTRL = (0.91, 0.97, 0.93)
BG_IFACE = (0.95, 0.93, 0.99)
BG_PLAN = (0.94, 0.95, 0.96)

TITLE_F = 10.5
MEMBER_F = 8.6
STEREO_F = 7.8
TOTAL = 7


class Box:
    def __init__(self, x, y, w, h):
        self.x, self.y, self.w, self.h = x, y, w, h
        self.cx, self.cy = x + w / 2, y + h / 2

    def top(self, dx=0):
        return (self.cx + dx, self.y + self.h)

    def bottom(self, dx=0):
        return (self.cx + dx, self.y)

    def left(self, dy=0):
        return (self.x, self.cy + dy)

    def right(self, dy=0):
        return (self.x + self.w, self.cy + dy)


def box(c, x, y, w, name, members=(), stereo=None, accent=MODEL, fill=BG_MODEL, planned=False):
    head = 19 + (11 if stereo else 0)
    body = 5 + len(members) * (MEMBER_F + 3.1) + (4 if members else 0)
    h = head + body
    y0 = y - h
    c.setLineWidth(1.1 if planned else 1.5)
    c.setStrokeColorRGB(*accent)
    c.setFillColorRGB(*fill)
    if planned:
        c.setDash(4, 3)
    c.roundRect(x, y0, w, h, 5, stroke=1, fill=1)
    c.setDash()
    c.setFillColorRGB(*accent)
    c.roundRect(x, y0 + h - head, w, head, 5, stroke=0, fill=1)
    c.rect(x, y0 + h - head, w, 6, stroke=0, fill=1)
    ty = y0 + h - 13.5
    c.setFillColorRGB(1, 1, 1)
    if stereo:
        c.setFont("Helvetica-Oblique", STEREO_F)
        c.drawCentredString(x + w / 2, y0 + h - 11.5, stereo)
        ty = y0 + h - 23
    c.setFont("Helvetica-Bold", TITLE_F)
    c.drawCentredString(x + w / 2, ty, name)
    c.setFillColorRGB(*INK)
    c.setFont("Helvetica", MEMBER_F)
    my = y0 + body - 11
    for m in members:
        c.drawString(x + 7, my, m)
        my -= MEMBER_F + 3.1
    return Box(x, y0, w, h)


def _arrow(c, x, y, dx, dy, size=6):
    a = math.atan2(dy, dx)
    p = c.beginPath()
    p.moveTo(x, y)
    p.lineTo(x - size * math.cos(a - 0.42), y - size * math.sin(a - 0.42))
    p.lineTo(x - size * math.cos(a + 0.42), y - size * math.sin(a + 0.42))
    p.close()
    c.drawPath(p, stroke=0, fill=1)


def _triangle(c, x, y, dx, dy, size=10):
    a = math.atan2(dy, dx)
    p = c.beginPath()
    p.moveTo(x, y)
    p.lineTo(x - size * math.cos(a - 0.42), y - size * math.sin(a - 0.42))
    p.lineTo(x - size * math.cos(a + 0.42), y - size * math.sin(a + 0.42))
    p.close()
    c.setFillColorRGB(1, 1, 1)
    c.drawPath(p, stroke=1, fill=1)


def _diamond(c, x, y, dx, dy, filled, size=8):
    a = math.atan2(dy, dx)
    hx, hy = math.cos(a), math.sin(a)
    px, py = -hy, hx
    p = c.beginPath()
    p.moveTo(x, y)
    p.lineTo(x + size * hx + size * 0.55 * px, y + size * hy + size * 0.55 * py)
    p.lineTo(x + 2 * size * hx, y + 2 * size * hy)
    p.lineTo(x + size * hx - size * 0.55 * px, y + size * hy - size * 0.55 * py)
    p.close()
    if not filled:
        c.setFillColorRGB(1, 1, 1)
    c.drawPath(p, stroke=1, fill=1)


def connect(c, a, b, kind="depend", label=None, route="v", bend=None, lab_dx=0, lab_dy=0,
            colour=LINE):
    """Orthogonal connector. route 'v' = leave vertically, 'h' = leave horizontally."""
    (ax, ay), (bx, by) = a, b
    mid = None
    if route == "v":
        mid = bend if bend is not None else (ay + by) / 2
        pts = [(ax, ay), (ax, mid), (bx, mid), (bx, by)]
    elif route == "h":
        mid = bend if bend is not None else (ax + bx) / 2
        pts = [(mid, ay), (bx, by)] if ay == by else [(ax, ay), (mid, ay), (mid, by), (bx, by)]
        pts = [(ax, ay)] + pts if ay == by else pts
    elif route == "vh":            # leave vertically, arrive horizontally
        pts = [(ax, ay), (ax, by), (bx, by)]
    else:                          # "hv": leave horizontally, arrive vertically
        pts = [(ax, ay), (bx, ay), (bx, by)]
    pts = [p for i, p in enumerate(pts) if i == 0 or p != pts[i - 1]]
    c.setStrokeColorRGB(*colour)
    c.setFillColorRGB(*colour)
    c.setLineWidth(1.25)
    if kind in ("realize", "depend"):
        c.setDash(5, 4)
    path = c.beginPath()
    path.moveTo(*pts[0])
    for p in pts[1:]:
        path.lineTo(*p)
    c.drawPath(path, stroke=1, fill=0)
    c.setDash()
    ex, ey = pts[-1]
    px, py = pts[-2]
    dx, dy = ex - px, ey - py
    if kind == "realize":
        _triangle(c, ex, ey, dx, dy)
    else:
        _arrow(c, ex, ey, dx, dy)
    if kind in ("compose", "aggregate"):
        sx, sy = pts[0]
        nx, ny = pts[1]
        _diamond(c, sx, sy, nx - sx, ny - sy, kind == "compose")
    if label:
        if route == "v":
            lx, ly = (pts[1][0] + pts[2][0]) / 2, mid
        elif route == "h" and mid is not None:
            lx, ly = mid, (pts[1][1] + pts[-2][1]) / 2
        elif route == "vh":
            lx, ly = (pts[1][0] + pts[2][0]) / 2, pts[1][1]
        else:
            lx, ly = pts[1][0], (pts[1][1] + pts[2][1]) / 2
        c.setFont("Helvetica", 8.2)
        tw = c.stringWidth(label, "Helvetica", 8.2)
        c.setFillColorRGB(1, 1, 1)
        c.rect(lx + lab_dx - tw / 2 - 2, ly + lab_dy - 2.5, tw + 4, 11, stroke=0, fill=1)
        c.setFillColorRGB(*MUTED)
        c.drawCentredString(lx + lab_dx, ly + lab_dy, label)


def header(c, title, subtitle, n):
    c.setFillColorRGB(*INK)
    c.setFont("Helvetica-Bold", 19)
    c.drawString(28, H - 42, title)
    c.setFillColorRGB(*MUTED)
    c.setFont("Helvetica", 10.2)
    c.drawString(28, H - 59, subtitle)
    c.setFont("Helvetica", 8.8)
    c.drawRightString(W - 28, H - 42,
                      "Educational Ludo — Team Sharks   |   Task N04   |   page %d of %d" % (n, TOTAL))
    c.setStrokeColorRGB(*LINE)
    c.setLineWidth(0.7)
    c.line(28, H - 68, W - 28, H - 68)


def footnote(c, text):
    c.setFillColorRGB(*MUTED)
    c.setFont("Helvetica", 8.8)
    c.drawString(28, 40, text)


# ------------------------------------------------------------------ pages

def page1(c):
    header(c, "Educational Ludo — Sharks",
           "Class diagram of the system · Iteration 1 implemented, Iterations 2–3 planned", 1)
    c.setFillColorRGB(*INK)
    c.setFont("Helvetica", 10.5)
    c.drawString(28, H - 94, "Owner: Mohamed Zereny   ·   Reviewer: Rakan Ziyan   ·   "
                             "SRS: NFR-08 (MVC), NFR-09 (design patterns), NFR-20 (packages)")
    c.setFillColorRGB(*MUTED)
    c.setFont("Helvetica", 9.6)
    c.drawString(28, H - 110, "Every class shown as implemented was read off Iteration1/src at "
                              "iteration-1 commit 3d3ce9b. Editable source: Iteration1/docs/CLASS_DIAGRAM.md")

    m = box(c, 90, H - 160, 300, "Model", [
        "Game data and all game rules.", "No Swing, no AWT, no View, no Controller.",
        "34 classes — pages 2, 3 and 4."], "package", MODEL, BG_MODEL)
    v = box(c, 800, H - 160, 300, "View", [
        "Swing screens and reusable widgets.", "No game rules.",
        "6 screens + 15 widgets — page 5."], "package", VIEW, BG_VIEW)
    ct = box(c, 445, H - 330, 300, "Controller", [
        "Main, screen routing, the game loop.", "Connects the View to the Model.",
        "3 classes — page 6."], "package", CTRL, BG_CTRL)

    # Direction matters: both arrows point AT the Model, which is what the import rule allows.
    # Every arrow points AT the Model, which is exactly what the import rule allows.
    connect(c, v.left(), m.right(), "depend", "reads Model state", "h")
    connect(c, ct.top(-60), m.bottom(60), "depend", "reads and commands", "hv", lab_dx=-54)
    connect(c, ct.right(), v.bottom(60), "depend", "updates the screens", "hv", lab_dx=58)
    # Observer: an event, not an import. Routed on its own level so no label collides.
    connect(c, m.bottom(-80), ct.left(), "depend", "GameEvent  (Observer, NFR-09)", "vh",
            lab_dy=6, colour=IFACE)

    c.setFillColorRGB(*INK)
    c.setFont("Helvetica-Bold", 11)
    c.drawString(90, 300, "The dependency rule (NFR-08, NFR-20) is enforced by a test, not only documented")
    c.setFont("Helvetica", 9.5)
    for i, t in enumerate([
            "Model may not import View or Controller, and may not import Swing or AWT.",
            "View may not import Controller.",
            "Only Model, View and Controller exist as top-level packages.",
            "Checked by Iteration1/test/Architecture/MvcDependencyTest.java — the build fails if it "
            "is broken.",
            "The Model never names the View or the Controller. It publishes a GameEvent to whoever "
            "registered as a GameListener,",
            "and the Controller passes the change on to the screens. That is why no arrow above "
            "leaves the Model."]):
        c.setFillColorRGB(*(MUTED if i >= 3 else INK))
        c.drawString(104, 280 - i * 15, "•  " + t)

    # notation
    x, y = 800, 300
    c.setFillColorRGB(*INK)
    c.setFont("Helvetica-Bold", 11)
    c.drawString(x, y, "Notation")
    yy = y - 22
    for kind, text in [("realize", "implements an interface"),
                       ("compose", "composition (owns it, same lifetime)"),
                       ("aggregate", "aggregation (holds it, separate lifetime)"),
                       ("depend", "uses / depends on")]:
        connect(c, (x, yy), (x + 60, yy), kind, None, "h")
        c.setFillColorRGB(*INK)
        c.setFont("Helvetica", 8.8)
        c.drawString(x + 74, yy - 3, text)
        yy -= 21
    c.setStrokeColorRGB(*PLAN)
    c.setFillColorRGB(*BG_PLAN)
    c.setDash(4, 3)
    c.setLineWidth(1.1)
    c.roundRect(x, yy - 11, 60, 15, 4, stroke=1, fill=1)
    c.setDash()
    c.setFillColorRGB(*INK)
    c.setFont("Helvetica", 8.8)
    c.drawString(x + 74, yy - 7, "planned for Iteration 2 or 3, not in the code yet")


def page2(c):
    header(c, "Model — game flow",
           "The GameModel facade and the contracts frozen at checkpoint CP1, each with one "
           "Standard implementation", 2)

    gm = box(c, 380, H - 92, 420, "GameModel", [
        "+ roll() : DiceRoll                              + phase() : TurnPhase",
        "+ move(Piece) : MoveResult                 + ranking() : List<Player>",
        "+ legalMoves() : Map<Piece, Move>     + clock() : GameClock",
        "+ currentPlayer() : Player                   + addListener(GameListener)"],
        "facade", MODEL, BG_MODEL)

    cols = [60, 350, 640, 930]
    tm = box(c, cols[0], H - 250, 240, "TurnManager", [
        "+ current() : Player", "+ roll() : DiceRoll",
        "+ legalMoves() : Map<Piece, Move>", "+ move(Piece) : MoveResult",
        "+ hasBonusRoll() : boolean", "+ endTurn() : String"], "interface", IFACE, BG_IFACE)
    rt = box(c, cols[1], H - 250, 240, "RankingTracker", [
        "+ onPieceFinished(Player)", "+ isGameOver() : boolean",
        "+ ranking() : List<Player>", "+ hasFinished(Player) : boolean"], "interface", IFACE, BG_IFACE)
    di = box(c, cols[2], H - 250, 240, "Dice", [
        "+ roll(Difficulty) : DiceRoll"], "interface", IFACE, BG_IFACE)
    gl = box(c, cols[3], H - 250, 200, "GameListener", [
        "+ onGameEvent(GameEvent)"], "interface", IFACE, BG_IFACE)

    stm = box(c, cols[0], H - 430, 240, "StandardTurnManager", [
        "- dice : Dice", "- moveEngine : MoveEngine",
        "- ranking : RankingTracker", "- players, level",
        "Rule of 6, third 6, turn order", "FR-15, FR-20..FR-23, FR-43"], None, MODEL, BG_MODEL)
    srt = box(c, cols[1], H - 430, 240, "StandardRankingTracker", [
        "- finished : List<Player>", "Finishing order and game end",
        "FR-43, FR-44, FR-45"], None, MODEL, BG_MODEL)
    rd = box(c, cols[2], H - 430, 240, "RandomDice", [
        "- random : Random", "Regular die 1–6 (Easy only)",
        "FR-16, FR-17"], None, MODEL, BG_MODEL)
    gc = box(c, cols[3], H - 430, 200, "GameClock", [
        "+ start() / stop()", "+ elapsedMillis() : long",
        "+ format(long)$ : String", "FR-46"], None, MODEL, BG_MODEL)

    connect(c, stm.top(), tm.bottom(), "realize", None, "v")
    connect(c, srt.top(), rt.bottom(), "realize", None, "v")
    connect(c, rd.top(), di.bottom(), "realize", None, "v")

    connect(c, gm.bottom(-150), tm.top(), "compose", None, "v", bend=H - 215)
    connect(c, gm.bottom(-50), rt.top(), "compose", None, "v", bend=H - 225)
    connect(c, gm.bottom(150), gl.top(), "aggregate", "listeners", "v", bend=H - 215)
    connect(c, gm.right(-20), gc.top(60), "compose", "clock", "h", bend=1160, lab_dy=-6)

    c.setFillColorRGB(*INK)
    c.setFont("Helvetica-Bold", 10.5)
    c.drawString(60, 250, "Why interfaces: the six contracts above were frozen at CP1 so the three "
                          "developers could work in parallel")
    c.setFillColorRGB(*MUTED)
    c.setFont("Helvetica", 9.3)
    for i, t in enumerate([
            "Changing one of them needs a change request agreed by all three members (CONTRIBUTING.md).",
            "MoveEngine, SquareRules and HomePathRules are the other three contracts — page 3.",
            "GameSettings, Player and Piece, the data a game is built from — page 4.",
            "StandardTurnManager holds Dice, MoveEngine and RankingTracker as fields "
            "(listed in its box above)."]):
        c.drawString(72, 232 - i * 14, "•  " + t)

    footnote(c, "Plain data types, drawn where they are used: Cell, DiceRoll, Move, MoveResult, "
                "GameEvent, PlayerColor, SquareType, DieType, Direction, Difficulty, TurnPhase.")


def page3(c):
    header(c, "Model — movement rules",
           "How one legal move is worked out: the engine asks two rule objects and never decides "
           "a square rule itself", 3)

    me = box(c, 60, H - 100, 310, "MoveEngine", [
        "+ legalMoves(Player, DiceRoll)", "       : Map<Piece, Move>",
        "+ apply(Move) : MoveResult", "+ isPassable(Cell, Player) : boolean"],
        "interface", IFACE, BG_IFACE)
    sr = box(c, 440, H - 100, 310, "SquareRules", [
        "+ isProtected(Square) : boolean", "+ capturesOnLanding(Piece, Cell)",
        "       : List<Piece>"], "interface", IFACE, BG_IFACE)
    hp = box(c, 820, H - 100, 310, "HomePathRules", [
        "+ stepsFor(Piece, DiceRoll) : int", "+ canReach(Piece, int) : boolean"],
        "interface", IFACE, BG_IFACE)

    sme = box(c, 60, H - 250, 310, "StandardMoveEngine", [
        "- board : Board", "- squareRules : SquareRules",
        "- homePathRules : HomePathRules", "Castle exit on a Regular 6, steps along",
        "the owner's own path, one move per roll", "FR-19 P, FR-20, FR-25 P"], None, MODEL, BG_MODEL)
    ssr = box(c, 440, H - 250, 310, "StandardSquareRules", [
        "- board, players", "Captures a lone opponent;",
        "start, protection, surprise and finish", "squares are protected",
        "FR-26, FR-27"], None, MODEL, BG_MODEL)
    shp = box(c, 820, H - 250, 310, "StandardHomePathRules", [
        "- board", "Regular die only on the home path,",
        "exact steps to reach the finish", "FR-18, FR-31, FR-33"], None, MODEL, BG_MODEL)

    connect(c, sme.top(), me.bottom(), "realize", None, "v")
    connect(c, ssr.top(), sr.bottom(), "realize", None, "v")
    connect(c, shp.top(), hp.bottom(), "realize", None, "v")
    connect(c, sme.right(-30), ssr.left(-30), "depend", "captures?", "h")
    connect(c, ssr.right(-30), shp.left(-30), "depend", "how many steps?", "h")

    bd = box(c, 60, H - 440, 310, "Board", [
        "+ size() : int", "+ squareAt(Cell) : Square",
        "+ path(PlayerColor) : List<Cell>", "+ finishIndex() / homeStartIndex()"], None, MODEL, BG_MODEL)
    sq = box(c, 440, H - 440, 310, "Square", [
        "+ cell() : Cell", "+ type() : SquareType",
        "+ owner() : PlayerColor", "+ isProtected() : boolean"], None, MODEL, BG_MODEL)
    mv = box(c, 820, H - 440, 310, "Move  /  MoveResult", [
        "Move: piece + target progress",
        "MoveResult: kind, cells travelled,", "captures, pieceFinished, gameOver"],
        "records", MODEL, BG_MODEL)

    connect(c, sme.bottom(), bd.top(), "depend", "reads the path", "v")
    connect(c, bd.right(), sq.left(), "compose", "squares", "h")
    connect(c, shp.bottom(), mv.top(), "depend", None, "v")

    c.setFillColorRGB(*INK)
    c.setFont("Helvetica-Bold", 10.5)
    c.drawString(60, 215, "One question, three owners — this split is what let three people work "
                          "on movement at the same time")
    c.setFillColorRGB(*MUTED)
    c.setFont("Helvetica", 9.3)
    for i, t in enumerate([
            "StandardMoveEngine (Task 16, Rakan) decides whether a piece may move and applies the move.",
            "StandardSquareRules (Task 17, Shafeek) decides capture and protection.",
            "StandardHomePathRules (Task 18, Shafeek) decides the steps and the exact finish.",
            "Double Blocks (FR-28..FR-30) arrive in Iteration 2; isPassable() is already the hook for them."]):
        c.drawString(72, 197 - i * 14, "•  " + t)

    footnote(c, "The engine holds the two rule objects behind their interfaces, so either can be "
                "replaced without touching the engine.")


def page4(c):
    header(c, "Model — building a board, and the data of a game",
           "Factory Method (NFR-09) and the settings, players and pieces a game is created from", 4)

    bf = box(c, 430, H - 100, 330, "BoardFactory", [
        "+ create(Difficulty, Random)$ : Board",
        "builds the grid, the tracks, the castles,", "the home paths and the special squares"],
        "factory method", MODEL, BG_MODEL)
    lc = box(c, 60, H - 100, 300, "LevelConfig", [
        "+ of(Difficulty)$ : LevelConfig", "+ gridSize() / trackSizes()",
        "+ specialDie() : DieType", "every level value lives here (NFR-19)"], None, MODEL, BG_MODEL)
    psp = box(c, 830, H - 100, 300, "ProtectionSquarePlacer", [
        "places the random protection squares", "of a newly built board",
        "FR-11, FR-12"], None, MODEL, BG_MODEL)
    bd = box(c, 430, H - 250, 330, "Board", [
        "the finished board: squares, the four", "castles and one path per colour"],
        None, MODEL, BG_MODEL)

    connect(c, bf.left(), lc.right(), "depend", "reads", "h")
    connect(c, bf.right(), psp.left(), "depend", "uses", "h")
    connect(c, bf.bottom(), bd.top(), "depend", "creates", "v")

    df = box(c, 60, H - 250, 300, "Difficulty", [
        "EASY · MEDIUM · HARD", "asks LevelConfig for its values"], "enum", MODEL, BG_MODEL)
    connect(c, df.top(), lc.bottom(), "depend", None, "v")

    gs = box(c, 60, H - 430, 300, "GameSettings", [
        "+ validate(Difficulty, List)$", "          : List<FieldError>",
        "+ difficulty() / players()", "immutable once the game starts (FR-09)",
        "FR-04, FR-06, FR-07, FR-08"], "value", MODEL, BG_MODEL)
    pl = box(c, 430, H - 430, 330, "Player", [
        "+ name() / color() / rank()", "+ pieces() : List<Piece>",
        "+ allFinished() : boolean", "FR-06, FR-07, FR-43"], None, MODEL, BG_MODEL)
    pc = box(c, 830, H - 430, 300, "Piece", [
        "+ progress() : int   (-1 = castle)", "+ inCastle() / onBoard() / finished()",
        "FR-13"], None, MODEL, BG_MODEL)

    connect(c, pl.right(), pc.left(), "compose", "4 per player", "h")
    connect(c, gs.right(), pl.left(), "depend", "names and colours", "h")

    gm = box(c, 430, H - 560, 330, "GameModel", ["(the facade of page 2)"], "facade", MODEL, BG_MODEL)
    connect(c, gm.left(), gs.bottom(), "compose", "settings", "h", bend=210)
    connect(c, gm.top(), pl.bottom(), "aggregate", "2..4 players", "v")
    connect(c, gm.right(), bd.right(20), "compose", "board", "h", bend=1160)

    footnote(c, "Factory Method: callers ask BoardFactory for a board of a level and never build one "
                "themselves, so Medium and Hard need no change at the call sites (NFR-09, NFR-19).")


def page5(c):
    header(c, "View — screens and widgets",
           "Four screens behind one interface; the board widget draws the Model and never decides "
           "a game rule", 5)

    sv = box(c, 450, H - 96, 300, "ScreenView", [
        "+ onShow()", "+ defaultButton() : JButton"], "interface", IFACE, BG_IFACE)

    mm = box(c, 40, H - 215, 255, "MainMenuView", [
        "New Game, Game History,", "Question Management, Exit",
        "FR-01, FR-02 P"], None, VIEW, BG_VIEW)
    gsv = box(c, 325, H - 215, 255, "GameSetupView", [
        "2–4 players, names, colours;", "Easy only in Iteration 1",
        "FR-04, FR-05 P, FR-08"], None, VIEW, BG_VIEW)
    gbv = box(c, 610, H - 215, 255, "GameBoardView", [
        "board, dice, turn card,", "player list, elapsed time",
        "FR-15, FR-24, FR-46"], None, VIEW, BG_VIEW)
    grv = box(c, 895, H - 215, 255, "GameResultsView", [
        "podium, level, duration", "FR-45, FR-47"], None, VIEW, BG_VIEW)

    for b in (mm, gsv, gbv, grv):
        connect(c, b.top(), (sv.cx + (b.cx - sv.cx) * 0.20, sv.y), "realize", None, "v", bend=H - 180)

    bc = box(c, 325, H - 400, 340, "BoardComponent", [
        "+ setGame(GameModel)", "+ setTargets(Map<Piece, Integer>)",
        "+ clearTargets()", "+ animateAlong(Piece, List<Cell>, Runnable)",
        "draws the board, highlights the legal", "pieces and animates a move"],
        None, VIEW, BG_VIEW)
    dc = box(c, 715, H - 400, 230, "DiceComponent", [
        "draws one die and its value"], None, VIEW, BG_VIEW)
    an = box(c, 985, H - 400, 165, "Animations", [
        "Swing Timer frames", "NFR-04"], None, VIEW, BG_VIEW)
    cd = box(c, 40, H - 400, 250, "ConfirmDialog", [
        "leave the game, exit", "FR-03"], None, VIEW, BG_VIEW)

    connect(c, gbv.bottom(-40), bc.top(80), "compose", None, "v", bend=H - 245)
    connect(c, gbv.bottom(40), dc.top(), "compose", "2", "v", bend=H - 258)
    connect(c, mm.bottom(), bc.top(-120), "compose", "decorative preview", "v", bend=H - 300)
    connect(c, bc.right(-40), an.left(-40), "depend", None, "h", bend=975)

    gm = box(c, 325, H - 540, 340, "GameModel", ["(Model package)"], "reads only", MODEL, BG_MODEL)
    connect(c, bc.bottom(), gm.top(), "depend", "reads state, never changes it", "v")

    box(c, 715, H - 540, 435, "View.components  —  15 reusable widgets", [
        "Reused from the Assignment 3 prototype through the reuse gate (SDP 6.1):",
        "Animations, BoardComponent, ColorSwatch, Confetti, CustomButton,",
        "CustomPanel, DiceComponent, GradientBackground, Icons, ModalOverlay,",
        "StyledTable, StyledTextField, Theme, ToggleChip, UiKit"], "package", VIEW, BG_VIEW)

    footnote(c, "The View imports Model but never Controller. The Controller registers the listeners "
                "that refresh these screens, so the Model stays free of any screen.")


def page6(c):
    header(c, "Controller — entry point, routing and the game loop",
           "The only package that knows both the screens and the Model", 6)

    mn = box(c, 60, H - 110, 270, "Main", [
        "+ main(String[])$", "+ createApp()$ : NavigationController",
        "starts Swing on the event thread", "NFR-07"], None, CTRL, BG_CTRL)
    nc = box(c, 410, H - 110, 350, "NavigationController", [
        "+ showMainMenu() / showSetup()", "+ startGame(GameSettings)",
        "+ showResults(List, Difficulty, String)", "+ confirmLeaveGame(Runnable)",
        "owns the window and every screen", "FR-01, FR-02 P, FR-03"], None, CTRL, BG_CTRL)
    gc = box(c, 840, H - 110, 290, "GameController", [
        "+ newGame(GameSettings, Random)", "+ stop()",
        "roll, highlight, move, animate", "FR-20, FR-24, NFR-04"], None, CTRL, BG_CTRL)

    connect(c, mn.right(), nc.left(), "depend", "creates", "h")
    connect(c, nc.right(), gc.left(), "compose", None, "h")
    connect(c, gc.bottom(-60), nc.bottom(60), "depend", "asks for dialogs", "v", bend=H - 215)

    scr = box(c, 410, H - 285, 350, "GameBoardView  ·  MainMenuView", [
        "GameSetupView  ·  GameResultsView"], "View package", VIEW, BG_VIEW)
    gm = box(c, 840, H - 285, 290, "GameModel", ["the Model facade"], "Model package", MODEL, BG_MODEL)

    connect(c, nc.bottom(-80), scr.top(-80), "compose", "shows", "v")
    connect(c, gc.bottom(60), gm.top(60), "depend", "commands", "v")

    c.setFillColorRGB(*INK)
    c.setFont("Helvetica-Bold", 12)
    c.drawString(60, H - 378, "One turn, end to end — the Observer pattern at work (NFR-09)")
    steps = [
        "The player presses Roll on GameBoardView.",
        "GameController calls GameModel.roll().",
        "The Model works the turn out and publishes GameEvent ROLLED — and TURN_PASSED plus "
        "TURN_CHANGED when nothing can move (FR-23).",
        "GameController, registered as a GameListener, hears the event and asks the Model for the "
        "legal moves.",
        "It hands them to BoardComponent.setTargets(...), which highlights exactly those pieces (FR-24).",
        "The player clicks a highlighted piece. A piece that is not highlighted cannot be clicked at all.",
        "GameController calls GameModel.move(piece), animates the result square by square (NFR-04) "
        "and clears the highlight.",
        "At the end the Model publishes PLAYER_FINISHED and GAME_OVER, and the Controller shows "
        "GameResultsView with the ranking and the duration (FR-45, FR-46, FR-47).",
    ]
    y = H - 404
    for i, t in enumerate(steps):
        c.setFillColorRGB(*CTRL)
        c.setFont("Helvetica-Bold", 10)
        c.drawString(72, y, "%d." % (i + 1))
        c.setFillColorRGB(*INK)
        c.setFont("Helvetica", 10)
        c.drawString(92, y, t)
        y -= 19

    footnote(c, "Step 3 is the reason no arrow points from the Model to the View or the Controller: "
                "the Model publishes an event, it never calls a screen.")


def page7(c):
    header(c, "Design patterns, and what comes next",
           "NFR-09 · Factory Method, Observer, State and Singleton · the classes planned for "
           "Iterations 2 and 3", 7)

    rows = [
        ("Factory Method", "BoardFactory.create(Difficulty, Random)",
         "Builds the whole board for a level: grid, tracks, castles, home paths and the random",
         "protection squares. All level data comes from LevelConfig.",
         "Implemented — Iteration 1", CTRL),
        ("Observer", "GameModel + GameListener + GameEvent",
         "GameModel publishes STATE_CHANGED, ROLLED, MOVED, TURN_PASSED, TURN_CHANGED,",
         "PLAYER_FINISHED and GAME_OVER. This is what keeps the Model free of any View import.",
         "Implemented — Iteration 1", CTRL),
        ("State", "TurnPhase + StandardTurnManager",
         "AWAITING_ROLL → AWAITING_MOVE → MOVING or AWAITING_ROLL → GAME_OVER.",
         "Today an enum, with the transitions written inside the turn manager.",
         "Partly — becomes State classes in Iteration 2", VIEW),
        ("Singleton", "GameHistory  (planned)",
         "One shared access point to the game-history CSV, so the file cannot be opened twice",
         "at the same time.",
         "Planned — Iteration 3", PLAN),
    ]
    y = H - 96
    for name, where, l1, l2, status, colour in rows:
        c.setStrokeColorRGB(*LINE)
        c.setFillColorRGB(0.975, 0.98, 0.99)
        c.setLineWidth(0.9)
        c.roundRect(28, y - 58, 1135, 56, 4, stroke=1, fill=1)
        c.setFillColorRGB(*INK)
        c.setFont("Helvetica-Bold", 11)
        c.drawString(42, y - 20, name)
        c.setFillColorRGB(*MODEL)
        c.setFont("Courier-Bold", 8.6)
        c.drawString(42, y - 37, where)
        c.setFillColorRGB(*INK)
        c.setFont("Helvetica", 9.2)
        c.drawString(330, y - 20, l1)
        c.drawString(330, y - 33, l2)
        c.setFillColorRGB(*colour)
        c.setFont("Helvetica-Bold", 9.3)
        c.drawRightString(1150, y - 20, status)
        y -= 64

    c.setFillColorRGB(*INK)
    c.setFont("Helvetica-Bold", 12)
    c.drawString(28, y - 6, "Planned for Iterations 2 and 3 — and where each new class attaches")

    ts = box(c, 40, y - 26, 255, "TurnState", [
        "+ onRoll() / onMove()", "replaces the TurnPhase enum",
        "attaches to StandardTurnManager"], "planned · I2", PLAN, BG_PLAN, True)
    qb = box(c, 325, y - 26, 255, "QuestionBank  /  Question", [
        "+ nextQuestion(Difficulty)", "FR-34..FR-38",
        "attaches to StandardMoveEngine"], "planned · I2", PLAN, BG_PLAN, True)
    sru = box(c, 610, y - 26, 255, "SurpriseRules", [
        "+ outcomeFor(Piece)", "FR-39..FR-42",
        "attaches to StandardMoveEngine"], "planned · I2", PLAN, BG_PLAN, True)
    gh = box(c, 895, y - 26, 255, "GameHistory  /  GameRecord", [
        "+ instance()$ : GameHistory", "+ save(GameRecord) / all()",
        "attaches to GameModel"], "planned · I3 · singleton", PLAN, BG_PLAN, True)

    c.setFillColorRGB(*INK)
    c.setFont("Helvetica", 9.4)
    c.drawString(28, 96, "Iteration 2 also adds: Double Block rules inside StandardMoveEngine, Boost "
                         "and UltraBoost in RandomDice, the Medium and Hard boards, and SurpriseOutcome.")
    c.drawString(28, 80, "Iteration 3 also adds: CSV storage for finished games, AdminAuthenticator "
                         "and the question-management screens.")
    footnote(c, "None of the four classes on this row exists in the code yet. They are drawn dashed, "
                "to show only where they will attach.")


def main(out):
    c = canvas.Canvas(out, pagesize=landscape(A3))
    c.setTitle("Educational Ludo - Sharks - Class Diagram (Task N04)")
    c.setAuthor("Mohamed Zereny - Team Sharks")
    c.setSubject("Class diagram of the system: Iteration 1 implemented, Iterations 2-3 planned "
                 "(NFR-08, NFR-09, NFR-20)")
    for fn in (page1, page2, page3, page4, page5, page6, page7):
        fn(c)
        c.showPage()
    c.save()
    print("wrote", out)


if __name__ == "__main__":
    main(sys.argv[1])
