package Controller;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import View.components.Theme;

/**
 * Educational Ludo - Team Sharks, Iteration 1. The application entry point.
 * Runs offline with Java 17 or later and the standard library only (NFR-07).
 *
 * <p>Owner: Rakan Ziyan.
 */
public final class Main {

    private Main() {
    }

    /**
     * Starts the application on the Swing event thread.
     *
     * @param args not used
     */
    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        SwingUtilities.invokeLater(Main::launch);
    }

    /**
     * Builds the application window and its screens.
     *
     * @return the navigation controller that owns the window
     */
    public static NavigationController createApp() {
        Theme.installLookAndFeel();
        return new NavigationController();
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
