package view;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
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
import model.GameHistory;
import model.GameRecord;

/** Game history (FR-49, FR-50, FR-63): completed games, newest first, with a difficulty filter. */
public class GameHistoryView extends GradientBackground implements ScreenView {

    private static final String[] COLUMNS = {"Game ID", "Difficulty", "Date & Time", "1st Place", "2nd Place",
            "3rd Place", "4th Place", "Duration"};

    private final GameHistory history;
    private final RecordModel model = new RecordModel();
    private final JTable table = new JTable(model);
    private final JLabel count = UiKit.label(" ", Theme.font(Font.BOLD, 14f), Theme.TEXT_ON_DARK_MUTED);
    private final CardLayout cards = new CardLayout();
    private final JPanel tableArea = new JPanel(cards);
    private final CustomButton back = new CustomButton("Main Menu", Icons.get(Icons.Type.BACK, 18, Color.WHITE),
            CustomButton.Variant.LIGHT);
    private Difficulty filter;

    public GameHistoryView(GameHistory history) {
        super(new BorderLayout(0, 16));
        this.history = history;
        setBorder(BorderFactory.createEmptyBorder(22, 40, 20, 40));
        back.setName("history.back");
        add(UiKit.header(back, "Game History", "Completed games, newest first", null), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        filters.setOpaque(false);
        filters.add(UiKit.label("Filter by difficulty:  ", Theme.font(Font.BOLD, 14f), Color.WHITE));
        ButtonGroup group = new ButtonGroup();
        addFilter(filters, group, "All", null, true);
        for (Difficulty d : Difficulty.values()) {
            addFilter(filters, group, d.label(), d, false);
        }
        toolbar.add(filters, BorderLayout.WEST);
        count.setName("history.count");
        toolbar.add(count, BorderLayout.EAST);
        center.add(toolbar, BorderLayout.NORTH);

        StyledTable.style(table);
        table.setName("history.table");
        table.setRowSelectionAllowed(true);
        int[] widths = {80, 110, 175, 120, 120, 120, 120, 95};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        CustomPanel card = new CustomPanel(new BorderLayout(), CustomPanel.Style.CARD);
        card.setPadding(6);
        tableArea.setOpaque(false);
        tableArea.add(StyledTable.scroll(table), "table");
        JPanel empty = new JPanel(new GridBagLayout());
        empty.setOpaque(false);
        JPanel emptyText = new JPanel();
        emptyText.setOpaque(false);
        emptyText.setLayout(new BoxLayout(emptyText, BoxLayout.Y_AXIS));
        emptyText.add(new JLabel(Icons.get(Icons.Type.HISTORY, 40, new Color(0xA8B0C6))));
        emptyText.add(UiKit.vgap(8));
        emptyText.add(UiKit.label("No completed games for this difficulty yet.", Theme.font(Font.BOLD, 16f), Theme.TEXT_MUTED));
        empty.add(emptyText);
        tableArea.add(empty, "empty");
        card.add(tableArea, BorderLayout.CENTER);
        center.add(card, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        add(UiKit.label("Prototype: sample records. Columns follow the history CSV template "
                        + "(gameID, difficulty, timestamp, 1st-4th place, game_duration).",
                Theme.font(Font.PLAIN, 13f), Theme.TEXT_ON_DARK_MUTED), BorderLayout.SOUTH);
    }

    private void addFilter(JPanel panel, ButtonGroup group, String label, Difficulty level, boolean selected) {
        ToggleChip chip = new ToggleChip(label, true);
        chip.setName("history.filter." + (level == null ? "all" : level.name().toLowerCase()));
        chip.setSelected(selected);
        if (level != null) {
            chip.setSelectedColor(Theme.level(level));
        }
        chip.addActionListener(e -> setFilter(level));
        group.add(chip);
        panel.add(chip);
        panel.add(UiKit.hgap(8));
    }

    public void setBackAction(Runnable onBack) {
        back.addActionListener(e -> onBack.run());
    }

    /** Shows only one level (null = all levels). */
    public void setFilter(Difficulty level) {
        filter = level;
        refresh();
    }

    public void refresh() {
        List<GameRecord> rows = history.newestFirst(filter);
        model.setRows(rows);
        count.setText(rows.size() + (rows.size() == 1 ? " game" : " games"));
        cards.show(tableArea, rows.isEmpty() ? "empty" : "table");
    }

    public JTable table() {
        return table;
    }

    @Override
    public void onShow() {
        refresh();
    }

    private static final class RecordModel extends AbstractTableModel {
        private List<GameRecord> rows = List.of();

        void setRows(List<GameRecord> rows) {
            this.rows = rows;
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
            return column == 1 ? Difficulty.class : Object.class;
        }

        @Override
        public Object getValueAt(int row, int column) {
            GameRecord r = rows.get(row);
            return switch (column) {
                case 0 -> String.valueOf(r.gameId());
                case 1 -> r.difficulty();
                case 2 -> r.timestampText();
                case 3, 4, 5, 6 -> r.place(column - 3);
                default -> r.durationText();
            };
        }
    }
}
