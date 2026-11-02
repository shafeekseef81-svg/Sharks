package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;

import components.CustomButton;
import components.CustomPanel;
import components.GradientBackground;
import components.Icons;
import components.StyledTable;
import components.Theme;
import components.ToggleChip;
import components.UiKit;
import model.Difficulty;
import model.Question;
import model.QuestionBank;

/** Question management (FR-56..FR-62): table, add/edit/delete, difficulty filter, logout. */
public class QuestionManagementView extends GradientBackground implements ScreenView {

    private static final String[] COLUMNS = {"ID", "Question", "Difficulty", "Correct Answer"};

    private final QuestionBank bank;
    private final QuestionModel model = new QuestionModel();
    private final JTable table = new JTable(model);
    private final CustomButton add = new CustomButton("Add Question", Icons.get(Icons.Type.PLUS, 18, Color.WHITE),
            CustomButton.Variant.PRIMARY);
    private final CustomButton edit = new CustomButton("Edit", Icons.get(Icons.Type.EDIT, 17, Color.WHITE),
            CustomButton.Variant.LIGHT);
    private final CustomButton delete = new CustomButton("Delete", Icons.get(Icons.Type.TRASH, 17, Color.WHITE),
            CustomButton.Variant.LIGHT);
    private final CustomButton logout = new CustomButton("Log Out", Icons.get(Icons.Type.EXIT, 18, Color.WHITE),
            CustomButton.Variant.LIGHT);
    private final JLabel stats = UiKit.label(" ", Theme.font(Font.PLAIN, 13.5f), Theme.TEXT_ON_DARK_MUTED);
    private final JLabel message = UiKit.label(" ", Theme.font(Font.BOLD, 14f), new Color(0x6EE7A5));
    private Difficulty filter;

    public QuestionManagementView(QuestionBank bank) {
        super(new BorderLayout(0, 14));
        this.bank = bank;
        setBorder(BorderFactory.createEmptyBorder(22, 40, 20, 40));
        logout.setName("questions.logout");
        logout.setToolTipText("End the administrator session and return to the main menu");
        add(UiKit.header(null, "Question Management",
                "Signed in as administrator. Changes apply from the next game.", logout), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        actions.setOpaque(false);
        add.setName("questions.add");
        edit.setName("questions.edit");
        delete.setName("questions.delete");
        actions.add(add);
        actions.add(UiKit.hgap(10));
        actions.add(edit);
        actions.add(UiKit.hgap(10));
        actions.add(delete);
        actions.add(UiKit.hgap(16));
        message.setName("questions.message");
        actions.add(message);
        toolbar.add(actions, BorderLayout.WEST);
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        filters.setOpaque(false);
        ButtonGroup group = new ButtonGroup();
        addFilter(filters, group, "All", null, true);
        for (Difficulty d : Difficulty.values()) {
            addFilter(filters, group, d.label(), d, false);
        }
        toolbar.add(filters, BorderLayout.EAST);
        center.add(toolbar, BorderLayout.NORTH);

        StyledTable.style(table);
        table.setName("questions.table");
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        int[] widths = {70, 560, 110, 260};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        table.getSelectionModel().addListSelectionListener(e -> updateButtons());
        CustomPanel card = new CustomPanel(new BorderLayout(), CustomPanel.Style.CARD);
        card.setPadding(6);
        card.add(StyledTable.scroll(table), BorderLayout.CENTER);
        center.add(card, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        stats.setName("questions.stats");
        add(stats, BorderLayout.SOUTH);
        updateButtons();
    }

    private void addFilter(JPanel panel, ButtonGroup group, String label, Difficulty level, boolean selected) {
        ToggleChip chip = new ToggleChip(label, true);
        chip.setName("questions.filter." + (level == null ? "all" : level.name().toLowerCase()));
        chip.setSelected(selected);
        if (level != null) {
            chip.setSelectedColor(Theme.level(level));
        }
        chip.addActionListener(e -> setFilter(level));
        group.add(chip);
        panel.add(UiKit.hgap(8));
        panel.add(chip);
    }

    public void setActions(Runnable onAdd, Consumer<Question> onEdit, Consumer<Question> onDelete, Runnable onLogout) {
        add.addActionListener(e -> onAdd.run());
        edit.addActionListener(e -> {
            Question q = selectedQuestion();
            if (q != null) {
                onEdit.accept(q);
            }
        });
        delete.addActionListener(e -> {
            Question q = selectedQuestion();
            if (q != null) {
                onDelete.accept(q);
            }
        });
        logout.addActionListener(e -> onLogout.run());
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && selectedQuestion() != null) {
                    onEdit.accept(selectedQuestion());
                }
            }
        });
    }

    public void setFilter(Difficulty level) {
        filter = level;
        refresh();
    }

    /** Reloads the table from the bank (FR-57). */
    public void refresh() {
        Question selected = selectedQuestion();
        model.setRows(filter == null ? bank.all() : bank.byLevel(filter));
        if (selected != null) {
            select(selected);
        }
        stats.setText(bank.size() + " questions in the bank  •  Easy " + bank.count(Difficulty.EASY)
                + "  •  Medium " + bank.count(Difficulty.MEDIUM) + "  •  Hard " + bank.count(Difficulty.HARD)
                + "  •  at least " + QuestionBank.MINIMUM_QUESTIONS + " must remain  •  double-click a row to edit");
        updateButtons();
    }

    public void select(Question q) {
        int row = model.rows.indexOf(q);
        if (row >= 0) {
            table.setRowSelectionInterval(row, row);
            table.scrollRectToVisible(table.getCellRect(row, 0, true));
        }
    }

    public Question selectedQuestion() {
        int row = table.getSelectedRow();
        return row >= 0 && row < model.rows.size() ? model.rows.get(row) : null;
    }

    /** Short confirmation text next to the toolbar, e.g. "Question Q-037 added". */
    public void showMessage(String text) {
        message.setText(text);
    }

    public JTable table() {
        return table;
    }

    private void updateButtons() {
        boolean has = selectedQuestion() != null;
        edit.setEnabled(has);
        delete.setEnabled(has);
    }

    @Override
    public void onShow() {
        message.setText(" ");
        refresh();
    }

    private static final class QuestionModel extends AbstractTableModel {
        private List<Question> rows = List.of();

        void setRows(List<Question> rows) {
            this.rows = List.copyOf(rows);
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMNS[column];
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return column == 2 ? Difficulty.class : Object.class;
        }

        @Override
        public Object getValueAt(int row, int column) {
            Question q = rows.get(row);
            return switch (column) {
                case 0 -> q.id();
                case 1 -> q.text();
                case 2 -> q.level();
                default -> (char) ('A' + q.correctIndex()) + ". " + q.correctAnswer();
            };
        }
    }
}
