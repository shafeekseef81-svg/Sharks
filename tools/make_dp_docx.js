#!/usr/bin/env node
/*
 * Builds iteration2_dp_Sharks.docx, the design patterns document of Assignment 5 (task N2-05, #71).
 *
 * The text comes from the four section files in Iteration2/docs/design_patterns/, which their owners keep up
 * to date. The code excerpts come from Iteration2/src/Model/ and replace the screenshots the sections list.
 * Each excerpt is given by line numbers and checked against a pattern, so the script stops if the code moved:
 * update the numbers in EXCERPTS and run it again.
 *
 * Run from the repository root (Node.js 18 or later):
 *   npm install --prefix <folder> docx@9.5.1
 *   NODE_PATH=<folder>/node_modules node tools/make_dp_docx.js --ref <commit> [--results tests.json] [--final]
 *
 *   --ref      the commit the code excerpts come from (default: HEAD)
 *   --results  optional JSON, e.g. {"DiceStrategyTest": "10/10"}, from a test run on that commit
 *   --final    the submitted version, without the draft line on the title page
 *   --date     the date on the title page (default: today, dd/mm/yyyy)
 *   --out      the output file (default: iteration2_dp_Sharks.docx)
 */
'use strict';

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');
const {
    Document, Packer, Paragraph, TextRun, HeadingLevel, Table, TableRow, TableCell, WidthType, ShadingType,
    BorderStyle, AlignmentType, Footer, PageNumber, ImageRun, LevelFormat,
} = require('docx');

// ------------------------------------------------------------------ arguments and paths

const args = process.argv.slice(2);
const arg = (name) => { const i = args.indexOf(name); return i >= 0 ? args[i + 1] : undefined; };
const ROOT = process.cwd();
const DP_DIR = path.join(ROOT, 'Iteration2', 'docs', 'design_patterns');
const SRC_DIR = path.join(ROOT, 'Iteration2', 'src', 'Model');
const TEST_DIR = path.join(ROOT, 'Iteration2', 'test', 'Model');
const FINAL = args.includes('--final');
const REF = arg('--ref') || execSync('git rev-parse --short HEAD', { cwd: ROOT }).toString().trim();
const OUT = arg('--out') || 'iteration2_dp_Sharks.docx';
const RESULTS = arg('--results') ? JSON.parse(fs.readFileSync(arg('--results'), 'utf8')) : {};
const DATE = arg('--date') || new Date().toLocaleDateString('en-GB');

// ------------------------------------------------------------------ layout

const FONT = 'Calibri';
const MONO = 'Consolas';
const NAVY = '1F3864';
const BLUE = '2E74B5';
const GREY = '595959';
const PAGE_WIDTH = 11906;              // A4, in DXA (1440 per inch)
const MARGIN = 1080;                   // 0.75 inch
const CONTENT = PAGE_WIDTH - 2 * MARGIN;
const BODY = 22;                       // half-points: 11 pt
const SMALL = 20;
const CODE = 15;                       // 7.5 pt

// ------------------------------------------------------------------ the sections, in the order a turn uses them

const SECTIONS = [
    { file: 'Strategy_Shafeek.md', key: 'Strategy', test: 'DiceStrategyTest' },
    { file: 'State_Rakan.md', key: 'State', test: 'TurnStateTest' },
    { file: 'FactoryMethod_Mohamed.md', key: 'FactoryMethod', test: 'QuestionFactoryTest' },
    { file: 'Command_Shafeek.md', key: 'Command', test: 'SquareEffectTest' },
];

