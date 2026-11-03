package View;

import javax.swing.JButton;

/** Hooks the navigation controller calls when a screen becomes visible. */
public interface ScreenView {

    /** Called every time the screen is shown. */
    default void onShow() {
    }

    /** Button activated by the Enter key on this screen, or null. */
    default JButton defaultButton() {
        return null;
    }
}
