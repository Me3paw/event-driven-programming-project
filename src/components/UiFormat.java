package components;

import java.math.BigDecimal;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

public final class UiFormat {
    private UiFormat() {
    }

    public static String money(BigDecimal value) {
        if (value == null)
            return "0";
        String plain = value.stripTrailingZeros().toPlainString();
        int dot = plain.indexOf('.');
        String integer = dot < 0 ? plain : plain.substring(0, dot);
        String fraction = dot < 0 ? "" : plain.substring(dot + 1);
        StringBuilder grouped = new StringBuilder();
        int count = 0;
        for (int i = integer.length() - 1; i >= 0; i--) {
            char c = integer.charAt(i);
            if (c == '-')
                continue;
            if (count == 3) {
                grouped.insert(0, '.');
                count = 0;
            }
            grouped.insert(0, c);
            count++;
        }
        if (integer.startsWith("-"))
            grouped.insert(0, '-');
        if (fraction.length() > 0)
            grouped.append(',').append(fraction);
        return grouped.toString();
    }

    public static String money(long value) {
        return money(BigDecimal.valueOf(value));
    }

    public static String money(Object value) {
        if (value instanceof BigDecimal)
            return money((BigDecimal) value);
        if (value instanceof Number)
            return money(BigDecimal.valueOf(((Number) value).longValue()));
        return value == null ? "" : String.valueOf(value);
    }

    public static TableCellRenderer moneyRenderer() {
        return new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;

            {
                setHorizontalAlignment(RIGHT);
            }

            @Override
            public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, Object value,
                    boolean selected, boolean focus, int row, int column) {
                super.getTableCellRendererComponent(table,
                        value instanceof Number || value instanceof BigDecimal ? money(value) : value,
                        selected, focus, row, column);
                return this;
            }
        };
    }

    public static String joinIdName(String id, String name) {
        return name == null || name.trim().isEmpty() ? id : id + " - " + name;
    }

    public static String idOf(String text) {
        if (text == null)
            return "";
        int dash = text.indexOf(" - ");
        return (dash < 0 ? text : text.substring(0, dash)).trim();
    }
}