/** Text changes for the Word version. Each `find` must occur exactly once, so a changed section stops the build. */
const EDITS = {
    Strategy: [
        { find: 'Section for `iteration2_dp_Sharks` (task N2-05, #71). ', replace: '' },
    ],
    State: [
        { find: 'Section for `iteration2_dp_Sharks` (task N2-05, #71). ', replace: '' },
    ],
    FactoryMethod: [
        {
            find: 'Section 3 of `iteration2_dp_Sharks` (task N2-05, [#71](https://github.com/shafeekseef81-svg/Sharks/issues/71)). ',
            replace: '',
        },
        // Section 5: only the baseline row of the measurement table (agreed at N2-05, see the overview file).
        {
            find: '| 21/11/2026 | the T2-08 branch, before #89 merged (`3352d4f`) | 18 found, **18 passed**, 0 failed | 415 found, **415 passed**, 0 failed |\n'
                + '| 22/11/2026 | `iteration-2` `d445a38`, after #89 merged | 18 found, **18 passed**, 0 failed | 531 found, **531 passed**, 0 failed |\n',
            replace: '',
        },
        {
            find: 'The suite grew from 415 to 540 because other tasks merged, not because this task changed; its own 18 tests are the same 18.\n\n',
            replace: '',
        },
        // Section 6: the shorter "Open/Closed" paragraph and "Scope" list agreed for the Word version.
        {
            find: /\*\*What Open\/Closed does and does not buy here\.\*\*[^\n]*\n/,
            replace: '**What Open/Closed does and does not buy here.** A fourth *question* level needs one new subclass and '
                + 'one line in `forBank`, and no change to the code of the pattern. A fourth *game* level is a much larger '
                + 'change (a `Difficulty` constant, a `LevelConfig` entry, its questions, its dice and its board geometry), '
                + 'and this pattern is not the part of it that changes.\n',
        },
        {
            find: /\*\*Scope: what this task deliberately does not do\*\*[\s\S]*$/,
            replace: '**Scope: what this task deliberately does not do**\n\n'
                + '* **The step values 1 / 2 / 3 come from the SRS**, FR-35 (`Assignment3_Prototype/Sharks_SRS.pdf`, p. 13): '
                + '"Correct answers shall move the piece forward 1 (Easy), 2 (Medium), or 3 (Hard) steps; incorrect answers '
                + 'move it back equally."\n'
                + '* **The question level is the square\'s level, not the board\'s**, as in the SRS and the prototype '
                + '(`Assignment3_Prototype/README.md`).\n'
                + '* **FR-38**, a capture on an unprotected question square before the question, is not part of this pattern: '
                + 'the order of the effects belongs to the effect queue of T2-10 (section 5), and it is tested in '
                + '`SquareEffectTest` and independently in `EffectChainIndependentTest` (T2-12).\n'
                + '* **Showing the question** is T2-11 and the **surprise outcomes** are T2-09.\n',
        },
        // After the section numbers become 4.x, these two references go to other sections of the document.
        { find: 'graded pattern of the dice (section 4.1)', replace: 'graded pattern of the dice (section 2)', late: true },
        { find: 'effect queue of T2-10 (section 4.5)', replace: 'effect queue of T2-10 (section 5)', late: true },
    ],
    Command: [
        { find: 'Section for `iteration2_dp_Sharks` (task N2-05, #71). ', replace: '' },
    ],
};

