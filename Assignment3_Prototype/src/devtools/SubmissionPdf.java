package devtools;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

/**
 * Builds the Assignment 3 submission PDF ("prototype_groupName"): a cover page, one page per
 * screenshot in the order of the user flows with a short caption, and the list of tools used.
 * Self-contained PDF writer (JPEG images, built-in Helvetica), so no library is needed.
 *
 * <p>Usage: {@code java -jar EducationalLudo.jar --submission-pdf [file] [screenshotDir]}
 */
public final class SubmissionPdf {

    private record Shot(String suffix, String flow, String title, String caption) {
    }

    private static final String F1 = "Flow 1 - Start a new game";
    private static final String F2 = "Flow 2 - Roll the dice and move a piece";
    private static final String F3 = "Flow 3 - Educational question";
    private static final String F4 = "Flow 4 - Surprise event";
    private static final String F5 = "Flow 5 - Game results";
    private static final String F6 = "Flow 6 - Game history";
    private static final String F7 = "Flow 7 - Administrator: question management";

    private static final List<Shot> SHOTS = List.of(
            new Shot("main_menu", F1, "Main Menu",
                    "The application opens on the main menu: New Game, Game History, Question Management "
                            + "(administrators only, behind a login) and Exit. (FR-01, FR-02)"),
            new Shot("game_setup", F1, "Game Setup",
                    "Choose 2, 3 or 4 players and the difficulty; each card explains the board, special squares and "
                            + "dice of the level. Every player enters a unique name (1-12 characters) and picks a unique "
                            + "colour; colours taken by another player are disabled. (FR-04 - FR-07)"),
            new Shot("game_setup_validation", F1, "Setup Validation",
                    "Invalid input is caught before the game starts: the empty name and the duplicate name are "
                            + "highlighted with a message next to the field. (FR-08)"),
            new Shot("board_easy_start", F1, "Game Board - Easy",
                    "Easy board (classic 15 x 15, drawn like the official diagram): castles with four pieces, the "
                            + "52-square track, coloured home paths, # start squares, arrows and protection stars. Left: "
                            + "level, timer, players and legend. Right: whose turn it is, the dice and Roll Dice. Easy "
                            + "has no question or surprise squares, so those two demo buttons are off. (FR-10 - FR-15, FR-46)"),
            new Shot("board_hard", F1, "Game Board - Hard",
                    "Hard board (19 x 19): two extra rings of 64 and 72 squares, start squares in the outer corners, "
                            + "three arrows per colour leading inward ring by ring, question squares coloured by question "
                            + "level (green / yellow / red) and purple surprise squares. (SRS Table 1)"),
            new Shot("board_medium", F1, "Game Board - Medium",
                    "Medium board (17 x 17, 52 + 64 squares) during a game: Regular + Boost dice, 8 protection squares, "
                            + "8 arrows, 6 question squares and 2 surprise squares."),
            new Shot("roll_and_highlight", F2, "Roll Dice",
                    "After a short dice animation the result and the movement Regular x (Boost - 1) are shown. The "
                            + "pieces that can move glow; pointing at one previews its path and landing square. "
                            + "(FR-16, FR-17, FR-24)"),
            new Shot("piece_moved", F2, "Piece Moved",
                    "Clicking a glowing piece moves it square by square. A 6 was rolled, so the same player rolls "
                            + "again. (FR-20, FR-21)"),
            new Shot("question", F3, "Educational Question",
                    "Landing on a question square opens a multiple-choice question of that square's level. The active "
                            + "player selects one of four answers and submits it. (FR-34)"),
            new Shot("question_feedback", F3, "Answer Feedback",
                    "Feedback after Submit: the chosen answer is marked wrong, the correct answer is shown and the "
                            + "piece moves back by the level's steps (forward when correct). (FR-35, FR-36)"),
            new Shot("surprise", F4, "Surprise Event",
                    "A surprise square reveals one outcome of SRS Table 2 with its chance and effect. Behaviour that "
                            + "is simplified in the prototype is labelled on the card. (FR-39, FR-40)"),
            new Shot("leave_game_confirmation", F4, "Leaving a Running Game",
                    "Back to Main Menu during a game asks for confirmation and explains that the game will not be "
                            + "saved. (FR-03)"),
            new Shot("results", F5, "Game Results",
                    "Podium with rankings, player colours, difficulty and duration, with Play Again and Main Menu. "
                            + "In the prototype the demo panel can end a game early; the ranking is then simulated from "
                            + "board progress and labelled as such. (FR-45, FR-47)"),
            new Shot("game_history", F6, "Game History",
                    "Completed games, newest first, with the fields of the history CSV template (game ID, difficulty, "
                            + "date and time, 1st - 4th place, duration) and a difficulty filter. The game just played is "
                            + "at the top. (FR-48 - FR-50, FR-63)"),
            new Shot("admin_login", F7, "Administrator Login",
                    "Question Management is protected by an administrator login. The prototype uses a simulated demo "
                            + "account, shown on the card so that testers can continue. (FR-51, FR-52)"),
            new Shot("admin_login_error", F7, "Login Error",
                    "Wrong credentials give a general error that does not reveal which field was wrong; three "
                            + "failures lock the login for 30 seconds. (FR-53, NFR-17)"),
            new Shot("question_management", F7, "Question Management",
                    "All questions with ID, text, difficulty and correct answer; add, edit, delete (with confirmation, "
                            + "keeping at least 30 questions), filter by level and Log Out. (FR-56 - FR-62)"),
            new Shot("question_form_validation", F7, "Question Form Validation",
                    "Add/Edit form: a missing answer, a duplicate answer and a missing correct answer are reported "
                            + "next to the fields; nothing is saved until the form is valid. (FR-58, FR-59)"));

