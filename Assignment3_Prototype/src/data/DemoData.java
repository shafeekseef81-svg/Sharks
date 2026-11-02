package data;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import model.Difficulty;
import model.GameRecord;
import model.Question;

/** Sample data for the prototype: question bank, game history and the demo administrator account. */
public final class DemoData {

    /** Demo administrator account (simulated login, shown on the login screen of the prototype). */
    public static final String ADMIN_USERNAME = "admin";
    public static final String ADMIN_PASSWORD = "Sharks2026";

    private DemoData() {
    }

    /** 36 course questions, 12 per level, so deleting can be shown before the 30-question minimum. */
    public static List<Question> questions() {
        List<Question> list = new ArrayList<>();
        // Easy
        add(list, Difficulty.EASY, "What does the abbreviation SRS stand for?", 0,
                "Software Requirements Specification", "System Release Schedule",
                "Software Review Summary", "Source Revision System");
        add(list, Difficulty.EASY, "Which Scrum role is responsible for maximizing the value of the product and ordering the Product Backlog?", 1,
                "Scrum Master", "Product Owner", "Developers", "Project Sponsor");
        add(list, Difficulty.EASY, "In the MVC pattern, which part handles user input and updates the model?", 2,
                "Model", "View", "Controller", "Repository");
        add(list, Difficulty.EASY, "What is a unit test?", 1,
                "A test of the whole system by end users", "An automatic test of one small part of the code",
                "A test of the system under heavy load", "A review meeting about the requirements");
        add(list, Difficulty.EASY, "Which UML diagram shows the classes of a system and the relationships between them?", 1,
                "Sequence diagram", "Class diagram", "Activity diagram", "Deployment diagram");
        add(list, Difficulty.EASY, "What is the usual length of a Sprint in Scrum?", 1,
                "One day", "One to four weeks", "Six months", "One year");
        add(list, Difficulty.EASY, "Which of these is a non-functional requirement?", 2,
                "The player can roll the dice", "The system saves completed games",
                "The main menu appears within 5 seconds", "The administrator can add questions");
        add(list, Difficulty.EASY, "What does QA stand for in software engineering?", 1,
                "Quick Analysis", "Quality Assurance", "Query Automation", "Quantitative Architecture");
        add(list, Difficulty.EASY, "What is a defect in the software usually called?", 0,
                "A bug", "A feature", "A sprint", "A patch");
        add(list, Difficulty.EASY, "A requirements document mainly describes...", 0,
                "What the system should do", "How the code is written line by line",
                "Which bugs were fixed", "The salaries of the team");
        add(list, Difficulty.EASY, "In a UML use case diagram, an external user of the system is drawn as...", 0,
                "An actor (stick figure)", "A class box", "A lifeline", "A swimlane");
        add(list, Difficulty.EASY, "Which practice keeps the code in a shared repository with a full history of changes?", 0,
                "Version control", "Code obfuscation", "Load balancing", "Pair programming");
        // Medium
        add(list, Difficulty.MEDIUM, "Which design pattern ensures that a class has only one instance?", 2,
                "Factory Method", "Observer", "Singleton", "Adapter");
        add(list, Difficulty.MEDIUM, "Which testing technique designs tests from the specification without looking at the code?", 1,
                "White-box testing", "Black-box testing", "Code inspection", "Static analysis");
        add(list, Difficulty.MEDIUM, "What does the Definition of Done describe in Scrum?", 1,
                "The release date of the product", "The conditions an item must meet to be considered complete",
                "The list of team roles", "The budget of the Sprint");
        add(list, Difficulty.MEDIUM, "Which UML diagram best shows the order of messages between objects over time?", 1,
                "Class diagram", "Sequence diagram", "Component diagram", "State diagram");
        add(list, Difficulty.MEDIUM, "Why is regression testing performed?", 1,
                "To test new hardware", "To check that changes did not break existing functionality",
                "To measure user satisfaction", "To write the requirements");
        add(list, Difficulty.MEDIUM, "Which design pattern notifies dependent objects automatically when an object changes state?", 0,
                "Observer", "Singleton", "Builder", "Facade");
        add(list, Difficulty.MEDIUM, "A name field accepts 1 to 12 characters. How many equivalence classes are there for the length?", 2,
                "1", "2", "3", "12");
        add(list, Difficulty.MEDIUM, "What is the main purpose of the Daily Scrum?", 1,
                "To demonstrate the product to customers", "To inspect progress toward the Sprint Goal and adapt the plan",
                "To find who caused a delay", "To write documentation");
        add(list, Difficulty.MEDIUM, "According to IEEE 830, a good requirement must be...", 1,
                "Ambiguous, so developers can choose", "Verifiable", "Written as source code", "Hidden from the customer");
        add(list, Difficulty.MEDIUM, "Which metric measures the percentage of code executed by the tests?", 1,
                "Cyclomatic complexity", "Code coverage", "Velocity", "Lines of code");
        add(list, Difficulty.MEDIUM, "What is technical debt?", 1,
                "Money the team owes to a vendor", "Future rework caused by choosing a quick solution now",
                "The cost of new hardware", "Unpaid overtime of developers");
        add(list, Difficulty.MEDIUM, "Which Scrum event is used to improve the way the team works?", 1,
                "Sprint Review", "Sprint Retrospective", "Sprint Planning", "Backlog Refinement");
        // Hard
        add(list, Difficulty.HARD, "Boundary value analysis for a field that accepts 1-12 characters. Which lengths should be tested?", 0,
                "0, 1, 12 and 13", "1, 6 and 12", "5, 6 and 7", "-1, 50 and 100");
        add(list, Difficulty.HARD, "Which pattern defines a family of algorithms and makes them interchangeable at runtime?", 0,
                "Strategy", "Template Method", "Composite", "Proxy");
        add(list, Difficulty.HARD, "What is the cyclomatic complexity of a function with a single if-else and no loops?", 1,
                "1", "2", "3", "4");
        add(list, Difficulty.HARD, "Which principle states that a class should have only one reason to change?", 2,
                "Open/Closed", "Liskov Substitution", "Single Responsibility", "Dependency Inversion");
        add(list, Difficulty.HARD, "What is the main purpose of mutation testing?", 0,
                "To evaluate how good a test suite is", "To generate requirements automatically",
                "To measure network latency", "To refactor code automatically");
        add(list, Difficulty.HARD, "In the State design pattern, the behavior of an object changes when...", 0,
                "Its internal state changes", "A new subclass is compiled", "The window is resized", "A singleton is created");
        add(list, Difficulty.HARD, "Which UML relationship is a strong whole-part relation where the parts cannot exist without the whole?", 2,
                "Association", "Aggregation", "Composition", "Dependency");
        add(list, Difficulty.HARD, "Which of these is NOT one of the three Scrum artifacts?", 3,
                "Product Backlog", "Sprint Backlog", "Increment", "Burndown Chart");
        add(list, Difficulty.HARD, "A test that checks the system against the customer's needs before delivery is called...", 2,
                "Unit test", "Integration test", "Acceptance test", "Smoke test");
        add(list, Difficulty.HARD, "Salting passwords before hashing mainly protects against...", 0,
                "Precomputed rainbow-table attacks", "Network packet loss", "SQL syntax errors", "Memory leaks");
        add(list, Difficulty.HARD, "In the Factory Method pattern, the decision of which class to instantiate is made by...", 0,
                "Subclasses that override the factory method", "A global variable",
                "The garbage collector", "The database");
        add(list, Difficulty.HARD, "Which statement about verification and validation is correct?", 0,
                "Verification: are we building the product right? Validation: are we building the right product?",
                "They mean exactly the same thing", "Validation is done only by developers",
                "Verification happens only after release");
        return list;
    }

