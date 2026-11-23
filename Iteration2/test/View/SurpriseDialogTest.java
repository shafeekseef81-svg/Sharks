package View;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import Model.Board;
import Model.BoardFactory;
import Model.Difficulty;
import Model.Piece;
import Model.PiecePlacement;
import Model.Player;
import Model.PlayerColor;
import Model.SurpriseFactory;
import Model.SurpriseOutcome;
import Model.SurprisePrompt;
import Model.SurpriseResult;
import Model.SurpriseResult.SurpriseAction;

/**
 * Tests for the surprise card of T2-11 (SRS FR-39, FR-40, FR-42; NFR-05). Owner: Mohamed Zereny.
 *
 * <p>The card shows the surprise and collects the active player's choice; drawing the outcome, rolling the
 * die, deciding which pieces are eligible and turning the pick into an instruction are all in the Model
 * (T2-09). These tests therefore wire the card to the <b>real</b> {@link SurpriseFactory}, as a controller
 * would, and check what the player ends up seeing and what the Model is handed.
 *
 * <p>No {@link javax.swing.JFrame} is created, so the tests run headless as well. Every Swing component is
 * built and used on the event dispatch thread.
 *
 * <p>Review level for T2-11 is Light (issue #62, reviewer Rakan Ziyan). These are developer tests; the
 * independent rule tests of the surprises themselves are T2-12 (issue #63, Rakan Ziyan).
 */
class SurpriseDialogTest {

    private final Board board = BoardFactory.create(Difficulty.MEDIUM, new Random(11));
    private final List<Player> players = new ArrayList<>();
    private final Player red;
    private final Player green;
    private final Player blue;

    SurpriseDialogTest() {
        red = addPlayer("Mohamed", PlayerColor.RED);
        green = addPlayer("Shafeek", PlayerColor.GREEN);
        blue = addPlayer("Rakan", PlayerColor.BLUE);
    }

    private Player addPlayer(String name, PlayerColor color) {
        Player player = new Player(name, color, board.finishIndex());
        players.add(player);
        return player;
    }

    private Piece onTrack(Player player, int pieceIndex, int progress) {
        return PiecePlacement.place(player.pieces().get(pieceIndex), progress);
    }

    /** The name lookup a controller would pass, reading the real players. */
    private Function<PlayerColor, String> names() {
        return color -> players.stream().filter(p -> p.color() == color)
                .findFirst().map(Player::name).orElse(color.label());
    }

    /** A random source whose draws are fixed, so a test can pick the Regular die of a push. */
    private static Random fixed(int... rawValues) {
        return new Random() {
            private static final long serialVersionUID = 1L;
            private int at;

            @Override
            public int nextInt(int bound) {
                if (at >= rawValues.length) {
                    throw new IllegalStateException("no scripted value left for bound " + bound);
                }
                return rawValues[at++];
            }
        };
    }

    // ------------------------------------------------------------------ Swing helpers

