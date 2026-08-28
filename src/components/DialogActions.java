package components;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;

public final class DialogActions {
    private DialogActions() {
    }

    public static boolean confirm(Component parent, JComponent content, String title) {
        final JOptionPane pane = new JOptionPane(content, JOptionPane.PLAIN_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION);
        final JDialog dialog = pane.createDialog(parent, title);
        bind(dialog, pane);
        dialog.setVisible(true);
        return Integer.valueOf(JOptionPane.OK_OPTION).equals(pane.getValue());
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
                pane.setValue(Integer.valueOf(JOptionPane.OK_OPTION));
                dialog.dispose();
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
