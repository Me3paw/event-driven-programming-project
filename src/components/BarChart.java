package components;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import javax.swing.JPanel;

public final class BarChart extends JPanel {
    private static final long serialVersionUID = 1L;
    private final String[] labels;
    private final BigDecimal[] values;

    public BarChart(String[] labels, BigDecimal[] values) {
        this.labels = labels == null ? new String[0] : labels.clone();
        this.values = values == null ? new BigDecimal[0] : values.clone();
        setBackground(Color.WHITE);
        setBorder(UiTheme.CARD);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int count = Math.min(labels.length, values.length);
        BigDecimal maximum = maximum(count);
        int width = Math.max(1, (getWidth() - 50) / Math.max(1, count));
        int plotHeight = Math.max(0, getHeight() - 60);
        for (int i = 0; i < count; i++) {
            int height = scaledHeight(values[i], maximum, plotHeight);
            int x = 28 + i * width;
            int y = getHeight() - 30 - height;
            g.setColor(UiTheme.BLUE);
            g.fillRoundRect(x, y, Math.max(12, width - 14), height, 8, 8);
            g.setColor(UiTheme.MUTED);
            g.drawString(labels[i], x, getHeight() - 10);
        }
        g.dispose();
    }

    static int scaledHeight(BigDecimal value, BigDecimal maximum, int plotHeight) {
        if (value == null || value.signum() <= 0 || maximum == null || maximum.signum() <= 0 || plotHeight <= 0) {
            return 0;
        }
        BigDecimal height = value.min(maximum).multiply(BigDecimal.valueOf(plotHeight))
                .divide(maximum, MathContext.DECIMAL128);
        return height.setScale(0, RoundingMode.HALF_UP).intValue();
    }

    private BigDecimal maximum(int count) {
        BigDecimal maximum = BigDecimal.ONE;
        for (int i = 0; i < count; i++) {
            if (values[i] != null && values[i].compareTo(maximum) > 0) maximum = values[i];
        }
        return maximum;
    }
}
