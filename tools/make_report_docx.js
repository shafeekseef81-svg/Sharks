#!/usr/bin/env node
/*
 * Builds iteration2_report_Sharks.docx, deliverable 3 of Assignment 5 (task N2-04, #70).
 *
 * It continues iteration1_report_Sharks: the same eight numbered sections, the same title page and page
 * footer, with Iteration 2 as the new iteration. The text is the finished content of
 * Iteration2/docs/N2-04_SystemReport.md, cut down to what a grader needs: the required structure
 * 3(a)-3(e), the measured evidence and the genuine screenshots, without the pull-request history.
 *
 * The figures are the files in Iteration2/docs/evidence/, which are screenshots of the real application
 * taken on a real Windows display.
 *
 * Run from the repository root (Node.js 18 or later):
 *   npm install --prefix <folder> docx@9.5.1
 *   NODE_PATH=<folder>/node_modules node tools/make_report_docx.js [--final] [--date dd/mm/yyyy]
 *
 *   --final  the submitted version, without the draft banner on the title page.
 *            Only pass it once the open items in section 7 are actually closed.
 */
'use strict';

const fs = require('fs');
const path = require('path');
const {
    Document, Packer, Paragraph, TextRun, HeadingLevel, Table, TableRow, TableCell, WidthType,
    ShadingType, BorderStyle, AlignmentType, Footer, PageNumber, ImageRun, TableOfContents,
    PageBreak, convertInchesToTwip,
} = require('docx');

const argv = process.argv.slice(2);
const FINAL = argv.includes('--final');
const dateArg = argv.indexOf('--date');
const today = new Date();
const DATE = dateArg >= 0 ? argv[dateArg + 1]
    : `${String(today.getDate()).padStart(2, '0')}/${String(today.getMonth() + 1).padStart(2, '0')}/${today.getFullYear()}`;
const ROOT = path.resolve(__dirname, '..');
const EVIDENCE = path.join(ROOT, 'Iteration2', 'docs', 'evidence');
const OUT = path.join(ROOT, 'iteration2_report_Sharks.docx');

const BASELINE = '06e2ab2';
const SUITE = '705';

// ------------------------------------------------------------------ small builders
const FONT = 'Calibri';
const ACCENT = '1F3864';

function p(text, opts = {}) {
    const runs = Array.isArray(text) ? text : [text];
    return new Paragraph({
        alignment: opts.align,
        bidirectional: false,
        spacing: { after: opts.after === undefined ? 120 : opts.after, line: 276 },
        indent: opts.indent,
        children: runs.map((r) => (typeof r === 'string'
            ? new TextRun({ text: r, font: FONT, size: opts.size || 22 })
            : new TextRun({ font: FONT, size: opts.size || 22, ...r }))),
    });
}

function h(text, level) {
    return new Paragraph({
        heading: level,
        bidirectional: false,
        spacing: { before: level === HeadingLevel.HEADING_1 ? 320 : 240, after: 120 },
        children: [new TextRun({ text, font: FONT, bold: true, color: ACCENT,
            size: level === HeadingLevel.HEADING_1 ? 30 : 25 })],
    });
}

function bullet(text, level = 0) {
    const runs = Array.isArray(text) ? text : [text];
    return new Paragraph({
        bullet: { level },
        bidirectional: false,
        spacing: { after: 80, line: 276 },
        children: runs.map((r) => (typeof r === 'string'
            ? new TextRun({ text: r, font: FONT, size: 22 })
            : new TextRun({ font: FONT, size: 22, ...r }))),
    });
}

function mono(text) {
    return new Paragraph({
        bidirectional: false,
        spacing: { after: 120 },
        shading: { type: ShadingType.CLEAR, fill: 'F2F2F2' },
        children: text.split('\n').flatMap((line, i) => (i === 0
            ? [new TextRun({ text: line, font: 'Consolas', size: 18 })]
            : [new TextRun({ text: line, font: 'Consolas', size: 18, break: 1 })])),
    });
}

function cell(text, { bold = false, fill, width } = {}) {
    const runs = Array.isArray(text) ? text : [text];
    return new TableCell({
        width: width ? { size: width, type: WidthType.PERCENTAGE } : undefined,
        shading: fill ? { type: ShadingType.CLEAR, fill } : undefined,
        margins: { top: 60, bottom: 60, left: 90, right: 90 },
        children: [new Paragraph({
            bidirectional: false,
            spacing: { after: 0, line: 240 },
            children: runs.map((r) => (typeof r === 'string'
                ? new TextRun({ text: r, font: FONT, size: 19, bold })
                : new TextRun({ font: FONT, size: 19, bold, ...r }))),
        })],
    });
}

/** rows[0] is the header. widths is an optional array of percentages. */
function table(rows, widths) {
    const border = { style: BorderStyle.SINGLE, size: 2, color: 'BFBFBF' };
    return new Table({
        visuallyRightToLeft: false,
        width: { size: 100, type: WidthType.PERCENTAGE },
        borders: { top: border, bottom: border, left: border, right: border,
            insideHorizontal: border, insideVertical: border },
        rows: rows.map((r, i) => new TableRow({
            tableHeader: i === 0,
            children: r.map((c, j) => cell(c, {
                bold: i === 0,
                fill: i === 0 ? 'DEE6F1' : undefined,
                width: widths ? widths[j] : undefined,
            })),
        })),
    });
}

