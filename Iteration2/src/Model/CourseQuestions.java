package Model;

import java.util.ArrayList;
import java.util.List;

/**
 * The course questions the game asks: 36 multiple-choice questions, 12 of each level (SRS FR-34).
 * Task T2-08, owner: Mohamed Zereny.
 *
 * <p>These are the questions the team wrote and submitted with the Assignment 3 prototype, in its
 * {@code data} package, carried over unchanged through the reuse gate (SDP 6.1): same texts, same answers,
 * same correct answers, same levels. Only the surrounding code is new. The prototype kept them in a class
 * that also held demo players and a demo history, which this iteration does not use and does not copy, so
 * only the questions themselves came across.
 *
 * <p>The SRS asks for at least 30 questions over the three levels (section 2.5, dependency 2); there are
 * 36 here. Adding, editing and deleting questions is the administrator feature of Iteration 3
 * (FR-56..FR-61) and is not offered yet, so the list is fixed and read-only.
 */
public final class CourseQuestions {

    private CourseQuestions() {
    }

    /**
     * @return a new list of every course question, in a fixed order; the order a game asks them in is
     *         decided by {@link QuestionDeck}
     */
    public static List<Question> all() {
        List<Question> list = new ArrayList<>();
        // Easy (12 questions)
        list.add(new Question("Q-001", Difficulty.EASY,
                "What does the abbreviation SRS stand for?", 0,
                "Software Requirements Specification", "System Release Schedule",
                "Software Review Summary", "Source Revision System"));
        list.add(new Question("Q-002", Difficulty.EASY,
                "Which Scrum role is responsible for maximizing the value of the product "
                        + "and ordering the Product Backlog?", 1,
                "Scrum Master", "Product Owner",
                "Developers", "Project Sponsor"));
        list.add(new Question("Q-003", Difficulty.EASY,
                "In the MVC pattern, which part handles user input and updates the model?", 2,
                "Model", "View",
                "Controller", "Repository"));
        list.add(new Question("Q-004", Difficulty.EASY,
                "What is a unit test?", 1,
                "A test of the whole system by end users", "An automatic test of one small part of the code",
                "A test of the system under heavy load", "A review meeting about the requirements"));
        list.add(new Question("Q-005", Difficulty.EASY,
                "Which UML diagram shows the classes of a system and the relationships between them?", 1,
                "Sequence diagram", "Class diagram",
                "Activity diagram", "Deployment diagram"));
        list.add(new Question("Q-006", Difficulty.EASY,
                "What is the usual length of a Sprint in Scrum?", 1,
                "One day", "One to four weeks",
                "Six months", "One year"));
        list.add(new Question("Q-007", Difficulty.EASY,
                "Which of these is a non-functional requirement?", 2,
                "The player can roll the dice", "The system saves completed games",
                "The main menu appears within 5 seconds", "The administrator can add questions"));
        list.add(new Question("Q-008", Difficulty.EASY,
                "What does QA stand for in software engineering?", 1,
                "Quick Analysis", "Quality Assurance",
                "Query Automation", "Quantitative Architecture"));
        list.add(new Question("Q-009", Difficulty.EASY,
                "What is a defect in the software usually called?", 0,
                "A bug", "A feature",
                "A sprint", "A patch"));
        list.add(new Question("Q-010", Difficulty.EASY,
                "A requirements document mainly describes...", 0,
                "What the system should do", "How the code is written line by line",
                "Which bugs were fixed", "The salaries of the team"));
        list.add(new Question("Q-011", Difficulty.EASY,
                "In a UML use case diagram, an external user of the system is drawn as...", 0,
                "An actor (stick figure)", "A class box",
                "A lifeline", "A swimlane"));
        list.add(new Question("Q-012", Difficulty.EASY,
                "Which practice keeps the code in a shared repository with a full history of changes?", 0,
                "Version control", "Code obfuscation",
                "Load balancing", "Pair programming"));

        // Medium (12 questions)
        list.add(new Question("Q-013", Difficulty.MEDIUM,
                "Which design pattern ensures that a class has only one instance?", 2,
                "Factory Method", "Observer",
                "Singleton", "Adapter"));
        list.add(new Question("Q-014", Difficulty.MEDIUM,
                "Which testing technique designs tests from the specification without looking at the code?", 1,
                "White-box testing", "Black-box testing",
                "Code inspection", "Static analysis"));
        list.add(new Question("Q-015", Difficulty.MEDIUM,
                "What does the Definition of Done describe in Scrum?", 1,
                "The release date of the product", "The conditions an item must meet to be considered complete",
                "The list of team roles", "The budget of the Sprint"));
        list.add(new Question("Q-016", Difficulty.MEDIUM,
                "Which UML diagram best shows the order of messages between objects over time?", 1,
                "Class diagram", "Sequence diagram",
                "Component diagram", "State diagram"));
        list.add(new Question("Q-017", Difficulty.MEDIUM,
                "Why is regression testing performed?", 1,
                "To test new hardware", "To check that changes did not break existing functionality",
                "To measure user satisfaction", "To write the requirements"));
        list.add(new Question("Q-018", Difficulty.MEDIUM,
                "Which design pattern notifies dependent objects automatically when an object changes state?", 0,
                "Observer", "Singleton",
                "Builder", "Facade"));
        list.add(new Question("Q-019", Difficulty.MEDIUM,
                "A name field accepts 1 to 12 characters. How many equivalence classes are there for the length?", 2,
                "1", "2",
                "3", "12"));
        list.add(new Question("Q-020", Difficulty.MEDIUM,
                "What is the main purpose of the Daily Scrum?", 1,
                "To demonstrate the product to customers",
                "To inspect progress toward the Sprint Goal and adapt the plan",
                "To find who caused a delay", "To write documentation"));
        list.add(new Question("Q-021", Difficulty.MEDIUM,
                "According to IEEE 830, a good requirement must be...", 1,
                "Ambiguous, so developers can choose", "Verifiable",
                "Written as source code", "Hidden from the customer"));
        list.add(new Question("Q-022", Difficulty.MEDIUM,
                "Which metric measures the percentage of code executed by the tests?", 1,
                "Cyclomatic complexity", "Code coverage",
                "Velocity", "Lines of code"));
        list.add(new Question("Q-023", Difficulty.MEDIUM,
                "What is technical debt?", 1,
                "Money the team owes to a vendor", "Future rework caused by choosing a quick solution now",
                "The cost of new hardware", "Unpaid overtime of developers"));
        list.add(new Question("Q-024", Difficulty.MEDIUM,
                "Which Scrum event is used to improve the way the team works?", 1,
                "Sprint Review", "Sprint Retrospective",
                "Sprint Planning", "Backlog Refinement"));

        // Hard (12 questions)
        list.add(new Question("Q-025", Difficulty.HARD,
                "Boundary value analysis for a field that accepts 1-12 characters. Which lengths should be tested?", 0,
                "0, 1, 12 and 13", "1, 6 and 12",
                "5, 6 and 7", "-1, 50 and 100"));
        list.add(new Question("Q-026", Difficulty.HARD,
                "Which pattern defines a family of algorithms and makes them interchangeable at runtime?", 0,
                "Strategy", "Template Method",
                "Composite", "Proxy"));
        list.add(new Question("Q-027", Difficulty.HARD,
                "What is the cyclomatic complexity of a function with a single if-else and no loops?", 1,
                "1", "2",
                "3", "4"));
        list.add(new Question("Q-028", Difficulty.HARD,
                "Which principle states that a class should have only one reason to change?", 2,
                "Open/Closed", "Liskov Substitution",
                "Single Responsibility", "Dependency Inversion"));
        list.add(new Question("Q-029", Difficulty.HARD,
                "What is the main purpose of mutation testing?", 0,
                "To evaluate how good a test suite is", "To generate requirements automatically",
                "To measure network latency", "To refactor code automatically"));
        list.add(new Question("Q-030", Difficulty.HARD,
                "In the State design pattern, the behavior of an object changes when...", 0,
                "Its internal state changes", "A new subclass is compiled",
                "The window is resized", "A singleton is created"));
        list.add(new Question("Q-031", Difficulty.HARD,
                "Which UML relationship is a strong whole-part relation where the parts "
                        + "cannot exist without the whole?", 2,
                "Association", "Aggregation",
                "Composition", "Dependency"));
        list.add(new Question("Q-032", Difficulty.HARD,
                "Which of these is NOT one of the three Scrum artifacts?", 3,
                "Product Backlog", "Sprint Backlog",
                "Increment", "Burndown Chart"));
        list.add(new Question("Q-033", Difficulty.HARD,
                "A test that checks the system against the customer's needs before delivery is called...", 2,
                "Unit test", "Integration test",
                "Acceptance test", "Smoke test"));
        list.add(new Question("Q-034", Difficulty.HARD,
                "Salting passwords before hashing mainly protects against...", 0,
                "Precomputed rainbow-table attacks", "Network packet loss",
                "SQL syntax errors", "Memory leaks"));
        list.add(new Question("Q-035", Difficulty.HARD,
                "In the Factory Method pattern, the decision of which class to instantiate is made by...", 0,
                "Subclasses that override the factory method", "A global variable",
                "The garbage collector", "The database"));
        list.add(new Question("Q-036", Difficulty.HARD,
                "Which statement about verification and validation is correct?", 0,
                "Verification: are we building the product right? Validation: are we building the right product?",
                "They mean exactly the same thing",
                "Validation is done only by developers", "Verification happens only after release"));
        return list;
    }
}