/** The code excerpts that replace the screenshots: line ranges, each checked against the line it must show. */
const EXCERPTS = {
    Strategy: [
        { file: 'DiceStrategy.java', ranges: [[5, 56]], check: [[28, /public interface DiceStrategy/]],
            text: 'The Strategy interface. Its Javadoc names the pattern and the roles.' },
        { file: 'SpecialDiceStrategy.java', ranges: [[18, 31]], check: [[23, /public final int movement/]],
            text: 'The movement formula of both special dice, Regular × (special − 1) (FR-17), written once and final.' },
        { file: 'RandomDice.java', ranges: [[27, 51]],
            check: [[35, /public DiceRoll roll\(Difficulty level\)/], [48, /public DiceRoll rollWith\(DiceStrategy strategy\)/]],
            text: 'The context: roll(level) takes the strategy of the level and rollWith(strategy) rolls it, with no if or switch on the level.' },
        { file: 'LevelConfig.java', ranges: [[29, 38]],
            check: [[33, /new RegularDiceStrategy\(\)/], [35, /new BoostDiceStrategy\(\)/], [37, /new UltraBoostDiceStrategy\(\)/]],
            text: 'The selection: the static level table gives each level its strategy.' },
        { file: 'StandardTurnManager.java', ranges: [[49, 49], [234, 246]],
            check: [[49, /SURPRISE_ROLL_DICE = Difficulty\.HARD/], [241, /dice\.roll\(SURPRISE_ROLL_DICE\)/]],
            text: 'The surprise roll (SRS Table 2, Q27) reuses the Hard strategy on any level: no new die and no change to the context.' },
    ],
    State: [
        { file: 'TurnState.java', ranges: [[5, 33], [89, 91], [100, 102]],
            check: [[33, /interface TurnState/], [89, /default MoveResult runEffect/], [100, /default void skipEffect/]],
            text: 'The State interface with its Javadoc, and two of the effect requests that refuse by default.' },
        { file: 'ResolvingState.java', ranges: [[18, 58]], check: [[18, /final class ResolvingState implements TurnState/]],
            text: 'The phase added for question and surprise squares: it refuses roll, move and end of turn, and runs the effects.' },
        { file: 'StandardTurnManager.java', ranges: [[128, 129], [145, 146], [159, 160], [174, 175], [196, 197], [208, 209], [298, 311]],
            check: [[129, /return state\.phase\(\)/], [146, /return state\.roll\(this\)/], [160, /return state\.legalMoves\(this\)/],
                [175, /return state\.move\(this, piece\)/], [197, /return state\.endTurn\(this\)/],
                [209, /return state\.runEffect\(this, effect\)/], [299, /private TurnState stateAfterMove\(\)/]],
            text: 'The context: every request goes to the current state, and stateAfterMove() picks the next state after a move or a chain of effects.' },
    ],
    FactoryMethod: [
        { file: 'QuestionFactory.java', ranges: [[39, 75]],
            check: [[39, /public abstract class QuestionFactory/], [56, /protected abstract Question createQuestion\(\)/], [65, /public final Question ask\(\)/]],
            text: 'The abstract Creator: the factory method createQuestion(), and the operation ask() that uses it.' },
        { file: 'EasyQuestionFactory.java', ranges: [[13, 40]],
            check: [[13, /extends QuestionFactory/], [38, /protected Question createQuestion\(\)/]],
            text: 'One Concrete Creator in full: its own deck and the overridden factory method, and no movement rule.' },
        { file: 'QuestionFactory.java', ranges: [[120, 128]], check: [[120, /forBank\(QuestionBank bank, Random random\)/]],
            text: 'The only place that names the three Concrete Creators, once per game.' },
        { file: 'QuestionFactory.java', ranges: [[93, 107]], check: [[93, /public final QuestionResult judge/]],
            text: 'The judging rule of FR-35 and FR-36, the same for every level.' },
        { file: 'QuestionDeck.java', ranges: [[83, 96]], check: [[88, /public Question deal\(\)/]],
            text: 'The no-repeat deck of one level (FR-37).' },
    ],
    Command: [
        { file: 'SquareEffect.java', ranges: [[3, 39]], check: [[30, /public interface SquareEffect/]],
            text: 'The Command interface. Its Javadoc names the pattern and the roles.' },
        { file: 'EffectQueue.java', ranges: [[115, 125]], check: [[115, /public MoveResult run\(SquareEffect effect\)/]],
            text: 'The invoker runs the command, keeps its result and queues the special square it reaches.' },
        { file: 'EffectQueue.java', ranges: [[146, 156]], check: [[147, /private void reach\(MoveResult move\)/]],
            text: 'The chain guard: zero steps start nothing (Q81), and a square starts at most once in a chain.' },
        { file: 'EffectMover.java', ranges: [[69, 99]], check: [[69, /public MoveResult moveSteps\(Piece piece, int steps\)/]],
            text: 'The receiver: it finds the target with the rules and carries out the captures (FR-26, FR-27).' },
        { file: 'EffectMover.java', ranges: [[135, 160]],
            check: [[136, /private int forwardTarget/], [151, /private int backwardTarget/]],
            text: 'The targets: stop before a Double Block in both directions (FR-29), and never behind the starting square (FR-32).' },
        { file: 'GameModel.java', ranges: [[275, 302]], check: [[275, /public MoveResult applySurprise\(SurpriseResult result\)/]],
            text: 'A client: a surprise outcome becomes one of the three commands, or a change of the turn.' },
    ],
};

/** Pictures of the running game, by section. */
const FIGURES = {
    FactoryMethod: [
        { file: path.join(ROOT, 'tools', 'dp_assets', 'question_card.png'),
            text: 'The question card in the running game: a Hard question square on a Medium board, after the player '
                + 'chose No Answer. The result of judge() shows the correct answer (FR-36) and 3 steps back (FR-35).' },
    ],
};