let figureNo = 0;
/** A screenshot with its numbered caption. heightIn is optional; the aspect ratio is kept. */
function figure(file, caption, widthIn = 5.75) {
    const full = path.join(EVIDENCE, file);
    const buf = fs.readFileSync(full);
    const w = buf.readUInt32BE(16);
    const hgt = buf.readUInt32BE(20);
    figureNo += 1;
    const width = Math.round(widthIn * 96);
    return [
        new Paragraph({
            alignment: AlignmentType.CENTER,
            bidirectional: false,
            spacing: { before: 160, after: 60 },
            children: [new ImageRun({
                type: 'png',
                data: buf,
                transformation: { width, height: Math.round(width * (hgt / w)) },
                altText: { title: `Figure ${figureNo}`, description: caption, name: file },
            })],
        }),
        new Paragraph({
            alignment: AlignmentType.CENTER,
            bidirectional: false,
            spacing: { after: 220 },
            children: [new TextRun({ text: `Figure ${figureNo} \u2013 ${caption}`, font: FONT, size: 18, italics: true, color: '595959' })],
        }),
    ];
}

// ------------------------------------------------------------------ title page
const title = [
    new Paragraph({ bidirectional: false, spacing: { before: 1800, after: 0 }, alignment: AlignmentType.CENTER,
        children: [new TextRun({ text: 'Educational Ludo', font: FONT, bold: true, size: 56, color: ACCENT })] }),
    new Paragraph({ bidirectional: false, spacing: { before: 240, after: 600 }, alignment: AlignmentType.CENTER,
        children: [new TextRun({ text: 'System Report \u2013 Iteration 2', font: FONT, size: 40, color: ACCENT })] }),
    p('Team Sharks', { align: AlignmentType.CENTER, size: 28, after: 240 }),
    p('Shafeek Saif \u00b7 Rakan Ziyan \u00b7 Mohamed Zereny', { align: AlignmentType.CENTER, size: 24, after: 120 }),
    p('Product Owner: Mohamed Zereny \u00b7 Scrum Master: Shafeek Saif \u00b7 all three are developers',
        { align: AlignmentType.CENTER, size: 20, after: 600 }),
    p('Software Engineering and Software Quality Assurance in Information Systems',
        { align: AlignmentType.CENTER, size: 22, after: 120 }),
    p(`Assignment 5 \u00b7 Iteration 2 \u00b7 ${DATE}`, { align: AlignmentType.CENTER, size: 22, after: 120 }),
    p('https://github.com/shafeekseef81-svg/Sharks', { align: AlignmentType.CENTER, size: 20, after: 400 }),
];
if (!FINAL) {
    title.push(new Paragraph({
        alignment: AlignmentType.CENTER,
        bidirectional: false,
        spacing: { before: 400, after: 0 },
        shading: { type: ShadingType.CLEAR, fill: 'FFF2CC' },
        children: [new TextRun({
            text: `Draft of ${DATE} for the team's review. Section 7 lists what is still to be verified before this is submitted.`,
            font: FONT, size: 20, italics: true,
        })],
    }));
}
title.push(new Paragraph({ children: [new PageBreak()] }));

// ------------------------------------------------------------------ contents
const contents = [
    new Paragraph({
        bidirectional: false,
        spacing: { before: 320, after: 120 },
        children: [new TextRun({ text: 'Contents', font: FONT, bold: true, color: ACCENT, size: 30 })],
    }),
    new TableOfContents('Contents', { hyperlink: true, headingStyleRange: '1-2' }),
    p(''),
    p([{ text: 'Where the required structure is. ', bold: true },
       { text: 'The assignment asks for five things, 3(a) to 3(e). This report continues the numbering of the Iteration 1 report, so they are in sections 2 to 6:' }],
      { after: 120 }),
    table([
        ['Required', 'Section here'],
        ['3(a) How to run the system, with libraries and versions', '2. How to run the system'],
        ['3(b) Inputs', '3. Inputs'],
        ['3(c) Expected results in the different scenarios', '5. Expected results in the different scenarios'],
        ['3(d) What the player can do (the screens)', '4. What the player can do'],
        ['3(e) How we tested it, and the evidence', '6. How we tested the system'],
    ], [55, 45]),
    new Paragraph({ children: [new PageBreak()] }),
];

// ------------------------------------------------------------------ 1. About this iteration
const s1 = [
    h('1. About this iteration', HeadingLevel.HEADING_1),
    p('Iteration 2 adds the two larger levels and every special rule that goes with them, on top of the playable Easy game delivered in Iteration 1 (SDP 5.2.2, user stories US-2.1 to US-2.4). The architecture is unchanged: a Java 17 Swing desktop program in the MVC packages Model, View and Controller.'),
    p([{ text: 'What is new in Iteration 2:', bold: true }], { after: 80 }),
    bullet('the Medium (17x17) and Hard (19x19) boards, with their outer rings, extra special squares and legends;'),
    bullet('the special dice \u2013 Boost on Medium and UltraBoost on Hard \u2013 and the movement formula Regular \u00d7 (special \u2212 1);'),
    bullet('Double Blocks: two pieces of one colour on one square, which an opponent can neither land on nor pass;'),
    bullet('question squares, which ask a question of the square\u2019s own level and move the piece forward or back by that level;'),
    bullet('surprise squares, with six outcomes, including moving an opponent\u2019s piece chosen by the active player;'),
    bullet('chained effects, where a question or surprise movement activates the square it lands on, with a guard against an endless chain.'),
    p([{ text: 'Four design patterns ', bold: true }, { text: 'were implemented in the Model this iteration \u2013 Strategy (the dice), State (the turn phases), Factory Method (the questions) and Command (the effects). They have their own deliverable, ' }, { text: 'iteration2_dp_Sharks', font: 'Consolas' }, { text: '.' }]),
    p([{ text: 'What comes in Iteration 3 (by the SDP plan):', bold: true }], { after: 80 }),
    bullet('the game history saved to the course CSV file, the Game History screen, administrator login and question management (FR-56 to FR-64).'),
    p([{ text: 'State of the code. ', bold: true },
       { text: `All Iteration 2 code is merged into the branch ` },
       { text: 'iteration-2', font: 'Consolas' },
       { text: `, at commit ` },
       { text: BASELINE, font: 'Consolas' },
       { text: `, in 32 reviewed pull requests. The whole automated suite is ` },
       { text: `${SUITE} tests, all passing`, bold: true },
       { text: ' (0 failed, 0 skipped, 0 aborted).' }]),
];

