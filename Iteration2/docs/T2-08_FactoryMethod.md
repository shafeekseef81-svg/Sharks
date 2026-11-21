# Factory Method — educational questions by square level

Task **T2-08** · Issue [#59](https://github.com/shafeekseef81-svg/Sharks/issues/59) · SRS **FR-34, FR-35, FR-36, FR-37**
Owner: **Mohamed Zereny**. Graded design pattern of this member (SDP, Iteration 2 plan section 4).

> Draft of the section for `iteration2_dp_Sharks` (N2-05, assembled by Shafeek Saif). It is kept here as its
> own file so that the shared report stays with its owner; copy or adapt it when the report is put together.

## 1. The problem

A question square carries a level of its own, drawn in its own colour, and that level is **not** the level of
the board: an Easy board has no question squares at all, a Medium board has six (two of each question level)
and a Hard board has nine (three of each), per SRS Table 1. When a piece stops on such a square the game has
to put a question of *that* square's level in front of the player, taken from *that* level's own questions,
and then move the piece by *that* level's own number of steps — 1, 2 or 3 — forward for a right answer and
back for a wrong one (FR-35). On top of that, FR-37 forbids a question from coming up again before all the
questions of its level have been asked, so each level needs its own memory of what it has already asked.

So the game needs an object it can hand a square to and get back "the question to ask now", without the
caller knowing where that question came from, how the no-repeat rule is kept, or how many steps the answer is
worth. Writing this as one method with a `switch (level)` would put all three levels into a single place that
has to be edited for every change, and it would let the three levels differ only in the data they carry,
never in how they behave.

## 2. Why Factory Method

**Factory Method** (GoF) is exactly the pattern for "a class that needs an object but lets its subclasses
decide which object to make". The creator here is "the thing a question square asks". The game holds that
creator through its abstract type and calls one operation on it; which concrete question appears, and from
which pool, is decided by the subclass.

The alternatives were considered and rejected:

| Alternative | Why not |
|---|---|
| **Simple Factory**: one `static Question create(Difficulty level)` with a `switch` | It would work today, but every new kind of question means editing that one method, and the levels can never differ in behaviour — only in the data passed to a single class. It is also not one of the patterns taught as a GoF pattern, and this task is graded on a GoF pattern. |
| **Abstract Factory** | It makes families of related products (question *and* surprise *and* …). Iteration 2 has one product family here, so it would add a second abstraction with nothing to put in it. |
| **Strategy** | It varies an *algorithm* that is chosen from outside. Here what varies is *which object is created*, which is the definition of Factory Method. Strategy is already used in this iteration for the dice (T2-04), and reusing it here would blur both. |

### How it differs from a Simple Factory

This is the point the pattern is usually graded on, so it is worth being precise:

* A Simple Factory is **one class with one method and a conditional**. The decision lives in that conditional.
  Adding a kind means changing existing, tested code.
* Factory Method has **no conditional at all**. The decision is made by *which subclass the client is holding*,
  that is, by polymorphism. The creator's own operation, `ask()`, is written once against the abstract product
  and never changes.
* In this implementation the only place that names the three subclasses is
  `QuestionFactory.forBank(bank, random)`, which builds them once when a game starts. After that the game
  looks a creator up in a map by the square's level and calls `ask()`. Adding a fourth question level means
  adding one class and one line in `forBank`; nothing that already works is touched (Open/Closed principle).

## 3. The roles and the classes

| GoF role | Class in this project | What it does |
|---|---|---|
| **Product** | `Model.Question` | An immutable question: id, level, text, exactly four different answers and the index of the one correct answer (FR-34). |
| **Creator** | `Model.QuestionFactory` (abstract) | Declares the **factory method** `protected abstract Question createQuestion()`, and uses it in its own operation `public final Question ask()`. Also holds `judge(question, answer)`, the rule that turns an answer into ±1/2/3 steps (FR-35, FR-36) — the same rule for every level, so it belongs to the creator and not to the subclasses. |
| **Concrete Creator** | `Model.EasyQuestionFactory` | Overrides the factory method to deal the next **Easy** question; ±1 step. |
| **Concrete Creator** | `Model.MediumQuestionFactory` | Overrides it to deal the next **Medium** question; ±2 steps. |
| **Concrete Creator** | `Model.HardQuestionFactory` | Overrides it to deal the next **Hard** question; ±3 steps. |
| Supporting | `Model.QuestionDeck` | One level's questions, shuffled and dealt without repeats until the level is used up, then shuffled again (FR-37). Each concrete creator owns one. |
| Supporting | `Model.QuestionBank` | The read-only set of questions of a game, grouped by level. |
| Supporting | `Model.CourseQuestions` | The 36 course questions (12 per level) reused from the Assignment 3 prototype through the reuse gate (SDP 6.1). |
| Result | `Model.QuestionResult` | Whether the answer was right, which answer was right, and the signed number of steps (FR-35, FR-36). |

## 4. How it runs

```
a piece stops on a question square
          │
          │  square.questionLevel()            ← the colour of the square (SRS Table 1)
          ▼
 Map<Difficulty, QuestionFactory>              ← built once per game by QuestionFactory.forBank(...)
          │
          ▼
 QuestionFactory (Creator)   ──ask()──►  createQuestion()        ← THE FACTORY METHOD
          │                                     │ overridden by
          │                                     ▼
          │                        EasyQuestionFactory / MediumQuestionFactory / HardQuestionFactory
          │                                     │ deals from its own QuestionDeck (FR-37)
          ▼                                     ▼
       Question  (Product)  ◄──────────────────┘
          │
          │  the dialog of T2-11 shows it; the player picks an answer
          ▼
 QuestionFactory.judge(question, chosenIndex)
          │
          ▼
    QuestionResult: correct? · the correct answer (FR-36) · +1/+2/+3 or −1/−2/−3 steps (FR-35)
          │
          ▼
 the effect queue of T2-10 moves the piece by those steps
```

The creator checks what its subclass produced: `ask()` refuses a `null` and refuses a question of the wrong
level, so a mistake in a subclass fails loudly at the moment it happens instead of showing the player a Hard
question on an Easy square.

## 5. Benefits and trade-offs

**Benefits**

* No `switch` on the level anywhere in the game; the level of a square selects a creator, and the creator does
  the rest.
* FR-37 is kept per level automatically, because each concrete creator owns its own deck.
* The rule that turns an answer into steps lives in one place, so the dialog (T2-11) and the effect queue
  (T2-10) cannot drift apart over it.
* The whole thing is pure Model: no Swing, no controller, no dependency on the board or the turn manager, so
  it is tested entirely on its own (NFR-08, NFR-20).
* Each creator takes the game's `Random`, so a game started with a fixed seed asks the same questions in the
  same order — which is what makes the tests repeatable.

**Trade-offs, stated honestly**

* For the three levels of today, a Simple Factory would have been fewer lines. Factory Method is chosen
  because the levels are an open set — Iteration 3 lets the administrator manage questions (FR-56..FR-61) —
  and because the pattern is a graded deliverable of this task. Three small subclasses is the price.
* The three concrete creators differ today in their deck and their step count, and not yet in a more
  elaborate behaviour. That is a real difference, but a modest one; it is the extension point, not the
  present-day complexity, that justifies the pattern.
* `forBank` still names the three subclasses. Some place must; the point is that it is one place, run once
  per game, and not a conditional inside the game logic.

## 6. Evidence from the tests

`Iteration2/test/Model/QuestionFactoryTest.java`, 18 tests, all green in a suite of 415:

* the pattern itself: the creator is abstract, `createQuestion` is the abstract, protected factory method
  returning the product, and each of the three concrete creators overrides it; a creator that returns the
  wrong level or nothing at all is refused;
* FR-34: every one of the 36 questions has four different, non-blank answers and exactly one correct answer;
  a question of the square's level is asked, 50 times per level; impossible questions are refused;
* FR-35: a correct answer gives +1/+2/+3 by level and a wrong one −1/−2/−3; not answering counts as wrong;
  a result that disagrees with its own question cannot even be built;
* FR-36: the result always carries the correct answer, also after a wrong answer;
* FR-37: over three full rounds per level no question repeats before the level is used up; the deck reshuffles
  on exhaustion and keeps dealing for 200 more questions; a level with one question is allowed; a level with
  none is refused when the game is set up, not in the middle of it;
* repeatability: the same seed asks the same questions, a different seed does not;
* it fits the rest of the game: on 20 random Medium and Hard boards, every question square of the board can be
  answered with a question of its own level and the right number of steps.

## 7. Assumptions, and what is not done here

* **The step values come from `LevelConfig.questionSteps()`** (1 / 2 / 3), which the team recorded from SRS
  Table 1 in Iteration 1. The copy of the SRS in this repository stops at the end of section 2, so FR-35 could
  not be read in the original; the values were taken from the team's own record of it and from the Assignment
  3 prototype, which states the same rule. **No staff approval is needed for the pattern itself**, which is
  one of the patterns taught in the course.
* **The question level is the square's level, not the board's.** This follows the SRS and the project
  description, and the prototype documented the same decision where the task brief read differently
  (`Assignment3_Prototype/README.md`, "Where the documents differ"). It is an existing team decision, not a
  new one.
* **FR-38 (a capture happens before the question) is not implemented in this task.** Nothing in the game yet
  notices that a piece stopped on a question square: the trigger and the ordering of effects belong to the
  effect queue of **T2-10** (`StandardMoveEngine`, `GameModel`, `StandardTurnManager` — files owned by Shafeek
  and Rakan). The capture already happens inside `StandardMoveEngine.apply()` before that method returns, so
  running a question effect on the result will satisfy FR-38; this task provides the question and the steps
  and makes no claim about the ordering.
* **Showing the question** is T2-11, and **surprise outcomes** are T2-09. Both are separate issues.