    private static final float PAGE_W = 842;
    private static final float PAGE_H = 595;
    private static final float MARGIN = 40;
    private static final Color NAVY = new Color(0x1D2E6B);
    private static final Color GREY = new Color(0x5E6784);

    private SubmissionPdf() {
    }

    public static void run(String pdfFile, String screenshotDir) throws IOException {
        File dir = new File(screenshotDir);
        File[] files = dir.listFiles((d, name) -> name.toLowerCase(Locale.ROOT).endsWith(".png"));
        if (files == null || files.length == 0) {
            throw new IOException("No screenshots in " + dir.getAbsolutePath() + " (run --screenshots first)");
        }
        Arrays.sort(files);
        Pdf pdf = new Pdf();
        cover(pdf);
        int number = 1;
        for (Shot shot : SHOTS) {
            File file = null;
            for (File f : files) {
                if (f.getName().matches("\\d+_" + shot.suffix() + "\\.png")) {
                    file = f;
                }
            }
            if (file == null) {
                System.out.println("  missing screenshot: " + shot.suffix());
                continue;
            }
            screenshotPage(pdf, number++, shot, file);
        }
        toolsPage(pdf);
        pdf.save(new File(pdfFile));
        System.out.println("PDF written to " + new File(pdfFile).getAbsolutePath() + " (" + pdf.pageCount() + " pages)");
    }