const OVERVIEW = `# Overview

Assignment 5 asks every student to implement one design pattern from the course (Singleton and MVC do not count), and a document that says which patterns were chosen, which problem each one solves in our game, why it was chosen, and where it is in the code. This is that document for Team Sharks.

The team has three members and four patterns: Shafeek Saif implemented two of them, Strategy and Command. All four are in the Model of the game (\`Iteration2/src/Model/\`), so the MVC separation of the game is kept. The Observer of Iteration 1 (\`GameListener\`) and the board factory are older code and are not counted here.

| Section | Pattern (GoF category) | Problem it solves in the game | Task | Owner | Main classes |
|---|---|---|---|---|---|
| 2 | Strategy (behavioural) | A different die and movement formula per level (FR-16, FR-17) | T2-04 | Shafeek Saif | \`DiceStrategy\`, \`RandomDice\`, \`LevelConfig\` |
| 3 | State (behavioural) | What the player may do in each phase of a turn (NFR-09) | T2-13 | Rakan Ziyan | \`TurnState\`, \`StandardTurnManager\`, \`ResolvingState\` |
| 4 | Factory Method (creational) | A question of the square's own level, without a \`switch\` on the level (FR-34..FR-37) | T2-08 | Mohamed Zereny | \`QuestionFactory\`, \`EasyQuestionFactory\`, \`MediumQuestionFactory\`, \`HardQuestionFactory\` |
| 5 | Command (behavioural) | Question and surprise effects run in order, with the game rules and a chain guard (FR-41, FR-42) | T2-10 | Shafeek Saif | \`SquareEffect\`, \`EffectQueue\`, \`EffectMover\` |

The sections follow the order in which a turn uses the patterns: the dice are rolled (Strategy), the turn moves through its phases (State), a question square asks its question (Factory Method), and the effect of the answer or the surprise is carried out (Command).

Every section has the same six parts: the pattern, the problem it solves, its structure, where it is in the code, how it was tested, and its trade-offs. The code excerpts are taken from the submitted code (\`iteration-2\` at commit \`${REF}\`) with their line numbers. FR and NFR numbers refer to the team's SRS (\`Assignment3_Prototype/Sharks_SRS.pdf\`).
`;

// ------------------------------------------------------------------ inline Markdown: **bold**, *italic*, `code`, [text](url)

function inline(text, base = {}) {
    const size = base.size || BODY;
    const runs = [];
    let bold = false;
    let italic = false;
    let buf = '';
    const style = () => ({ bold: bold || !!base.bold, italics: italic || !!base.italics, color: base.color });
    const flush = () => {
        if (buf) {
            runs.push(new TextRun({ text: buf, font: FONT, size, ...style() }));
            buf = '';
        }
    };
    for (let i = 0; i < text.length;) {
        const c = text[i];
        if (c === '`') {
            const j = text.indexOf('`', i + 1);
            if (j > i) {
                flush();
                runs.push(new TextRun({ text: text.slice(i + 1, j), font: MONO, size: size - 3, ...style() }));
                i = j + 1;
                continue;
            }
        }
        if (text.startsWith('**', i)) { flush(); bold = !bold; i += 2; continue; }
        if (c === '*' && (italic || (text[i + 1] && text[i + 1] !== ' '))) { flush(); italic = !italic; i++; continue; }
        if (c === '[') {
            const m = /^\[([^\]]+)\]\(([^)]+)\)/.exec(text.slice(i));
            if (m) { buf += m[1]; i += m[0].length; continue; }
        }
        if (c === '\\' && i + 1 < text.length) { buf += text[i + 1]; i += 2; continue; }
        buf += c;
        i++;
    }
    flush();
    return runs;
}

/**
 * Every paragraph is left to right and left aligned unless it says otherwise. Word with a Hebrew default applies
 * its own right-to-left Normal style over the document defaults, so the direction is set on each paragraph.
 */
function P(options) {
    return new Paragraph({ bidirectional: false, alignment: AlignmentType.LEFT, ...options });
}

// ------------------------------------------------------------------ block Markdown: headings, paragraphs, lists, tables, code, quotes

