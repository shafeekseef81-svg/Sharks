package devtools;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import components.Animations;
import components.ModalOverlay;
import controller.NavigationController;
import controller.PrototypeGameController;
import data.DemoData;
import main.Main;
import model.AdminAuthenticator;
import model.Board;
import model.BoardFactory;
import model.Cell;
import model.DiceRoll;
import model.Difficulty;
import model.DieType;
import model.GameHistory;
import model.GameRecord;
import model.GameSettings;
import model.GameState;
import model.MoveResult;
import model.Piece;
import model.Player;
import model.PlayerColor;
import model.Question;
import model.QuestionBank;
import model.Square;
import model.SquareType;
import model.SurpriseOutcome;
import model.TurnPhase;
import view.QuestionDialog;
import view.QuestionFormDialog;

/**
 * Automatic checks of the prototype: board configurations, dice, light rules, question bank,
 * history, login, and the main user flows driven through the real screens (offscreen).
 *
 * <p>Usage: {@code java -jar EducationalLudo.jar --selftest}; the exit code is 0 when everything passes.
 */
public final class SelfTest {

    private int passed;
    private final List<String> failures = new ArrayList<>();
    private String section = "";

    public static int run() throws Exception {
        Animations.instant = true;
        SelfTest t = new SelfTest();
        t.modelTests();
        Throwable[] crash = new Throwable[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                t.uiTests();
            } catch (Throwable e) {
                crash[0] = e;
            }
        });
        if (crash[0] != null) {
            crash[0].printStackTrace();
            t.check("UI tests finished without an exception", false, crash[0].toString());
        }
        System.out.println();
        System.out.println("Self-test: " + t.passed + " passed, " + t.failures.size() + " failed");
        for (String f : t.failures) {
            System.out.println("  FAILED: " + f);
        }
        return t.failures.isEmpty() ? 0 : 1;
    }

    private void section(String name) {
        section = name;
        System.out.println("[" + name + "]");
    }

    private void check(String name, boolean ok) {
        check(name, ok, "");
    }

    private void check(String name, boolean ok, String detail) {
        if (ok) {
            passed++;
            System.out.println("  ok    " + name);
        } else {
            failures.add(section + ": " + name + (detail.isEmpty() ? "" : " (" + detail + ")"));
            System.out.println("  FAIL  " + name + (detail.isEmpty() ? "" : " (" + detail + ")"));
        }
    }

    // ================================================================== model

    private void modelTests() {
        boards();
        dice();
        rules();
        setupValidation();
        questions();
        history();
        authentication();
    }

    private void boards() {
        section("Board configurations (SRS Table 1)");
        int[][] expected = {
                // track, protection, arrows, questions, surprise, path length
                {52, 4, 4, 0, 0, 57},
                {116, 8, 8, 6, 2, 116},
                {188, 12, 12, 9, 3, 188},
        };
        for (Difficulty d : Difficulty.values()) {
            int[] e = expected[d.ordinal()];
            boolean countsOk = true;
            boolean levelsOk = true;
            boolean placementOk = true;
            boolean pathsOk = true;
            String detail = "";
            for (long seed = 1; seed <= 25; seed++) {
                Board b = BoardFactory.create(d, new Random(seed));
                if (b.trackSquareCount() != e[0] || b.count(SquareType.PROTECTION) != e[1]
                        || b.count(SquareType.ARROW) != e[2] || b.count(SquareType.QUESTION) != e[3]
                        || b.count(SquareType.SURPRISE) != e[4] || b.count(SquareType.START) != 4
                        || b.count(SquareType.HOME_PATH) != 20) {
                    countsOk = false;
                    detail = "seed " + seed;
                }
                for (Difficulty level : Difficulty.values()) {
                    if (b.countQuestions(level) != d.questionsPerLevel()) {
                        levelsOk = false;
                    }
                }
                for (Square s : b.squares()) {
                    if (s.type() == SquareType.HOME_PATH && s.ring() != -1) {
                        placementOk = false;
                    }
                }
                for (PlayerColor c : PlayerColor.values()) {
                    List<Cell> path = b.path(c);
                    if (path.size() != e[5] || new HashSet<>(path).size() != path.size()) {
                        pathsOk = false;
                    }
                    for (int i = 1; i < path.size(); i++) {
                        Cell a = path.get(i - 1);
                        Cell z = path.get(i);
                        if (Math.max(Math.abs(a.row() - z.row()), Math.abs(a.col() - z.col())) != 1) {
                            pathsOk = false;
                        }
                    }
                }
            }
            check(d.label() + ": " + e[0] + " track squares, " + e[1] + " protection, " + e[2] + " arrows, " + e[3]
                    + " questions, " + e[4] + " surprises, 4 starts, 6-square home paths (25 random boards)", countsOk, detail);
            check(d.label() + ": question squares split evenly between the three question levels", levelsOk);
            check(d.label() + ": special squares only on shared tracks", placementOk);
            check(d.label() + ": every colour has a continuous path of " + e[5] + " positions", pathsOk);
        }
        Board easy = BoardFactory.create(Difficulty.EASY, new Random(1));
        check("Easy start squares match the official diagram (green (6,1), yellow (1,8), blue (8,13), red (13,6))",
                easy.startCell(PlayerColor.GREEN).equals(new Cell(6, 1)) && easy.startCell(PlayerColor.YELLOW).equals(new Cell(1, 8))
                        && easy.startCell(PlayerColor.BLUE).equals(new Cell(8, 13)) && easy.startCell(PlayerColor.RED).equals(new Cell(13, 6)));
        Board hard = BoardFactory.create(Difficulty.HARD, new Random(1));
        boolean staircase = hard.squareAt(new Cell(7, 0)).type() == SquareType.ARROW
                && hard.squareAt(new Cell(8, 1)).type() == SquareType.ARROW
                && hard.squareAt(new Cell(9, 2)).type() == SquareType.ARROW
                && hard.squareAt(new Cell(9, 2)).owner() == PlayerColor.RED;
        check("Hard red arrows at (7,0), (8,1), (9,2) as in the official diagram", staircase);
        check("Hard start squares are the outer corners", hard.startCell(PlayerColor.RED).equals(new Cell(0, 0))
                && hard.startCell(PlayerColor.YELLOW).equals(new Cell(18, 18)));
    }

    private void dice() {
        section("Dice (FR-16, FR-17, NFR-13)");
        for (Difficulty d : Difficulty.values()) {
            GameState s = game(d, 2, 99);
            int[] regular = new int[7];
            int[] special = new int[7];
            boolean formula = true;
            int n = 10_000;
            for (int i = 0; i < n; i++) {
                DiceRoll r = s.roll();
                regular[r.regular()]++;
                if (r.hasSpecial()) {
                    special[r.special()]++;
                }
                int expected = d.specialDie() == DieType.NONE ? r.regular() : r.regular() * (r.special() - 1);
                if (r.movement() != expected || r.specialType() != d.specialDie()) {
                    formula = false;
                }
            }
            check(d.label() + ": movement = " + (d.specialDie() == DieType.NONE ? "regular die"
                    : "regular x (" + d.specialDie().label() + " - 1)"), formula);
            boolean uniform = true;
            for (int v = 1; v <= 6; v++) {
                if (Math.abs(regular[v] * 100.0 / n - 100.0 / 6) > 2) {
                    uniform = false;
                }
            }
            check(d.label() + ": regular die values 1-6 within 2 percentage points", uniform, Arrays.toString(regular));
            if (d.specialDie() != DieType.NONE) {
                int faces = d.specialDie().faces();
                boolean specialOk = special[0] == 0;
                for (int v = 1; v <= 6; v++) {
                    double share = special[v] * 100.0 / n;
                    if (v <= faces ? Math.abs(share - 100.0 / faces) > 2 : special[v] != 0) {
                        specialOk = false;
                    }
                }
                check(d.label() + ": " + d.specialDie().label() + " die values 1-" + faces + " within 2 percentage points",
                        specialOk, Arrays.toString(special));
            }
        }
        Random r = new Random(5);
        int[] counts = new int[SurpriseOutcome.values().length];
        int n = 10_000;
        for (int i = 0; i < n; i++) {
            counts[SurpriseOutcome.draw(r).ordinal()]++;
        }
        boolean table2 = true;
        for (SurpriseOutcome o : SurpriseOutcome.values()) {
            if (Math.abs(counts[o.ordinal()] * 100.0 / n - o.percent()) > 2) {
                table2 = false;
            }
        }
        check("Surprise outcomes follow Table 2 (10/10/10/30/30/10 %) within 2 percentage points", table2,
                Arrays.toString(counts));
    }

    private void rules() {
        section("Light game rules");
        GameState s = game(Difficulty.EASY, 2, 3);
        Player p1 = s.currentPlayer();
        s.forceNextRegular(5);
        s.roll();
        check("Without a 6 no piece can leave the castle (FR-19)", s.legalTargets().isEmpty());
        s.endTurn();
        check("Turn passes to the next player in setup order (FR-15)", s.currentPlayer() != p1);
        s.endTurn();
        s.forceNextRegular(6);
        s.roll();
        check("A 6 lets every castle piece move to the start square", s.legalTargets().size() == 4
                && s.legalTargets().values().stream().allMatch(t -> t == 0));
        Piece piece = p1.pieces().get(0);
        MoveResult exit = s.movePiece(piece);
        check("Moving out of the castle puts the piece on its start square",
                exit.kind() == MoveResult.Kind.EXIT_CASTLE && piece.progress() == 0);
        check("A 6 grants an extra roll (FR-21)", s.hasBonusRoll());
        s.forceNextRegular(4);
        s.roll();
        s.movePiece(piece);
        check("The piece moves by the regular die on Easy", piece.progress() == 4 && !s.hasBonusRoll());
        s.forceNextRegular(6);
        s.roll();
        s.forceNextRegular(6);
        s.roll();
        s.forceNextRegular(6);
        DiceRoll third = s.roll();
        check("The third 6 in a row is cancelled (FR-22)", third.cancelled() && s.legalTargets().isEmpty()
                && !s.hasBonusRoll());

        GameState m = game(Difficulty.MEDIUM, 2, 4);
        Piece home = m.currentPlayer().pieces().get(0);
        home.setProgress(m.board().homeStartIndex() + 1);
        m.forceNextRoll(2, 3);
        m.roll();
        check("On the home path a piece moves by the regular die only (FR-18)",
                m.legalTargets().get(home) == m.board().homeStartIndex() + 3);
        home.setProgress(m.board().finishIndex() - 1);
        m.forceNextRoll(3, 2);
        m.roll();
        check("Finishing needs exact steps (FR-31)", !m.legalTargets().containsKey(home));
        m.forceNextRoll(4, 1);
        m.roll();
        check("Special die 1 gives 0 steps and no move (FR-17)", m.lastRoll().movement() == 0);

        GameState c = game(Difficulty.EASY, 2, 8);
        Player a = c.players().get(0);
        Player b = c.players().get(1);
        Piece attacker = a.pieces().get(0);
        Piece victim = b.pieces().get(0);
        Cell target = null;
        int victimProgress = -1;
        for (int i = 4; i < 40 && target == null; i++) {
            Cell cell = c.board().cellAt(a.color(), i);
            Square sq = c.board().squareAt(cell);
            int vp = c.board().path(b.color()).indexOf(cell);
            if (sq != null && !sq.isProtected() && sq.type() != SquareType.QUESTION && vp > 0) {
                target = cell;
                attacker.setProgress(i - 3);
                victimProgress = vp;
            }
        }
        victim.setProgress(victimProgress);
        c.forceNextRegular(3);
        c.roll();
        MoveResult cap = c.movePiece(attacker);
        check("Landing on a lone opponent on an unprotected square captures it (FR-26)",
                cap.captures().size() == 1 && victim.inCastle());
        c.skipNextTurn(b);
        List<Player> skipped = c.endTurn();
        check("A skipped player loses exactly one turn", skipped.contains(b) && c.currentPlayer() == a && !b.skipNextTurn());
        Piece back = a.pieces().get(1);
        back.setProgress(1);
        MoveResult backward = c.shiftPiece(back, -3);
        check("Backward movement stops at the start square (FR-32)", back.progress() == 0 && backward.toProgress() == 0);

        GameState f = game(Difficulty.EASY, 3, 6);
        Player finisher = f.currentPlayer();
        for (int i = 0; i < 3; i++) {
            finisher.pieces().get(i).setProgress(f.board().finishIndex());
        }
        Piece last = finisher.pieces().get(3);
        last.setProgress(f.board().finishIndex() - 6);
        f.forceNextRegular(6);
        f.roll();
        MoveResult finishMove = f.movePiece(last);
        check("Finishing the last piece with a 6 ranks the player and gives no extra roll (FR-43)",
                finishMove.playerFinished() && finisher.rank() == 1 && !f.hasBonusRoll());
        f.endTurn();
        f.endTurn();
        f.endTurn();
        check("A finished player is skipped in the turn order (FR-43)", f.currentPlayer() != finisher);
    }

    private void setupValidation() {
        section("Game setup validation (FR-04..FR-08)");
        check("Empty name is rejected", !GameSettings.validate(Difficulty.EASY, List.of(
                new GameSettings.PlayerSetup("", PlayerColor.RED), new GameSettings.PlayerSetup("Omer", PlayerColor.BLUE))).isEmpty());
        check("13 characters are rejected", !GameSettings.validate(Difficulty.EASY, List.of(
                new GameSettings.PlayerSetup("ABCDEFGHIJKLM", PlayerColor.RED),
                new GameSettings.PlayerSetup("Omer", PlayerColor.BLUE))).isEmpty());
        check("Duplicate names are rejected (case-insensitive)", !GameSettings.validate(Difficulty.EASY, List.of(
                new GameSettings.PlayerSetup("Noa", PlayerColor.RED), new GameSettings.PlayerSetup("noa", PlayerColor.BLUE))).isEmpty());
        check("Duplicate colours are rejected", !GameSettings.validate(Difficulty.EASY, List.of(
                new GameSettings.PlayerSetup("Noa", PlayerColor.RED), new GameSettings.PlayerSetup("Omer", PlayerColor.RED))).isEmpty());
        check("Missing difficulty is rejected", !GameSettings.validate(null, List.of(
                new GameSettings.PlayerSetup("Noa", PlayerColor.RED), new GameSettings.PlayerSetup("Omer", PlayerColor.BLUE))).isEmpty());
        check("A single player is rejected", !GameSettings.validate(Difficulty.EASY, List.of(
                new GameSettings.PlayerSetup("Noa", PlayerColor.RED))).isEmpty());
        check("12 characters are accepted", GameSettings.validate(Difficulty.HARD, List.of(
                new GameSettings.PlayerSetup("ABCDEFGHIJKL", PlayerColor.RED),
                new GameSettings.PlayerSetup("Omer", PlayerColor.BLUE))).isEmpty());
    }

    private void questions() {
        section("Question bank (FR-56..FR-61)");
        QuestionBank bank = new QuestionBank(DemoData.questions());
        check("At least 30 sample questions on all three levels", bank.size() >= 30
                && bank.count(Difficulty.EASY) > 0 && bank.count(Difficulty.MEDIUM) > 0 && bank.count(Difficulty.HARD) > 0,
                bank.size() + " questions");
        boolean answersOk = bank.all().stream().allMatch(q ->
                QuestionBank.validate(q.text(), q.answers(), q.correctIndex(), q.level()).isValid());
        check("Every sample question has four different answers and one correct answer", answersOk);
        check("Empty text is rejected", !QuestionBank.validate(" ", new String[] {"a", "b", "c", "d"}, 0, Difficulty.EASY).isValid());
        check("A missing answer is rejected", !QuestionBank.validate("Q?", new String[] {"a", "", "c", "d"}, 0, Difficulty.EASY).isValid());
        check("Duplicate answers are rejected", !QuestionBank.validate("Q?", new String[] {"a", "b", "A", "d"}, 0, Difficulty.EASY).isValid());
        check("A missing correct answer is rejected", !QuestionBank.validate("Q?", new String[] {"a", "b", "c", "d"}, -1, Difficulty.EASY).isValid());
        check("A missing level is rejected", !QuestionBank.validate("Q?", new String[] {"a", "b", "c", "d"}, 1, null).isValid());
        Question added = bank.add("New?", new String[] {"a", "b", "c", "d"}, 2, Difficulty.HARD);
        check("Adding gives a new unique ID", added.id().equals("Q-037") && bank.size() == 37, added.id());
        bank.update(added, "Changed?", new String[] {"w", "x", "y", "z"}, 3, Difficulty.EASY);
        check("Editing changes every field", added.text().equals("Changed?") && added.correctAnswer().equals("z")
                && added.level() == Difficulty.EASY);
        while (bank.canDelete()) {
            bank.delete(bank.all().get(0));
        }
        check("Deleting stops at 30 questions (FR-60)", bank.size() == QuestionBank.MINIMUM_QUESTIONS);
        QuestionBank.Deck deck = new QuestionBank(DemoData.questions()).newDeck(new Random(2));
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 12; i++) {
            seen.add(deck.next(Difficulty.MEDIUM).id());
        }
        check("No question repeats until all of its level were asked (FR-37)", seen.size() == 12);
    }

    private void history() {
        section("Game history (FR-48..FR-50, FR-63)");
        GameHistory h = GameHistory.getInstance();
        h.load(DemoData.history());
        List<GameRecord> all = h.newestFirst(null);
        boolean sorted = true;
        for (int i = 1; i < all.size(); i++) {
            if (all.get(i).timestamp().isAfter(all.get(i - 1).timestamp())) {
                sorted = false;
            }
        }
        check("Sample records are listed newest first", sorted && all.size() == 14);
        check("Filter by difficulty", h.newestFirst(Difficulty.HARD).stream().allMatch(r -> r.difficulty() == Difficulty.HARD)
                && !h.newestFirst(Difficulty.HARD).isEmpty());
        check("Timestamp uses DD/MM/YYYY; HH:MM:SS", all.get(0).timestampText().matches("\\d{2}/\\d{2}/\\d{4}; \\d{2}:\\d{2}:\\d{2}"),
                all.get(0).timestampText());
        check("Two-player games leave places 3 and 4 empty", h.newestFirst(null).stream()
                .filter(r -> r.places().size() == 2).allMatch(r -> r.place(2).isEmpty() && r.place(3).isEmpty()));
    }

    private void authentication() {
        section("Administrator login (FR-51..FR-54, NFR-17)");
        long[] now = {1_000_000};
        AdminAuthenticator auth = new AdminAuthenticator("admin", "Sharks2026", () -> now[0]);
        check("Wrong password fails", auth.login("admin", "x".toCharArray()) == AdminAuthenticator.Result.FAILURE);
        check("Wrong username fails", auth.login("root", "Sharks2026".toCharArray()) == AdminAuthenticator.Result.FAILURE);
        check("Third failure locks the login", auth.login("admin", "y".toCharArray()) == AdminAuthenticator.Result.LOCKED);
        check("Correct password is refused while locked", auth.login("admin", "Sharks2026".toCharArray())
                == AdminAuthenticator.Result.LOCKED);
        now[0] += 30_001;
        check("Login works again after 30 seconds", auth.login("admin", "Sharks2026".toCharArray())
                == AdminAuthenticator.Result.SUCCESS && auth.isSignedIn());
        auth.logout();
        check("Logout ends the session", !auth.isSignedIn());
    }

    private static GameState game(Difficulty d, int players, long seed) {
        PlayerColor[] colors = PlayerColor.values();
        String[] names = {"Noa", "Omer", "Yara", "Adam"};
        List<GameSettings.PlayerSetup> list = new ArrayList<>();
        for (int i = 0; i < players; i++) {
            list.add(new GameSettings.PlayerSetup(names[i], colors[i]));
        }
        return new GameState(new GameSettings(d, list), new QuestionBank(DemoData.questions()), new Random(seed));
    }

    // ================================================================== UI flows

    private void uiTests() {
        NavigationController nav = Main.createApp();
        boolean[] exited = {false};
        nav.setExitHandler(() -> exited[0] = true);
        JFrame frame = nav.frame();
        frame.getRootPane().setPreferredSize(new java.awt.Dimension(1264, 641));
        frame.pack();

        section("Main menu and navigation (FR-01..FR-03)");
        check("Application starts on the main menu", NavigationController.MENU.equals(nav.currentScreen()));
        UiFinder.click(frame, "menu.newGame");
        check("New Game opens Game Setup", NavigationController.SETUP.equals(nav.currentScreen()));
        UiFinder.click(frame, "setup.back");
        check("Back returns to the main menu", NavigationController.MENU.equals(nav.currentScreen()));
        UiFinder.click(frame, "menu.history");
        check("Game History opens the history screen", NavigationController.HISTORY.equals(nav.currentScreen()));
        UiFinder.click(frame, "history.back");
        UiFinder.click(frame, "menu.questions");
        check("Question Management asks for the administrator login first",
                NavigationController.LOGIN.equals(nav.currentScreen()));
        UiFinder.click(frame, "login.back");
        UiFinder.click(frame, "menu.exit");
        check("Exit asks for confirmation", ModalOverlay.openCount() == 1 && !exited[0]);
        UiFinder.click(frame, "dialog.confirm");
        check("Confirming Exit closes the application", exited[0] && ModalOverlay.openCount() == 0);

        section("Game setup screen (FR-04..FR-09)");
        nav.showSetup();
        nav.setupView().setPlayerCount(2);
        nav.setupView().setPlayerName(0, "");
        nav.setupView().setPlayerName(1, "");
        UiFinder.click(frame, "setup.start");
        check("Empty names keep the user on the setup screen with messages",
                NavigationController.SETUP.equals(nav.currentScreen()) && nav.setupView().visibleErrors().size() == 2,
                nav.setupView().visibleErrors().toString());
        nav.setupView().setPlayerName(0, "Noa");
        nav.setupView().setPlayerName(1, "NOA");
        UiFinder.click(frame, "setup.start");
        check("Duplicate names show a message", nav.setupView().visibleErrors().size() == 1
                && nav.setupView().visibleErrors().get(0).contains("already uses"));
        nav.setupView().setPlayerName(1, "ABCDEFGHIJKLMNOP");
        check("Names are limited to 12 characters while typing",
                UiFinder.find(frame, "setup.name.1", javax.swing.JTextField.class).getText().length() == 12);
        String[] names = {"Noa", "Omer", "Yara", "Adam"};
        for (int count = 2; count <= 4; count++) {
            for (Difficulty d : Difficulty.values()) {
                nav.showSetup();
                nav.setupView().setPlayerCount(count);
                nav.setupView().setDifficulty(d);
                for (int i = 0; i < count; i++) {
                    nav.setupView().setPlayerName(i, names[i]);
                }
                UiFinder.click(frame, "setup.start");
                GameState s = nav.game().state();
                boolean ok = NavigationController.GAME.equals(nav.currentScreen()) && s.players().size() == count
                        && s.difficulty() == d && s.players().stream().allMatch(p -> p.pieces().stream().allMatch(Piece::inCastle));
                check(count + " players, " + d.label() + ": game board opens with all pieces in the castles", ok);
                UiFinder.click(frame, "game.back");
                boolean asked = ModalOverlay.openCount() == 1;
                UiFinder.click(frame, "dialog.confirm");
                if (count == 4 && d == Difficulty.HARD) {
                    check("Leaving a running game asks for confirmation and returns to the menu (FR-03)",
                            asked && NavigationController.MENU.equals(nav.currentScreen()));
                }
            }
        }

        section("Dice and piece movement (FR-15..FR-24)");
        nav.startGame(new GameSettings(Difficulty.EASY, List.of(new GameSettings.PlayerSetup("Noa", PlayerColor.RED),
                new GameSettings.PlayerSetup("Omer", PlayerColor.BLUE))), new Random(12));
        PrototypeGameController game = nav.game();
        GameState s = game.state();
        Player first = s.currentPlayer();
        s.forceNextRegular(6);
        UiFinder.click(frame, "game.roll");
        Map<Piece, Integer> targets = nav.gameView().board().targets();
        check("Roll Dice shows the result and highlights the four castle pieces", targets.size() == 4
                && nav.gameView().regularDie().getValue() == 6 && s.phase() == TurnPhase.AWAITING_MOVE);
        Piece piece = first.pieces().get(0);
        frame.validate();
        Point at = nav.gameView().board().screenPosition(piece);
        check("Clicking the screen position of a piece hits that piece", nav.gameView().board().pieceAt(at) == piece);
        game.pieceClicked(piece);
        check("Clicking a highlighted piece moves it to the start square", piece.progress() == 0);
        check("After a 6 the same player rolls again", s.currentPlayer() == first && s.phase() == TurnPhase.AWAITING_ROLL);
        s.forceNextRegular(3);
        UiFinder.click(frame, "game.roll");
        game.pieceClicked(piece);
        check("The piece moves three squares and the turn passes to the next player",
                piece.progress() == 3 && s.currentPlayer() != first);
        check("The status line tells the next player to roll", nav.gameView().statusText().contains("roll"));

        section("Educational question (FR-34..FR-36)");
        game.demoQuestion();
        check("Easy has no question squares: the demo Question button is off and opens nothing",
                !nav.gameView().demoQuestionButton().isEnabled() && game.lastQuestion() == null
                        && ModalOverlay.openCount() == 0);
        game.demoSurprise();
        check("Easy has no surprise squares: the demo Surprise button is off and opens nothing",
                !nav.gameView().demoSurpriseButton().isEnabled() && game.lastSurprise() == null
                        && ModalOverlay.openCount() == 0);
        nav.startGame(new GameSettings(Difficulty.MEDIUM, List.of(new GameSettings.PlayerSetup("Noa", PlayerColor.RED),
                new GameSettings.PlayerSetup("Omer", PlayerColor.BLUE))), new Random(12));
        game = nav.game();
        s = game.state();
        check("Medium: the demo Question button is on", nav.gameView().demoQuestionButton().isEnabled());
        Player asker = s.currentPlayer();
        Piece qp = asker.pieces().get(0);
        qp.setProgress(5);
        game.demoQuestion();
        QuestionDialog dialog = game.lastQuestion();
        check("Demo question opens the question dialog", dialog != null && ModalOverlay.openCount() == 1);
        int steps = dialog.question().level().questionSteps();
        check("Submit is disabled until an answer is selected", !dialog.primaryButton().isEnabled());
        dialog.select(dialog.question().correctIndex());
        dialog.submit();
        check("A correct answer is recognised", dialog.isAnswered() && dialog.isCorrect());
        dialog.primaryButton().doClick(0);
        check("Continue closes the dialog and moves the piece forward by the question level",
                ModalOverlay.openCount() == 0 && qp.progress() == 5 + steps, "progress " + qp.progress());
        game.demoQuestion();
        QuestionDialog second = game.lastQuestion();
        int before = qp.progress();
        second.select((second.question().correctIndex() + 1) % 4);
        second.submit();
        boolean wrong = second.isAnswered() && !second.isCorrect();
        second.primaryButton().doClick(0);
        check("A wrong answer moves the piece back", wrong
                && qp.progress() == Math.max(0, before - second.question().level().questionSteps()));

        section("Surprise events (FR-39..FR-42)");
        int surprises = 0;
        for (int i = 0; i < SurpriseOutcome.values().length; i++) {
            if (s.currentPlayer().pieces().stream().noneMatch(Piece::onBoard)) {
                s.currentPlayer().pieces().stream().filter(Piece::inCastle).findFirst()
                        .ifPresent(castle -> castle.setProgress(10));
            }
            game.demoSurprise();
            if (game.lastSurprise() != null && ModalOverlay.openCount() == 1) {
                surprises++;
            }
            game.lastSurprise().primaryButton().doClick(0);
            if (s.phase() != TurnPhase.AWAITING_ROLL) {
                break;
            }
        }
        check("All six surprise outcomes open the surprise dialog and close with Continue",
                surprises == SurpriseOutcome.values().length && ModalOverlay.openCount() == 0, surprises + " opened");

        section("Results and history (FR-43..FR-50)");
        int historyBefore = GameHistory.getInstance().size();
        game.demoResults();
        check("Show Results opens the results screen", NavigationController.RESULTS.equals(nav.currentScreen()));
        check("The finished game is added to the history", GameHistory.getInstance().size() == historyBefore + 1);
        UiFinder.click(frame, "results.playAgain");
        check("Play Again starts a new game with the same settings", NavigationController.GAME.equals(nav.currentScreen())
                && nav.game().state().players().size() == 2);
        nav.game().demoResults();
        UiFinder.click(frame, "results.menu");
        check("Main Menu from the results screen", NavigationController.MENU.equals(nav.currentScreen()));
        UiFinder.click(frame, "menu.history");
        check("History table shows all records", nav.historyView().table().getRowCount() == GameHistory.getInstance().size());
        nav.historyView().setFilter(Difficulty.MEDIUM);
        boolean filtered = true;
        for (int r = 0; r < nav.historyView().table().getRowCount(); r++) {
            filtered &= nav.historyView().table().getValueAt(r, 1) == Difficulty.MEDIUM;
        }
        check("History filter shows only the chosen difficulty", filtered && nav.historyView().table().getRowCount() > 0);
        nav.historyView().setFilter(null);

        section("Administrator and question management (FR-51..FR-62)");
        nav.openQuestionManagement();
        nav.loginView().usernameField().setText("admin");
        nav.loginView().passwordField().setText("not-it");
        UiFinder.click(frame, "login.submit");
        check("Wrong credentials keep the login screen with a general error (FR-53)",
                NavigationController.LOGIN.equals(nav.currentScreen()) && nav.loginView().isErrorVisible()
                        && nav.loginView().errorMessage().contains("username or password"));
        nav.loginView().usernameField().setText("admin");
        nav.loginView().passwordField().setText("Sharks2026");
        UiFinder.click(frame, "login.submit");
        check("Correct credentials open Question Management", NavigationController.QUESTIONS.equals(nav.currentScreen()));
        int rows = nav.questionsView().table().getRowCount();
        check("Question table lists the question bank", rows == 36, rows + " rows");
        UiFinder.click(frame, "questions.add");
        QuestionFormDialog form = nav.admin().lastForm();
        UiFinder.click(frame, "form.save");
        check("An empty form is rejected and stays open", ModalOverlay.openCount() == 1
                && nav.questionsView().table().getRowCount() == 36);
        form.setLevel(Difficulty.HARD);
        form.setQuestionText("Which document defines what a system must do?");
        form.setAnswer(0, "SRS");
        form.setAnswer(1, "SDP");
        form.setAnswer(2, "Test log");
        form.setAnswer(3, "User manual");
        form.setCorrect(0);
        UiFinder.click(frame, "form.save");
        check("A valid question is added and the table updates", ModalOverlay.openCount() == 0
                && nav.questionsView().table().getRowCount() == 37);
        Question q = nav.questionsView().selectedQuestion();
        check("The new question is selected", q != null && q.text().startsWith("Which document"));
        nav.admin().editQuestion(q);
        nav.admin().lastForm().setQuestionText("Which document defines the requirements?");
        UiFinder.click(frame, "form.save");
        check("Editing a question updates the table", q.text().equals("Which document defines the requirements?"));
        int deleted = 0;
        while (nav.questionsView().table().getRowCount() > QuestionBank.MINIMUM_QUESTIONS && deleted < 10) {
            nav.questionsView().table().setRowSelectionInterval(0, 0);
            UiFinder.click(frame, "questions.delete");
            UiFinder.click(frame, "dialog.confirm");
            deleted++;
        }
        check("Delete asks for confirmation and removes questions down to 30", deleted == 7
                && nav.questionsView().table().getRowCount() == 30);
        nav.questionsView().table().setRowSelectionInterval(0, 0);
        UiFinder.click(frame, "questions.delete");
        boolean refused = ModalOverlay.openCount() == 1 && nav.questionsView().table().getRowCount() == 30;
        UiFinder.click(frame, "dialog.confirm");
        check("Deleting below 30 questions is refused with a message", refused);
        UiFinder.click(frame, "questions.logout");
        check("Log Out returns to the menu", NavigationController.MENU.equals(nav.currentScreen()));
        UiFinder.click(frame, "menu.questions");
        check("After logging out, Question Management asks for the login again (FR-54)",
                NavigationController.LOGIN.equals(nav.currentScreen()));

        section("Performance (NFR-02, NFR-06)");
        long start = System.nanoTime();
        nav.startGame(new GameSettings(Difficulty.HARD, List.of(new GameSettings.PlayerSetup("Noa", PlayerColor.RED),
                new GameSettings.PlayerSetup("Omer", PlayerColor.GREEN), new GameSettings.PlayerSetup("Yara", PlayerColor.YELLOW),
                new GameSettings.PlayerSetup("Adam", PlayerColor.BLUE))), new Random(5));
        render(frame);
        long boardMs = (System.nanoTime() - start) / 1_000_000;
        check("Hard board with 4 players is generated and drawn within 2 seconds (" + boardMs + " ms)", boardMs < 2000);
        nav.showMainMenu();
        List<GameRecord> many = new ArrayList<>(DemoData.history());
        for (int i = 0; i < 1000; i++) {
            many.add(new GameRecord(5000 + i, Difficulty.values()[i % 3],
                    java.time.LocalDateTime.of(2025, 1, 1, 12, 0).plusMinutes(i), List.of("Noa", "Omer", "Yara"),
                    java.time.Duration.ofMinutes(20 + i % 40)));
        }
        GameHistory.getInstance().load(many);
        start = System.nanoTime();
        UiFinder.click(frame, "menu.history");
        render(frame);
        long historyMs = (System.nanoTime() - start) / 1_000_000;
        check("Game History with " + many.size() + " games opens within 2 seconds (" + historyMs + " ms)",
                historyMs < 2000 && nav.historyView().table().getRowCount() == many.size());
        GameHistory.getInstance().load(DemoData.history());
        frame.dispose();
    }

    /** Lays out and paints the whole window once, like the first frame on screen. */
    private static void render(JFrame frame) {
        frame.validate();
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(Math.max(1, frame.getWidth()),
                Math.max(1, frame.getHeight()), java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = img.createGraphics();
        frame.getLayeredPane().paint(g);
        g.dispose();
    }
}