// ------------------------------------------------------------------ 2. How to run  [3(a)]
const s2 = [
    h('2. How to run the system', HeadingLevel.HEADING_1),
    p([{ text: 'This section is the required part 3(a).', italics: true }], { after: 160 }),
    h('2.1 Requirements', HeadingLevel.HEADING_2),
    table([
        ['', 'Version', 'Verified'],
        ['Java (JDK or JRE) to run', '17 or later', 'yes \u2013 built and run on Temurin 17.0.17+10'],
        ['JDK to build', '17 or later; compiled with javac --release 17', 'yes \u2013 javac 17.0.17'],
        ['Operating system', 'Windows 10/11, macOS or Linux', 'Windows 11 Pro 10.0.26200 verified; the second release computer is still to be done (section 7)'],
        ['Internet connection', 'not needed, to build or to run', 'yes \u2013 the build uses no network'],
    ], [26, 30, 44]),
    p(''),
    h('2.2 Libraries and versions', HeadingLevel.HEADING_2),
    table([
        ['Library', 'Version', 'Used for', 'In the game JAR?'],
        ['Java Standard Library, Swing (javax.swing) and AWT 2D', 'part of the JDK (17)', 'the whole user interface and all drawing', 'n/a \u2013 part of Java'],
        ['junit-platform-console-standalone', '1.10.2, SHA-1 552303cce67dc3485ded393d744c0f6b5cae355b, from Maven Central, in Iteration2/lib/', 'running the JUnit 5 tests only', 'no'],
    ], [22, 34, 26, 18]),
    p(''),
    p([{ text: 'There is no other third-party library.', bold: true },
       { text: ' No build tool (no Maven or Gradle), no external service and no database. The game is plain Java and Swing, built by a shell script and a batch file.' }]),
    h('2.3 Running and building', HeadingLevel.HEADING_2),
    p([{ text: 'The submitted folder is ' }, { text: 'iteration2_Sharks', font: 'Consolas' }, { text: '. From inside it:' }]),
    table([
        ['Action', 'Windows', 'macOS / Linux / Git Bash'],
        ['Start the game', 'double-click run.bat', './run.sh'],
        ['Build EducationalLudo.jar', 'build.bat', './build.sh'],
        ['Build and run every test', 'test.bat', './test.sh'],
    ], [34, 33, 33]),
    p(''),
    bullet([{ text: 'Start the game by double-clicking run.bat. ', bold: true },
            { text: 'The assignment requires a run file that works on a double-click.' }]),
    bullet([{ text: 'Why run.bat is the documented way in. ', bold: true },
            { text: 'On Windows a path containing Hebrew letters made java -jar "C:\\...\\EducationalLudo.jar" fail with ' },
            { text: 'Unable to access jarfile', italics: true },
            { text: '. The scripts change into their own folder and use a relative path, so they work whatever the folder is called. This was found in Iteration 1.' }]),
    bullet([{ text: 'A double-click on EducationalLudo.jar also works', bold: true },
            { text: ' and is checked separately in the release procedure. It was verified on this computer, including from a folder with Hebrew letters, where the Iteration 1 failure did not reproduce.' }]),
    bullet([{ text: 'EducationalLudo.jar and build/ are not stored in Git', bold: true },
            { text: '; build.bat recreates them. Verified from a clean tree: Main-Class: Controller.Main, all class files Java 17.' }]),
    p([{ text: 'Running the tests prints a JUnit summary. On the submitted code it reads:' }]),
    mono(`[       ${SUITE} tests found           ]\n[         0 tests skipped         ]\n[         0 tests aborted         ]\n[       ${SUITE} tests successful      ]\n[         0 tests failed          ]`),
];

// ------------------------------------------------------------------ 3. Inputs  [3(b)]
const s3 = [
    h('3. Inputs', HeadingLevel.HEADING_1),
    p([{ text: 'This section is the required part 3(b).', italics: true }], { after: 160 }),
    p('All input is by mouse and keyboard through the Swing interface. There is no file, command-line or network input. The settings are entered on the New Game screen; the game itself is played with mouse clicks, and the dialogs can also be used from the keyboard.'),
    table([
        ['Screen', 'Input', 'Values accepted', 'Verified by'],
        ['Main menu', 'click', 'New game, exit (history and administration are Iteration 3)', 'MainMenuViewTest, ScreenNavigationTest'],
        ['Game setup', 'level', 'Easy, Medium, Hard \u2013 all three selectable since T2-01 (FR-05)', 'GameSetupViewTest, acceptance tests'],
        ['Game setup', 'number of players', '2, 3 or 4', 'SetupAcceptanceTest, SettingsValidationTest'],
        ['Game setup', 'player names and colours', 'non-blank, 1\u201312 characters, unique; each colour once', 'SetupInputIndependentTest'],
        ['Game board', 'roll', 'a click on the dice', 'GameControllerTest'],
        ['Game board', 'choose a piece', 'a click on one of the highlighted pieces', 'LegalMoveHighlightTest, GameControllerTest'],
        ['Game board', 'leave the game', 'a click, then a confirmation', 'ScreenNavigationTest'],
        ['Question dialog', 'one of four answers, or No Answer', '0\u20133, or an explicit no-answer which counts as wrong (decision D3)', 'QuestionDialogTest (23 tests)'],
        ['Surprise dialog', 'which opponent piece to move', 'one of the offered pieces, chosen by the active player and never auto-selected (FR-40)', 'SurpriseDialogTest (16 tests)'],
    ], [15, 18, 39, 28]),
    p(''),
    p('While any setup field is invalid the game does not start, and a line at the bottom says how many fields must be fixed. The settings cannot be changed once the game has started.'),
];

