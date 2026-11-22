package View;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;

import Model.Piece;
import Model.PlayerColor;
import Model.SurpriseOutcome;
import Model.SurprisePrompt;
import Model.SurpriseResult;
import View.components.CustomButton;
import View.components.CustomPanel;
import View.components.Icons;
import View.components.Theme;
import View.components.ToggleChip;
import View.components.UiKit;

/**
 * The card that shows one surprise and, for the push of FR-40, lets the active player choose which opponent
 * piece goes back (SRS FR-39, FR-40; NFR-05). Task T2-11, owner: Mohamed Zereny.
 *
 * <p><b>What it does.</b> It shows the outcome that the Model drew, its chance, and the sentence that explains
 * it. For "push an opponent back" it also shows the Regular die the Model rolled, how far the chosen piece
 * goes back, and one chip per eligible piece; the active player picks one and confirms. For the five other
 * outcomes, and for a push with no eligible piece, there is a single <i>Continue</i> button.
 *
 * <p><b>What it does not do (MVC, NFR-08).</b> It does not draw the outcome, does not roll any die, does not
 * work out how far anything moves, does not decide which pieces are eligible and moves nothing. All of that
 * is the {@link SurprisePrompt} the Model handed it (T2-09). Turning the player's pick into an instruction is
 * also the Model's: the caller passes a {@link Resolver}, which should be
 * {@link Model.SurpriseFactory#resolve(SurprisePrompt, Piece)}, because that one re-checks the pick against
 * the board as it is now and so refuses a piece that a chain captured or moved out of reach in the meantime.
 * Carrying the instruction out is T2-10, and the turn is T2-13.
 *
 * <p><b>The turn outcomes (Product Owner decision D2, approved 21/11/2026).</b> A {@code SKIP_TURN} or
 * {@code EXTRA_ROLL} always falls on the <b>active</b> player, even when an effect chain set this square off
 * with another player's piece (FR-42). The card therefore shows the Model's own wording unchanged - it is
 * written to the player whose turn it is ("Skip <b>your</b> next turn"), which under D2 is exactly right -
 * next to the {@code surprise.activePlayer} pill that names that player. When the chain was set off by an
 * opponent's piece, {@code surprise.chainNote} still says so, so the player can see why the square was
 * reached; that note is factual and names both players, and it never claims the opponent is the one affected.
 *
 * <p>Built to be shown inside a {@link View.components.ModalOverlay}, like {@link ConfirmDialog}. No window of
 * its own is created, so it can also be laid out in a headless test.
 */
public class SurpriseDialog extends CustomPanel {

    /** Width of the card in pixels. */
    private static final int CARD_WIDTH = 560;

    /** Name of this card, for tests and for {@code findByName} lookups. */
    public static final String NAME = "dialog.surprise";

    /** Name prefix of the target chips: {@code surprise.target.0}, {@code surprise.target.1}, and so on. */
    public static final String TARGET_NAME = "surprise.target.";

    /** Name of the button that confirms the pick, or closes a surprise that needs no pick. */
    public static final String CONFIRM_NAME = "surprise.confirm";

    /**
     * Turns the piece the player picked into an instruction. Implemented in the Model by
     * {@link Model.SurpriseFactory#resolve(SurprisePrompt, Piece)}; the caller passes it in so that no rule,
     * and no eligibility check, lives in the view.
     *
     * <p>It is asked only when there is a pick to check, that is for a push with at least one eligible piece
     * ({@link SurprisePrompt#needsChoice()}). Every other surprise resolves through
     * {@link SurprisePrompt#resolve()}, which is Model code as well, so a resolver never has to deal with a
     * missing piece.
     */
    public interface Resolver {

        /**
         * @param prompt the surprise being resolved
         * @param target the piece the active player picked; never null
         * @return what must happen (FR-39, FR-40)
         * @throws IllegalArgumentException if the pick is not an eligible opponent piece any more
         */
        SurpriseResult resolve(SurprisePrompt prompt, Piece target);
    }

    private final SurprisePrompt prompt;
    private final Function<PlayerColor, String> playerName;
    private final Resolver resolver;
    private final Consumer<SurpriseResult> onResolved;
    private final List<ToggleChip> targetChips = new ArrayList<>();
    /**
     * Keeps the chips mutually exclusive, and is the only way to unselect them again:
     * {@code chip.setSelected(false)} on the chip the group currently holds does nothing, because
     * {@code ButtonGroup.setSelected} ignores a request to deselect and the toggle then reads its own state
     * back from the group. Only {@link ButtonGroup#clearSelection()} drops the selection.
     */
    private final ButtonGroup targetGroup = new ButtonGroup();
    private final CustomButton confirmButton;
    private final JLabel error = UiKit.wrap(" ", CARD_WIDTH - 60, Theme.font(Font.PLAIN, 13.5f), Theme.DANGER);
    private Piece picked;
    private SurpriseResult result;

