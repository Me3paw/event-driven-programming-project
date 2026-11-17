package graphicUI;

import java.awt.FlowLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public final class DateTimeField extends JPanel {
    private static final long serialVersionUID = 1L;
    private final DateField date = new DateField();
    private final JTextField time = new JTextField("00:00", 5);

    public DateTimeField() {
        super(new FlowLayout(FlowLayout.LEFT, 4, 0));
        add(date);
        add(new JLabel("lúc"));
        add(time);
    }

    public String getText() {
        String day = date.getText().trim();
        String clock = time.getText().trim();
        if (day.isEmpty() && clock.isEmpty())
            return "";
        return day + "T" + clock;
    }

    public void setText(String value) {
        if (value == null || value.trim().isEmpty()) {
            date.setText("");
            time.setText("");
            return;
        }
        String trimmed = value.trim();
        int split = trimmed.indexOf('T');
        if (split < 0) {
            date.setText(trimmed);
            time.setText("");
            return;
        }
        date.setText(trimmed.substring(0, split));
        String clock = trimmed.substring(split + 1);
        time.setText(clock.length() > 5 ? clock.substring(0, 5) : clock);
    }
}
