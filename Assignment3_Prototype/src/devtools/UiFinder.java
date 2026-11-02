package devtools;

import java.awt.Component;
import java.awt.Container;

import javax.swing.AbstractButton;
import javax.swing.JFrame;

/** Finds visible components by name, top-most overlay first (used by the self-test and the screenshot tool). */
final class UiFinder {

    private UiFinder() {
    }

    static <T extends Component> T find(Container root, String name, Class<T> type) {
        for (Component c : root.getComponents()) {
            if (!c.isVisible()) {
                continue;
            }
            if (name.equals(c.getName()) && type.isInstance(c)) {
                return type.cast(c);
            }
            if (c instanceof Container container) {
                T found = find(container, name, type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    static <T extends Component> T find(JFrame frame, String name, Class<T> type) {
        return find(frame.getLayeredPane(), name, type);
    }

    static void click(JFrame frame, String name) {
        AbstractButton button = find(frame, name, AbstractButton.class);
        if (button == null) {
            throw new IllegalStateException("No visible button named " + name);
        }
        button.doClick(0);
    }
}
