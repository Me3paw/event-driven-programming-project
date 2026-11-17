package graphicUI;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.time.LocalDate;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

final class CalendarDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    private static final String[] WEEKDAYS = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};

    interface Callback {
        void picked(LocalDate date);
    }

    private final Callback callback;
    private final JLabel title = new JLabel("", SwingConstants.CENTER);
    private final JPanel grid = new JPanel(new GridLayout(7, 7, 2, 2));
    private LocalDate selected;
    private int year;
    private int month;

    CalendarDialog(Window owner, LocalDate initial, Callback callback) {
        super(owner, "Chọn ngày", ModalityType.APPLICATION_MODAL);
        this.callback = callback;
        LocalDate base = initial == null ? LocalDate.now() : initial;
        year = base.getYear();
        month = base.getMonthValue();
        selected = base;

        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel header = new JPanel(new BorderLayout());
        JButton prev = new JButton("<");
        JButton next = new JButton(">");
        prev.addActionListener(event -> shift(-1));
        next.addActionListener(event -> shift(1));
        header.add(prev, BorderLayout.WEST);
        header.add(title, BorderLayout.CENTER);
        header.add(next, BorderLayout.EAST);
        content.add(header, BorderLayout.NORTH);

        JPanel weekdays = new JPanel(new GridLayout(1, 7, 2, 2));
        for (String day : WEEKDAYS) {
            JLabel label = new JLabel(day, SwingConstants.CENTER);
            label.setForeground(Color.GRAY);
            weekdays.add(label);
        }
        JPanel center = new JPanel(new BorderLayout(0, 4));
        center.add(weekdays, BorderLayout.NORTH);
        center.add(grid, BorderLayout.CENTER);
        content.add(center, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancel = new JButton("Hủy");
        JButton ok = new JButton("OK");
        cancel.addActionListener(event -> dispose());
        ok.addActionListener(event -> {
            if (selected != null) {
                callback.picked(selected);
                dispose();
            }
        });
        footer.add(cancel);
        footer.add(ok);
        content.add(footer, BorderLayout.SOUTH);

        setContentPane(content);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getRootPane().setDefaultButton(ok);
        content.setPreferredSize(new Dimension(320, 340));
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
        refresh();
    }

    private void shift(int months) {
        LocalDate base = LocalDate.of(year, month, 1).plusMonths(months);
        year = base.getYear();
        month = base.getMonthValue();
        refresh();
    }

    private void refresh() {
        title.setText("Tháng " + month + " / " + year);
        grid.removeAll();
        LocalDate first = LocalDate.of(year, month, 1);
        int offset = first.getDayOfWeek().getValue() - 1;
        int days = first.lengthOfMonth();
        for (int i = 0; i < offset; i++)
            grid.add(new JLabel());
        for (int day = 1; day <= days; day++) {
            final LocalDate date = LocalDate.of(year, month, day);
            JButton button = new JButton(String.valueOf(day));
            button.setMargin(new java.awt.Insets(2, 2, 2, 2));
            button.setPreferredSize(new Dimension(34, 26));
            if (date.equals(selected))
                button.setBackground(new Color(70, 130, 200));
            button.addActionListener(event -> {
                selected = date;
                refresh();
            });
            grid.add(button);
        }
        grid.revalidate();
        grid.repaint();
        pack();
    }
}