    private static void cover(Pdf pdf) {
        Page p = new Page();
        p.rect(0, PAGE_H - 150, PAGE_W, 150, NAVY);
        float[] bar = {0, PAGE_W / 4, PAGE_W / 2, PAGE_W * 3 / 4};
        Color[] colors = {new Color(0xE8252B), new Color(0x13A24B), new Color(0xFFC619), new Color(0x1E8BEA)};
        for (int i = 0; i < 4; i++) {
            p.rect(bar[i], PAGE_H - 156, PAGE_W / 4, 6, colors[i]);
        }
        p.text("Educational Ludo - Interactive Prototype", MARGIN, PAGE_H - 70, true, 30, Color.WHITE);
        p.text("Assignment 3, part 1: prototype  |  presented in class on 02/11/2026", MARGIN, PAGE_H - 100, false, 14,
                new Color(0xC9D2F0));
        p.text("Team Sharks", MARGIN, PAGE_H - 200, true, 20, NAVY);
        p.text("Shafeek Saif (211614821)   Rakan Ziyan (324849249)   Mohamed Zereny (327892972)", MARGIN,
                PAGE_H - 224, false, 12.5f, Color.BLACK);
        p.text("Software Engineering and Software Quality Assurance, Department of Information Systems, University of Haifa",
                MARGIN, PAGE_H - 244, false, 12.5f, GREY);
        float y = PAGE_H - 290;
        p.text("Contents (screenshots in the order of the user flows)", MARGIN, y, true, 14, NAVY);
        y -= 22;
        String[] flows = {F1, F2, F3, F4, F5, F6, F7};
        for (String flow : flows) {
            int first = -1;
            int last = -1;
            for (int i = 0; i < SHOTS.size(); i++) {
                if (SHOTS.get(i).flow().equals(flow)) {
                    first = first < 0 ? i + 1 : first;
                    last = i + 1;
                }
            }
            String range = first == last ? "screen " + first : "screens " + first + "-" + last;
            p.text(flow, MARGIN + 10, y, false, 12, Color.BLACK);
            p.text(range, MARGIN + 380, y, false, 12, GREY);
            y -= 18;
        }
        y -= 12;
        p.text("About the prototype", MARGIN, y, true, 14, NAVY);
        y -= 20;
        String about = "A Java 17 Swing desktop application that runs completely offline (EducationalLudo.jar, started "
                + "with run.bat). Screens show simulated data; the dice, piece moves, questions and surprises follow a "
                + "simplified version of the SRS rules. A small, clearly labelled PROTOTYPE DEMO panel lets testers open a "
                + "question, a surprise or the results without playing a full game. Public link: not applicable - the "
                + "prototype is a local desktop application provided with the submission.";
        p.paragraph(about, MARGIN, y, PAGE_W - 2 * MARGIN, 12, 16, Color.BLACK);
        footer(p, pdf.pageCount() + 1);
        pdf.add(p);
    }

    private static void screenshotPage(Pdf pdf, int number, Shot shot, File file) throws IOException {
        Page p = new Page();
        p.rect(0, PAGE_H - 8, PAGE_W, 8, NAVY);
        p.text(shot.flow(), MARGIN, PAGE_H - 34, false, 10.5f, GREY);
        p.text(number + ". " + shot.title(), MARGIN, PAGE_H - 56, true, 19, NAVY);
        float y = p.paragraph(shot.caption(), MARGIN, PAGE_H - 76, PAGE_W - 2 * MARGIN, 11, 14.5f, Color.BLACK);
        BufferedImage img = ImageIO.read(file);
        float maxW = PAGE_W - 2 * MARGIN;
        float maxH = y - 8 - 34;
        float scale = Math.min(maxW / img.getWidth(), maxH / img.getHeight());
        float w = img.getWidth() * scale;
        float h = img.getHeight() * scale;
        float x = (PAGE_W - w) / 2;
        float top = y - 8;
        p.rect(x - 1, top - h - 1, w + 2, h + 2, new Color(0xB9C1D6));
        p.image(jpeg(img), img.getWidth(), img.getHeight(), x, top - h, w, h);
        footer(p, pdf.pageCount() + 1);
        pdf.add(p);
    }

