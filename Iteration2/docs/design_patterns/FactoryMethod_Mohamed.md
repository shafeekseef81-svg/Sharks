# Factory Method — educational questions by square level

Section 3 of `iteration2_dp_Sharks` (N2-05, [#71](https://github.com/shafeekseef81-svg/Sharks/issues/71)).

Task **T2-08** · Issue [#59](https://github.com/shafeekseef81-svg/Sharks/issues/59) · SRS **FR-34, FR-35, FR-36, FR-37** (FR-38: see section 6)
Owner: **Mohamed Zereny**. Graded design pattern of this member (Iteration 2 plan, section 4).

> Headings 1–6 follow the shared structure in `iteration2_dp_Sharks.md` section 3, so the four sections read
> as one document. Every class name, file path, line number and test count below was checked against the code
> of this task on 22/11/2026, against the merged code on `iteration-2` (`d445a38`); T2-08 reached it through
> PR #89. The line numbers below are unchanged from the branch they were written on (`3352d4f`), because the
> merge did not touch these files.

## 1. The design pattern used

**Factory Method** — a *creational* pattern of the GoF catalogue.

It defines an operation for creating an object in a base class, but leaves the choice of *which* object to
create to subclasses: "define an interface for creating an object, but let subclasses decide which class to
instantiate." The base class here is the thing a question square asks for a question; each level of question
is a subclass of it. The game holds the creator through its abstract type and never names a concrete one.

## 2. The problem it solves

A question square carries a level of its own, drawn in its own colour, and that level is **not** the level of
the board: an Easy board has no question squares at all, a Medium board has six (two of each question level)
and a Hard board has nine (three of each), per SRS Table 1. When a piece stops on such a square the game must

* put a question of **that square's** level in front of the player, taken from **that level's** own questions
  (FR-34);
* move the piece by **that level's** own number of steps — 1, 2 or 3 — forward for a right answer and back for
  a wrong one (FR-35);
* show afterwards whether the answer was right, and which answer was the right one (FR-36);
* never ask a question again before every question of its level has been asked, so each level needs its own
  memory of what it has already dealt (FR-37).

So the game needs an object it can hand a square to, and get back "the question to ask now", without the
caller knowing where that question came from, how the no-repeat rule is kept, or how many steps the answer is
worth.

Writing this as one method with a `switch (level)` would put all three levels in a single place that has to be
edited for every change, and would let the three levels differ only in the data they carry, never in how they
behave. Factory Method removes the conditional entirely: the decision is made by *which subclass the game is
holding*, that is, by polymorphism.

## 3. Its structure and participating classes

| GoF role | Class in this project | What it does |
|---|---|---|
| **Creator** (abstract) | `Model.QuestionFactory` | Declares the **factory method** `protected abstract Question createQuestion()` and uses it in its own operation `public final Question ask()`. Also holds `judge(question, answer)`, the rule that turns an answer into ±1/2/3 steps (FR-35, FR-36) — the same rule at every level, so it belongs to the creator, not to the subclasses. |
| **Concrete Creator** | `Model.EasyQuestionFactory` | Owns the **Easy** `QuestionDeck` and overrides the factory method to deal the next Easy question. |
| **Concrete Creator** | `Model.MediumQuestionFactory` | The same for **Medium**. |
| **Concrete Creator** | `Model.HardQuestionFactory` | The same for **Hard**. |
| **Product** | `Model.Question` | A `record`, so immutable: id, level, text, exactly four different answers and the index of the one correct answer (FR-34). Its `steps()` reads the step count of its own level. |
| Supporting | `Model.QuestionDeck` | One level's questions, shuffled and dealt without repeats until the level is used up, then shuffled again (FR-37). Each concrete creator owns one. |
| Supporting | `Model.QuestionBank` | The read-only set of questions of a game, grouped by level. |
| Supporting | `Model.QuestionResult` | Whether the answer was right, which answer was right, and the signed number of steps (FR-35, FR-36). |
| Supporting | `Model.CourseQuestions` | The 36 course questions (12 per level), reused from the Assignment 3 prototype through the reuse gate (SDP 6.1). |

**What a Concrete Creator does and does not do.** Each one differs from the others in exactly one respect:
**which `QuestionDeck` it deals from**, and therefore which level of question it produces. None of them
implements the movement of FR-35. The step count belongs to the level — `Question.steps()` returns
`level.questionSteps()`, which reaches `LevelConfig` — and the **sign** is applied once, in the abstract
Creator's `judge()`:

```java
return new QuestionResult(question, chosenIndex, correct,
        correct ? question.steps() : -question.steps());
```

So "Easy means ±1" is a fact about the *level*, not behaviour coded into `EasyQuestionFactory`. That is why
`judge()` is `final` on the Creator: one rule for every level, and a subclass cannot drift from it.

### The structure

```
            +------------------------------------------+
            |  QuestionFactory              <<Creator>>|
            |------------------------------------------|
            | + ask() : Question              (final)  |---------> Question   <<Product>>
            | + judge(Question, int)                   |           (immutable: id, level,
            |          : QuestionResult       (final)  |            text, 4 answers, correct)
            | + level() : Difficulty       <<abstract>>|
            | # createQuestion() : Question<<abstract>>|  <== THE FACTORY METHOD
            | + forBank(bank, random)         (static) |
            +------------------------------------------+
                                 ^ extends
            +--------------------+--------------------+
            |                    |                    |
 +--------------------+ +----------------------+ +--------------------+
 | EasyQuestionFactory| | MediumQuestionFactory| | HardQuestionFactory|  <<Concrete Creators>>
 | # createQuestion() | | # createQuestion()   | | # createQuestion() |
 |   +/-1 step        | |   +/-2 steps         | |   +/-3 steps       |
 +---------+----------+ +----------+-----------+ +---------+----------+
           | owns one              | owns one              | owns one
           v                       v                       v
     QuestionDeck (EASY)    QuestionDeck (MEDIUM)   QuestionDeck (HARD)    <-- FR-37, one per level
           +-----------------------+-----------------------+
                            reads from QuestionBank <-- CourseQuestions (36)
```

### How it runs in a turn

```
a piece stops on a question square
        |  square.questionLevel()           <-- the colour of the square (SRS Table 1)
        v
Map<Difficulty, QuestionFactory>            <-- built once per game by QuestionFactory.forBank(...)
        |
        v
QuestionFactory.ask()  --calls-->  createQuestion()   <-- the factory method, overridden per level
        |                                |  deals from that level's own QuestionDeck (FR-37)
        v                                v
     Question  (Product)  <--------------+
        |  the dialog of T2-11 shows it; the player picks an answer
        v
QuestionFactory.judge(question, chosenIndex)
        v
QuestionResult: correct? - the correct answer (FR-36) - +1/+2/+3 or -1/-2/-3 steps (FR-35)
        v
the effect queue of T2-10 moves the piece by those steps
```

`ask()` checks what its subclass produced: it refuses a `null` and refuses a question of the wrong level, so a
mistake in a subclass fails loudly where it happens instead of showing a Hard question on an Easy square.

## 4. Exactly where it appears in the code

All paths are relative to the repository root. Line numbers are from commit `3352d4f`.

| Role | File | Lines | What to look at |
|---|---|---|---|
| Creator | `Iteration2/src/Model/QuestionFactory.java` | 134 | `public abstract class QuestionFactory` (39); `level()` (48); **`protected abstract Question createQuestion()` (56)** — the factory method; `ask()` (65); `judge()` (93); `forBank()` (120) |
| Concrete Creator | `Iteration2/src/Model/EasyQuestionFactory.java` | 46 | `extends QuestionFactory`; `createQuestion()` deals from the Easy deck |
| Concrete Creator | `Iteration2/src/Model/MediumQuestionFactory.java` | 46 | the same for Medium |
| Concrete Creator | `Iteration2/src/Model/HardQuestionFactory.java` | 46 | the same for Hard |
| Product | `Iteration2/src/Model/Question.java` | 100 | immutable; four different answers, one correct (FR-34) |
| Supporting | `Iteration2/src/Model/QuestionDeck.java` | 97 | the no-repeat rule of FR-37 |
| Supporting | `Iteration2/src/Model/QuestionBank.java` | 113 | the questions of a game, grouped by level |
| Supporting | `Iteration2/src/Model/QuestionResult.java` | 56 | right/wrong, the correct answer, the signed steps |
| Supporting | `Iteration2/src/Model/CourseQuestions.java` | 186 | the 36 course questions, 12 per level |
| Tests | `Iteration2/test/Model/QuestionFactoryTest.java` | 418 | 18 tests, see section 5 |

The only place in the whole program that names the three concrete creators is
`QuestionFactory.forBank(bank, random)` (line 120), which builds them once when a game starts and returns an
`EnumMap<Difficulty, QuestionFactory>`. After that the game looks a creator up by the square's level and calls
`ask()`.

**What Open/Closed does and does not buy here.** Adding a fourth *question* level leaves this pattern's own
code untouched: one new `QuestionFactory` subclass, plus one line in `forBank`. That is the real benefit, and
it is worth stating precisely rather than overstating it - **a fourth difficulty level of the game is a much
larger change than that**, and it would also need at least:

* a new constant in the `Difficulty` enum;
* a `LevelConfig` entry for it: grid size, outer rings, track sizes, and the counts of protection, arrow,
  question and surprise squares, plus its `questionSteps`;
* questions of that level in `CourseQuestions` / `QuestionBank`, or `forBank` refuses the level (FR-34);
* a `DiceStrategy` and its movement formula (FR-16, FR-17);
* board geometry in `BoardFactory`, and the level offered on the setup screen (FR-05).

So the claim is narrow and holds only for the question factory: **this pattern is not the part that would have
to change.** Nothing that already works inside it is touched.

### Screenshots for the Word/PDF document

**Not yet taken.** To be captured from the final code when the document is assembled (N2-05 step 4). Proposed
list, in the order they should appear in this section:

| # | Screenshot | Exact location | Shows |
|---|---|---|---|
| S1 | The abstract Creator with the factory method | `Iteration2/src/Model/QuestionFactory.java`, lines 39–70 (class declaration, `level()`, `createQuestion()`, `ask()`) | that the Creator is abstract and declares the factory method, and that `ask()` is written against the abstract product |
| S2 | One Concrete Creator in full | `Iteration2/src/Model/EasyQuestionFactory.java`, lines 13–40 | the `extends`, the `@Override` of `createQuestion()`, and that the whole class is just a deck and one dealt question - no movement rule in it |
| S3 | The three creators built in one place | `Iteration2/src/Model/QuestionFactory.java`, lines 120–130 (`forBank`) | that the only mention of the concrete classes is one call per game, not a `switch` in the game logic |
| S4 | The judging rule | `Iteration2/src/Model/QuestionFactory.java`, lines 93–118 (`judge`) | FR-35 and FR-36 in one place for every level |
| S5 | The no-repeat deck | `Iteration2/src/Model/QuestionDeck.java`, the `deal()` method | FR-37 kept per level |
| S6 | The test run | terminal output of `Iteration2/test.sh`, filtered to `Model.QuestionFactoryTest` | 18 tests passing, with the `@DisplayName` texts naming the FRs |
| S7 | The question dialog in the running game | the game window on a Medium or Hard board, on a question square | the pattern's product reaching the player (the dialog itself is T2-11, cross-reference only) |

S1–S5 are code screenshots and S6 is terminal output, so all six can be taken from the final build without
playing a game. **S7 needs a real game run** and must be taken by hand when the final build is played; it
belongs to the T2-15 and N2-04 evidence as well.

## 5. Tests and verification

`Iteration2/test/Model/QuestionFactoryTest.java` — **18 tests, all passing.**

Measured twice, both with `Iteration2/test.sh` on Java 17 (Temurin 17.0.17):

| When | Where | `Model.QuestionFactoryTest` | Whole suite |
|---|---|---|---|
| 21/11/2026 | the T2-08 branch, before #89 merged (`3352d4f`) | 18 found, **18 passed**, 0 failed | 415 found, **415 passed**, 0 failed (41 containers) |
| 22/11/2026 | `iteration-2` `d445a38`, after #89 merged | 18 found, **18 passed**, 0 failed | 531 found, **531 passed**, 0 failed |

The suite grew from 415 to 531 because other tasks merged in the meantime, not because this task changed; its
own 18 tests are the same 18.

What the tests show, grouped by what they verify:

| Group | Tests | What is shown |
|---|---|---|
| The pattern itself | 3 | the Creator is abstract and `createQuestion` is its abstract, `protected` factory method returning the product, and each of the three concrete creators overrides it; the game asks a Creator and not a level; a creator that returns the wrong level, or nothing at all, is refused |
| FR-34 (the question shown) | 5 | every one of the 36 questions has four different, non-blank answers and exactly one correct answer; a question of the square's level is asked and never of another level; a question cannot be built with the wrong number of answers or a bad correct index; a level with no question at all is refused when the game is set up, not in the middle of it; on 20 random Medium and Hard boards every question square can be answered |
| FR-35 (the movement) | 4 | a correct answer moves 1, 2 or 3 steps forward by the level of the question; a wrong answer moves the same number back; not answering counts as wrong; a result that disagrees with its own question cannot be built |
| FR-36 (the feedback) | covered by the FR-35 group | the result always carries the correct answer, also after a wrong answer |
| FR-37 (no repeats) | 3 | over full rounds per level no question comes up again until the level is used up; the deck is shuffled again once the level is exhausted and keeps dealing; a level with a single question is allowed and simply repeats it |
| Judging and isolation | 3 | judging refuses an answer that is not one of the four, or a question of another level; the same seed asks the same questions in the same order and a different seed does not; a factory is tied to one game and two games do not share their questions (NFR-08) |

The whole pattern is pure Model: no Swing, no controller, no dependency on the board or the turn manager, so
it is tested entirely on its own (NFR-08, NFR-20). Each creator takes the game's `Random`, so a game started
with a fixed seed asks the same questions in the same order — which is what makes these tests repeatable.

**Independent verification.** SDP 5.3.4 asks for rule tests by a member who did not write the code. The 18
tests above are by the author. Independent tests of the question rules belong to **T2-12** (Rakan Ziyan,
[#63](https://github.com/shafeekseef81-svg/Sharks/issues/63)); the T2-14 coverage snapshot
(`Iteration2/docs/T2-14_RTM_Coverage.md`) lists FR-36 and FR-37 among the weak spots for exactly that reason.
This section should not be read as claiming independent verification yet.

## 6. Trade-offs and alternatives

**What was gained**

* No `switch` on the level anywhere in the game: the level of a square selects a creator, and the creator does
  the rest.
* FR-37 is kept per level automatically, because each concrete creator owns its own deck.
* The rule that turns an answer into steps lives in one place, so the dialog (T2-11) and the effect queue
  (T2-10) cannot drift apart over it.
* Pure Model, so it is unit-tested on its own and the MVC separation of NFR-08 and NFR-20 is kept.

**The alternatives that were considered and rejected**

| Alternative | Why not |
|---|---|
| **Simple Factory**: one `static Question create(Difficulty level)` with a `switch` | It would work today, but every new kind of question means editing that one tested method, and the levels could only differ in the data passed to a single class, never in behaviour. It is also not a GoF pattern, and this task is graded on one. |
| **Abstract Factory** | It makes *families* of related products (question *and* surprise *and* …). Iteration 2 has one product family here, so it would add a second abstraction with nothing to put in it. |
| **Strategy** | It varies an *algorithm* chosen from outside. Here what varies is *which object is created*, which is the definition of Factory Method. Strategy is already the graded pattern of the dice (T2-04, Shafeek), and reusing it here would blur both sections of this document. |

### How it differs from a Simple Factory

This is the point the pattern is usually graded on, so it is worth being precise:

* A Simple Factory is **one class with one method and a conditional**. The decision lives in that conditional,
  and adding a kind means changing existing, tested code.
* Factory Method has **no conditional at all**. The decision is made by *which subclass the client is holding*,
  that is, by polymorphism. The creator's own operation, `ask()`, is written once against the abstract product
  and never changes.

**What it costs, stated honestly**

* For the three levels of today, a Simple Factory would have been fewer lines. Factory Method is chosen
  because the levels are an open set — Iteration 3 lets the administrator manage questions (FR-56..FR-61) —
  and because the pattern is a graded deliverable of this task. Three small subclasses is the price.
* The three concrete creators differ today in their deck and their step count, and not yet in more elaborate
  behaviour. That is a real difference, but a modest one; it is the extension point, not the present-day
  complexity, that justifies the pattern.
* `forBank` still names the three subclasses. Some place must; the point is that it is one place, run once per
  game, and not a conditional inside the game logic.

**Scope of this task, and what is deliberately not in it**

* **The step values (1 / 2 / 3) are FR-35, read from the SRS in this repository.** An earlier draft of this
  section said the repository's SRS "stops at the end of section 2, so FR-35 could not be read in the
  original". **That was wrong, and it is corrected here.** The full specification, with the complete FR table
  (FR-01 to FR-65), is in the repository at **`Assignment3_Prototype/Sharks_SRS.pdf`** - 20 pages - and
  **FR-35 is on page 13**:

  > **FR-35** "Correct answers shall move the piece forward 1 (Easy), 2 (Medium), or 3 (Hard) steps;
  > incorrect answers move it back equally."

  That is exactly the +1 / +2 / +3 and −1 / −2 / −3 rule this task implements, so the values are specified and
  not inferred. (The 10-page `SRS_Sharks.pdf` at the repository root is the Assignment 2 document and does end
  after section 2 - it contains no FR table at all. Confusing the two is what produced the wrong claim.)

  FR-34 on the same page is also worth quoting, because it is the requirement the pattern exists to serve:

  > **FR-34** "Landing on a question square shall display to all players a random question of matching
  > difficulty with four options; only the active player may answer."

  No staff approval is needed for the pattern itself, which is one of the patterns taught in the course.
* **The question level is the square's level, not the board's.** This follows the SRS and the project
  description, and the prototype documented the same decision where the task brief read differently
  (`Assignment3_Prototype/README.md`, "Where the documents differ"). It is an existing team decision, not a
  new one.
* **FR-38 (a capture on an unprotected question square happens before the question) is not implemented in this
  task.** Nothing in T2-08 notices that a piece stopped on a question square: the trigger and the ordering of
  effects belong to the effect queue of **T2-10** (`StandardMoveEngine`, `GameModel`, `StandardTurnManager` —
  files owned by Shafeek and Rakan). The capture already happens inside `StandardMoveEngine.apply()` before
  that method returns, so running a question effect on the result will satisfy FR-38. This task provides the
  question and the steps and makes no claim about the ordering.
* **Showing the question** is T2-11 ([#62](https://github.com/shafeekseef81-svg/Sharks/issues/62)) and
  **surprise outcomes** are T2-09 ([#60](https://github.com/shafeekseef81-svg/Sharks/issues/60)). Both are
  separate issues with their own sections of evidence.