// ------------------------------------------------------------------ 4. What the player can do  [3(d)]
const s4 = [
    h('4. What the player can do', HeadingLevel.HEADING_1),
    p([{ text: 'This section is the required part 3(d). ', italics: true },
       { text: 'The screenshots are of the real application running on Windows 11 at 1600\u00d7950, started from the release ZIP. They are also in the repository, under ' },
       { text: 'Iteration2/docs/evidence/', font: 'Consolas' },
       { text: '.' }], { after: 160 }),
    table([
        ['The player can', 'State'],
        ['Start a new game and choose Easy, Medium or Hard', 'works (FR-05)'],
        ['Choose 2, 3 or 4 players and name them', 'works'],
        ['Roll the dice, and see both dice on Medium and Hard', 'works (FR-16)'],
        ['See which pieces may legally move, highlighted after a roll', 'works (FR-24)'],
        ['Choose a piece and watch it move square by square', 'works (FR-20, NFR-04)'],
        ['Bring a piece out of its castle with a Regular 6, and roll again', 'works (FR-19, FR-21)'],
        ['Capture an opponent\u2019s piece on an unprotected square', 'works (FR-26)'],
        ['Be stopped by, and form, a Double Block', 'works (FR-28 to FR-30)'],
        ['Bring pieces up the home path and finish them exactly', 'works (FR-18, FR-31)'],
        ['Answer a question on a question square, or decline with No Answer', 'works (FR-34 to FR-38, decision D3)'],
        ['Receive a surprise, and choose which opponent piece to move', 'works (FR-39 to FR-42, decision D2)'],
        ['See one effect set off another, as a chain', 'works (FR-41, FR-42)'],
        ['See the game clock, and the ranking and duration at the end', 'works (FR-43 to FR-47)'],
        ['See the history, or manage questions as an administrator', 'not in Iteration 2 \u2013 Iteration 3 (FR-56 to FR-64)'],
    ], [62, 38]),
    p(''),
    h('4.1 The main menu and the boards', HeadingLevel.HEADING_2),
    ...figure('01_launch_main_menu.png', 'The main menu, with the game started from the release ZIP'),
    p('The two new boards are the visible heart of this iteration. The left panel names the level and prints the track arithmetic and the count of every kind of special square; those counts are the ones in section 5, read from the running game rather than from the code.'),
    ...figure('02_medium_board.png', 'The Medium board: 17\u00d717, one outer ring, 52 + 64 = 116 track squares, 6 question and 2 surprise squares'),
    ...figure('03_hard_board.png', 'The Hard board: 19\u00d719, two outer rings, 52 + 64 + 72 = 188 track squares, 9 question and 3 surprise squares. The dice panel shows Regular \u00d7 (UltraBoost \u2212 1)'),
    h('4.2 Question squares', HeadingLevel.HEADING_2),
    p([{ text: 'A piece that stops on a question square shows all players a question ' },
       { text: 'of that square\u2019s level, not the board\u2019s', bold: true },
       { text: ' (FR-34), with four answers. A correct answer moves the piece forward 1, 2 or 3 steps by the level of the question and a wrong answer moves it back the same number; the card then says whether it was right and names the correct answer (FR-35, FR-36). No question repeats until its level\u2019s deck is used up (FR-37).' }]),
    ...figure('04_question_card_level.png', 'FR-34 on screen: an Easy question, with the Easy badge and \u201c1 steps at stake\u201d, on a Medium board. The level follows the square, not the board'),
    ...figure('05_answer_feedback.png', 'FR-35 and FR-36: the card names the correct answer and says how far the piece moves'),
    h('4.3 The No Answer button', HeadingLevel.HEADING_2),
    p([{ text: 'Product Owner decision D3 gives the player an explicit way to answer nothing, which counts as a wrong answer (FR-35). ' },
       { text: 'Testing the real application found that this button was invisible', bold: true },
       { text: ' \u2013 it was laid out and it worked, but it was painted white on the white card, so a player using the mouse could not reach it at all. It is fixed, and the fix is covered by tests that read the painted pixels; section 6.6 has the detail.' }]),
    ...figure('06_noanswer_visible_after_fix.png', 'The question card with the No Answer button visible'),
    ...figure('07_noanswer_focus_ring.png', 'The same button holding keyboard focus, with its focus ring'),
    h('4.4 Surprise squares', HeadingLevel.HEADING_2),
    p([{ text: 'Landing on a surprise square triggers exactly one of six outcomes (FR-39). Two of them act on the turn rather than on a piece; for “move an opponent” the active player chooses which piece moves, and nothing is auto-selected (FR-40).' }]),
    ...figure('10_surprise_card_skip_turn.png', 'A surprise square on the Hard board. One card shows three rules at once: exactly one outcome is drawn and named (FR-39); the badge says whose turn it falls on, and it is the active player’s (decision D2); and the text states that an extra roll from a 6 is cancelled, which is the rule ratified as decision D5'),
];

