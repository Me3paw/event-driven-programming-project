package graphicUI;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public final class DateField extends JPanel {
    private static final long serialVersionUID = 1L;
    private final JTextField text = new JTextField(10);

    public DateField() {
        super(new BorderLayout(0, 0));
        JButton pick = new JButton(new CalendarIcon());
        pick.setToolTipText("Chọn ngày từ lịch");
        pick.setFocusPainted(false);
        pick.addActionListener(event -> {
            Window owner = SwingUtilities.getWindowAncestor(this);
            CalendarDialog dialog = new CalendarDialog(owner, getDate(), date -> setText(date.toString()));
            dialog.setVisible(true);
        });
        add(text, BorderLayout.CENTER);
        add(pick, BorderLayout.EAST);
    }

    public String getText() {
        return text.getText();
    }

    public void setText(String value) {
        text.setText(value == null ? "" : value);
    }

    public LocalDate getDate() {
        String value = text.getText().trim();
        if (value.isEmpty())
            return null;
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    public void setDate(LocalDate date) {
        setText(date == null ? "" : date.toString());
    }

    private static final class CalendarIcon implements Icon {
        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(0x1F, 0x3A, 0x5F));
            g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawRoundRect(x + 2, y + 3, 12, 11, 2, 2);
            g.drawLine(x + 2, y + 6, x + 14, y + 6);
            g.drawLine(x + 5, y + 1, x + 5, y + 4);
            g.drawLine(x + 11, y + 1, x + 11, y + 4);
            g.setColor(new Color(0x2F, 0x6F, 0xB3));
            g.fillOval(x + 5, y + 8, 2, 2);
            g.fillOval(x + 9, y + 8, 2, 2);
            g.fillOval(x + 5, y + 11, 2, 2);
            g.fillOval(x + 9, y + 11, 2, 2);
            g.dispose();
        }

        @Override
        public int getIconWidth() {
            return 16;
        }

        @Override
        public int getIconHeight() {
            return 16;
        }
    }
}