    /**
     * @param prompt the surprise the Model drew (T2-09)
     * @param playerName the name of a player by colour; the caller reads it from the Model
     * @param resolver the Model rule that turns the pick into an instruction
     * @param onResolved called once, with the instruction, when the player confirms
     * @throws NullPointerException if an argument is null
     */
    public SurpriseDialog(SurprisePrompt prompt, Function<PlayerColor, String> playerName,
                          Resolver resolver, Consumer<SurpriseResult> onResolved) {
        super(new BorderLayout(0, 16), Style.CARD);
        this.prompt = Objects.requireNonNull(prompt, "prompt");
        this.playerName = Objects.requireNonNull(playerName, "playerName");
        this.resolver = Objects.requireNonNull(resolver, "resolver");
        this.onResolved = Objects.requireNonNull(onResolved, "onResolved");
        setPadding(22, 26, 20, 26);
        setName(NAME);

        this.confirmButton = new CustomButton(prompt.needsChoice() ? "Push it back" : "Continue",
                CustomButton.Variant.PRIMARY);
        add(header(), BorderLayout.NORTH);
        add(body(), BorderLayout.CENTER);
        add(footer(), BorderLayout.SOUTH);
    }

    private JPanel header() {
        SurpriseOutcome outcome = prompt.outcome();
        JPanel row = new JPanel(new BorderLayout(14, 0));
        row.setOpaque(false);
        row.add(new ConfirmDialog.IconBadge(icon(outcome), Theme.SURPRISE), BorderLayout.WEST);

        JPanel texts = new JPanel();
        texts.setOpaque(false);
        texts.setLayout(new BoxLayout(texts, BoxLayout.Y_AXIS));
        JLabel head = UiKit.label("Surprise square", Theme.font(Font.BOLD, 14f), Theme.SURPRISE);
        texts.add(head);
        texts.add(UiKit.vgap(3));
        JLabel title = UiKit.wrap(outcome.title(),
                CARD_WIDTH - 130, Theme.font(Font.BOLD, 19f), Theme.TEXT);
        title.setName("surprise.title");
        texts.add(title);
        texts.add(UiKit.vgap(6));
        JPanel tags = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 0));
        tags.setOpaque(false);
        JLabel chance = UiKit.pill(outcome.percent() + "% chance",
                Theme.alpha(Theme.SURPRISE, 32), Theme.SURPRISE);
        chance.setName("surprise.chance");
        tags.add(chance);
        JLabel turn = UiKit.pill(UiKit.ltr(playerName.apply(prompt.active()) + "'s turn"),
                Theme.SURFACE_ALT, Theme.TEXT_MUTED);
        turn.setName("surprise.activePlayer");
        tags.add(turn);
        texts.add(tags);
        row.add(texts, BorderLayout.CENTER);
        return row;
    }

    private JPanel body() {
        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));

        JLabel description = UiKit.wrap(
                prompt.outcome().description(),
                CARD_WIDTH - 60, Theme.font(Font.PLAIN, 14.5f), Theme.TEXT_MUTED);
        description.setName("surprise.description");
        column.add(description);

        if (prompt.opponentTriggered()) {
            column.add(UiKit.vgap(8));
            JLabel chain = UiKit.wrap("This square was reached by "
                            + playerName.apply(prompt.subjectOwner()) + "'s piece during "
                            + playerName.apply(prompt.active()) + "'s turn.",
                    CARD_WIDTH - 60, Theme.font(Font.ITALIC, 13.5f), Theme.TEXT_MUTED);
            chain.setName("surprise.chainNote");
            column.add(chain);
        }

        if (prompt.outcome().needsOpponentPiece()) {
            column.add(UiKit.vgap(12));
            column.add(pushSection());
        }

        error.setName("surprise.error");
        error.setVisible(false);
        column.add(UiKit.vgap(8));
        column.add(error);
        return column;
    }

    /** The Regular die, the distance, and the pieces to choose from (FR-40). */
    private JPanel pushSection() {
        JPanel section = new JPanel();
        section.setOpaque(false);
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));

        int steps = prompt.backwardSteps();
        JLabel roll = UiKit.wrap("Regular die: " + prompt.regularRoll() + ". The piece you choose moves back "
                        + steps + (steps == 1 ? " square." : " squares."),
                CARD_WIDTH - 60, Theme.font(Font.BOLD, 14f), Theme.TEXT);
        roll.setName("surprise.roll");
        section.add(roll);
        section.add(UiKit.vgap(10));

        if (!prompt.needsChoice()) {
            JLabel none = UiKit.wrap(SurprisePrompt.NO_TARGET_NOTE, CARD_WIDTH - 60,
                    Theme.font(Font.PLAIN, 14f), Theme.TEXT_MUTED);
            none.setName("surprise.noTargets");
            section.add(none);
            return section;
        }

        JLabel pick = UiKit.label("Choose the piece to push back:", Theme.font(Font.PLAIN, 13.5f),
                Theme.TEXT_MUTED);
        pick.setName("surprise.pickLabel");
        section.add(pick);
        section.add(UiKit.vgap(8));

        List<Piece> targets = prompt.targets();
        JPanel grid = new JPanel(new GridLayout(0, 2, 8, 8));
        grid.setOpaque(false);
        for (int i = 0; i < targets.size(); i++) {
            Piece target = targets.get(i);
            ToggleChip chip = new ToggleChip(UiKit.ltr(playerName.apply(target.color())
                    + " - piece " + (target.index() + 1)), false);
            chip.setName(TARGET_NAME + i);
            chip.setSelectedColor(Theme.player(target.color()));
            chip.addActionListener(e -> select(target));
            targetGroup.add(chip);
            targetChips.add(chip);
            grid.add(chip);
        }
        section.add(grid);
        return section;
    }

    private JPanel footer() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        row.setOpaque(false);
        confirmButton.setName(CONFIRM_NAME);
        // FR-40: nothing can be confirmed until the player has actually chosen a piece.
        confirmButton.setEnabled(!prompt.needsChoice());
        confirmButton.addActionListener(e -> confirm());
        row.add(confirmButton);
        return row;
    }

    private void select(Piece target) {
        if (result != null) {
            return;
        }
        picked = target;
        confirmButton.setEnabled(true);
        error.setVisible(false);
        revalidate();
        repaint();
    }

    /**
     * Confirms the surprise: asks the Model for the instruction and hands it to the caller. For a push it
     * needs a piece to have been chosen first; without one it does nothing, so the button being disabled is
     * not the only guard (FR-40).
     *
     * <p>The first call wins, so a double click cannot resolve the same surprise twice. If the Model refuses
     * the pick, because a chain moved or captured that piece in the meantime, the message is shown on the
     * card and the player may choose again.
     */
    public void confirm() {
        if (result != null) {
            return;
        }
        if (prompt.needsChoice() && picked == null) {
            showError("Choose one of the pieces first.");
            return;
        }
        SurpriseResult resolved;
        try {
            // Only a pick needs re-checking against the live board; without one the prompt resolves itself,
            // which is Model code either way.
            resolved = prompt.needsChoice() ? resolver.resolve(prompt, picked) : prompt.resolve();
        } catch (IllegalArgumentException | IllegalStateException refused) {
            showError(refused.getMessage());
            picked = null;
            // Must go through the group: a chip the group holds ignores setSelected(false).
            targetGroup.clearSelection();
            confirmButton.setEnabled(!prompt.needsChoice());
            return;
        }
        if (resolved == null) {
            throw new IllegalStateException("The Model resolved " + prompt.outcome() + " as nothing");
        }
        result = resolved;
        confirmButton.setEnabled(false);
        for (ToggleChip chip : targetChips) {
            chip.setEnabled(false);
        }
        onResolved.accept(resolved);
    }

    private void showError(String message) {
        UiKit.setWrapped(error, message == null ? "That piece cannot be pushed back." : message,
                CARD_WIDTH - 60);
        error.setVisible(true);
        revalidate();
        repaint();
    }

    private static Icons.Type icon(SurpriseOutcome outcome) {
        return switch (outcome) {
            case JUMP_TO_FINISH -> Icons.Type.FLAG;
            case OPPONENT_BACK -> Icons.Type.PUSH;
            case BACK_TO_CASTLE -> Icons.Type.CASTLE;
            case BACK_THREE -> Icons.Type.STEPS_BACK;
            case SKIP_TURN -> Icons.Type.SKIP;
            case EXTRA_ROLL -> Icons.Type.ROCKET;
        };
    }

    /** @return the surprise this card shows */
    public SurprisePrompt prompt() {
        return prompt;
    }

    /** @return the instruction, or null while the player has not confirmed */
    public SurpriseResult result() {
        return result;
    }

    /** @return true once the surprise has been confirmed, so it cannot be confirmed again */
    public boolean resolved() {
        return result != null;
    }

    /** @return the piece the player has chosen, or null */
    public Piece picked() {
        return picked;
    }

    /**
     * @param index position in {@link SurprisePrompt#targets()}
     * @return the chip of that piece, for tests
     */
    public ToggleChip targetChip(int index) {
        return targetChips.get(index);
    }

    /** @return how many pieces the player may choose from */
    public int targetCount() {
        return targetChips.size();
    }

    /** @return the button that confirms the pick; disabled until a piece is chosen for a push */
    public CustomButton confirmButton() {
        return confirmButton;
    }

    /** @return the message shown when the Model refused the pick, or null when none is shown */
    public String errorMessage() {
        return error.isVisible() ? error.getText() : null;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(CARD_WIDTH, super.getPreferredSize().height);
    }
}