    private static void toolsPage(Pdf pdf) {
        Page p = new Page();
        p.rect(0, PAGE_H - 8, PAGE_W, 8, NAVY);
        p.text("Tools Used", MARGIN, PAGE_H - 56, true, 22, NAVY);
        String[][] tools = {
                {"Java 17 (Eclipse Temurin JDK)", "Programming language required by the course. Compiled with "
                        + "javac --release 17 so it runs on any computer with Java 17 or later."},
                {"Java Swing and Java2D", "Standard Java library for the desktop user interface and the custom-drawn "
                        + "Ludo boards, pieces, dice and icons. No external libraries, fonts or images, so the prototype "
                        + "works offline."},
                {"javac, jar, build.bat and run.bat", "Compiling and packaging into one runnable JAR, and starting it "
                        + "with a double-click on Windows."},
                {"Claude Code (Anthropic AI assistant)", "Used to analyse the SRS and the project description, plan the "
                        + "prototype, write the Java code, the self-test, the screenshot tool, this PDF and the "
                        + "documentation. The team reviewed and tested the result."},
                {"Visual Studio Code", "Editing, reviewing and running the project."},
                {"Built-in screenshot and self-test tools", "java -jar EducationalLudo.jar --screenshots creates the "
                        + "screenshots in this document; --selftest checks the boards, dice, rules and every user flow."},
                {"Python with PyMuPDF and Pillow", "Used once to extract the official board diagrams from the project "
                        + "description at full resolution, to compare the drawn boards with them."},
        };
        float y = PAGE_H - 96;
        for (String[] tool : tools) {
            p.text(tool[0], MARGIN, y, true, 12.5f, Color.BLACK);
            y = p.paragraph(tool[1], MARGIN + 270, y, PAGE_W - 2 * MARGIN - 270, 11.5f, 15, GREY) - 12;
        }
        y -= 6;
        p.text("Public link", MARGIN, y, true, 12.5f, Color.BLACK);
        p.paragraph("Not applicable: the prototype is an offline desktop application (EducationalLudo.jar + run.bat) "
                + "that is handed in with this document.", MARGIN + 270, y, PAGE_W - 2 * MARGIN - 270, 11.5f, 15, GREY);
        footer(p, pdf.pageCount() + 1);
        pdf.add(p);
    }

    private static void footer(Page p, int pageNumber) {
        p.text("Team Sharks  -  Educational Ludo interactive prototype", MARGIN, 20, false, 9, GREY);
        String n = "Page " + pageNumber;
        p.text(n, PAGE_W - MARGIN - Page.width(n, false, 9), 20, false, 9, GREY);
    }