// ------------------------------------------------------------------ 5. Expected results  [3(c)]
const s5 = [
    h('5. Expected results in the different scenarios', HeadingLevel.HEADING_1),
    p([{ text: 'This section is the required part 3(c).', italics: true }], { after: 160 }),
    h('5.1 The three levels', HeadingLevel.HEADING_2),
    p([{ text: 'Read from ' }, { text: 'Model.LevelConfig', font: 'Consolas' },
       { text: ', and confirmed on screen in figures 2 and 3:' }]),
    table([
        ['', 'Easy', 'Medium', 'Hard'],
        ['Grid', '15×15', '17×17', '19×19'],
        ['Outer rings', '0', '1', '2'],
        ['Track squares', '52', '52 + 64 = 116', '52 + 64 + 72 = 188'],
        ['Home path', '6', '6', '6'],
        ['Start squares', '4', '4', '4'],
        ['Protection squares', '4', '8', '12'],
        ['Arrows', '4', '8', '12'],
        ['Question squares', '0', '6 (2 of each level)', '9 (3 of each level)'],
        ['Surprise squares', '0', '2', '3'],
        ['Dice rolled', 'Regular (1–6)', 'Regular + Boost (1–3)', 'Regular + UltraBoost (1–6)'],
        ['Movement', 'the Regular value', 'Regular × (Boost − 1)', 'Regular × (UltraBoost − 1)'],
        ['Steps a question moves', '1', '2', '3'],
    ], [25, 20, 27, 28]),
    p(''),
    h('5.2 Scenario by scenario', HeadingLevel.HEADING_2),
    table([
        ['#', 'Scenario', 'Expected result'],
        ['1', 'A complete Easy game', 'Unchanged from Iteration 1 and still passing.'],
        ['2\u20133', 'A complete Medium or Hard game', 'The chosen board appears with exactly the counts in 5.1, the special squares placed at random, and a legend. Each roll shows both dice and the movement is Regular \u00d7 (special \u2212 1).'],
        ['4', 'A special die showing 1', 'The movement is 0 and nothing moves. On Medium this happens on about a third of rolls and on Hard about a sixth. This is what the SRS formula gives; it is written here because a player will notice it.'],
        ['5', 'The longest movement', 'From the level\u2019s own dice: 12 on Medium and 30 on Hard. On Medium that is not the overall bound \u2013 the extra-roll surprise rolls Regular + UltraBoost on every level (SRS Table 2), so a Medium move can also reach 30. The longest Medium surprise roll actually measured is 24.'],
        ['6', 'A Regular 6', 'Brings a piece out of its castle onto its start square and grants an extra roll; three in a row cancel the third (FR-19, FR-21, FR-22).'],
        ['7', 'Landing on a single opponent piece on an unprotected square', 'The opponent piece is captured and returns to its castle. Start, protection and surprise squares are protected; question squares are not (FR-26, FR-27).'],
        ['8', 'Double Blocks', 'Two or more pieces of one colour on one track square form a block. An opponent cannot land on it or pass it; if that is the only possible move, the piece stops on the square before. The block ends as soon as one piece moves away (FR-28 to FR-30).'],
        ['9', 'A question square', 'A question of the square\u2019s level with four answers; right moves forward and wrong moves back by 1, 2 or 3 steps; the card names the correct answer. No repeat until the level\u2019s deck is used up (FR-34 to FR-37). A capture on the square happens before the question (FR-38).'],
        ['10', 'A surprise square', 'Exactly one of six outcomes (FR-39). For \u201cmove an opponent\u201d the active player chooses which piece, and nothing is auto-selected (FR-40).'],
        ['11', 'A chained effect', 'A question or surprise movement activates any special square it lands on, obeying the ordinary movement and blocking rules, with a guard against an endless chain (FR-41, FR-42). A backward movement stops at the piece\u2019s own start square (FR-32).'],
        ['12', 'An illegal action in a turn', 'Moving before rolling, rolling twice without moving, or rolling while a special square is still waiting, are all refused (NFR-09).'],
        ['13', 'The end of the game', 'The game ends when all but one player have all four pieces home; the results screen shows the ranking in finishing order and the duration (FR-43 to FR-47).'],
    ], [5, 24, 71]),
    p(''),
    h('5.3 Product Owner decisions', HeadingLevel.HEADING_2),
    p([{ text: 'Where the SRS left a rule open, the Product Owner decided it and the decision was written down with its evidence, in ' },
       { text: 'Iteration2/docs/T2-15_PO_Decisions.md', font: 'Consolas' }, { text: '.' }]),
    table([
        ['#', 'Question', 'State'],
        ['D1', 'An opponent Double Block standing on a player\u2019s start square keeps that player\u2019s pieces in the castle, even on a Regular 6.', 'OPEN. This is what FR-19 specifies \u2013 it states the exception itself, \u201cunless an opponent Double Block occupies that square\u201d. What is open is narrower: whether two pieces on a start square form a Double Block there at all. The course answers Q7/Q35 are cited by the SRS but not reproduced in it, so this needs the staff, not the Product Owner. The implemented rule follows FR-19 meanwhile.'],
        ['D2', 'When an effect chain started by an opponent\u2019s piece gives a skip-turn or an extra roll, who gets it?', 'DECIDED: the active player, never the owner of the piece that landed on the square.'],
        ['D3', 'May a player decline to answer a question?', 'DECIDED: yes, by an explicit No Answer button, and it counts as a wrong answer.'],
        ['D4', 'Where does a backward movement stop?', 'RATIFIED 28/11/2026: at the moving piece’s own starting square, never crossing it (FR-32). No gameplay change was needed – this is the behaviour already implemented, merged and independently tested.'],
        ['D5', 'How do a surprise extra roll and a pending roll from a 6 interact?', 'RATIFIED 28/11/2026: the surprise extra roll is taken first; a later skip-turn also cancels the pending roll from the 6, and the player misses exactly one turn (Q27, Q56). No gameplay change was needed – and the running game states this rule on the surprise card itself.'],
    ], [6, 30, 64]),
];

