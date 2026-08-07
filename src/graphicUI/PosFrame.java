package graphicUI;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableModel;

/** Native Swing, in-memory visual scaffold. No data is persisted. */
public final class PosFrame extends JFrame {
    private static final Color NAVY = new Color(31, 41, 55);
    private static final Color BLUE = new Color(37, 99, 235);
    private static final Color BACKGROUND = new Color(248, 250, 252);
    private static final Border CARD_BORDER = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(226, 232, 240)),
            BorderFactory.createEmptyBorder(18, 18, 18, 18));

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final JLabel pageTitle = new JLabel("Dashboard");
    private final JLabel message = new JLabel("Sample data only — no changes are saved.");
    private JButton selectedButton;

    public PosFrame() {
        super("POS Nature");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(920, 600));
        setSize(1100, 700);
        setLocationByPlatform(true);
        setContentPane(buildRoot());
    }

    private JPanel buildRoot() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BACKGROUND);
        root.add(buildSidebar(), BorderLayout.WEST);
        root.add(buildMain(), BorderLayout.CENTER);
        return root;
    }

    private JPanel buildSidebar() {
        JPanel side = new JPanel(new BorderLayout(0, 16));
        side.setBackground(NAVY);
        side.setBorder(BorderFactory.createEmptyBorder(20, 14, 20, 14));
        side.setPreferredSize(new Dimension(190, 0));
        JLabel brand = new JLabel("POS Nature");
        brand.setForeground(Color.WHITE);
        brand.setFont(brand.getFont().deriveFont(Font.BOLD, 21f));
        brand.getAccessibleContext().setAccessibleName("POS Nature navigation");
        side.add(brand, BorderLayout.NORTH);

        JPanel navigation = new JPanel(new GridLayout(4, 1, 0, 8));
        navigation.setOpaque(false);
        addNavigation(navigation, "Dashboard", 'D');
        addNavigation(navigation, "Products", 'P');
        addNavigation(navigation, "Customers", 'C');
        addNavigation(navigation, "Sales", 'S');
        side.add(navigation, BorderLayout.CENTER);
        return side;
    }

    private void addNavigation(JPanel navigation, final String page, int mnemonic) {
        final JButton button = new JButton(page);
        button.setMnemonic(mnemonic);
        button.setToolTipText("Open " + page);
        button.getAccessibleContext().setAccessibleName("Open " + page + " page");
        button.setFocusPainted(true);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setMargin(new Insets(10, 12, 10, 12));
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                showPage(page, button);
            }
        });
        navigation.add(button);
        if (selectedButton == null) {
            selectedButton = button;
            styleNavigation(button, true);
        } else {
            styleNavigation(button, false);
        }
    }

    private Component buildMain() {
        JPanel main = new JPanel(new BorderLayout(0, 14));
        main.setBackground(BACKGROUND);
        main.setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));
        pageTitle.setFont(pageTitle.getFont().deriveFont(Font.BOLD, 26f));
        pageTitle.getAccessibleContext().setAccessibleName("Current page");
        main.add(pageTitle, BorderLayout.NORTH);

        content.setBackground(BACKGROUND);
        content.add(dashboard(), "Dashboard");
        content.add(listPage("Products", "Add product", "Product name", "Name", "Category", "Price", "Stock",
                new Object[][] {{"Organic Tea", "Drinks", "$4.50", 18}, {"Oat Bar", "Snacks", "$2.20", 31}, {"Canvas Tote", "Goods", "$12.00", 9}}), "Products");
        content.add(listPage("Customers", "Add customer", "Customer name", "Name", "Phone", "Visits", "Last purchase",
                new Object[][] {{"Avery Nguyen", "0901 234 567", 12, "Today"}, {"Minh Tran", "0908 456 789", 8, "Yesterday"}, {"Linh Pham", "0912 345 678", 3, "Mon"}}), "Customers");
        content.add(listPage("Sales", "New sale", "Reference", "Receipt", "Customer", "Total", "Status",
                new Object[][] {{"#1048", "Avery Nguyen", "$18.40", "Paid"}, {"#1047", "Walk-in", "$6.70", "Paid"}, {"#1046", "Minh Tran", "$12.00", "Paid"}}), "Sales");
        main.add(content, BorderLayout.CENTER);
        message.setBorder(BorderFactory.createEmptyBorder(4, 2, 0, 2));
        message.setForeground(new Color(71, 85, 105));
        message.getAccessibleContext().setAccessibleName("Application status");
        main.add(message, BorderLayout.SOUTH);
        return main;
    }

    private JPanel dashboard() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(BACKGROUND);
        JPanel metrics = new JPanel(new GridLayout(1, 3, 16, 0));
        metrics.setBackground(BACKGROUND);
        metrics.add(metric("Today’s sales", "$37.10"));
        metrics.add(metric("Orders", "3"));
        metrics.add(metric("Low stock", "1 item"));
        panel.add(metrics, BorderLayout.NORTH);
        panel.add(tableCard("Recent sales", new String[] {"Receipt", "Customer", "Total", "Time"},
                new Object[][] {{"#1048", "Avery Nguyen", "$18.40", "10:24"}, {"#1047", "Walk-in", "$6.70", "09:51"}, {"#1046", "Minh Tran", "$12.00", "09:17"}}), BorderLayout.CENTER);
        return panel;
    }

    private JPanel metric(String label, String value) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(Color.WHITE);
        card.setBorder(CARD_BORDER);
        JLabel labelView = new JLabel(label);
        labelView.setForeground(new Color(71, 85, 105));
        JLabel valueView = new JLabel(value);
        valueView.setFont(valueView.getFont().deriveFont(Font.BOLD, 24f));
        card.add(labelView, BorderLayout.NORTH);
        card.add(valueView, BorderLayout.CENTER);
        return card;
    }

    private JPanel listPage(String title, String action, String inputLabel, String first, String second, String third, String fourth, Object[][] rows) {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(BACKGROUND);
        JPanel form = new JPanel(new BorderLayout(10, 0));
        form.setBackground(Color.WHITE);
        form.setBorder(CARD_BORDER);
        JLabel label = new JLabel(inputLabel + ":");
        JTextField input = new JTextField();
        input.getAccessibleContext().setAccessibleName(inputLabel);
        input.setToolTipText("Sample field; entries are not saved");
        label.setLabelFor(input);
        JButton actionButton = new JButton(action);
        actionButton.setToolTipText("Shows a sample-only message");
        actionButton.getAccessibleContext().setAccessibleName(action + " (sample only)");
        actionButton.addActionListener(new SampleAction(title, input));
        form.add(label, BorderLayout.WEST);
        form.add(input, BorderLayout.CENTER);
        form.add(actionButton, BorderLayout.EAST);
        panel.add(form, BorderLayout.NORTH);
        panel.add(tableCard(title, new String[] {first, second, third, fourth}, rows), BorderLayout.CENTER);
        return panel;
    }

    private JPanel tableCard(String title, String[] columns, Object[][] rows) {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(CARD_BORDER);
        JLabel heading = new JLabel(title);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 16f));
        JTable table = new JTable(new DefaultTableModel(rows, columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(28);
        table.getAccessibleContext().setAccessibleName(title + " sample table");
        table.setToolTipText("Sample in-memory rows");
        card.add(heading, BorderLayout.NORTH);
        card.add(new JScrollPane(table), BorderLayout.CENTER);
        return card;
    }

    private void showPage(String page, JButton button) {
        cards.show(content, page);
        pageTitle.setText(page);
        message.setText(page + " uses sample in-memory data only — no changes are saved.");
        styleNavigation(selectedButton, false);
        selectedButton = button;
        styleNavigation(selectedButton, true);
    }

    private void styleNavigation(JButton button, boolean selected) {
        if (button == null) {
            return;
        }
        button.setBackground(selected ? BLUE : NAVY);
        button.setForeground(Color.WHITE);
        button.setBorderPainted(false);
        button.setOpaque(true);
    }

    private final class SampleAction implements ActionListener {
        private final String page;
        private final JTextField input;

        private SampleAction(String page, JTextField input) {
            this.page = page;
            this.input = input;
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            String value = input.getText().trim();
            message.setText(value.length() == 0 ? page + " sample action selected; nothing is saved."
                    : "Sample " + page.toLowerCase() + " entry ‘" + value + "’ is not saved.");
            input.requestFocusInWindow();
        }
    }
}
