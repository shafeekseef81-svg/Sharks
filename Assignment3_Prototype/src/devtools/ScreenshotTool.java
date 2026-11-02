package devtools;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;

import components.Animations;
import components.ModalOverlay;
import controller.NavigationController;
import controller.PrototypeGameController;
import main.Main;
import model.Difficulty;
import model.GameSettings;
import model.GameState;
import model.Piece;
import model.Player;
import model.PlayerColor;
import model.Question;
import model.Square;
import model.SquareType;
import view.QuestionDialog;
import view.QuestionFormDialog;

/**
 * Renders every screen of the prototype to PNG files, in the order of the user flows, for the
 * submission PDF and for visual checks at 1280x720. Runs offscreen (no window is shown).
 *
 * <p>Usage: {@code java -jar EducationalLudo.jar --screenshots [dir] [width height]}
 */
public final class ScreenshotTool {

    private final File dir;
    private final NavigationController nav;
    private final JFrame frame;
    private int count;

    private ScreenshotTool(File dir, int width, int height) {
        this.dir = dir;
        this.nav = Main.createApp();
        nav.setExitHandler(() -> { });
        frame = nav.frame();
        frame.getRootPane().setPreferredSize(new Dimension(width, height));
        frame.pack();
    }

    public static void run(String dirName) throws Exception {
        run(dirName, 1280, 720);
    }