// ------------------------------------------------------------------ 6. Testing  [3(e)]
const s6 = [
    h('6. How we tested the system', HeadingLevel.HEADING_1),
    p([{ text: 'This section is the required part 3(e).', italics: true }], { after: 160 }),
    h('6.1 Automated tests', HeadingLevel.HEADING_2),
    p([{ text: `The project has ` }, { text: `${SUITE} automated JUnit 5 tests`, bold: true },
       { text: `; on the submitted code all of them pass (${SUITE} found, ${SUITE} successful, 0 failed, 0 skipped, 0 aborted), run with test.bat or test.sh on Temurin 17.0.17+10. Iteration 1 finished with 380.` }]),
    table([
        ['Package', 'Tests', 'What they check'],
        ['Model', '542', 'The three levels and their dice, the movement formula, routes and board geometry, capture and protection, Double Blocks, the home path and exact finish, questions, surprises and chained effects, the turn phases, ranking and the game clock, and complete games played end to end.'],
        ['View', '142', 'The menu and setup screens, the three boards and their legends, legal-move highlighting, the square-by-square animation and its timing, and the question and surprise cards \u2013 including what is actually painted.'],
        ['Controller', '17', 'Navigation between screens, and the GameController with the real screens and model.'],
        ['Architecture', '4', 'The MVC dependency rules: the Model has no Swing, View or Controller imports.'],
    ], [14, 10, 76]),
    p(''),
    p([{ text: '607 of the 705 test names', bold: true },
       { text: ' name the requirement, decision or course answer they verify, so the test output is itself a large part of the traceability evidence. The other 98 are the ones with no single requirement to name: argument validation, helper classes and checks on the structure of a design pattern.' }]),
    h('6.2 Our testing method', HeadingLevel.HEADING_2),
    bullet([{ text: 'Unit tests by the author ', bold: true }, { text: 'for every class (Definition of Done, SDP 5.3.4).' }]),
    bullet([{ text: 'Independent tests: ', bold: true },
            { text: 'every game rule is tested again by a member who did not write it. Expected values are written out by hand from the SRS \u2013 cells from the board\u2019s paths, step counts, the Table 2 shares \u2013 and never read from the code under test.' }]),
    bullet([{ text: 'Acceptance tests ', bold: true },
            { text: 'play complete games through the GameModel facade with the real dice, the real random boards and all the real rule classes \u2013 no stubs \u2013 with fixed seeds, so any failure reproduces exactly.' }]),
    bullet([{ text: 'View tests ', bold: true },
            { text: 'lay a component out and paint it into an image, so the drawing and the animation are tested without a visible window.' }]),
    bullet([{ text: 'Fault seeding (mutation checking): ', bold: true },
            { text: 'each rule was broken on purpose in a copy of the code, one at a time, to prove the tests catch it.' }]),
    bullet([{ text: 'Code review on GitHub: ', bold: true },
            { text: 'every change went through a pull request and was reviewed before merging; the rule classes had a deep review by both other members.' }]),
    bullet([{ text: 'Testing the real application: ', bold: true },
            { text: 'the game was driven on a real Windows display with real mouse clicks and key presses, which is how the No Answer defect was found (section 6.6).' }]),
    h('6.3 Requirements traceability', HeadingLevel.HEADING_2),
    p([{ text: 'Checked with the team\u2019s own tool, ' }, { text: 'Iteration2/tools/rtm_coverage.py', font: 'Consolas' },
       { text: ', against the SRS text, for the Iteration 2 range FR-15 to FR-47:' }]),
    mono('pdftotext -layout Assignment3_Prototype/Sharks_SRS.pdf srs.txt\ncd Iteration2 && python tools/rtm_coverage.py ../srs.txt test 15 47\n...\nGAPS: none'),
    p([{ text: 'Every requirement in the range is named by at least one test.', bold: true },
       { text: ' The thinnest are FR-47, the end-of-game summary screen, with 2 tests, and FR-38, a capture on a question square, with 3; both are also on the manual checklist.' }]),
    h('6.4 Independent tests and mutation checking', HeadingLevel.HEADING_2),
    p('SDP 5.3.4 asks that rules be tested by someone other than their author. Three test classes exist only for that:'),
    table([
        ['Rules', 'Test class', 'Tests', 'Code author', 'Tests written by'],
        ['Dice distribution, movement formula, routes (FR-16, FR-17, FR-25, NFR-13)', 'DiceRouteIndependentTest', '15', 'Shafeek Saif', 'Rakan Ziyan'],
        ['Double Blocks (FR-19, FR-28 to FR-30)', 'DoubleBlockIndependentTest', '19', 'Rakan Ziyan', 'Shafeek Saif'],
        ['Questions, surprises, chains (FR-32, FR-34 to FR-42, Table 2)', 'EffectChainIndependentTest', '25', 'Mohamed Zereny, Shafeek Saif', 'Rakan Ziyan'],
    ], [32, 22, 8, 19, 19]),
    p(''),
    p([{ text: 'A test that passes on its first run proves little', bold: true },
       { text: ', so each rule was broken on purpose in a copy of the code and the suite run against the broken copy. All eight seeded faults in the special-square rules were caught, among them: a backward movement not stopped at the start square; effects ignoring Double Blocks; only the first square of a chain being activated; and a turn surprise falling on the owner of the piece instead of the active player (decision D2), which exactly one test in the suite catches. ' },
       { text: 'One test was rewritten because of this check', bold: true },
       { text: ' \u2013 the first version did not catch its broken copy, because another guard hid the fault. Switching Double Blocks off makes 16 of the 19 independent block tests fail.' }]),
    h('6.5 Timing (NFR-02 and NFR-04)', HeadingLevel.HEADING_2),
    p([{ text: 'NFR-02, the board appearing: ', bold: true },
       { text: 'worst 78 ms over 21 random Hard boards, against a 2000 ms limit.' }]),
    p([{ text: 'NFR-04, the longest movement, is the one requirement where a test found a real problem.', bold: true },
       { text: ' The 30-square Hard move passed when timed on its own, but with nine test suites running in parallel on another member\u2019s computer the same move took 2140 ms \u2013 over the limit. The finding was not that the game is slow, but that the Iteration 1 animation budget of 1600 ms left too little headroom, because the measured time is the budget plus the Swing timer\u2019s latency and the final repaints. The budget was lowered to 1200 ms.' }]),
    table([
        ['Measurement', 'Result', 'Limit'],
        ['30 squares, off-screen, budget 1600 ms (before)', '1649 ms, and 1992\u20132043 ms on a cold JVM on a slower computer \u2013 2 of 3 runs failed', '2000 ms'],
        ['30 squares, off-screen, budget 1200 ms (after)', '1278 ms', '2000 ms'],
        ['30 squares, in a real visible window, running the real board component', '1296\u20131313 ms over 5 runs, mean about 1305 ms, measured after the final frame is painted', '2000 ms'],
        ['Inside a complete running game, measured from outside the process', '16 measurements, all under 2000 ms; the longest 1578 ms for a 10-step move, which is already at the 1200 ms cap', '2000 ms'],
    ], [34, 50, 16]),
    p(''),
    ...figure('09_nfr04_harness.png', 'The timing harness: the real board component in a real window, mid-move', 3.6),
    p([{ text: 'What is still open on NFR-04: ', bold: true },
       { text: 'a 30-square move arising inside a full game, timed on screen with a stopwatch. Every figure above either comes from a harness rather than a played game, or from a shorter move.' }]),
    h('6.6 Testing the real application on screen', HeadingLevel.HEADING_2),
    p('The game was driven on a real Windows display with real mouse clicks and key presses, and every screenshot was captured from the game window only. This is where the automated tests and the running program were checked against each other.'),
    p([{ text: 'Confirmed on screen: ', bold: true },
       { text: 'both new boards with the counts of section 5.1; the dice panels and the movement formula; legal-move highlighting; the turn order and the game clock; a roll of 1 correctly passing the turn with all pieces in the castle; name validation; the leave-game confirmation; a question carrying the square\u2019s level on both boards (FR-34); and a right answer, a wrong answer and a No Answer each moving the piece the specified number of squares and naming the correct answer (FR-35, FR-36).' }]),
    p([{ text: 'One defect was found, and no automated test could have found it.', bold: true },
       { text: ' On the question card the No Answer button was ' },
       { text: 'completely invisible', bold: true },
       { text: ': the region below the answers was a single uniform white across 80,400 sampled pixels. The button was laid out, it held keyboard focus and the keyboard could press it, but nothing was painted \u2013 so a player using the mouse could not reach decision D3\u2019s \u201canswer nothing\u201d route at all. The cause was a button style built for dark backgrounds being used on the white card: no fill, a white border, a white label and a white focus ring.' }]),
    p([{ text: 'Why 23 existing tests missed it: ', bold: true },
       { text: 'they all assert behaviour \u2013 the label text, visibility, a simulated click, the key binding, the result submitted \u2013 and every one of those passed while the button was invisible, because none of them looks at a painted pixel. The fix is one word, the button style; the regression test renders the real card and measures the pixels. Against the old code 3 of its 5 tests fail; with only the focus ring disabled, the 2 focus tests fail and the label test correctly still passes.' }]),
    p([{ text: 'The lesson is written into the Iteration 3 actions: ', italics: true },
       { text: 'a pull request that changes a screen or a dialog is opened in the running game before it is approved, and has at least one test that looks at the painted result.', italics: true }]),
    h('6.7 Release checks', HeadingLevel.HEADING_2),
    p([{ text: 'The submission ZIP is built only by ' }, { text: 'tools/make_iteration2_zip.py', font: 'Consolas' },
       { text: '. It exports the versioned files, builds the JAR with --release 17, checks the manifest and that every class file is Java 17, runs the whole suite, and writes the ZIP with forward slashes under one top folder, printing the file count, the test count and the SHA-256. ' },
       { text: 'The script stops if any test failed or was aborted, or if no test ran at all', bold: true },
       { text: ' \u2013 that last case was found in review, because the JUnit launcher exits 0 on an empty run.' }]),
    table([
        ['Check', 'Result'],
        ['ZIP structure: one top folder, no backslash in any entry, no build/ directory', 'PASS, 169 files'],
        ['JAR manifest and Java 17 class files', 'PASS'],
        ['The whole suite inside the staged copy', 'PASS, 685/685 on the CP1 commit'],
        ['java -jar from a folder with Hebrew letters', 'PASS'],
        ['Double-click run.bat opens the game, with no console left behind', 'PASS on both computers used so far'],
        ['Double-click EducationalLudo.jar opens the game', 'PASS'],
        ['Easy, Medium and Hard each start and accept a roll', 'PASS (by hand, computer 1)'],
        ['A full game reaches the results screen', 'PASS (by hand, computer 1)'],
        ['All of the above on a second computer', 'NOT YET DONE \u2013 see section 7'],
    ], [62, 38]),
    p(''),
    p([{ text: 'One finding worth recording: a release ZIP cannot be rebuilt byte for byte.', bold: true },
       { text: ' Two of us built the ZIP from the same commit with the same script and got different SHA-256 values, for two independent reasons: the ZIP stores build timestamps, and the two JDKs produced JARs of different sizes (261,533 and 263,868 bytes) from identical source. So the final artifact is built ' },
       { text: 'once', bold: true },
       { text: ', its SHA-256 is recorded, and ' },
       { text: 'that same file', bold: true },
       { text: ' is copied to the second computer and checked with Get-FileHash before it is tested. Two rebuilds would be two different files, and the recorded hash would verify neither.' }]),
];

