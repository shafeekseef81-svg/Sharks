package controller;

import java.awt.BasicStroke;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import components.Icons;
import components.ModalOverlay;
import components.Theme;
import model.AdminAuthenticator;
import model.Difficulty;
import model.GameHistory;
import model.GameRecord;
import model.GameSettings;
import model.GameState;
import model.Player;
import model.PlayerColor;
import model.QuestionBank;
import view.AdminLoginView;
import view.ConfirmDialog;
import view.GameBoardView;
import view.GameHistoryView;
import view.GameResultsView;
import view.GameSetupView;
import view.MainMenuView;
import view.QuestionManagementView;
import view.ScreenView;

/** Owns the window and moves between screens (FR-01..FR-03); also shows modal dialogs. */
public final class NavigationController {

    public static final String MENU = "menu";
    public static final String SETUP = "setup";
    public static final String GAME = "game";
    public static final String RESULTS = "results";
    public static final String HISTORY = "history";
    public static final String LOGIN = "login";
    public static final String QUESTIONS = "questions";

    private final JFrame frame = new JFrame("Educational Ludo - Team Sharks (Interactive Prototype)");
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);
    private final Map<String, JComponent> screens = new HashMap<>();
    private final AdminAuthenticator auth;
    private final MainMenuView menuView;
    private final GameSetupView setupView;
    private final GameResultsView resultsView;
    private final GameHistoryView historyView;
    private final AdminLoginView loginView;
    private final QuestionManagementView questionsView;
    private final PrototypeGameController game;
    private final AdminController admin;
    private GameBoardView gameView;
    private GameSettings lastSettings;
    private String current;
    private Runnable exitHandler = () -> {
        frame.dispose();
        System.exit(0);
    };

    public NavigationController(QuestionBank bank, GameHistory history, AdminAuthenticator auth) {
        this.auth = auth;
        this.game = new PrototypeGameController(this, bank, history);
        this.admin = new AdminController(this, auth, bank);

        menuView = new MainMenuView(previewGame(bank));
        menuView.setActions(this::showSetup, this::showHistory, this::openQuestionManagement, this::requestExit);
        setupView = new GameSetupView();
        setupView.setBackAction(this::showMainMenu);
        setupView.setStartHandler((difficulty, players) -> {
            List<GameSettings.FieldError> errors = GameSettings.validate(difficulty, players);
            if (errors.isEmpty()) {
                startGame(new GameSettings(difficulty, players));
            }
            return errors;
        });
        resultsView = new GameResultsView();
        resultsView.setActions(this::playAgain, this::showMainMenu);
        historyView = new GameHistoryView(history);
        historyView.setBackAction(this::showMainMenu);
        loginView = new AdminLoginView();
        loginView.setActions(this::showMainMenu, admin::login);
        questionsView = new QuestionManagementView(bank);
        questionsView.setActions(admin::addQuestion, admin::editQuestion, admin::deleteQuestion, admin::logout);

        register(MENU, menuView);
        register(SETUP, setupView);
        register(RESULTS, resultsView);
        register(HISTORY, historyView);
        register(LOGIN, loginView);
        register(QUESTIONS, questionsView);

        frame.setContentPane(cards);
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (GAME.equals(current)) {
                    confirmLeaveGame(exitHandler);
                } else {
                    exitHandler.run();
                }
            }
        });
        List<Image> icons = new ArrayList<>();
        for (int size : new int[] {16, 24, 32, 48, 64, 128}) {
            icons.add(appIcon(size));
        }
        frame.setIconImages(icons);
        frame.setMinimumSize(new Dimension(1024, 640));
        show(MENU);
    }

    private void register(String name, JComponent screen) {
        screens.put(name, screen);
        cards.add(screen, name);
    }

    // ------------------------------------------------------------------ navigation

    private void show(String name) {
        ModalOverlay.closeAll();
        current = name;
        cardLayout.show(cards, name);
        JComponent screen = screens.get(name);
        frame.getRootPane().setDefaultButton(defaultButtonOf(screen));
        if (screen instanceof ScreenView sv) {
            sv.onShow();
        }
        cards.revalidate();
        cards.repaint();
    }

    private static JButton defaultButtonOf(JComponent screen) {
        return screen instanceof ScreenView sv ? sv.defaultButton() : null;
    }

    public void showMainMenu() {
        game.stop();
        show(MENU);
    }

    public void showSetup() {
        show(SETUP);
    }

    public void showHistory() {
        show(HISTORY);
    }

    public void showAdminLogin() {
        show(LOGIN);
    }

    /** Question Management requires a login first (FR-02, FR-51). */
    public void openQuestionManagement() {
        if (auth.isSignedIn()) {
            showQuestionManagement();
        } else {
            showAdminLogin();
        }
    }

    public void showQuestionManagement() {
        show(QUESTIONS);
    }

    /** Starts a game with validated settings (names, colours and level are fixed from now on, FR-09). */
    public void startGame(GameSettings settings) {
        startGame(settings, new Random());
    }

    public void startGame(GameSettings settings, Random random) {
        startGame(settings, random, System::currentTimeMillis);
    }

    /** Starts a game with a given random generator and clock (the tools use fixed ones). */
    public void startGame(GameSettings settings, Random random, java.util.function.LongSupplier clock) {
        lastSettings = settings;
        GameBoardView view = game.newGame(settings, random, clock);
        if (gameView != null) {
            cards.remove(gameView);
        }
        gameView = view;
        register(GAME, view);
        show(GAME);
    }

    private void playAgain() {
        if (lastSettings != null) {
            startGame(lastSettings);
        } else {
            showSetup();
        }
    }

    public void showResults(List<Player> ranking, Difficulty difficulty, Duration duration, boolean simulated,
                            GameRecord record) {
        resultsView.showResults(ranking, difficulty, duration, simulated, record);
        show(RESULTS);
    }

    /** Leaving a running game needs a confirmation and the game is not saved (FR-03). */
    public void confirmLeaveGame(Runnable afterLeaving) {
        confirm("Leave this game?",
                "The game is still running. If you leave now, it will not be saved to the game history.",
                "Leave Game", "Keep Playing", true, Icons.Type.WARNING, () -> {
                    game.stop();
                    afterLeaving.run();
                });
    }

    public void requestExit() {
        confirm("Exit Educational Ludo?", "The application will close.", "Exit", "Cancel", false,
                Icons.Type.EXIT, exitHandler);
    }

    public void setExitHandler(Runnable exitHandler) {
        this.exitHandler = exitHandler;
    }

    // ------------------------------------------------------------------ dialogs

    /** Shows a card in a modal overlay; Enter is routed to the card while it is open. */
    public ModalOverlay overlay(JComponent card) {
        frame.getRootPane().setDefaultButton(null);
        ModalOverlay overlay = ModalOverlay.show(frame.getLayeredPane(), card);
        overlay.setOnClose(() -> {
            if (ModalOverlay.openCount() == 0) {
                frame.getRootPane().setDefaultButton(defaultButtonOf(screens.get(current)));
            }
        });
        return overlay;
    }

    public void confirm(String title, String message, String confirmText, String cancelText, boolean danger,
                        Icons.Type icon, Runnable onConfirm) {
        ModalOverlay[] holder = new ModalOverlay[1];
        ConfirmDialog dialog = new ConfirmDialog(title, message, confirmText, cancelText, danger, icon, () -> {
            holder[0].close();
            onConfirm.run();
        }, () -> holder[0].close());
        holder[0] = overlay(dialog);
        holder[0].setOnEscape(() -> holder[0].close());
    }

    public void info(String title, String message, Icons.Type icon) {
        ModalOverlay[] holder = new ModalOverlay[1];
        ConfirmDialog dialog = new ConfirmDialog(title, message, "OK", null, false, icon, () -> holder[0].close(),
                () -> holder[0].close());
        holder[0] = overlay(dialog);
        holder[0].setOnEscape(() -> holder[0].close());
    }

    // ------------------------------------------------------------------ accessors

    public JFrame frame() {
        return frame;
    }

    public String currentScreen() {
        return current;
    }

    public PrototypeGameController game() {
        return game;
    }

    public AdminController admin() {
        return admin;
    }

    public MainMenuView menuView() {
        return menuView;
    }

    public GameSetupView setupView() {
        return setupView;
    }

    public GameBoardView gameView() {
        return gameView;
    }

    public GameResultsView resultsView() {
        return resultsView;
    }

    public GameHistoryView historyView() {
        return historyView;
    }

    public AdminLoginView loginView() {
        return loginView;
    }

    public QuestionManagementView questionsView() {
        return questionsView;
    }

    // ------------------------------------------------------------------ helpers

    /** Decorative board for the main menu: an Easy board with a few pieces on it. */
    private static GameState previewGame(QuestionBank bank) {
        List<GameSettings.PlayerSetup> players = List.of(
                new GameSettings.PlayerSetup("Red", PlayerColor.RED),
                new GameSettings.PlayerSetup("Green", PlayerColor.GREEN),
                new GameSettings.PlayerSetup("Yellow", PlayerColor.YELLOW),
                new GameSettings.PlayerSetup("Blue", PlayerColor.BLUE));
        GameState preview = new GameState(new GameSettings(Difficulty.EASY, players), bank, new Random(2026));
        int[][] progress = {{9, 30}, {4, 52}, {17, -1}, {23, 40}};
        List<Player> list = preview.players();
        for (int i = 0; i < list.size(); i++) {
            list.get(i).pieces().get(0).setProgress(progress[i][0]);
            list.get(i).pieces().get(1).setProgress(progress[i][1]);
        }
        return preview;
    }

    /** Window icon: the four Ludo colours with a white die in the middle. */
    private static Image appIcon(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        Theme.antialias(g);
        double s = size;
        double r = s * 0.22;
        g.setClip(new RoundRectangle2D.Double(0, 0, s, s, r, r));
        PlayerColor[] colors = {PlayerColor.RED, PlayerColor.GREEN, PlayerColor.BLUE, PlayerColor.YELLOW};
        for (int i = 0; i < 4; i++) {
            g.setColor(Theme.player(colors[i]));
            g.fillRect((int) (i % 2 * s / 2), (int) (i / 2 * s / 2), (int) Math.ceil(s / 2), (int) Math.ceil(s / 2));
        }
        g.setClip(null);
        double d = s * 0.5;
        g.setColor(Color.WHITE);
        g.fill(new RoundRectangle2D.Double((s - d) / 2, (s - d) / 2, d, d, d * 0.3, d * 0.3));
        g.setColor(Theme.BOARD_LINE);
        g.setStroke(new BasicStroke((float) Math.max(1, s * 0.03)));
        g.draw(new RoundRectangle2D.Double((s - d) / 2, (s - d) / 2, d, d, d * 0.3, d * 0.3));
        double pip = d * 0.18;
        for (double[] p : new double[][] {{0.3, 0.3}, {0.5, 0.5}, {0.7, 0.7}}) {
            g.fill(new Ellipse2D.Double((s - d) / 2 + d * p[0] - pip / 2, (s - d) / 2 + d * p[1] - pip / 2, pip, pip));
        }
        g.dispose();
        return img;
    }
}
