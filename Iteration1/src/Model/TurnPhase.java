package Model;

/** Phases of a turn (NFR-09 "State (turn phases)"); the controller moves between them. */
public enum TurnPhase {
    /** The active player may roll. */
    AWAITING_ROLL,
    /** Dice animation is running. */
    ROLLING,
    /** Legal pieces are highlighted and the player must click one. */
    AWAITING_MOVE,
    /** A piece is moving on the board. */
    MOVING,
    /** A message, question or surprise is being handled. */
    RESOLVING,
    /** All but one player have finished. */
    GAME_OVER
}
