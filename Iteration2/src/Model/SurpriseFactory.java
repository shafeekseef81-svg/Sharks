package Model;

import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * The one place that creates surprises: it draws an outcome with the probabilities of FR-39, rolls the
 * Regular die of a push, and collects the opponent pieces the active player may choose from (FR-40).
 * Task T2-09, owner: Mohamed Zereny.
 *
 * <p><b>On the word "factory".</b> Issue #60 asks for surprise outcomes "created by the factory", and this
 * class is that single creation point: nothing else in the game draws an outcome or rolls the die for a push,
 * so the surprise rules sit in one class and a test can drive them with a fixed random source. It is
 * <i>not</i> a second <b>Factory Method</b> in the sense of GoF, and it is deliberately not a subclass of
 * {@link QuestionFactory}. Factory Method earns its place in T2-08 because a question square carries a level
 * of its own and each level produces its own kind of product, so the creator varies by subclass. A surprise
 * square has no levels and no family of products: there is one fixed table of six outcomes. Giving it an
 * abstract creator and six subclasses would add a class hierarchy with nothing to vary, which is exactly the
 * sort of pattern-for-its-own-sake the course warns against. The graded Factory Method of this team stays the
 * question factory of T2-08 ({@code Iteration2/docs/T2-08_FactoryMethod.md}).
 *
 * <p><b>What it does not do.</b> It moves no piece, changes no turn and touches no player state. Its answer
 * is a {@link SurprisePrompt}, which becomes a {@link SurpriseResult}; carrying that out, with the movement
 * rules, the Double Blocks, the captures and the order of a chain, belongs to the effect commands of T2-10,
 * and the turn changes belong to T2-13.
 *
 * <p><b>Repeatable.</b> The random source is handed in, as in {@link QuestionFactory#forBank}, so a seeded
 * game draws the same surprises again and a test can script every draw (NFR-13, SDP 6.2).
 */
public final class SurpriseFactory {

    private final Board board;
    private final List<Player> players;
    private final Random random;

    /**
     * @param board the board of the game, for the end of the shared track ({@link Board#homeStartIndex()})
     * @param players the players of the game
     * @param random the random source of the game: it draws the outcome and rolls the Regular die of a push
     * @throws NullPointerException if an argument or a player is null
     * @throws IllegalArgumentException if there are fewer than two players (FR-06)
     */
    public SurpriseFactory(Board board, List<Player> players, Random random) {
        this.board = Objects.requireNonNull(board, "board");
        this.players = List.copyOf(Objects.requireNonNull(players, "players"));
        this.random = Objects.requireNonNull(random, "random");
        if (this.players.size() < 2) {
            throw new IllegalArgumentException("A game has at least two players (FR-06), not "
                    + this.players.size());
        }
    }

    /**
     * Draws one surprise for the piece that stopped on a surprise square (FR-39), and prepares everything the
     * player has to be shown, including the choice of FR-40.
     *
     * <p>It draws in a fixed order, so a seeded or scripted random source is easy to read: first the outcome
     * from the 0-99 table of {@link SurpriseOutcome#at(int)}, then, only for a push, the Regular die.
     *
     * @param subject the piece that landed on the surprise square; it must be on the board and belong to one
     *        of the players of this game
     * @return the drawn surprise, ready to be shown and then resolved
     * @throws NullPointerException if {@code subject} is null
     * @throws IllegalArgumentException if the piece is not on the board or belongs to no player of this game
     */
    public SurprisePrompt draw(Piece subject) {
        requireSubject(subject);
        return promptFor(SurpriseOutcome.draw(random), subject);
    }

    /**
     * Prepares one named outcome instead of drawing it: the same creation step as {@link #draw(Piece)}
     * without the draw. It is what a test uses to check one outcome, and what a demo screen would use to walk
     * through the six outcomes in turn.
     *
     * @param outcome the outcome to prepare
     * @param subject the piece that landed on the surprise square
     * @return the prepared surprise; for a push it already carries the Regular die and the eligible pieces
     * @throws NullPointerException if an argument is null
     * @throws IllegalArgumentException if the piece is not on the board or belongs to no player of this game
     */
    public SurprisePrompt promptFor(SurpriseOutcome outcome, Piece subject) {
        Objects.requireNonNull(outcome, "outcome");
        requireSubject(subject);
        PlayerColor active = subject.color();
        if (!outcome.needsOpponentPiece()) {
            return new SurprisePrompt(outcome, subject, active, 0, List.of());
        }
        int regular = RegularDiceStrategy.rollRegular(random);
        return new SurprisePrompt(outcome, subject, active, regular, eligibleTargets(active));
    }

    /**
     * The opponent pieces the active player may choose for a push (FR-40). Never the leading piece by itself:
     * the whole list is offered, and the player picks.
     *
     * @param active the colour of the active player
     * @return the eligible pieces, possibly empty; the list cannot be modified
     * @throws NullPointerException if {@code active} is null
     */
    public List<Piece> eligibleTargets(PlayerColor active) {
        return OpponentTargets.eligible(players, board, active);
    }

    /** @return the players of this game; the list cannot be modified */
    public List<Player> players() {
        return players;
    }

    private void requireSubject(Piece subject) {
        Objects.requireNonNull(subject, "subject");
        if (!subject.onBoard()) {
            throw new IllegalArgumentException("A surprise square is reached by a piece on the board, not by "
                    + subject);
        }
        boolean known = players.stream().anyMatch(player -> player.pieces().contains(subject));
        if (!known) {
            throw new IllegalArgumentException(subject + " belongs to no player of this game");
        }
    }

    @Override
    public String toString() {
        return "Surprise squares of the " + board.difficulty().label() + " board";
    }
}
