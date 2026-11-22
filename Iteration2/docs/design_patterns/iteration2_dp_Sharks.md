# iteration2_dp_Sharks: design patterns of Iteration 2 (Team Sharks)

Assembly file of task **N2-05** ([#71](https://github.com/shafeekseef81-svg/Sharks/issues/71)), assembled by Shafeek Saif from the sections the pattern owners write. The deliverable is the Word or PDF file `iteration2_dp_Sharks` (Assignment 5, deliverable 4), due with the submission on 06/12/2026 23:59 (internal deadline 05/12).

**Status 19/11: skeleton.** The four sections exist, but none of them is merged into `iteration-2` yet (see the table in section 2).

## 1. What the assignment asks for
- Each student implements one design pattern. Singleton and MVC do not count. A pattern that was not taught in the course needs the TA's approval several days before the deadline.
- For each pattern the document says **which pattern** it is, **which problem** it solves in our game, and **where it is in the code**. Screenshots are allowed.
- Observer (`GameListener`) and the board factory exist since Iteration 1 and are not counted as Iteration 2 patterns (plan, section 4).
- Open point from N2-01: the TA's answer on three or four patterns for a team of three. The team has four, because Shafeek has two (Strategy and Command).

## 2. The four patterns

Order in the document: the order in which a turn uses them (dice → turn phases → question → effects).

| # | Pattern (GoF) | Problem it solves | Task | Owner | Section file | Status 19/11 |
|---|---|---|---|---|---|---|
| 1 | Strategy (behavioural) | A different die and movement formula per level (FR-16, FR-17) | T2-04 [#55](https://github.com/shafeekseef81-svg/Sharks/issues/55) | Shafeek Saif | `Strategy_Shafeek.md` | in PR #88, waiting for a Light review |
| 2 | State (behavioural) | What the player may do in each phase of a turn (D3, NFR-09) | T2-13 [#64](https://github.com/shafeekseef81-svg/Sharks/issues/64) | Rakan Ziyan | `State_Rakan.md` | in PR #79, Deep review 1/2 |
| 3 | Factory Method (creational) | A question of the square's own level, without a `switch` on the level (FR-34..FR-37) | T2-08 [#59](https://github.com/shafeekseef81-svg/Sharks/issues/59) | Mohamed Zereny | `FactoryMethod_Mohamed.md` (today `Iteration2/docs/T2-08_FactoryMethod.md`; a move into this folder was asked in the review of #89) | in PR #89, Deep review 1/2 |
| 4 | Command (behavioural) | Question and surprise effects run in order, with the game rules and a chain guard (FR-41, FR-42) | T2-10 [#61](https://github.com/shafeekseef81-svg/Sharks/issues/61) | Shafeek Saif | `Command_Shafeek.md` | part 1 in PR #87 (Deep 1/2); part 2 updates it |

## 3. One structure for every section

Each section uses the same headings, so the document reads as one text:

1. **The pattern:** its GoF name and category, in two or three sentences.
2. **The problem it solves:** in our game, with the FR numbers.
3. **Structure:** the roles of the pattern and the class that plays each one, with a small diagram.
4. **Where it is in the code:** a table of files, plus the screenshots listed at the end of the section.
5. **How it was tested:** the test classes and what they show.
6. **Trade-offs:** why this pattern and not a simpler solution, and what it costs.

| Section | 1–5 | 6 | Screenshot list | To do |
|---|---|---|---|---|
| Strategy | yes | "Use in the rest of Iteration 2" | yes | add the trade-offs |
| State | yes | "Extension in Iteration 2" | yes | mention `ResolvingState` (T2-10 part 2) once it is merged |
| Factory Method | different headings | "Benefits and trade-offs" | no | map to headings 1–6, shorten, add a screenshot list, move the file |
| Command | yes | no | yes | add the trade-offs; update the tests after part 2 |

## 4. Assembly steps (by 05/12)
1. Every section is merged into `iteration-2` with its pattern task.
2. Each owner checks their own section against section 3 above.
3. The class names, file names and test counts in each section match the final code.
4. The screenshots are taken from the final code, as each section lists them.
5. Shafeek puts the document together: a title page (team, members and roles, Iteration 2, date), this section 2 as the overview, the four sections in the order of section 2, then Word or PDF `iteration2_dp_Sharks`.
6. Review by Rakan and Mohamed (N2-05), then the file goes into the submission ZIP (N2-06).