    private static void onEdt(Runnable action) throws Exception {
        try {
            SwingUtilities.invokeAndWait(action);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Exception cause) {
                throw cause;
            }
            throw e;
        }
    }

    private static void layOut(Component component) {
        component.setSize(component.getPreferredSize());
        component.doLayout();
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                layOut(child);
            }
        }
    }

    private static SurpriseDialog build(SurprisePrompt prompt, Function<PlayerColor, String> names,
                                        SurpriseDialog.Resolver resolver,
                                        List<SurpriseResult> resolved) throws Exception {
        SurpriseDialog[] holder = new SurpriseDialog[1];
        onEdt(() -> {
            holder[0] = new SurpriseDialog(prompt, names, resolver, resolved::add);
            layOut(holder[0]);
        });
        return holder[0];
    }

    private static Component find(Component root, String name) {
        if (name.equals(root.getName())) {
            return root;
        }
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                Component hit = find(child, name);
                if (hit != null) {
                    return hit;
                }
            }
        }
        return null;
    }

    private static String text(Component root, String name) {
        Component found = find(root, name);
        assertNotNull(found, "no component named " + name);
        assertTrue(found instanceof JLabel, name + " is not a label");
        return plain(((JLabel) found).getText());
    }

    private static String plain(String html) {
        return html == null ? "" : html.replaceAll("<[^>]*>", "").replace("‎", "").strip();
    }

    // ------------------------------------------------------------------ what the player sees (FR-39)

    @Test
    @DisplayName("FR-39: the card shows the drawn outcome, its chance and its explanation")
    void theCardShowsTheOutcome() throws Exception {
        Piece mine = onTrack(red, 0, 4);
        onTrack(green, 0, 12);
        SurpriseFactory factory = new SurpriseFactory(board, players, fixed(0));

        for (SurpriseOutcome outcome : SurpriseOutcome.values()) {
            SurpriseFactory each = new SurpriseFactory(board, players, fixed(3));
            SurprisePrompt prompt = each.promptFor(outcome, mine);
            SurpriseDialog dialog = build(prompt, names(), each::resolve, new ArrayList<>());

            assertEquals(SurpriseDialog.NAME, dialog.getName());
            assertEquals(outcome.title(), text(dialog, "surprise.title"), outcome.name());
            assertEquals(outcome.description(), text(dialog, "surprise.description"), outcome.name());
            assertEquals(outcome.percent() + "% chance", text(dialog, "surprise.chance"), outcome.name());
            assertTrue(text(dialog, "surprise.activePlayer").contains("Mohamed"), outcome.name());
        }
        assertNotNull(factory);
    }

    @Test
    @DisplayName("FR-39: an outcome that needs no choice has one button and resolves straight away")
    void outcomesWithoutAChoiceHaveOneButton() throws Exception {
        Piece mine = onTrack(red, 0, 7);
        onTrack(green, 0, 12);

        for (SurpriseOutcome outcome : SurpriseOutcome.values()) {
            if (outcome.needsOpponentPiece()) {
                continue;
            }
            SurpriseFactory factory = new SurpriseFactory(board, players, fixed());
            SurprisePrompt prompt = factory.promptFor(outcome, mine);
            List<SurpriseResult> resolved = new ArrayList<>();
            SurpriseDialog dialog = build(prompt, names(), factory::resolve, resolved);

            assertEquals(0, dialog.targetCount(), outcome.name());
            assertNull(find(dialog, SurpriseDialog.TARGET_NAME + 0), outcome.name());
            assertTrue(dialog.confirmButton().isEnabled(), outcome.name());

            onEdt(() -> dialog.confirmButton().doClick());

            assertEquals(1, resolved.size(), outcome.name());
            assertSame(dialog.result(), resolved.get(0), outcome.name());
            assertEquals(prompt.resolve().action(), dialog.result().action(), outcome.name());
        }
    }

    // ------------------------------------------------------------------ FR-40: the player chooses

    @Test
    @DisplayName("FR-40: every eligible opponent piece is offered, and the active player's own never is")
    void everyEligibleOpponentPieceIsOffered() throws Exception {
        Piece mine = onTrack(red, 0, 4);
        Piece greenPiece = onTrack(green, 0, 30);
        Piece bluePiece = onTrack(blue, 1, 17);
        onTrack(red, 1, 20);

        SurpriseFactory factory = new SurpriseFactory(board, players, fixed(3));
        SurprisePrompt prompt = factory.promptFor(SurpriseOutcome.OPPONENT_BACK, mine);
        SurpriseDialog dialog = build(prompt, names(), factory::resolve, new ArrayList<>());

        assertEquals(List.of(greenPiece, bluePiece), prompt.targets());
        assertEquals(2, dialog.targetCount());
        assertTrue(dialog.targetChip(0).getText().contains("Shafeek"));
        assertTrue(dialog.targetChip(1).getText().contains("Rakan"));
        for (int i = 0; i < dialog.targetCount(); i++) {
            assertFalse(dialog.targetChip(i).getText().contains("Mohamed"),
                    "the active player's own pieces must never be offered (FR-40)");
        }
        assertEquals("Regular die: 4. The piece you choose moves back 2 squares.",
                text(dialog, "surprise.roll"));
    }

    @Test
    @DisplayName("FR-40: nothing can be confirmed before a piece has been chosen")
    void nothingIsConfirmedWithoutAChoice() throws Exception {
        Piece mine = onTrack(red, 0, 4);
        onTrack(green, 0, 12);
        SurpriseFactory factory = new SurpriseFactory(board, players, fixed(5));
        SurprisePrompt prompt = factory.promptFor(SurpriseOutcome.OPPONENT_BACK, mine);
        List<SurpriseResult> resolved = new ArrayList<>();
        SurpriseDialog dialog = build(prompt, names(), factory::resolve, resolved);

        assertTrue(prompt.needsChoice());
        assertFalse(dialog.confirmButton().isEnabled(), "the button must be disabled until a piece is picked");

        onEdt(dialog::confirm);

        assertFalse(dialog.resolved());
        assertTrue(resolved.isEmpty());
        assertNotNull(dialog.errorMessage(), "the player is told to choose first");
    }

    @Test
    @DisplayName("FR-40: the piece the player chose is the piece the Model is told to push, not the leader")
    void theChosenPieceIsThePushedPiece() throws Exception {
        Piece mine = onTrack(red, 0, 4);
        Piece leader = onTrack(green, 0, 40);
        Piece trailing = onTrack(blue, 0, 2);

        SurpriseFactory factory = new SurpriseFactory(board, players, fixed(4));
        SurprisePrompt prompt = factory.promptFor(SurpriseOutcome.OPPONENT_BACK, mine);
        List<SurpriseResult> resolved = new ArrayList<>();
        SurpriseDialog dialog = build(prompt, names(), factory::resolve, resolved);

        assertEquals(List.of(leader, trailing), prompt.targets());
        onEdt(() -> dialog.targetChip(1).doClick());
        assertSame(trailing, dialog.picked());
        assertTrue(dialog.confirmButton().isEnabled());

        onEdt(() -> dialog.confirmButton().doClick());

        assertEquals(1, resolved.size());
        SurpriseResult result = resolved.get(0);
        assertSame(SurpriseAction.MOVE_STEPS, result.action());
        assertSame(trailing, result.piece(), "the piece the player chose, not the leading one");
        assertEquals(-3, result.steps(), "a Regular die of 5 pushes back 3");
        assertSame(PlayerColor.BLUE, result.affected());
    }

    @Test
    @DisplayName("FR-40: with no eligible opponent piece the card explains it and resolves to nothing")
    void noEligibleOpponentIsExplained() throws Exception {
        Piece mine = onTrack(red, 0, 4);
        PiecePlacement.place(green.pieces().get(0), board.homeStartIndex());
        PiecePlacement.place(blue.pieces().get(0), board.finishIndex());

        SurpriseFactory factory = new SurpriseFactory(board, players, fixed(5));
        SurprisePrompt prompt = factory.promptFor(SurpriseOutcome.OPPONENT_BACK, mine);
        List<SurpriseResult> resolved = new ArrayList<>();
        SurpriseDialog dialog = build(prompt, names(), factory::resolve, resolved);

        assertFalse(prompt.needsChoice());
        assertEquals(0, dialog.targetCount());
        assertEquals(SurprisePrompt.NO_TARGET_NOTE, text(dialog, "surprise.noTargets"));
        assertNull(find(dialog, "surprise.pickLabel"), "there is nothing to pick");
        assertTrue(dialog.confirmButton().isEnabled());

        onEdt(() -> dialog.confirmButton().doClick());

        assertEquals(1, resolved.size());
        assertSame(SurpriseAction.NOTHING, resolved.get(0).action());
        assertTrue(resolved.get(0).cancelled());
    }

    @Test
    @DisplayName("FR-40: a pick the Model refuses is reported, and the player may choose again")
    void aRefusedPickIsReportedAndCanBeRetried() throws Exception {
        Piece mine = onTrack(red, 0, 4);
        Piece stale = onTrack(green, 0, 12);
        Piece other = onTrack(blue, 0, 20);

        SurpriseFactory factory = new SurpriseFactory(board, players, fixed(3));
        SurprisePrompt prompt = factory.promptFor(SurpriseOutcome.OPPONENT_BACK, mine);
        List<SurpriseResult> resolved = new ArrayList<>();
        SurpriseDialog dialog = build(prompt, names(), factory::resolve, resolved);

        onEdt(() -> dialog.targetChip(0).doClick());
        // a later effect of the same chain moved that piece out of reach before the player confirmed
        PiecePlacement.place(stale, board.homeStartIndex());
        onEdt(dialog::confirm);

        assertFalse(dialog.resolved(), "a stale pick must not be pushed");
        assertTrue(resolved.isEmpty());
        assertNotNull(dialog.errorMessage());
        assertNull(dialog.picked(), "the refused pick is cleared");
        assertFalse(dialog.confirmButton().isEnabled());

        onEdt(() -> dialog.targetChip(1).doClick());
        onEdt(dialog::confirm);

        assertEquals(1, resolved.size());
        assertSame(other, resolved.get(0).piece());
        assertNull(dialog.errorMessage(), "the message goes away once a valid piece is chosen");
    }

    @Test
    @DisplayName("FR-40: a refused pick leaves no chip looking selected, and the player can finish with another")
    void aRefusedPickClearsTheSelectionItself() throws Exception {
        Piece mine = onTrack(red, 0, 4);
        Piece stale = onTrack(green, 0, 12);
        Piece other = onTrack(blue, 0, 20);

        SurpriseFactory factory = new SurpriseFactory(board, players, fixed(3));
        SurprisePrompt prompt = factory.promptFor(SurpriseOutcome.OPPONENT_BACK, mine);
        List<SurpriseResult> resolved = new ArrayList<>();
        SurpriseDialog dialog = build(prompt, names(), factory::resolve, resolved);

        assertEquals(List.of(stale, other), prompt.targets());
        onEdt(() -> dialog.targetChip(0).doClick());
        assertTrue(dialog.targetChip(0).isSelected(), "the chip the player clicked is selected");

        // a later effect of the same chain moved that piece out of reach before the player confirmed
        PiecePlacement.place(stale, board.homeStartIndex());
        onEdt(dialog::confirm);

        // the card must not keep showing a pick it no longer holds
        for (int i = 0; i < dialog.targetCount(); i++) {
            assertFalse(dialog.targetChip(i).isSelected(),
                    "chip " + i + " still looks selected after the Model refused the pick");
        }
        assertNull(dialog.picked());
        assertFalse(dialog.confirmButton().isEnabled());
        assertNotNull(dialog.errorMessage());
        assertFalse(dialog.resolved());
        assertTrue(resolved.isEmpty());

        // and the player can still finish the surprise with a piece that is still eligible
        onEdt(() -> dialog.targetChip(1).doClick());
        assertTrue(dialog.targetChip(1).isSelected());
        assertFalse(dialog.targetChip(0).isSelected());
        assertSame(other, dialog.picked());
        assertTrue(dialog.confirmButton().isEnabled());

        onEdt(() -> dialog.confirmButton().doClick());

        assertEquals(1, resolved.size());
        assertSame(other, resolved.get(0).piece());
        assertSame(SurpriseAction.MOVE_STEPS, resolved.get(0).action());
        assertNull(dialog.errorMessage());
    }

    @Test
    @DisplayName("A second confirmation cannot resolve the same surprise again")
    void onlyTheFirstConfirmationCounts() throws Exception {
        Piece mine = onTrack(red, 0, 4);
        Piece victim = onTrack(green, 0, 12);
        SurpriseFactory factory = new SurpriseFactory(board, players, fixed(3));
        SurprisePrompt prompt = factory.promptFor(SurpriseOutcome.OPPONENT_BACK, mine);
        List<SurpriseResult> resolved = new ArrayList<>();
        SurpriseDialog dialog = build(prompt, names(), factory::resolve, resolved);

        onEdt(() -> dialog.targetChip(0).doClick());
        onEdt(() -> dialog.confirmButton().doClick());
        SurpriseResult first = dialog.result();
        onEdt(() -> dialog.confirmButton().doClick());
        onEdt(dialog::confirm);

        assertEquals(1, resolved.size(), "the Model must be asked only once");
        assertSame(first, dialog.result());
        assertSame(victim, first.piece());
        for (int i = 0; i < dialog.targetCount(); i++) {
            assertFalse(dialog.targetChip(i).isEnabled(), "chip " + i + " is still clickable");
        }
    }

    // ------------------------------------------------------------------ FR-42 and the open decision

    @Test
    @DisplayName("FR-42: a chain set off by another player's piece names the turn holder, not that owner")
    void anOpponentTriggeredChainNamesTheTurnHolder() throws Exception {
        onTrack(red, 0, 4);
        Piece greenTrigger = onTrack(green, 0, 12);

        SurpriseFactory factory = new SurpriseFactory(board, players, fixed());
        SurprisePrompt prompt = factory.promptFor(SurpriseOutcome.BACK_THREE, greenTrigger, PlayerColor.RED);
        SurpriseDialog dialog = build(prompt, names(), factory::resolve, new ArrayList<>());

        assertTrue(prompt.opponentTriggered());
        assertTrue(text(dialog, "surprise.activePlayer").contains("Mohamed"), "Mohamed holds the turn");
        String chain = text(dialog, "surprise.chainNote");
        assertTrue(chain.contains("Shafeek"), chain);
        assertTrue(chain.contains("Mohamed"), chain);
    }

    @Test
    @DisplayName("D2 FR-42: a turn outcome in such a chain is shown as the active player's, and resolves to them")
    void turnOutcomesInSuchAChainBelongToTheActivePlayer() throws Exception {
        // Product Owner decision D2, approved 21/11/2026: the outcome falls on the active player (Mohamed,
        // RED), never on the owner of the piece that landed (Shafeek, GREEN). The obsolete "undecided" note
        // and the neutral wording that named nobody are gone.
        onTrack(red, 0, 4);
        Piece greenTrigger = onTrack(green, 0, 12);

        int checked = 0;
        for (SurpriseOutcome outcome : SurpriseOutcome.values()) {
            if (!outcome.affectsTurnOnly()) {
                continue;
            }
            SurpriseFactory factory = new SurpriseFactory(board, players, fixed());
            SurprisePrompt prompt = factory.promptFor(outcome, greenTrigger, PlayerColor.RED);
            List<SurpriseResult> resolved = new ArrayList<>();
            SurpriseDialog dialog = build(prompt, names(), factory::resolve, resolved);

            assertNull(find(dialog, "surprise.undecided"),
                    outcome + ": the undecided note is gone now that D2 is decided");

            // The card shows the Model's own wording, which is written to the player whose turn it is.
            assertEquals(outcome.title(), text(dialog, "surprise.title"), outcome.name());
            assertEquals(outcome.description(), text(dialog, "surprise.description"), outcome.name());

            // The pill names the active player, and the chain note explains how the square was reached
            // without ever claiming the opponent is the one affected.
            assertTrue(text(dialog, "surprise.activePlayer").contains("Mohamed"),
                    outcome + ": the active player is named");
            String chain = text(dialog, "surprise.chainNote");
            assertTrue(chain.contains("Shafeek") && chain.contains("Mohamed"), outcome + ": " + chain);

            onEdt(() -> dialog.confirmButton().doClick());
            SurpriseResult result = resolved.get(0);
            assertSame(PlayerColor.RED, result.affected(),
                    outcome + " (D2) falls on the active player, not on the owner of the piece");
            assertSame(PlayerColor.RED, result.turnAffected(), outcome.name());
            assertSame(PlayerColor.GREEN, result.triggerOwner(), outcome.name());
            checked++;
        }
        assertEquals(2, checked, "both SKIP_TURN and EXTRA_ROLL were checked");
    }

    @Test
    @DisplayName("An ordinary surprise shows no chain note, and no card ever shows an undecided note")
    void ordinarySurprisesShowNoNotes() throws Exception {
        Piece mine = onTrack(red, 0, 4);
        onTrack(green, 0, 12);

        for (SurpriseOutcome outcome : SurpriseOutcome.values()) {
            SurpriseFactory factory = new SurpriseFactory(board, players, fixed(3));
            SurprisePrompt prompt = factory.promptFor(outcome, mine);
            SurpriseDialog dialog = build(prompt, names(), factory::resolve, new ArrayList<>());
            assertNull(find(dialog, "surprise.chainNote"), outcome.name());
            assertNull(find(dialog, "surprise.undecided"), outcome.name());
            assertEquals(outcome.title(), text(dialog, "surprise.title"), outcome.name());
        }
    }

    // ------------------------------------------------------------------ input validation

    @Test
    @DisplayName("The card needs a surprise, a name lookup, a Model rule and somewhere to send the result")
    void theCardNeedsItsParts() throws Exception {
        Piece mine = onTrack(red, 0, 4);
        onTrack(green, 0, 12);
        SurpriseFactory factory = new SurpriseFactory(board, players, fixed(3));
        SurprisePrompt prompt = factory.promptFor(SurpriseOutcome.OPPONENT_BACK, mine);

        onEdt(() -> {
            assertThrows(NullPointerException.class,
                    () -> new SurpriseDialog(null, names(), factory::resolve, r -> { }));
            assertThrows(NullPointerException.class,
                    () -> new SurpriseDialog(prompt, null, factory::resolve, r -> { }));
            assertThrows(NullPointerException.class,
                    () -> new SurpriseDialog(prompt, names(), null, r -> { }));
            assertThrows(NullPointerException.class,
                    () -> new SurpriseDialog(prompt, names(), factory::resolve, null));
        });
    }

    @Test
    @DisplayName("A Model rule that resolves nothing is a fault, not a silent empty card")
    void aResolverThatReturnsNothingFails() throws Exception {
        Piece mine = onTrack(red, 0, 7);
        onTrack(green, 0, 12);
        SurpriseFactory factory = new SurpriseFactory(board, players, fixed(3));
        SurprisePrompt prompt = factory.promptFor(SurpriseOutcome.OPPONENT_BACK, mine);
        SurpriseDialog dialog = build(prompt, names(), (p, t) -> null, new ArrayList<>());

        onEdt(() -> dialog.targetChip(0).doClick());
        onEdt(() -> assertThrows(IllegalStateException.class, dialog::confirm));
        assertFalse(dialog.resolved());
    }

    @Test
    @DisplayName("The Model rule is asked only when there is a pick to check")
    void theResolverIsOnlyAskedForAPick() throws Exception {
        Piece mine = onTrack(red, 0, 7);
        Piece victim = onTrack(green, 0, 12);
        List<Piece> asked = new ArrayList<>();
        SurpriseDialog.Resolver recording = (p, t) -> {
            asked.add(t);
            return p.resolve(t);
        };

        SurpriseFactory noChoice = new SurpriseFactory(board, players, fixed());
        SurpriseDialog plain = build(noChoice.promptFor(SurpriseOutcome.BACK_THREE, mine), names(),
                recording, new ArrayList<>());
        onEdt(() -> plain.confirmButton().doClick());
        assertTrue(plain.resolved());
        assertTrue(asked.isEmpty(), "nothing was picked, so there was nothing to re-check");

        SurpriseFactory push = new SurpriseFactory(board, players, fixed(3));
        SurpriseDialog pushDialog = build(push.promptFor(SurpriseOutcome.OPPONENT_BACK, mine), names(),
                recording, new ArrayList<>());
        onEdt(() -> pushDialog.targetChip(0).doClick());
        onEdt(() -> pushDialog.confirmButton().doClick());
        assertEquals(List.of(victim), asked, "the pick went to the Model to be re-checked");
    }

    // ------------------------------------------------------------------ NFR-05, partly

    @Test
    @DisplayName("NFR-05 (part): building and laying out the card is far under one second")
    void buildingTheCardIsFast() throws Exception {
        Piece mine = onTrack(red, 0, 4);
        onTrack(green, 0, 12);
        onTrack(blue, 0, 20);
        SurpriseFactory warm = new SurpriseFactory(board, players, fixed(3));
        build(warm.promptFor(SurpriseOutcome.OPPONENT_BACK, mine), names(), warm::resolve, new ArrayList<>());

        SurpriseFactory factory = new SurpriseFactory(board, players, fixed(3));
        SurprisePrompt prompt = factory.promptFor(SurpriseOutcome.OPPONENT_BACK, mine);
        long start = System.nanoTime();
        SurpriseDialog dialog = build(prompt, names(), factory::resolve, new ArrayList<>());
        long millis = (System.nanoTime() - start) / 1_000_000;

        assertNotNull(dialog);
        assertTrue(millis < 500, "the card took " + millis + " ms to build and lay out");
    }
}
