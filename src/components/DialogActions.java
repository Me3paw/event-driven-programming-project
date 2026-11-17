package components;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;

public final class DialogActions {
    public static final int FORM_MIN_WIDTH = 440;
    public static final int DETAIL_MIN_WIDTH = 520;
    public static final int DETAIL_MIN_HEIGHT = 220;
    public static final Dimension DETAIL_TABLE = new Dimension(560, 280);
    public static final Dimension PREVIEW_TEXT = new Dimension(480, 360);

    private DialogActions() {
    }

    public static boolean confirm(Component parent, JComponent content, String title) {
        final JOptionPane pane = new JOptionPane(content, JOptionPane.PLAIN_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION);
        final JDialog dialog = pane.createDialog(parent, title);
        dialog.pack();
        Dimension size = dialog.getSize();
        if (size.width < FORM_MIN_WIDTH)
            dialog.setSize(FORM_MIN_WIDTH, size.height);
        bind(dialog, pane);
        dialog.setVisible(true);
        return Integer.valueOf(JOptionPane.OK_OPTION).equals(pane.getValue());
    }

    /**
     * Dialog xác nhận có kiểm tra form: nếu validator.validate() trả false thì dialog
     * không đóng, lỗi đỏ hiển thị dưới từng ô và con trỏ về ô sai đầu tiên.
     */
    public static boolean confirm(Component parent, JComponent content, String title,
            FormValidator validator) {
        Window owner = SwingUtilities.getWindowAncestor(parent);
        final JDialog dialog = new JDialog(
                owner instanceof Frame ? (Frame) owner : null, title, true);
        JPanel root = new JPanel(new BorderLayout(0, 8));
        root.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        root.add(content, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        final boolean[] accepted = {false};
        JButton ok = new JButton("Đồng ý");
        JButton cancel = new JButton("Hủy");
        ok.addActionListener(event -> {
            if (validator == null || validator.validate()) {
                accepted[0] = true;
                dialog.dispose();
            }
        });
        cancel.addActionListener(event -> dialog.dispose());
        buttons.add(ok);
        buttons.add(cancel);
        root.add(buttons, BorderLayout.SOUTH);
        dialog.setContentPane(root);
        dialog.getRootPane().setDefaultButton(ok);
        dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "dialog.cancel");
        dialog.getRootPane().getActionMap().put("dialog.cancel", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                dialog.dispose();
            }
        });
        dialog.pack();
        Dimension size = dialog.getSize();
        if (size.width < FORM_MIN_WIDTH)
            dialog.setSize(FORM_MIN_WIDTH, size.height);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
        return accepted[0];
    }

    public static void info(Component parent, JComponent content, String title) {
        JOptionPane pane = new JOptionPane(content, JOptionPane.INFORMATION_MESSAGE);
        JDialog dialog = pane.createDialog(parent, title);
        dialog.pack();
        Dimension size = dialog.getSize();
        if (size.width < DETAIL_MIN_WIDTH || size.height < DETAIL_MIN_HEIGHT)
            dialog.setSize(Math.max(size.width, DETAIL_MIN_WIDTH),
                    Math.max(size.height, DETAIL_MIN_HEIGHT));
        dialog.setVisible(true);
    }

    @SuppressWarnings("serial")
    private static void bind(final JDialog dialog, final JOptionPane pane) {
        JRootPane root = dialog.getRootPane();
        JButton save = firstButton(pane);
        if (save != null) root.setDefaultButton(save);
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK), "dialog.save");
        root.getActionMap().put("dialog.save", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                JButton save = firstButton(pane);
                if (save != null) save.doClick();
            }
        });
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "dialog.cancel");
        root.getActionMap().put("dialog.cancel", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                pane.setValue(Integer.valueOf(JOptionPane.CANCEL_OPTION));
                dialog.dispose();
            }
        });
    }

    private static JButton firstButton(Container parent) {
        for (Component child : parent.getComponents()) {
            if (child instanceof JButton) return (JButton) child;
            if (child instanceof Container) {
                JButton button = firstButton((Container) child);
                if (button != null) return button;
            }
        }
        return null;
    }
}