// ------------------------------------------------------------------ 7. Limitations
const s7 = [
    h('7. Known limitations and what is still to be verified', HeadingLevel.HEADING_1),
    p([{ text: 'Everything in sections 4 to 6 is measured. This section is what an examiner should treat as ' },
       { text: 'not', italics: true }, { text: ' yet evidenced, so that nothing above is read as more than it is.' }]),
    table([
        ['#', 'Open item', 'State'],
        ['1', 'A complete Medium and Hard game played by hand, from the menu to the results screen, by the Product Owner, following the acceptance script', 'One of us has played a full game by hand on computer 1 as part of the release checks. The scripted acceptance run is still to be done. The automated play covered setup, both boards, the dice, highlighting, four question cards and about 200 turns without a freeze, but reached neither the results screen nor a surprise square.'],
        ['2', 'The release checks on a second physical computer', 'NOT DONE for Iteration 2. It is run on a byte-identical copy of the final ZIP, not on a rebuild.'],
        ['3', 'A 30-square movement arising in a full game, timed on screen', 'Partly covered: a real-window harness measures the 30-square move at about 1305 ms, and 16 in-game measurements are all under the limit. The combination of the two is open.'],
        ['4', 'Surprise squares seen on screen', 'Covered end to end by automated tests, and all six outcomes are drawn in the full-game runs, but no surprise square was reached in the on-screen run.'],
        ['5', 'Decision D1', 'OPEN, and not closable by the Product Owner alone: it needs the course staff’s answer on Q7/Q35. The implemented rule follows FR-19 meanwhile. D2 and D3 were decided on 21/11 and D4 and D5 ratified on 28/11, so D1 is the only decision still open.'],
        ['6', 'FR-40’s “move an opponent” case, and a chained effect, seen on screen', 'A surprise card was reached on screen on 28/11, confirming FR-39 and decisions D2 and D5 in the running game. But the outcome drawn moves no piece, so the case where the player chooses which opponent piece moves has still not been seen, and neither has a chain.'],
    ], [5, 30, 65]),
    p(''),
    p([{ text: 'Carried over from Iteration 1: ', bold: true },
       { text: 'on Windows, starting the JAR by its full path from a folder with Hebrew letters can fail, which is why run.bat is the documented way in. The game history, the Game History screen and the administrator functions are Iteration 3.' }]),
];

