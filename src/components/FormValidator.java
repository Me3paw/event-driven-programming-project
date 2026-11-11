package components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.math.BigDecimal;
import javax.swing.BorderFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.text.JTextComponent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Tạo form có nhãn lỗi màu đỏ ngay dưới từng ô nhập và chặn đóng dialog khi dữ liệu
 * chưa hợp lệ. Lỗi hiển thị khi người dùng rời khỏi ô nhập; khi bấm nút xác nhận,
 * validate() đánh dấu tất cả ô là "đã chạm" và đưa con trỏ về ô sai đầu tiên.
 */
public final class FormValidator {
    private static final Color ERROR_COLOR = new Color(200, 30, 30);
    private final List<Entry> entries = new ArrayList<Entry>();
    private final Map<JComponent, JLabel> labels = new HashMap<JComponent, JLabel>();

    private interface Rule {
        String error();
    }

    private static final class Entry {
        final JComponent field;
        final JLabel error;
        final List<Rule> rules = new ArrayList<Rule>();
        final javax.swing.border.Border originalBorder;
        boolean touched;
        boolean valid = true;

        Entry(JComponent field, JLabel error) {
            this.field = field;
            this.error = error;
            this.originalBorder = field.getBorder();
        }
    }

    /** JPanel dọc: mỗi dòng giữ chiều cao tự nhiên và rộng hết khung (thay GridLayout). */
    public static JPanel form() {
        JPanel panel = new JPanel(new GridBagLayout()) {
            private static final long serialVersionUID = 1L;
            private int row;

            @Override
            public Component add(Component component) {
                GridBagConstraints constraints = new GridBagConstraints();
                constraints.gridx = 0;
                constraints.gridy = row++;
                constraints.weightx = 1.0;
                constraints.fill = GridBagConstraints.HORIZONTAL;
                constraints.insets = new Insets(0, 0, 4, 0);
                add(component, constraints);
                return component;
            }
        };
        panel.setOpaque(false);
        return panel;
    }

    /** Bọc nhãn + ô nhập + dòng lỗi thành một dòng form. */
    public JComponent row(String label, JComponent field) {
        return row(label, field, new JComponent[0]);
    }

    /** Như row() nhưng gắn thêm nhãn lỗi cho các component phụ trong cùng dòng. */
    public JComponent row(String label, JComponent field, JComponent... alsoFields) {
        JLabel caption = new JLabel(label);
        caption.setLabelFor(field);
        field.getAccessibleContext().setAccessibleName(label);
        JLabel error = errorLabel();
        labels.put(field, error);
        for (JComponent extra : alsoFields)
            labels.put(extra, error);
        JPanel wrap = new JPanel(new BorderLayout(0, 1));
        wrap.setOpaque(false);
        wrap.add(caption, BorderLayout.NORTH);
        wrap.add(field, BorderLayout.CENTER);
        wrap.add(error, BorderLayout.SOUTH);
        return wrap;
    }

    private JLabel errorLabel() {
        JLabel error = new JLabel(" ");
        error.setForeground(ERROR_COLOR);
        error.setFont(error.getFont().deriveFont(Font.PLAIN, 11f));
        return error;
    }

    /** Ô bắt buộc không được để trống. */
    public void require(JTextComponent field, String message) {
        addRule(field, () -> field.getText().trim().isEmpty() ? message : null);
    }

    /** Khớp regex; ô trống được coi là hợp lệ (dùng kèm require nếu bắt buộc). */
    public void matches(JTextComponent field, String regex, String message) {
        addRule(field, () -> {
            String text = field.getText().trim();
            return text.isEmpty() || text.matches(regex) ? null : message;
        });
    }

    /** Giá trị phải parse được thành số. */
    public void number(JTextComponent field, String message) {
        addRule(field, () -> {
            String text = field.getText().trim();
            if (text.isEmpty())
                return null;
            try {
                new BigDecimal(text);
                return null;
            } catch (NumberFormatException exception) {
                return message;
            }
        });
    }

    /** Điều kiện tùy ý trên nội dung ô (trim). Trả true = hợp lệ. */
    public void check(JTextComponent field, Predicate<String> rule, String message) {
        addRule(field, () -> rule.test(field.getText().trim()) ? null : message);
    }

    /** Điều kiện tùy ý trên component bất kỳ (ví dụ JComboBox, panel ngày giờ). Trả null = hợp lệ. */
    public void custom(JComponent field, Supplier<String> error) {
        Entry entry = entry(field);
        entry.rules.add(error::get);
        if (field instanceof JComboBox)
            ((JComboBox<?>) field).addItemListener(event -> {
                entry.touched = true;
                refresh(entry);
            });
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent event) {
                entry.touched = true;
                refresh(entry);
            }
        });
    }

    /** Kiểm tra toàn bộ; tô đỏ ô sai và focus ô sai đầu tiên. Trả true nếu hợp lệ. */
    public boolean validate() {
        boolean allValid = true;
        Entry firstInvalid = null;
        for (Entry entry : entries) {
            entry.touched = true;
            refresh(entry);
            if (!entry.valid) {
                allValid = false;
                if (firstInvalid == null)
                    firstInvalid = entry;
            }
        }
        if (firstInvalid != null)
            firstInvalid.field.requestFocusInWindow();
        return allValid;
    }

    private void addRule(JTextComponent field, Rule rule) {
        Entry entry = entry(field);
        entry.rules.add(rule);
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                changed();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                changed();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                changed();
            }

            private void changed() {
                if (entry.touched)
                    refresh(entry);
            }
        });
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent event) {
                entry.touched = true;
                refresh(entry);
            }
        });
    }

    private Entry entry(JComponent field) {
        for (Entry entry : entries)
            if (entry.field == field)
                return entry;
        JLabel error = labels.get(field);
        if (error == null)
            error = errorLabel();
        Entry entry = new Entry(field, error);
        entries.add(entry);
        return entry;
    }

    private void refresh(Entry entry) {
        String message = null;
        for (Rule rule : entry.rules) {
            message = rule.error();
            if (message != null)
                break;
        }
        entry.valid = message == null;
        boolean show = entry.touched && message != null;
        entry.error.setText(show ? "<html>" + escape(message) + "</html>" : " ");
        entry.error.setToolTipText(show ? message : null);
        if (show)
            entry.field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ERROR_COLOR, 1),
                    BorderFactory.createEmptyBorder(3, 6, 3, 6)));
        else
            entry.field.setBorder(entry.originalBorder);
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
