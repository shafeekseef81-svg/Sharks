package main;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import components.Theme;
import controller.NavigationController;
import data.DemoData;
import devtools.ScreenshotTool;
import devtools.SelfTest;
import devtools.SubmissionPdf;
import model.AdminAuthenticator;
import model.GameHistory;
import model.QuestionBank;

/**
 * Educational Ludo - interactive prototype of Team Sharks (Assignment 3).
 * Runs fully offline with Java 17+ and the standard library only.
 *
 * <p>Options: {@code --selftest}, {@code --screenshots [dir]}, {@code --submission-pdf [file] [screenshotDir]}.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        if (args.length > 0) {
            switch (args[0]) {
                case "--selftest" -> System.exit(SelfTest.run());
                case "--screenshots" -> {
                    String dir = args.length > 1 ? args[1] : "screenshots";
                    if (args.length > 3) {
                        ScreenshotTool.run(dir, Integer.parseInt(args[2]), Integer.parseInt(args[3]));
                    } else {
                        ScreenshotTool.run(dir);
                    }
                    System.exit(0);
                }
                case "--submission-pdf" -> {
                    SubmissionPdf.run(args.length > 1 ? args[1] : "prototype_Sharks.pdf",
                            args.length > 2 ? args[2] : "screenshots");
                    System.exit(0);
                }
                default -> {
                    // unknown options are ignored: start the application normally
                }
            }
        }
        SwingUtilities.invokeLater(Main::launch);
    }

    /** Builds the application with its sample data (also used by the tools). */
    public static NavigationController createApp() {
        Theme.installLookAndFeel();
        GameHistory.getInstance().load(DemoData.history());
        QuestionBank bank = new QuestionBank(DemoData.questions());
        AdminAuthenticator auth = new AdminAuthenticator(DemoData.ADMIN_USERNAME, DemoData.ADMIN_PASSWORD);
        return new NavigationController(bank, GameHistory.getInstance(), auth);
    }

    private static void launch() {
        NavigationController nav = createApp();
        JFrame frame = nav.frame();
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        Dimension size = new Dimension(Math.min(1280, screen.width), Math.min(760, screen.height));
        frame.setSize(size);
        frame.setLocation(screen.x + (screen.width - size.width) / 2, screen.y + (screen.height - size.height) / 2);
        frame.setVisible(true);
        nav.showMainMenu();
    }
}