// ------------------------------------------------------------------ 8. Repository
const s8 = [
    h('8. GitHub repository', HeadingLevel.HEADING_1),
    p('Repository: https://github.com/shafeekseef81-svg/Sharks'),
    p([{ text: 'The repository name carries the team name. The Iteration 2 code is in the folder ' },
       { text: 'Iteration2', font: 'Consolas' }, { text: ' on the branch ' },
       { text: 'iteration-2', font: 'Consolas' }, { text: ', at commit ' },
       { text: BASELINE, font: 'Consolas' },
       { text: '. Every change was made on a feature branch and merged through a reviewed pull request; the branch is protected and requires an approval.' }]),
    p([{ text: 'The documents behind this report, all in ' }, { text: 'Iteration2/docs/', font: 'Consolas' }, { text: ':' }]),
    table([
        ['File', 'What it is'],
        ['N2-04_SystemReport.md', 'the full working source of this report, with the measurements in more detail'],
        ['T2-15_PO_Decisions.md', 'the Product Owner decision register, D1 to D5'],
        ['T2-15_AcceptancePlan.md, T2-15_ManualChecklist_CP1.md', 'the acceptance and manual test scripts'],
        ['N2-06_Release.md', 'the release checklist, including the two double-click checks and the second computer'],
        ['T2-14_RTM_Coverage.md', 'the requirements traceability snapshot'],
        ['evidence/', 'the screenshots used as figures in this report'],
    ], [38, 62]),
];

// ------------------------------------------------------------------ assemble
const doc = new Document({
    creator: 'Team Sharks',
    title: 'Educational Ludo \u2013 System Report \u2013 Iteration 2',
    description: 'Assignment 5, Iteration 2 system report, Team Sharks',
    styles: {
        default: {
            document: { run: { font: FONT, size: 22 } },
            heading1: { run: { font: FONT, size: 30, bold: true, color: ACCENT } },
            heading2: { run: { font: FONT, size: 25, bold: true, color: ACCENT } },
        },
    },
    sections: [{
        properties: {
            page: { margin: { top: convertInchesToTwip(0.9), bottom: convertInchesToTwip(0.9),
                left: convertInchesToTwip(1), right: convertInchesToTwip(1) } },
        },
        footers: {
            default: new Footer({
                children: [new Paragraph({
                    alignment: AlignmentType.CENTER,
                    bidirectional: false,
                    children: [new TextRun({
                        text: 'Team Sharks \u00b7 Iteration 2 System Report \u00b7 page ', font: FONT, size: 16, color: '808080',
                    }), new TextRun({ children: [PageNumber.CURRENT], font: FONT, size: 16, color: '808080' })],
                })],
            }),
        },
        children: [
            ...title, ...contents, ...s1, ...s2, ...s3, ...s4, ...s5, ...s6, ...s7, ...s8,
        ],
    }],
});

Packer.toBuffer(doc).then((buf) => {
    fs.writeFileSync(OUT, buf);
    console.log(`wrote ${OUT} (${buf.length} bytes), ${figureNo} figures, ${FINAL ? 'FINAL' : 'DRAFT'}`);
});
