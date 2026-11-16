package View.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

import Model.Difficulty;

/** Consistent table styling: roomy rows, soft header, zebra stripes and difficulty "pills". */
public final class StyledTable {

    private static final Color STRIPE = new Color(0xF8F9FC);
    private static final Color SELECTION = new Color(0xE2EAFF);

    private StyledTable() {
    }

    public static void style(JTable table) {
        table.setFont(Theme.font(Font.PLAIN, 14f));
        table.setRowHeight(38);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(new Color(0xE9ECF3));
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setSelectionBackground(SELECTION);
        table.setSelectionForeground(Theme.TEXT);
        table.setFillsViewportHeight(true);
        table.setBackground(Color.WHITE);
        table.setForeground(Theme.TEXT);
        table.setDefaultRenderer(Object.class, new TextRenderer());
        table.setDefaultRenderer(Difficulty.class, new PillRenderer());
        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new HeaderRenderer());
        header.setPreferredSize(new Dimension(0, 40));
        header.setBackground(Theme.SURFACE_ALT);
    }

    public static JScrollPane scroll(JTable table) {
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(Color.WHITE);
        sp.setOpaque(false);
        return sp;
    }

    private static final class HeaderRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, false, false, row, column);
            l.setFont(Theme.font(Font.BOLD, 13f));
            l.setForeground(Theme.TEXT_MUTED);
            l.setBackground(Theme.SURFACE_ALT);
            l.setOpaque(true);
            l.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xDCE1EC)),
                    BorderFactory.createEmptyBorder(0, 12, 0, 8)));
            return l;
        }
    }

    /** Plain text cell with padding; empty values are shown as a light dash. */
    public static class TextRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            String text = value == null ? "" : value.toString();
            boolean empty = text.isEmpty();
            JLabel l = (JLabel) super.getTableCellRendererComponent(table, empty ? "–" : text, isSelected,
                    false, row, column);
            l.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 8));
            l.setForeground(empty ? new Color(0xB3BACB) : Theme.TEXT);
            l.setBackground(isSelected ? SELECTION : row % 2 == 1 ? STRIPE : Color.WHITE);
            l.setToolTipText(empty || text.length() < 40 ? null : text);
            return l;
        }
    }

    /** Coloured pill for a difficulty value. */
    public static class PillRenderer extends DefaultTableCellRenderer {
        private Difficulty level;
        private boolean selected;
        private int row;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, "", isSelected, false, row, column);
            this.level = value instanceof Difficulty d ? d : null;
            this.selected = isSelected;
            this.row = row;
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            g2.setColor(selected ? SELECTION : row % 2 == 1 ? STRIPE : Color.WHITE);
            g2.fillRect(0, 0, getWidth(), getHeight());
            if (level != null) {
                g2.setFont(Theme.font(Font.BOLD, 12.5f));
                FontMetrics fm = g2.getFontMetrics();
                String text = level.label();
                int w = fm.stringWidth(text) + 22;
                int h = 24;
                int x = 12;
                int y = (getHeight() - h) / 2;
                g2.setColor(Theme.levelLight(level));
                g2.fill(new RoundRectangle2D.Double(x, y, w, h, h, h));
                g2.setColor(Theme.mix(Theme.level(level), Color.BLACK, 0.2));
                g2.drawString(text, x + 11, y + (h + fm.getAscent() - fm.getDescent()) / 2f);
            }
            g2.dispose();
        }
    }
}
