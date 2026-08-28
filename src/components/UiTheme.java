package components;

import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.border.Border;

public final class UiTheme {
    public static final Color NAVY = new Color(15, 23, 42);
    public static final Color BLUE = new Color(37, 99, 235);
    public static final Color BACKGROUND = new Color(248, 250, 252);
    public static final Color MUTED = new Color(100, 116, 139);
    public static final Color BORDER = new Color(226, 232, 240);
    public static final Border CARD = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER), BorderFactory.createEmptyBorder(16, 16, 16, 16));

    private UiTheme() {
    }

    public static void title(JComponent component) {
        component.setFont(component.getFont().deriveFont(Font.BOLD, 24f));
    }
}