function parseBlocks(md) {
    const lines = md.replace(/\r\n/g, '\n').split('\n');
    const blocks = [];
    let para = [];
    const endPara = () => { if (para.length) { blocks.push({ type: 'p', text: para.join(' ') }); para = []; } };
    for (let i = 0; i < lines.length; i++) {
        const line = lines[i];
        if (/^\s*```/.test(line)) {
            endPara();
            const code = [];
            for (i++; i < lines.length && !/^\s*```/.test(lines[i]); i++) code.push(lines[i]);
            blocks.push({ type: 'code', lines: code });
            continue;
        }
        let m;
        if ((m = /^(#{1,3}) (.*)$/.exec(line))) { endPara(); blocks.push({ type: 'h' + m[1].length, text: m[2] }); continue; }
        if (/^\s*\|/.test(line)) {
            endPara();
            const rows = [];
            for (; i < lines.length && /^\s*\|/.test(lines[i]); i++) {
                const cells = lines[i].trim().replace(/^\|/, '').replace(/\|$/, '').split('|').map((s) => s.trim());
                if (!cells.every((s) => /^:?-+:?$/.test(s))) rows.push(cells);
            }
            i--;
            blocks.push({ type: 'table', rows });
            continue;
        }
        if ((m = /^(\s*)[-*] (.*)$/.exec(line))) {
            endPara();
            blocks.push({ type: 'li', level: Math.floor(m[1].length / 2), text: m[2] });
            continue;
        }
        if ((m = /^\s*> ?(.*)$/.exec(line))) { endPara(); blocks.push({ type: 'quote', text: m[1] }); continue; }
        if (line.trim() === '') { endPara(); continue; }
        if (/^\s+\S/.test(line) && !para.length && blocks.length && blocks[blocks.length - 1].type === 'li') {
            blocks[blocks.length - 1].text += ' ' + line.trim();
            continue;
        }
        para.push(line.trim());
    }
    endPara();
    return blocks;
}

// ------------------------------------------------------------------ Word elements

const thin = { style: BorderStyle.SINGLE, size: 4, color: 'BFBFBF' };
const cellBorders = { top: thin, bottom: thin, left: thin, right: thin };

function table(rows) {
    const cols = rows[0].length;
    const weight = [];
    for (let c = 0; c < cols; c++) {
        const longest = Math.max(...rows.map((r) => (r[c] || '').replace(/[`*]/g, '').length));
        weight.push(Math.min(Math.max(longest, 6), 70));
    }
    const sum = weight.reduce((a, b) => a + b, 0);
    const widths = weight.map((w) => Math.floor((CONTENT * w) / sum));
    widths[cols - 1] += CONTENT - widths.reduce((a, b) => a + b, 0);
    return new Table({
        width: { size: CONTENT, type: WidthType.DXA },
        columnWidths: widths,
        rows: rows.map((r, ri) => new TableRow({
            tableHeader: ri === 0,
            cantSplit: true,
            children: widths.map((w, c) => new TableCell({
                width: { size: w, type: WidthType.DXA },
                borders: cellBorders,
                margins: { top: 50, bottom: 50, left: 90, right: 90 },
                shading: ri === 0 ? { type: ShadingType.CLEAR, color: 'auto', fill: NAVY } : undefined,
                children: [P({
                    spacing: { before: 0, after: 0 },
                    children: inline(r[c] || '', ri === 0 ? { size: SMALL, bold: true, color: 'FFFFFF' } : { size: SMALL }),
                })],
            })),
        })),
    });
}

/** A grey box of monospace lines; each line is [number or null, text]. */
function codeBox(lines) {
    const numbered = lines.some(([n]) => n !== null);
    const width = numbered ? Math.max(...lines.map(([n]) => String(n || '').length)) : 0;
    const paras = lines.map(([n, text]) => {
        if (n === '…') {
            return P({ spacing: { before: 20, after: 20 }, children: [new TextRun({ text: '   ⋮', font: MONO, size: CODE, color: '808080' })] });
        }
        const runs = [];
        if (numbered) runs.push(new TextRun({ text: String(n).padStart(width) + '  ', font: MONO, size: CODE, color: '808080' }));
        runs.push(new TextRun({ text: text.replace(/\t/g, '    ') || ' ', font: MONO, size: CODE }));
        const hang = numbered ? Math.round((width + 2) * CODE * 5.5) : 0; // Consolas is 0.55 em wide
        return P({ spacing: { before: 0, after: 0, line: 240 }, indent: { left: hang, hanging: hang }, children: runs });
    });
    return new Table({
        width: { size: CONTENT, type: WidthType.DXA },
        columnWidths: [CONTENT],
        rows: [new TableRow({
            cantSplit: lines.length <= 40, // a long excerpt may continue on the next page
            children: [new TableCell({
                width: { size: CONTENT, type: WidthType.DXA },
                borders: { top: thin, bottom: thin, left: { style: BorderStyle.SINGLE, size: 18, color: BLUE }, right: thin },
                shading: { type: ShadingType.CLEAR, color: 'auto', fill: 'F3F5F8' },
                margins: { top: 80, bottom: 80, left: 120, right: 120 },
                children: paras,
            })],
        })],
    });
}

function caption(label, text) {
    return P({
        keepNext: true,
        spacing: { before: 200, after: 80 },
        children: [new TextRun({ text: label + ' ', font: FONT, size: SMALL, bold: true, color: NAVY }), ...inline(text, { size: SMALL, color: GREY })],
    });
}

function dedent(lines) {
    const indents = lines.filter(([, t]) => t.trim() !== '').map(([, t]) => t.match(/^ */)[0].length);
    const cut = indents.length ? Math.min(...indents) : 0;
    return lines.map(([n, t]) => [n, n === '…' ? t : t.slice(cut)]);
}

function excerptLines(spec) {
    const file = path.join(SRC_DIR, spec.file);
    const src = fs.readFileSync(file, 'utf8').replace(/\r\n/g, '\n').split('\n');
    for (const [n, re] of spec.check) {
        if (!re.test(src[n - 1] || '')) {
            throw new Error(`${spec.file}:${n} no longer shows ${re} - the code moved; update EXCERPTS`);
        }
    }
    const out = [];
    spec.ranges.forEach(([a, b], k) => {
        if (k > 0) out.push(['…', '']);
        for (let n = a; n <= b; n++) out.push([n, src[n - 1]]);
    });
    return dedent(out);
}

function pngSize(file) {
    const b = fs.readFileSync(file);
    return { w: b.readUInt32BE(16), h: b.readUInt32BE(20), data: b };
}

/** The @DisplayName of every test method in a test class, in file order. */
function testNames(cls) {
    const src = fs.readFileSync(path.join(TEST_DIR, cls + '.java'), 'utf8');
    const re = /@DisplayName\(((?:\s*"(?:[^"\\]|\\.)*"\s*\+?)+)\)\s*(?:@\w+(?:\([^)]*\))?\s*)*(?:public\s+)?void\s+\w+/g;
    const names = [];
    let m;
    while ((m = re.exec(src))) {
        names.push([...m[1].matchAll(/"((?:[^"\\]|\\.)*)"/g)].map((x) => x[1].replace(/\\(.)/g, '$1')).join(''));
    }
    const tests = (src.match(/@Test\b/g) || []).length;
    return { names, tests };
}

// ------------------------------------------------------------------ one section of the document

function applyEdits(key, md, late) {
    for (const e of EDITS[key] || []) {
        if (!!e.late !== late) continue;
        const count = typeof e.find === 'string' ? md.split(e.find).length - 1 : (md.match(new RegExp(e.find.source, 'g')) || []).length;
        if (count !== 1) throw new Error(`${key}: the text to change was found ${count} times, not once: ${String(e.find).slice(0, 80)}`);
        md = md.replace(e.find, e.replace);
    }
    return md;
}

/** A heading's text in the heading style; `code` in it is set in the code font, without the backticks. */
function headingRuns(text) {
    return text.split('`').filter((s) => s !== '').map((s) => {
        const code = text.indexOf('`' + s + '`') >= 0;
        return new TextRun(code ? { text: s, font: MONO } : { text: s });
    });
}

function renderBlocks(blocks, number) {
    const out = [];
    for (const b of blocks) {
        if (b.type === 'h1') {
            out.push(P({ heading: HeadingLevel.HEADING_1, pageBreakBefore: true, children: headingRuns(`${number}. ${b.text}`) }));
        } else if (b.type === 'h2') {
            const m = /^(\d+)\.\s+(.*)$/.exec(b.text);
            out.push(P({ heading: HeadingLevel.HEADING_2, children: headingRuns(m ? `${number}.${m[1]} ${m[2]}` : b.text) }));
        } else if (b.type === 'h3') {
            out.push(P({ heading: HeadingLevel.HEADING_3, children: headingRuns(b.text) }));
        } else if (b.type === 'p') {
            out.push(P({ spacing: { after: 120 }, children: inline(b.text) }));
        } else if (b.type === 'li') {
            out.push(P({ numbering: { reference: 'bullets', level: Math.min(b.level, 2) }, spacing: { after: 60 }, children: inline(b.text) }));
        } else if (b.type === 'quote') {
            out.push(P({
                indent: { left: 720 },
                border: { left: { style: BorderStyle.SINGLE, size: 12, color: 'BFBFBF', space: 8 } },
                spacing: { after: 100 },
                children: inline(b.text, { italics: true, color: GREY }),
            }));
        } else if (b.type === 'table') {
            out.push(table(b.rows), P({ spacing: { after: 60 }, children: [] }));
        } else if (b.type === 'code') {
            out.push(codeBox(dedent(b.lines.map((t) => [null, t]))), P({ spacing: { after: 60 }, children: [] }));
        }
    }
    return out;
}

function section(spec, number) {
    let md = fs.readFileSync(path.join(DP_DIR, spec.file), 'utf8').replace(/\r\n/g, '\n');
    md = applyEdits(spec.key, md, false);
    md = md.replace(/\bsection ([1-6])\b/g, `section ${number}.$1`);
    md = applyEdits(spec.key, md, true);
    let blocks = parseBlocks(md);

    // The screenshot lists become the excerpts: drop the list (a closing italic line, or a "Screenshots" subsection).
    blocks = blocks.filter((b) => !(b.type === 'p' && /^\*Screenshots for the Word version/.test(b.text)));
    const shots = blocks.findIndex((b) => b.type === 'h3' && /^Screenshots/.test(b.text));
    if (shots >= 0) {
        let end = shots + 1;
        while (end < blocks.length && !/^h[12]$/.test(blocks[end].type)) end++;
        blocks.splice(shots, end - shots);
    }

    const at = (n) => blocks.findIndex((b) => b.type === 'h2' && b.text.startsWith(n + '.'));
    if (at(5) < 0 || at(6) < 0) throw new Error(spec.file + ': headings 5 and 6 are missing');
    const parts = { before5: blocks.slice(0, at(5)), section5: blocks.slice(at(5), at(6)), rest: blocks.slice(at(6)) };

    const out = renderBlocks(parts.before5, number);

    // Section x.4: the code excerpts, then the pictures of the running game.
    out.push(P({ heading: HeadingLevel.HEADING_3, children: [new TextRun('Code excerpts')] }));
    out.push(P({ spacing: { after: 60 }, children: inline(`From \`iteration-2\` at commit \`${REF}\`, folder \`Iteration2/src/Model/\`, with the line numbers of the files. A ⋮ marks lines left out.`, { size: SMALL, color: GREY }) }));
    EXCERPTS[spec.key].forEach((ex, k) => {
        const [first] = ex.ranges[0];
        const last = ex.ranges[ex.ranges.length - 1][1];
        const where = ex.ranges.length === 1 ? `lines ${first}–${last}` : `lines ${ex.ranges.map(([a, b]) => (a === b ? a : `${a}–${b}`)).join(', ')}`;
        out.push(caption(`Excerpt ${number}.${k + 1}`, `\`${ex.file}\`, ${where}. ${ex.text}`));
        out.push(codeBox(excerptLines(ex)));
    });
    (FIGURES[spec.key] || []).forEach((fig, k) => {
        const img = pngSize(fig.file);
        const w = 600;
        out.push(caption(`Figure ${number}.${k + 1}`, fig.text));
        out.push(P({
            alignment: AlignmentType.CENTER,
            children: [new ImageRun({ type: 'png', data: img.data, transformation: { width: w, height: Math.round((w * img.h) / img.w) } })],
        }));
    });

    out.push(...renderBlocks(parts.section5, number));

    // End of section x.5: the names of the tests, as the test run lists them.
    const t = testNames(spec.test);
    if (t.names.length !== t.tests) throw new Error(`${spec.test}: ${t.tests} tests but ${t.names.length} display names`);
    const result = RESULTS[spec.test];
    out.push(caption(`Tests of ${number}.5`, `The ${t.tests} tests of \`${spec.test}\`, by their display names`
        + (result ? `. Result on \`${REF}\`: ${result} passed.` : '.')));
    out.push(codeBox(t.names.map((n) => [null, (result ? '[OK] ' : '- ') + n])));

    out.push(...renderBlocks(parts.rest, number));
    return out;
}

// ------------------------------------------------------------------ title page and document

function centred(text, opts = {}) {
    return P({
        alignment: AlignmentType.CENTER,
        spacing: { before: opts.before || 0, after: opts.after === undefined ? 120 : opts.after },
        children: [new TextRun({ text, font: opts.font || FONT, size: opts.size || BODY, bold: !!opts.bold, color: opts.color, italics: !!opts.italics })],
    });
}

function titlePage() {
    const page = [
        centred('Educational Ludo', { before: 3600, size: 56, bold: true, color: NAVY, after: 160 }),
        centred('Design Patterns – Iteration 2', { size: 34, color: BLUE, after: 480 }),
        centred('Team Sharks', { size: 26, bold: true }),
        centred('Shafeek Saif · Rakan Ziyan · Mohamed Zereny'),
        centred('Product Owner: Mohamed Zereny · Scrum Master: Shafeek Saif · all three are developers', { size: SMALL, color: GREY }),
        centred('Software Engineering and Software Quality Assurance in Information Systems', { size: SMALL, before: 240 }),
        centred(`Assignment 5 – Iteration 2 · ${FINAL ? 'December 2026' : DATE}`, { size: SMALL }),
        centred('https://github.com/shafeekseef81-svg/Sharks', { size: SMALL, color: BLUE, before: 120 }),
    ];
    if (!FINAL) {
        page.push(centred(`Draft of ${DATE} for the team's review. The final version is due on 05/12/2026.`,
            { size: SMALL, italics: true, color: 'C00000', before: 600 }));
    }
    return page;
}

function build() {
    const body = [...titlePage()];
    body.push(...renderBlocks(parseBlocks(OVERVIEW), 1));
    SECTIONS.forEach((s, k) => body.push(...section(s, k + 2)));

    const heading = (size, color, before, after) => ({
        run: { font: FONT, size, bold: true, color },
        paragraph: { spacing: { before, after }, keepNext: true, keepLines: true },
    });
    const doc = new Document({
        creator: 'Team Sharks',
        title: 'iteration2_dp_Sharks',
        description: 'Design patterns of Iteration 2 (task N2-05)',
        styles: {
            // Left to right explicitly: Word with a Hebrew default otherwise lays the paragraphs out right to left.
            default: {
                document: {
                    run: { font: FONT, size: BODY },
                    paragraph: { spacing: { after: 120, line: 264 }, bidirectional: false, alignment: AlignmentType.LEFT },
                },
            },
            paragraphStyles: [
                { id: 'Heading1', name: 'Heading 1', basedOn: 'Normal', next: 'Normal', quickFormat: true, ...heading(32, NAVY, 0, 200), paragraph: { ...heading(32, NAVY, 0, 200).paragraph, outlineLevel: 0 } },
                { id: 'Heading2', name: 'Heading 2', basedOn: 'Normal', next: 'Normal', quickFormat: true, ...heading(26, BLUE, 280, 100), paragraph: { ...heading(26, BLUE, 280, 100).paragraph, outlineLevel: 1 } },
                { id: 'Heading3', name: 'Heading 3', basedOn: 'Normal', next: 'Normal', quickFormat: true, ...heading(23, NAVY, 200, 80), paragraph: { ...heading(23, NAVY, 200, 80).paragraph, outlineLevel: 2 } },
            ],
        },
        numbering: {
            config: [{
                reference: 'bullets',
                levels: [0, 1, 2].map((level) => ({
                    level,
                    format: LevelFormat.BULLET,
                    text: ['•', '◦', '▪'][level],
                    alignment: AlignmentType.LEFT,
                    style: { paragraph: { indent: { left: 360 * (level + 1), hanging: 260 } } },
                })),
            }],
        },
        sections: [{
            properties: { titlePage: true, page: { size: { width: PAGE_WIDTH, height: 16838 }, margin: { top: 1080, bottom: 1080, left: MARGIN, right: MARGIN } } },
            footers: {
                first: new Footer({ children: [] }),
                default: new Footer({
                    children: [P({
                        alignment: AlignmentType.CENTER,
                        children: [new TextRun({ text: 'Team Sharks – Iteration 2 Design Patterns – page ', font: FONT, size: 18, color: '808080' }),
                            new TextRun({ children: [PageNumber.CURRENT], font: FONT, size: 18, color: '808080' })],
                    })],
                }),
            },
            children: body,
        }],
    });
    return Packer.toBuffer(doc).then((buf) => {
        fs.writeFileSync(path.join(ROOT, OUT), buf);
        console.log(`${OUT}: ${buf.length} bytes, code at ${REF}${FINAL ? ', final' : ', draft'}`);
    });
}

build().catch((e) => { console.error(e.message); process.exit(1); });