    private static byte[] jpeg(BufferedImage source) throws IOException {
        BufferedImage rgb = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(0.92f);
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(rgb, null, null), param);
        } finally {
            writer.dispose();
        }
        return out.toByteArray();
    }

    // ------------------------------------------------------------------ minimal PDF writer

    /** Content of one page: text, rectangles and at most a few JPEG images. */
    private static final class Page {
        private static final FontRenderContext FRC = new FontRenderContext(null, true, true);
        private final StringBuilder content = new StringBuilder();
        private final List<byte[]> images = new ArrayList<>();
        private final List<int[]> sizes = new ArrayList<>();

        static float width(String s, boolean bold, float size) {
            Font f = new Font("Arial", bold ? Font.BOLD : Font.PLAIN, 100);
            return (float) (f.getStringBounds(s, FRC).getWidth() * size / 100.0);
        }

        void text(String s, float x, float y, boolean bold, float size, Color c) {
            content.append(String.format(Locale.ROOT, "BT /%s %.2f Tf %s rg %.2f %.2f Td (%s) Tj ET%n",
                    bold ? "F2" : "F1", size, rgb(c), x, y, escape(s)));
        }

        /** Writes wrapped text and returns the baseline below the last line. */
        float paragraph(String s, float x, float y, float width, float size, float leading, Color c) {
            StringBuilder line = new StringBuilder();
            for (String word : s.split(" ")) {
                String candidate = line.length() == 0 ? word : line + " " + word;
                if (width(candidate, false, size) > width && line.length() > 0) {
                    text(line.toString(), x, y, false, size, c);
                    y -= leading;
                    line = new StringBuilder(word);
                } else {
                    line = new StringBuilder(candidate);
                }
            }
            if (line.length() > 0) {
                text(line.toString(), x, y, false, size, c);
                y -= leading;
            }
            return y;
        }

        void rect(float x, float y, float w, float h, Color c) {
            content.append(String.format(Locale.ROOT, "%s rg %.2f %.2f %.2f %.2f re f%n", rgb(c), x, y, w, h));
        }

        void image(byte[] jpeg, int pixelW, int pixelH, float x, float y, float w, float h) {
            content.append(String.format(Locale.ROOT, "q %.2f 0 0 %.2f %.2f %.2f cm /Im%d Do Q%n", w, h, x, y,
                    images.size()));
            images.add(jpeg);
            sizes.add(new int[] {pixelW, pixelH});
        }

        private static String rgb(Color c) {
            return String.format(Locale.ROOT, "%.3f %.3f %.3f", c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f);
        }

        private static String escape(String s) {
            return s.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
        }
    }

    /** Collects objects and writes them with a cross-reference table. */
    private static final class Pdf {
        private static final Charset WIN_ANSI = Charset.forName("windows-1252");
        private final List<byte[]> objects = new ArrayList<>();
        private final List<Integer> pages = new ArrayList<>();

        Pdf() {
            objects.add(null);
            objects.add(null);
            add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>");
            add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>");
        }

        int pageCount() {
            return pages.size();
        }

        private int add(String body) {
            return add(body.getBytes(WIN_ANSI));
        }

        private int add(byte[] body) {
            objects.add(body);
            return objects.size();
        }

        private int stream(String dict, byte[] data) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] head = (dict.replace("%LEN%", String.valueOf(data.length)) + "\nstream\n").getBytes(WIN_ANSI);
            out.write(head, 0, head.length);
            out.write(data, 0, data.length);
            byte[] tail = "\nendstream".getBytes(WIN_ANSI);
            out.write(tail, 0, tail.length);
            return add(out.toByteArray());
        }

        void add(Page page) {
            StringBuilder xobjects = new StringBuilder();
            for (int i = 0; i < page.images.size(); i++) {
                int[] size = page.sizes.get(i);
                int id = stream("<< /Type /XObject /Subtype /Image /Width " + size[0] + " /Height " + size[1]
                        + " /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length %LEN% >>",
                        page.images.get(i));
                xobjects.append("/Im").append(i).append(' ').append(id).append(" 0 R ");
            }
            int contents = stream("<< /Length %LEN% >>", page.content.toString().getBytes(WIN_ANSI));
            int id = add("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + (int) PAGE_W + " " + (int) PAGE_H + "] "
                    + "/Resources << /Font << /F1 3 0 R /F2 4 0 R >> /XObject << " + xobjects + ">> >> "
                    + "/Contents " + contents + " 0 R >>");
            pages.add(id);
        }

        void save(File file) throws IOException {
            StringBuilder kids = new StringBuilder();
            for (int id : pages) {
                kids.append(id).append(" 0 R ");
            }
            objects.set(0, "<< /Type /Catalog /Pages 2 0 R >>".getBytes(WIN_ANSI));
            objects.set(1, ("<< /Type /Pages /Kids [" + kids + "] /Count " + pages.size() + " >>").getBytes(WIN_ANSI));
            try (OutputStream out = new FileOutputStream(file)) {
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                buf.write("%PDF-1.4\n%âãÏÓ\n".getBytes(WIN_ANSI));
                long[] offsets = new long[objects.size()];
                for (int i = 0; i < objects.size(); i++) {
                    offsets[i] = buf.size();
                    buf.write(((i + 1) + " 0 obj\n").getBytes(WIN_ANSI));
                    buf.write(objects.get(i));
                    buf.write("\nendobj\n".getBytes(WIN_ANSI));
                }
                long xref = buf.size();
                StringBuilder table = new StringBuilder();
                table.append("xref\n0 ").append(objects.size() + 1).append("\n0000000000 65535 f \n");
                for (long offset : offsets) {
                    table.append(String.format(Locale.ROOT, "%010d 00000 n \n", offset));
                }
                table.append("trailer\n<< /Size ").append(objects.size() + 1).append(" /Root 1 0 R >>\nstartxref\n")
                        .append(xref).append("\n%%EOF\n");
                buf.write(table.toString().getBytes(WIN_ANSI));
                buf.writeTo(out);
            }
        }
    }
}