    private static void add(List<Question> list, Difficulty level, String text, int correct, String... answers) {
        list.add(new Question(String.format("Q-%03d", list.size() + 1), level, text, answers, correct));
    }

    /** Sample completed games following the history CSV template (newest is shown first). */
    public static List<GameRecord> history() {
        List<GameRecord> list = new ArrayList<>();
        rec(list, 1001, Difficulty.EASY, "10/10/2026 15:40:05", "00:28:36", "Shira", "Adam", "Tal", "Rana");
        rec(list, 1002, Difficulty.MEDIUM, "12/10/2026 11:12:39", "00:24:19", "Omer", "Nour");
        rec(list, 1003, Difficulty.HARD, "14/10/2026 20:30:12", "00:58:04", "Lina", "Yosef", "Maya", "Karim");
        rec(list, 1004, Difficulty.EASY, "16/10/2026 13:58:47", "00:21:30", "Amit", "Itay", "Noa");
        rec(list, 1005, Difficulty.MEDIUM, "18/10/2026 17:20:03", "00:38:55", "Eitan", "Dana", "Rami", "Yara");
        rec(list, 1006, Difficulty.HARD, "19/10/2026 19:44:26", "00:41:16", "Nour", "Lior");
        rec(list, 1007, Difficulty.EASY, "20/10/2026 09:05:18", "00:15:41", "Tal", "Shira");
        rec(list, 1008, Difficulty.MEDIUM, "21/10/2026 14:36:57", "00:35:02", "Maya", "Karim", "Adam");
        rec(list, 1009, Difficulty.HARD, "22/10/2026 21:11:40", "01:04:29", "Yosef", "Amit", "Rana", "Omer");
        rec(list, 1010, Difficulty.EASY, "23/10/2026 10:48:22", "00:26:13", "Rami", "Noa", "Eitan", "Lina");
        rec(list, 1011, Difficulty.MEDIUM, "24/10/2026 16:27:09", "00:22:54", "Dana", "Itay");
        rec(list, 1012, Difficulty.HARD, "25/10/2026 20:03:51", "00:52:38", "Karim", "Shira", "Tal");
        rec(list, 1013, Difficulty.EASY, "26/10/2026 12:15:33", "00:17:05", "Lior", "Maya");
        rec(list, 1014, Difficulty.MEDIUM, "27/10/2026 18:42:10", "00:31:47", "Noa", "Omer", "Yara", "Adam");
        return list;
    }

    private static void rec(List<GameRecord> list, int id, Difficulty d, String when, String duration, String... names) {
        String[] dt = when.split("[/ :]");
        LocalDateTime ts = LocalDateTime.of(Integer.parseInt(dt[2]), Integer.parseInt(dt[1]), Integer.parseInt(dt[0]),
                Integer.parseInt(dt[3]), Integer.parseInt(dt[4]), Integer.parseInt(dt[5]));
        String[] hms = duration.split(":");
        Duration dur = Duration.ofHours(Long.parseLong(hms[0])).plusMinutes(Long.parseLong(hms[1]))
                .plusSeconds(Long.parseLong(hms[2]));
        list.add(new GameRecord(id, d, ts, Arrays.asList(names), dur));
    }
}