    public static void run(String dirName, int width, int height) throws Exception {
        Animations.instant = true;
        File dir = new File(dirName);
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new IOException("Cannot create " + dir.getAbsolutePath());
        }
        Exception[] error = new Exception[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                new ScreenshotTool(dir, width, height).capture();
            } catch (Exception e) {
                error[0] = e;
            }
        });
        if (error[0] != null) {
            throw error[0];
        }
        System.out.println("Screenshots written to " + dir.getAbsolutePath());
    }

    private void capture() throws IOException {
        // Flow 1: main menu -> setup -> board
        nav.showMainMenu();
        shot("main_menu");

        nav.showSetup();
        nav.setupView().setPlayerCount(3);
        nav.setupView().setDifficulty(Difficulty.MEDIUM);
        nav.setupView().setPlayerName(0, "Noa");
        nav.setupView().setPlayerName(1, "Omer");
        nav.setupView().setPlayerName(2, "Yara");
        nav.setupView().setPlayerColor(0, PlayerColor.RED);
        nav.setupView().setPlayerColor(1, PlayerColor.GREEN);
        nav.setupView().setPlayerColor(2, PlayerColor.YELLOW);
        shot("game_setup");

        nav.setupView().setPlayerName(1, "");
        nav.setupView().setPlayerName(2, "noa");
        UiFinder.click(frame, "setup.start");
        shot("game_setup_validation");

        startGame(Difficulty.EASY, 23, 4_000, new String[] {"Noa", "Omer"},
                new PlayerColor[] {PlayerColor.RED, PlayerColor.BLUE});
        shot("board_easy_start");

        GameState hard = startGame(Difficulty.HARD, 7, 1_265_000, new String[] {"Noa", "Omer", "Yara"},
                new PlayerColor[] {PlayerColor.RED, PlayerColor.GREEN, PlayerColor.BLUE});
        place(hard, 0, 14, 77);
        place(hard, 1, 31, 120);
        place(hard, 2, 3, 150);
        nav.gameView().board().refreshStatic();
        nav.game().refreshClock();
        shot("board_hard");

        // Flow 2: dice -> piece movement (Medium game in progress)
        GameState medium = startGame(Difficulty.MEDIUM, 21, 462_000, new String[] {"Noa", "Omer", "Yara", "Adam"},
                new PlayerColor[] {PlayerColor.RED, PlayerColor.GREEN, PlayerColor.YELLOW, PlayerColor.BLUE});
        place(medium, 0, 12, 41);
        place(medium, 1, 9, -1);
        place(medium, 2, 63, 112);
        place(medium, 3, 27, -1);
        nav.game().refreshClock();
        nav.gameView().board().refreshStatic();
        shot("board_medium");

        PrototypeGameController game = nav.game();
        medium.forceNextRoll(6, 3);
        game.roll();
        Map<Piece, Integer> targets = nav.gameView().board().targets();
        Piece preview = pickPlainMove(medium, targets);
        nav.gameView().board().setHovered(preview);
        shot("roll_and_highlight");

        nav.gameView().board().setHovered(null);
        game.pieceClicked(preview);
        shot("piece_moved");

        // Flow 3: educational question -> feedback
        game.demoQuestion();
        QuestionDialog question = game.lastQuestion();
        Question q = question.question();
        question.select((q.correctIndex() + 1) % 4);
        shot("question");
        question.submit();
        shot("question_feedback");
        question.primaryButton().doClick(0);

        // Flow 4: surprise event (second outcome of the demo cycle: "push an opponent back")
        game.demoSurprise();
        game.lastSurprise().primaryButton().doClick(0);
        game.demoSurprise();
        shot("surprise");
        game.lastSurprise().primaryButton().doClick(0);

        UiFinder.click(frame, "game.back");
        shot("leave_game_confirmation");
        ModalOverlay.closeAll();

        // Flow 5: results
        game.demoResults();
        shot("results");

        // Flow 6: game history
        nav.showHistory();
        shot("game_history");

        // Flow 7: administrator login -> question management
        nav.openQuestionManagement();
        shot("admin_login");
        nav.loginView().usernameField().setText("admin");
        nav.loginView().passwordField().setText("wrong-password");
        nav.loginView().loginButton().doClick(0);
        shot("admin_login_error");
        nav.loginView().usernameField().setText("admin");
        nav.loginView().passwordField().setText("Sharks2026");
        nav.loginView().loginButton().doClick(0);
        nav.questionsView().table().setRowSelectionInterval(3, 3);
        shot("question_management");

        nav.admin().addQuestion();
        QuestionFormDialog form = nav.admin().lastForm();
        form.setLevel(Difficulty.MEDIUM);
        form.setQuestionText("Which test level checks that separately developed modules work together?");
        form.setAnswer(0, "Integration testing");
        form.setAnswer(1, "Unit testing");
        form.setAnswer(3, "integration testing");
        form.saveButton().doClick(0);
        shot("question_form_validation");
        ModalOverlay.closeAll();
    }

    private GameState startGame(Difficulty level, long seed, long elapsedMillis, String[] names, PlayerColor[] colors) {
        List<GameSettings.PlayerSetup> players = new java.util.ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            players.add(new GameSettings.PlayerSetup(names[i], colors[i]));
        }
        long[] offset = {0};
        long base = System.currentTimeMillis();
        nav.startGame(new GameSettings(level, players), new Random(seed), () -> base + offset[0]);
        offset[0] = elapsedMillis;
        nav.game().refreshClock();
        return nav.game().state();
    }

    /** Puts the first two pieces of a player at the given progress values (-1 = castle). */
    private static void place(GameState state, int player, int first, int second) {
        Player p = state.players().get(player);
        p.pieces().get(0).setProgress(first);
        p.pieces().get(1).setProgress(second);
    }

    /** A legal move that does not end on a question or surprise square (keeps the screenshot calm). */
    private static Piece pickPlainMove(GameState state, Map<Piece, Integer> targets) {
        Piece fallback = null;
        for (Map.Entry<Piece, Integer> e : targets.entrySet()) {
            Square s = state.board().squareAt(state.board().cellAt(e.getKey().color(), e.getValue()));
            boolean special = s != null && (s.type() == SquareType.QUESTION || s.type() == SquareType.SURPRISE);
            if (!special && !e.getKey().inCastle()) {
                return e.getKey();
            }
            if (!special && fallback == null) {
                fallback = e.getKey();
            }
        }
        return fallback != null ? fallback : targets.keySet().iterator().next();
    }

    private void shot(String name) throws IOException {
        frame.validate();
        JLayeredPane pane = frame.getLayeredPane();
        pane.validate();
        // Render at the display scale (e.g. 1.5 at 150 %), so text is measured and drawn exactly as on screen.
        double scale = Math.max(1.0, frame.getGraphicsConfiguration().getDefaultTransform().getScaleX());
        BufferedImage img = new BufferedImage((int) Math.ceil(pane.getWidth() * scale),
                (int) Math.ceil(pane.getHeight() * scale), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.scale(scale, scale);
        // paint() instead of printAll(): printing mode re-justifies text and would distort the labels.
        javax.swing.RepaintManager rm = javax.swing.RepaintManager.currentManager(pane);
        boolean buffered = rm.isDoubleBufferingEnabled();
        rm.setDoubleBufferingEnabled(false);
        try {
            pane.paint(g);
        } finally {
            rm.setDoubleBufferingEnabled(buffered);
        }
        g.dispose();
        count++;
        File file = new File(dir, String.format("%02d_%s.png", count, name));
        ImageIO.write(img, "png", file);
        System.out.println("  " + file.getName());
    }
}
