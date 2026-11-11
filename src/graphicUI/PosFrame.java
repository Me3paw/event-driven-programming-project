package graphicUI;

import components.DialogActions;
import components.FormValidator;
import components.ReceiptPrintable;
import components.ReceiptPdfExporter;
import components.UiFormat;
import components.UiTheme;
import entity.HoaDon;
import entity.CauHinhCuaHang;
import entity.KhachHang;
import entity.KhuyenMai;
import entity.SanPham;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Rectangle;
import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.function.Supplier;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JComboBox;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JFileChooser;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.text.JTextComponent;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import service.AuthService;
import service.Actor;
import service.BaoGia;
import service.DongThanhToan;
import service.SaleService;
import service.ThongKeCa;
import service.YeuCauThanhToan;
import service.SettingsService;
import service.MasterDataService;

@SuppressWarnings("serial")
public final class PosFrame extends JFrame {
    private static final String MANAGER = "QUAN_LY";
    private static final String CASHIER = "THU_NGAN";
    private final java.awt.CardLayout cards = new java.awt.CardLayout();
    private final JPanel pages = new JPanel(cards);
    private final JLabel title = new JLabel();
    private final JLabel status = new JLabel("Sẵn sàng.");
    private String role = CASHIER;
    private Actor actor;
    private JButton selectedNavigation;

    public PosFrame() {
        super("POS Cửa Hàng Tiện Lợi");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1024, 680));
        setSize(1280, 780);
        setLocationByPlatform(true);
        showLogin();
    }

    private void showLogin() {
        JPanel root = new JPanel(new GridLayout(1, 2));
        JPanel intro = new JPanel(new BorderLayout(0, 18));
        intro.setBackground(UiTheme.NAVY);
        intro.setBorder(BorderFactory.createEmptyBorder(54, 50, 54, 50));
        JLabel brand = new JLabel("POS\nCỬA HÀNG TIỆN LỢI");
        brand.setForeground(Color.WHITE);
        brand.setFont(brand.getFont().deriveFont(Font.BOLD, 34f));
        intro.add(brand, BorderLayout.CENTER);
        root.add(intro);
        JPanel holder = new JPanel(new java.awt.GridBagLayout());
        holder.setBackground(UiTheme.BACKGROUND);
        JPanel form = new JPanel(new GridLayout(0, 1, 0, 10));
        form.setPreferredSize(new Dimension(360, 300));
        form.setBackground(Color.WHITE);
        form.setBorder(UiTheme.CARD);
        JLabel heading = new JLabel("Đăng nhập");
        UiTheme.title(heading);
        final JTextField username = new JTextField();
        final JPasswordField password = new JPasswordField();
        form.add(heading);
        form.add(labeled("Tên đăng nhập", username));
        JPanel passwordRow = new JPanel(new BorderLayout(4, 0));
        passwordRow.setOpaque(false);
        passwordRow.add(password, BorderLayout.CENTER);
        final JButton eye = new JButton("Hiện");
        eye.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                boolean show = "Hiện".equals(eye.getText());
                password.setEchoChar(show ? (char) 0 : '●');
                eye.setText(show ? "Ẩn" : "Hiện");
            }
        });
        passwordRow.add(eye, BorderLayout.EAST);
        form.add(labeled("Mật khẩu", passwordRow));
        final JButton login = new JButton("Đăng nhập");
        login.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                final String loginName = username.getText().trim();
                final char[] loginPassword = password.getPassword();
                if (loginName.length() == 0 || loginPassword.length == 0) {
                    Arrays.fill(loginPassword, '\0');
                    password.setText("");
                    JOptionPane.showMessageDialog(PosFrame.this, "Nhập tên đăng nhập và mật khẩu.", "Thiếu thông tin",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }
                login.setEnabled(false);
                new SwingWorker<Actor, Void>() {
                    @Override
                    protected Actor doInBackground() throws Exception {
                        return new AuthService().dangNhapActor(loginName, loginPassword);
                    }
                    @Override
                    protected void done() {
                        login.setEnabled(true);
                        Arrays.fill(loginPassword, '\0');
                        password.setText("");
                        try {
                            actor = get();
                            role = actor.getVaiTro();
                            showShell();
                        } catch (Exception exception) {
                            showLoginError(exception);
                        }
                    }

                    private void showLoginError(Exception exception) {
                        Throwable cause = exception;
                        while (cause.getCause() != null
                                && (cause instanceof java.util.concurrent.ExecutionException
                                    || cause instanceof java.util.concurrent.CompletionException))
                            cause = cause.getCause();
                        String message;
                        if (cause instanceof SecurityException)
                            message = cause.getMessage();
                        else if (cause instanceof java.sql.SQLException)
                            message = "Không thể kết nối cơ sở dữ liệu. Kiểm tra SQL Server rồi thử lại.";
                        else
                            message = "Không thể hoàn tất đăng nhập. Vui lòng thử lại.";
                        if (message == null || message.trim().isEmpty())
                            message = "Không thể hoàn tất đăng nhập. Vui lòng thử lại.";
                        JOptionPane.showMessageDialog(PosFrame.this, message, "Đăng nhập", JOptionPane.ERROR_MESSAGE);
                    }
                }.execute();
            }
        });
        form.add(login);
        holder.add(form);
        root.add(holder);
        setContentPane(root);
        getRootPane().setDefaultButton(login);
    }

    private JPanel labeled(String text, JComponent input) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setOpaque(false);
        JLabel label = new JLabel(text);
        label.setLabelFor(input);
        label.setDisplayedMnemonic(text.charAt(0));
        input.getAccessibleContext().setAccessibleName(text);
        panel.add(label, BorderLayout.NORTH);
        panel.add(input, BorderLayout.CENTER);
        return panel;
    }

    private void showShell() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UiTheme.BACKGROUND);
        root.add(sidebar(), BorderLayout.WEST);
        root.add(main(), BorderLayout.CENTER);
        setContentPane(root);
        bindShortcuts();
        showPage("dashboard", null);
        revalidate();
        repaint();
    }

    private JPanel sidebar() {
        JPanel side = new JPanel(new BorderLayout());
        side.setPreferredSize(new Dimension(224, 0));
        side.setBackground(UiTheme.NAVY);
        side.setBorder(BorderFactory.createEmptyBorder(20, 12, 20, 12));
        JLabel brand = new JLabel("POS\nCỬA HÀNG");
        brand.setForeground(Color.WHITE);
        brand.setFont(brand.getFont().deriveFont(Font.BOLD, 20f));
        side.add(brand, BorderLayout.NORTH);
        JPanel nav = new JPanel();
        nav.setOpaque(false);
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        navigation(nav, "dashboard", "Tổng quan");
        navigation(nav, "sale", "Bán hàng  F2");
        navigation(nav, "customers", "Khách hàng / CRM");
        navigation(nav, "products", "Sản phẩm");
        navigation(nav, "categories", "Loại sản phẩm");
        navigation(nav, "stock", "Tồn kho");
        navigation(nav, "invoices", "Hóa đơn");
        if (MANAGER.equals(role)) {
            navigation(nav, "staff", "Nhân viên");
            navigation(nav, "accounts", "Tài khoản");
            navigation(nav, "suppliers", "Nhà cung cấp");
            navigation(nav, "purchases", "Phiếu nhập");
            navigation(nav, "adjustments", "Phiếu điều chỉnh tồn");
            navigation(nav, "promotions", "Khuyến mãi");
            navigation(nav, "settings", "Cấu hình");
        }
        JScrollPane scroll = new JScrollPane(nav);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        side.add(scroll, BorderLayout.CENTER);
        JPanel account = new JPanel();
        account.setOpaque(false);
        account.setLayout(new BoxLayout(account, BoxLayout.Y_AXIS));
        JLabel user = new JLabel(displayUser());
        user.setForeground(Color.WHITE);
        user.setAlignmentX(Component.LEFT_ALIGNMENT);
        account.add(user);
        account.add(Box.createVerticalStrut(8));
        JButton changePassword = new JButton("Đổi mật khẩu");
        changePassword.setAlignmentX(Component.LEFT_ALIGNMENT);
        changePassword.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        changePassword.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                changePassword();
            }
        });
        account.add(changePassword);
        account.add(Box.createVerticalStrut(4));
        JButton logout = new JButton("Đăng xuất");
        logout.setAlignmentX(Component.LEFT_ALIGNMENT);
        logout.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        logout.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                actor = null;
                role = CASHIER;
                showLogin();
            }
        });
        account.add(logout);
        side.add(account, BorderLayout.SOUTH);
        return side;
    }

    private void navigation(JPanel nav, final String key, String text) {
        final JButton button = new JButton(text);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        button.setMargin(new Insets(8, 10, 8, 10));
        button.setForeground(Color.WHITE);
        button.setBackground(UiTheme.NAVY);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.putClientProperty("navigationButton", Boolean.TRUE);
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                showPage(key, button);
            }
        });
        nav.add(button);
        nav.add(Box.createVerticalStrut(4));
    }

    private JPanel main() {
        JPanel main = new JPanel(new BorderLayout(0, 12));
        main.setBackground(UiTheme.BACKGROUND);
        main.setBorder(BorderFactory.createEmptyBorder(22, 24, 16, 24));
        JPanel top = new JPanel(new BorderLayout(12, 0));
        top.setOpaque(false);
        JPanel leftBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        leftBox.setOpaque(false);
        UiTheme.title(title);
        leftBox.add(title);
        top.add(leftBox, BorderLayout.WEST);
        main.add(top, BorderLayout.NORTH);
        pages.setBackground(UiTheme.BACKGROUND);
        addPages();
        main.add(pages, BorderLayout.CENTER);
        status.setForeground(UiTheme.MUTED);
        main.add(status, BorderLayout.SOUTH);
        return main;
    }

    private void addPages() {
        pages.removeAll();
        pages.add(dashboard(), "dashboard");
        pages.add(sale(), "sale");
        pages.add(new CustomerPanel(actor), "customers");
        pages.add(new ProductPanel(actor, MANAGER.equals(role)), "products");
        pages.add(new CategoryPanel(actor, MANAGER.equals(role)), "categories");
        pages.add(new InventoryPanel(actor), "stock");
        pages.add(new InvoicePanel(actor), "invoices");
        if (MANAGER.equals(role)) {
            pages.add(new StaffPanel(actor), "staff");
            pages.add(new AccountPanel(actor), "accounts");
            pages.add(new SupplierPanel(actor), "suppliers");
            pages.add(new PurchasePanel(actor), "purchases");
            pages.add(new InventoryPanel(actor, true), "adjustments");
            pages.add(new PromotionPanel(actor), "promotions");
            pages.add(settings(), "settings");
        }
    }

    private JPanel dashboard() {
        if (MANAGER.equals(role))
            return new ReportPanel(actor);
        JPanel panel = page();
        JPanel stats = new JPanel(new GridLayout(1, 5, 12, 0));
        stats.setOpaque(false);
        final JLabel revenueValue = statCard(stats, "Doanh thu hôm nay");
        final JLabel cashValue = statCard(stats, "Tiền mặt");
        final JLabel cardValue = statCard(stats, "Thẻ / chuyển khoản");
        final JLabel invoiceValue = statCard(stats, "Số hóa đơn");
        final JLabel customerValue = statCard(stats, "Khách mua hàng");
        panel.add(stats, BorderLayout.NORTH);
        JPanel content = new JPanel(new BorderLayout(0, 14));
        content.setOpaque(false);
        JPanel quickActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        quickActions.setOpaque(false);
        quickActions.add(actionButton("Bán hàng mới (F2)", "sale"));
        quickActions.add(actionButton("Tra cứu khách hàng", "customers"));
        quickActions.add(actionButton("Tìm sản phẩm / tồn kho", "stock"));
        quickActions.add(actionButton("Xem hóa đơn của tôi", "invoices"));
        content.add(quickActions, BorderLayout.CENTER);
        panel.add(content, BorderLayout.CENTER);
        new SwingWorker<ThongKeCa, Void>() {
            @Override
            protected ThongKeCa doInBackground() throws Exception {
                return new SaleService().thongKeCa(actor);
            }
            @Override
            protected void done() {
                try {
                    ThongKeCa statsToday = get();
                    revenueValue.setText(UiFormat.money(statsToday.getDoanhThu()));
                    cashValue.setText(UiFormat.money(statsToday.getTienMat()));
                    cardValue.setText(UiFormat.money(statsToday.getTienThe()));
                    invoiceValue.setText(String.valueOf(statsToday.getSoHoaDon()));
                    customerValue.setText(String.valueOf(statsToday.getSoKhachMua()));
                } catch (Exception exception) {
                    revenueValue.setText("—");
                    cashValue.setText("—");
                    cardValue.setText("—");
                    invoiceValue.setText("—");
                    customerValue.setText("—");
                }
            }
        }.execute();
        return panel;
    }

    private void changePassword() {
        if (actor == null)
            return;
        final JPasswordField current = new JPasswordField();
        final JPasswordField next = new JPasswordField();
        final JPasswordField confirm = new JPasswordField();
        JPanel form = FormValidator.form();
        FormValidator validator = new FormValidator();
        form.add(validator.row("Mật khẩu hiện tại", current));
        form.add(validator.row("Mật khẩu mới (ít nhất 6 ký tự)", next));
        form.add(validator.row("Nhập lại mật khẩu mới", confirm));
        validator.require(current, "Vui lòng nhập mật khẩu hiện tại.");
        validator.require(next, "Vui lòng nhập mật khẩu mới.");
        validator.check(next, text -> text.length() >= 6,
                "Mật khẩu mới phải có ít nhất 6 ký tự.");
        validator.custom(confirm, () -> {
            char[] first = next.getPassword();
            char[] second = confirm.getPassword();
            boolean same = first.length == second.length;
            if (same)
                for (int i = 0; i < first.length; i++)
                    if (first[i] != second[i]) {
                        same = false;
                        break;
                    }
            return same ? null : "Mật khẩu mới nhập lại không khớp.";
        });
        if (!DialogActions.confirm(this, form, "Đổi mật khẩu", validator)) {
            current.setText("");
            next.setText("");
            confirm.setText("");
            return;
        }
        final char[] oldPassword = current.getPassword();
        final char[] newPassword = next.getPassword();
        final char[] repeat = confirm.getPassword();
        current.setText("");
        next.setText("");
        confirm.setText("");
        Arrays.fill(repeat, '\0');
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                new AuthService().doiMatKhau(actor, oldPassword, newPassword);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(PosFrame.this, "Đổi mật khẩu thành công.");
                } catch (Exception exception) {
                    Throwable cause = exception.getCause() == null ? exception : exception.getCause();
                    String detail = cause.getMessage();
                    JOptionPane.showMessageDialog(PosFrame.this,
                            detail == null ? "Không đổi được mật khẩu." : detail,
                            "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private String displayUser() {
        if (actor == null)
            return "";
        String name = actor.getTenNhanVien();
        if (name == null || name.trim().isEmpty())
            name = actor.getTenDangNhap();
        return name + " · " + (actor.isQuanLy() ? "Quản lý" : "Thu ngân");
    }

    private JLabel statCard(JPanel parent, String caption) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(UiTheme.CARD);
        JPanel wrap = new JPanel(new BorderLayout(0, 6));
        wrap.setOpaque(false);
        JLabel titleLabel = new JLabel(caption);
        titleLabel.setForeground(UiTheme.MUTED);
        JLabel value = new JLabel("…");
        value.setFont(value.getFont().deriveFont(Font.BOLD, 20f));
        wrap.add(titleLabel, BorderLayout.NORTH);
        wrap.add(value, BorderLayout.CENTER);
        card.add(wrap, BorderLayout.CENTER);
        parent.add(card);
        return value;
    }

    private static void place(JPanel panel, java.awt.Component component, int x, int y,
            double weightX, int fill) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = x;
        constraints.gridy = y;
        constraints.weightx = weightX;
        constraints.fill = fill;
        constraints.insets = new Insets(2, 4, 2, 4);
        panel.add(component, constraints);
    }

    private static void placeWest(JPanel panel, java.awt.Component component, int x, int y) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = x;
        constraints.gridy = y;
        constraints.weightx = 0;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(2, 4, 2, 4);
        panel.add(component, constraints);
    }

    private JButton actionButton(String text, final String pageKey) {
        JButton button = new JButton(text);
        button.setForeground(Color.WHITE);
        button.setBackground(UiTheme.NAVY);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.addActionListener(event -> showPage(pageKey, button));
        return button;
    }

    private JPanel sale() {
        JPanel panel = page();
        final DefaultTableModel cart =
                new DefaultTableModel(new String[] {"Mã", "Sản phẩm", "SL", "Đơn giá", "Giảm dòng", "Thành tiền", "Thao tác"}, 0) {
                    @Override
                    public boolean isCellEditable(int row, int column) {
                        return false;
                    }
                };
        final JComboBox<SanPham> products = new JComboBox<SanPham>();
        final JComboBox<KhachHang> customers = new JComboBox<KhachHang>();
        final JComboBox<KhuyenMai> promotions = new JComboBox<KhuyenMai>();
        products.setRenderer(productRenderer());
        products.setPreferredSize(new Dimension(170, products.getPreferredSize().height));
        customers.setRenderer(customerRenderer());
        customers.setPreferredSize(new Dimension(170, customers.getPreferredSize().height));
        customers.addItem(null);
        promotions.setRenderer(promotionRenderer());
        promotions.getAccessibleContext().setAccessibleName("Khuyến mãi áp dụng");

        JPanel productForm = new JPanel(new GridBagLayout());
        productForm.setOpaque(false);
        final JTextField product = new JTextField();
        final JSpinner quantity = new JSpinner(new SpinnerNumberModel(1, 1, 9999, 1));
        JLabel productLabel = posLabel("Mã / tên sản phẩm");
        productLabel.setLabelFor(product);
        productLabel.setDisplayedMnemonic(KeyEvent.VK_M);
        place(productForm, productLabel, 0, 0, 0, GridBagConstraints.NONE);
        product.setPreferredSize(new Dimension(130, product.getPreferredSize().height));
        placeWest(productForm, product, 1, 0);
        product.getAccessibleContext().setAccessibleName("Mã hoặc tên sản phẩm");
        product.setToolTipText("Gõ để tìm ngay — kết quả tự hiện trong danh sách");
        final Timer productSearch = new Timer(300,
                event -> findProducts(product.getText().trim(), products, false, product));
        productSearch.setRepeats(false);
        product.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { productSearch.restart(); }
            @Override public void removeUpdate(DocumentEvent event) { productSearch.restart(); }
            @Override public void changedUpdate(DocumentEvent event) { productSearch.restart(); }
        });
        products.getAccessibleContext().setAccessibleName("Sản phẩm tìm được");
        place(productForm, products, 2, 0, 1.0, GridBagConstraints.HORIZONTAL);
        JLabel quantityLabel = posLabel("Số lượng");
        quantityLabel.setLabelFor(quantity);
        quantityLabel.setDisplayedMnemonic(KeyEvent.VK_S);
        place(productForm, quantityLabel, 0, 1, 0, GridBagConstraints.NONE);
        placeWest(productForm, quantity, 1, 1);
        quantity.getAccessibleContext().setAccessibleName("Số lượng sản phẩm");
        final JRadioButton noDiscount = new JRadioButton("Không giảm", true);
        final JRadioButton tierDiscount = new JRadioButton("Giảm hạng");
        final JRadioButton eventDiscount = new JRadioButton("Khuyến mãi sự kiện");
        ButtonGroup discounts = new ButtonGroup();
        discounts.add(noDiscount);
        discounts.add(tierDiscount);
        discounts.add(eventDiscount);
        final JTextField rewards = new JTextField("0", 6);
        final JComboBox<String> method = new JComboBox<String>(new String[] {"TIEN_MAT", "THE", "QR"});
        final JComboBox<String> result = new JComboBox<String>(new String[] {"THÀNH CÔNG", "THẤT BẠI"});
        final JTextField tender = new JTextField(10);
        final JLabel subtotal = new JLabel("0"), discount = new JLabel("0"),
                     reward = new JLabel("0"), vat = new JLabel("0"), total = new JLabel("0"),
                     change = new JLabel("0");
        JButton add = new JButton("Thêm vào giỏ");
        add.setMnemonic(KeyEvent.VK_A);
        add.setBackground(UiTheme.BLUE);
        add.setForeground(Color.WHITE);
        add.setOpaque(true);
        add.setBorderPainted(false);
        add.setFocusPainted(false);
        add.setFont(add.getFont().deriveFont(Font.BOLD, 15f));
        add.setMargin(new Insets(8, 12, 8, 12));
        add.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                final SanPham selected = (SanPham) products.getSelectedItem();
                if (selected == null) {
                    JOptionPane.showMessageDialog(PosFrame.this, "Tìm và chọn sản phẩm đang kinh doanh.");
                    return;
                }
                final int amount = ((Number) quantity.getValue()).intValue();
                final java.util.Map<String, Integer> needed = cartQuantities(cart);
                final Integer previous = needed.get(selected.getMaSanPham());
                needed.put(selected.getMaSanPham(), Integer.valueOf((previous == null ? 0 : previous.intValue()) + amount));
                checkStock(needed, new Runnable() {
                    @Override
                    public void run() {
                        addCartLine(cart, selected, amount);
                        product.setText("");
                        status.setText(previous == null
                                ? "Đã thêm " + selected.getTenSanPham() + " vào giỏ."
                                : selected.getTenSanPham() + " đã có trong giỏ — đã cộng dồn số lượng.");
                        refreshQuote(cart, customers, promotions, noDiscount, tierDiscount, eventDiscount,
                                rewards, method, tender, result, subtotal, discount, reward, vat, total, change);
                        loadPromotions(cart, promotions, new Runnable() {
                            @Override
                            public void run() {
                                refreshQuote(cart, customers, promotions, noDiscount, tierDiscount, eventDiscount,
                                        rewards, method, tender, result, subtotal, discount, reward, vat, total, change);
                            }
                        });
                    }
                });
            }
        });
        GridBagConstraints addConstraints = new GridBagConstraints();
        addConstraints.gridx = 2;
        addConstraints.gridy = 1;
        addConstraints.weightx = 1.0;
        addConstraints.fill = GridBagConstraints.NONE;
        addConstraints.anchor = GridBagConstraints.EAST;
        addConstraints.insets = new Insets(2, 4, 2, 4);
        productForm.add(add, addConstraints);
        JPanel addCard = titledCard("Thêm sản phẩm", productForm);

        final Runnable refreshQuoteAction = new Runnable() {
            @Override
            public void run() {
                refreshQuote(cart, customers, promotions, noDiscount, tierDiscount, eventDiscount,
                        rewards, method, tender, result, subtotal, discount, reward, vat, total, change);
            }
        };
        final Runnable syncRewards = new Runnable() {
            @Override
            public void run() {
                boolean hasCustomer = customers.getSelectedItem() != null;
                if (!hasCustomer)
                    rewards.setText("0");
            }
        };
        final Runnable customerChanged = new Runnable() {
            @Override
            public void run() {
                syncRewards.run();
                refreshQuoteAction.run();
            }
        };
        final Runnable afterCartChange = new Runnable() {
            @Override
            public void run() {
                refreshQuoteAction.run();
                loadPromotions(cart, promotions, new Runnable() {
                    @Override
                    public void run() {
                        refreshQuoteAction.run();
                    }
                });
            }
        };

        JPanel customerForm = new JPanel(new GridBagLayout());
        customerForm.setOpaque(false);
        final JTextField customer = new JTextField();
        final JLabel loyalty = new JLabel("Khách lẻ · không dùng điểm");
        loyalty.setForeground(UiTheme.MUTED);
        JLabel customerLabel = posLabel("Tìm khách hàng");
        customerLabel.setLabelFor(customer);
        customerLabel.setDisplayedMnemonic(KeyEvent.VK_K);
        place(customerForm, customerLabel, 0, 0, 0, GridBagConstraints.NONE);
        customer.setPreferredSize(new Dimension(150, customer.getPreferredSize().height));
        placeWest(customerForm, customer, 1, 0);
        customer.getAccessibleContext().setAccessibleName("Mã hoặc tên khách hàng");
        customer.setToolTipText("Gõ để tìm ngay — kết quả tự hiện trong danh sách");
        final Timer customerSearch = new Timer(300,
                event -> findCustomers(customer.getText().trim(), customers, loyalty, customerChanged, customer));
        customerSearch.setRepeats(false);
        customer.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { customerSearch.restart(); }
            @Override public void removeUpdate(DocumentEvent event) { customerSearch.restart(); }
            @Override public void changedUpdate(DocumentEvent event) { customerSearch.restart(); }
        });
        customers.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                showLoyalty((KhachHang) customers.getSelectedItem(), loyalty);
                syncRewards.run();
                refreshQuote(cart, customers, promotions, noDiscount, tierDiscount, eventDiscount,
                        rewards, method, tender, result, subtotal, discount, reward, vat, total, change);
            }
        });
        customers.getAccessibleContext().setAccessibleName("Khách hàng tìm được");
        place(customerForm, customers, 2, 0, 1.0, GridBagConstraints.HORIZONTAL);
        GridBagConstraints loyaltyConstraints = new GridBagConstraints();
        loyaltyConstraints.gridx = 1;
        loyaltyConstraints.gridy = 1;
        loyaltyConstraints.gridwidth = 2;
        loyaltyConstraints.weightx = 1.0;
        loyaltyConstraints.fill = GridBagConstraints.HORIZONTAL;
        loyaltyConstraints.anchor = GridBagConstraints.WEST;
        loyaltyConstraints.insets = new Insets(2, 4, 2, 4);
        customerForm.add(loyalty, loyaltyConstraints);
        JPanel customerCard = titledCard("Thông tin khách hàng", customerForm);
        syncRewards.run();
        findProducts("", products, false);

        JPanel promoBox = new JPanel(new GridBagLayout());
        promoBox.setOpaque(false);
        JPanel segRow = new JPanel(new GridLayout(1, 3, 6, 0));
        segRow.setOpaque(false);
        final JToggleButton segNone = segment("Không giảm");
        final JToggleButton segTier = segment("Giảm hạng");
        final JToggleButton segEvent = segment("Khuyến mãi");
        ButtonGroup segGroup = new ButtonGroup();
        segGroup.add(segNone);
        segGroup.add(segTier);
        segGroup.add(segEvent);
        segNone.setSelected(true);
        segNone.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent event) { noDiscount.setSelected(true); }
        });
        segTier.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent event) { tierDiscount.setSelected(true); }
        });
        segEvent.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent event) { eventDiscount.setSelected(true); }
        });
        segRow.add(segNone);
        segRow.add(segTier);
        segRow.add(segEvent);
        place(promoBox, segRow, 1, 0, 1.0, GridBagConstraints.HORIZONTAL);
        place(promoBox, promotions, 1, 1, 1.0, GridBagConstraints.HORIZONTAL);
        JLabel rewardsLabel = posLabel("Điểm dùng");
        rewardsLabel.setLabelFor(rewards);
        rewardsLabel.setDisplayedMnemonic(KeyEvent.VK_D);
        rewards.getAccessibleContext().setAccessibleName("Điểm thưởng sử dụng");
        place(promoBox, rewardsLabel, 0, 2, 0, GridBagConstraints.NONE);
        placeWest(promoBox, rewards, 1, 2);
        JLabel rewardsError = fieldError();
        place(promoBox, rewardsError, 1, 3, 1.0, GridBagConstraints.HORIZONTAL);
        validateOnBlur(rewards, rewardsError, () -> {
            String text = rewards.getText().trim();
            if (text.isEmpty())
                return null;
            try {
                return new BigDecimal(text).signum() < 0 ? "Điểm dùng không được âm." : null;
            } catch (NumberFormatException exception) {
                return "Điểm dùng phải là số.";
            }
        });
        JPanel promoCard = titledCard("Khuyến mãi", promoBox);

        JLabel methodLabel = posLabel("Phương thức");
        methodLabel.setLabelFor(method);
        methodLabel.setDisplayedMnemonic(KeyEvent.VK_P);
        method.getAccessibleContext().setAccessibleName("Phương thức thanh toán");
        JLabel tenderLabel = posLabel("Tiền khách đưa");
        tenderLabel.setLabelFor(tender);
        tenderLabel.setDisplayedMnemonic(KeyEvent.VK_I);
        tender.getAccessibleContext().setAccessibleName("Tiền khách đưa");
        JPanel payFields = new JPanel(new GridBagLayout());
        payFields.setOpaque(false);
        place(payFields, methodLabel, 0, 0, 0, GridBagConstraints.NONE);
        place(payFields, method, 1, 0, 1.0, GridBagConstraints.HORIZONTAL);
        JLabel resultLabel = posLabel("Kết quả");
        resultLabel.setLabelFor(result);
        result.getAccessibleContext().setAccessibleName("Kết quả thanh toán điện tử");
        place(payFields, resultLabel, 0, 1, 0, GridBagConstraints.NONE);
        place(payFields, result, 1, 1, 1.0, GridBagConstraints.HORIZONTAL);
        place(payFields, tenderLabel, 0, 2, 0, GridBagConstraints.NONE);
        place(payFields, tender, 1, 2, 1.0, GridBagConstraints.HORIZONTAL);
        JLabel tenderError = fieldError();
        place(payFields, tenderError, 1, 3, 1.0, GridBagConstraints.HORIZONTAL);
        validateOnBlur(tender, tenderError, () -> {
            String text = tender.getText().trim();
            if (text.isEmpty())
                return null;
            try {
                return new BigDecimal(text).signum() < 0 ? "Tiền khách đưa không được âm." : null;
            } catch (NumberFormatException exception) {
                return "Tiền khách đưa phải là số.";
            }
        });
        JButton checkout = new JButton("Thanh toán");
        checkout.setBackground(UiTheme.BLUE);
        checkout.setForeground(Color.WHITE);
        checkout.setOpaque(true);
        checkout.setBorderPainted(false);
        checkout.setFocusPainted(false);
        checkout.setFont(checkout.getFont().deriveFont(Font.BOLD, 16f));
        checkout.setMargin(new Insets(8, 0, 8, 0));
        checkout.setEnabled(false);
        checkout.setToolTipText("Giỏ hàng đang trống — hãy thêm sản phẩm");
        checkout.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                YeuCauThanhToan request = paymentRequest(cart, (KhachHang) customers.getSelectedItem(),
                        (KhuyenMai) promotions.getSelectedItem(), tierDiscount.isSelected(), eventDiscount.isSelected(),
                        rewards.getText().trim(), String.valueOf(method.getSelectedItem()), tender.getText().trim(),
                        "THÀNH CÔNG".equals(result.getSelectedItem()));
                if (request != null
                        && JOptionPane.showConfirmDialog(
                                   PosFrame.this, "Xác nhận thanh toán?", "Thanh toán", JOptionPane.YES_NO_OPTION)
                                == JOptionPane.YES_OPTION) {
                    checkout(request, cart, product, products, quantity, customer, customers, promotions, noDiscount,
                            rewards, tender, method, result, subtotal, discount, reward, vat, total, change, loyalty);
                }
            }
        });
        JPanel payBody = new JPanel(new BorderLayout(0, 8));
        payBody.setOpaque(false);
        payBody.add(payFields, BorderLayout.CENTER);
        payBody.add(checkout, BorderLayout.SOUTH);
        JPanel payCard = titledCard("Thanh toán", payBody);

        final Runnable refresh = new Runnable() {
            @Override
            public void run() {
                refreshQuote(cart, customers, promotions, noDiscount, tierDiscount, eventDiscount,
                        rewards, method, tender, result, subtotal, discount, reward, vat, total, change);
            }
        };
        noDiscount.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                segNone.setSelected(true);
                refresh.run();
            }
        });
        tierDiscount.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                segTier.setSelected(true);
                refresh.run();
            }
        });
        eventDiscount.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                segEvent.setSelected(true);
                refresh.run();
            }
        });
        promotions.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                refresh.run();
            }
        });
        method.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                refresh.run();
            }
        });
        result.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                refresh.run();
            }
        });
        addQuoteRefreshListener(rewards, refresh, true);
        addQuoteRefreshListener(tender, refresh, false);
        total.setFont(total.getFont().deriveFont(Font.BOLD, 18f));
        total.setForeground(UiTheme.BLUE);
        JPanel totalsBox = new JPanel(new GridBagLayout());
        totalsBox.setOpaque(false);
        place(totalsBox, moneyRow("Tạm tính", subtotal), 0, 0, 1.0, GridBagConstraints.HORIZONTAL);
        place(totalsBox, moneyRow("Giảm giá", discount), 0, 1, 1.0, GridBagConstraints.HORIZONTAL);
        place(totalsBox, moneyRow("Điểm sử dụng", reward), 0, 2, 1.0, GridBagConstraints.HORIZONTAL);
        place(totalsBox, moneyRow("VAT", vat), 0, 3, 1.0, GridBagConstraints.HORIZONTAL);
        place(totalsBox, totalRow(total), 0, 4, 1.0, GridBagConstraints.HORIZONTAL);
        place(totalsBox, moneyRow("Tiền thừa", change), 0, 5, 1.0, GridBagConstraints.HORIZONTAL);
        JPanel totalsCard = titledCard("Tổng tiền", totalsBox);

        JTable table = new JTable(cart);
        table.setRowHeight(30);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(center);
        table.getColumnModel().getColumn(2).setCellRenderer(center);
        for (int moneyColumn = 3; moneyColumn <= 5; moneyColumn++)
            table.getColumnModel().getColumn(moneyColumn).setCellRenderer(UiFormat.moneyRenderer());
        final JButton cellEdit = new JButton("Sửa");
        final JButton cellDelete = new JButton("Xóa");
        final JPanel actionCell = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        actionCell.add(cellEdit);
        actionCell.add(cellDelete);
        table.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                    boolean focused, int row, int column) {
                actionCell.setBackground(selected ? table.getSelectionBackground() : table.getBackground());
                actionCell.setOpaque(true);
                return actionCell;
            }
        });
        table.getColumnModel().getColumn(0).setPreferredWidth(70);
        table.getColumnModel().getColumn(1).setPreferredWidth(190);
        table.getColumnModel().getColumn(2).setPreferredWidth(50);
        table.getColumnModel().getColumn(3).setPreferredWidth(100);
        table.getColumnModel().getColumn(4).setPreferredWidth(90);
        table.getColumnModel().getColumn(5).setPreferredWidth(110);
        table.getColumnModel().getColumn(6).setPreferredWidth(160);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                int viewRow = table.rowAtPoint(event.getPoint());
                int viewCol = table.columnAtPoint(event.getPoint());
                if (viewRow < 0 || viewCol != 6)
                    return;
                table.setRowSelectionInterval(viewRow, viewRow);
                Rectangle rect = table.getCellRect(viewRow, viewCol, false);
                if (event.getX() - rect.x < rect.width / 2)
                    editQuantity(cart, table, afterCartChange);
                else {
                    cart.removeRow(viewRow);
                    afterCartChange.run();
                }
            }
        });
        JPanel emptyPanel = new JPanel(new BorderLayout());
        emptyPanel.setOpaque(false);
        JLabel emptyLabel = new JLabel("Chưa có sản phẩm trong giỏ hàng", SwingConstants.CENTER);
        emptyLabel.setForeground(UiTheme.MUTED);
        emptyLabel.setFont(emptyLabel.getFont().deriveFont(Font.PLAIN, 16f));
        emptyPanel.add(emptyLabel, BorderLayout.CENTER);
        final JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(null);
        scroll.setPreferredSize(new Dimension(0, 140));
        final java.awt.CardLayout tableCards = new java.awt.CardLayout();
        final JPanel tableArea = new JPanel(tableCards);
        tableArea.setOpaque(false);
        tableArea.add(emptyPanel, "empty");
        tableArea.add(scroll, "table");
        tableCards.show(tableArea, "empty");

        JPanel cartCard = new JPanel(new BorderLayout());
        cartCard.setBackground(Color.WHITE);
        cartCard.setBorder(UiTheme.CARD);
        JPanel cartHead = new JPanel(new BorderLayout());
        cartHead.setOpaque(false);
        JLabel cartTitle = new JLabel("Giỏ hàng");
        cartTitle.setFont(cartTitle.getFont().deriveFont(Font.BOLD, 16f));
        final JLabel summaryLabel = new JLabel("0 món");
        summaryLabel.setForeground(UiTheme.MUTED);
        cartHead.add(cartTitle, BorderLayout.WEST);
        cartHead.add(summaryLabel, BorderLayout.EAST);
        cartCard.add(cartHead, BorderLayout.NORTH);
        cartCard.add(tableArea, BorderLayout.CENTER);

        cart.addTableModelListener(event -> {
            boolean empty = cart.getRowCount() == 0;
            tableCards.show(tableArea, empty ? "empty" : "table");
            summaryLabel.setText(cart.getRowCount() + " món");
            checkout.setEnabled(!empty);
            checkout.setToolTipText(empty ? "Giỏ hàng đang trống — hãy thêm sản phẩm" : null);
        });

        JPanel side = FormValidator.form();
        side.setOpaque(false);
        side.add(addCard);
        side.add(customerCard);
        side.add(promoCard);
        side.add(payCard);
        side.add(totalsCard);
        JPanel rightWrap = new JPanel(new BorderLayout());
        rightWrap.setOpaque(false);
        rightWrap.add(side, BorderLayout.NORTH);
        rightWrap.setPreferredSize(new Dimension(370, 1));

        JPanel leftCol = new JPanel(new GridBagLayout());
        leftCol.setOpaque(false);
        GridBagConstraints leftConstraints = new GridBagConstraints();
        leftConstraints.gridx = 0;
        leftConstraints.gridy = 0;
        leftConstraints.weightx = 1.0;
        leftConstraints.weighty = 1.0;
        leftConstraints.fill = GridBagConstraints.BOTH;
        leftConstraints.anchor = GridBagConstraints.NORTH;
        leftCol.add(cartCard, leftConstraints);

        JPanel columns = new JPanel(new GridBagLayout());
        columns.setOpaque(false);
        GridBagConstraints leftArea = new GridBagConstraints();
        leftArea.gridx = 0;
        leftArea.gridy = 0;
        leftArea.weightx = 1.7;
        leftArea.weighty = 1.0;
        leftArea.fill = GridBagConstraints.BOTH;
        leftArea.insets = new Insets(0, 0, 0, 14);
        GridBagConstraints rightArea = new GridBagConstraints();
        rightArea.gridx = 1;
        rightArea.gridy = 0;
        rightArea.weightx = 1.0;
        rightArea.weighty = 1.0;
        rightArea.fill = GridBagConstraints.BOTH;
        columns.add(leftCol, leftArea);
        columns.add(rightWrap, rightArea);
        panel.add(columns, BorderLayout.CENTER);
        return panel;
    }

    private static JLabel fieldError() {
        JLabel error = new JLabel(" ");
        error.setForeground(new Color(200, 30, 30));
        error.setFont(error.getFont().deriveFont(Font.PLAIN, 11f));
        return error;
    }

    private static void validateOnBlur(JTextComponent field, JLabel error, Supplier<String> rule) {
        final javax.swing.border.Border original = field.getBorder();
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent event) {
                String message = rule.get();
                boolean invalid = message != null;
                error.setText(invalid ? "<html>" + message + "</html>" : " ");
                error.setToolTipText(invalid ? message : null);
                field.setBorder(invalid
                        ? BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(new Color(200, 30, 30), 1),
                                BorderFactory.createEmptyBorder(3, 6, 3, 6))
                        : original);
            }
        });
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { cleared(); }
            @Override public void removeUpdate(DocumentEvent event) { cleared(); }
            @Override public void changedUpdate(DocumentEvent event) { cleared(); }
            private void cleared() {
                error.setText(" ");
                error.setToolTipText(null);
                field.setBorder(original);
            }
        });
    }

    private static JLabel posLabel(String text) {
        JLabel label = new JLabel(text);
        label.setPreferredSize(new Dimension(140, label.getPreferredSize().height));
        return label;
    }

    private static JPanel titledCard(String title, JComponent body) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(UiTheme.CARD);
        JPanel wrapper = new JPanel(new BorderLayout(0, 10));
        wrapper.setOpaque(false);
        JLabel heading = new JLabel(title);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 16f));
        wrapper.add(heading, BorderLayout.NORTH);
        wrapper.add(body, BorderLayout.CENTER);
        card.add(wrapper, BorderLayout.CENTER);
        return card;
    }

    private static JPanel moneyRow(String name, JLabel value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        JLabel caption = new JLabel(name);
        caption.setForeground(UiTheme.MUTED);
        value.setHorizontalAlignment(SwingConstants.RIGHT);
        value.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 12));
        row.add(caption, BorderLayout.WEST);
        row.add(value, BorderLayout.EAST);
        return row;
    }

    private static JPanel totalRow(JLabel value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UiTheme.BORDER),
                BorderFactory.createEmptyBorder(8, 0, 4, 0)));
        JLabel caption = new JLabel("TỔNG THANH TOÁN");
        caption.setFont(caption.getFont().deriveFont(Font.BOLD, 15f));
        value.setHorizontalAlignment(SwingConstants.RIGHT);
        row.add(caption, BorderLayout.WEST);
        row.add(value, BorderLayout.EAST);
        return row;
    }

    private static JToggleButton segment(String text) {
        JToggleButton button = new JToggleButton(text);
        button.setFont(button.getFont().deriveFont(Font.PLAIN, 13f));
        button.setMargin(new Insets(6, 4, 6, 4));
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        button.setBackground(Color.WHITE);
        button.setForeground(UiTheme.NAVY);
        button.addItemListener(event -> {
            if (button.isSelected()) {
                button.setBackground(UiTheme.BLUE);
                button.setForeground(Color.WHITE);
                button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(UiTheme.BLUE, 1, true),
                        BorderFactory.createEmptyBorder(6, 8, 6, 8)));
            } else {
                button.setBackground(Color.WHITE);
                button.setForeground(UiTheme.NAVY);
                button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(UiTheme.BORDER, 1, true),
                        BorderFactory.createEmptyBorder(6, 8, 6, 8)));
            }
        });
        return button;
    }

    private JPanel settings() {
        JPanel panel = page();
        JPanel form = new JPanel(new GridLayout(0, 1, 0, 10));
        form.setBackground(Color.WHITE);
        form.setBorder(UiTheme.CARD);
        final JTextField store = new JTextField();
        final JTextField phone = new JTextField();
        final JTextField vat = new JTextField("10");
        JTextArea address = new JTextArea(3, 20);
        JButton save = new JButton("Lưu cấu hình");
        save.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                try {
                    BigDecimal rate = new BigDecimal(vat.getText().trim());
                    if (rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(new BigDecimal("100")) > 0)
                        throw new NumberFormatException();
                    String storeValue = store.getText().trim();
                    String phoneValue = phone.getText().trim();
                    String addressValue = address.getText().trim();
                    if (storeValue.length() == 0 || phoneValue.length() == 0)
                        throw new NumberFormatException();
                    luuCauHinh(storeValue, addressValue, phoneValue, rate);
                } catch (NumberFormatException exception) {
                    JOptionPane.showMessageDialog(PosFrame.this, "VAT phải từ 0 đến 100.", "Dữ liệu không hợp lệ",
                            JOptionPane.WARNING_MESSAGE);
                }
            }
        });
        form.add(labeled("Tên cửa hàng", store));
        form.add(labeled("Số điện thoại", phone));
        form.add(labeled("VAT mặc định (%)", vat));
        form.add(labeled("Địa chỉ", new JScrollPane(address)));
        form.add(save);
        panel.add(form, BorderLayout.NORTH);
        new SwingWorker<CauHinhCuaHang, Void>() {
            @Override
            protected CauHinhCuaHang doInBackground() throws Exception {
                return new SettingsService().lay(actor);
            }
            @Override
            protected void done() {
                if (actor == null) return;
                try {
                    CauHinhCuaHang value = get();
                    if (value != null) {
                        store.setText(value.getTenCuaHang());
                        phone.setText(value.getSoDienThoai());
                        vat.setText(value.getTyLeVat().toPlainString());
                        address.setText(value.getDiaChi());
                    }
                } catch (Exception exception) {
                    status.setText("Không tải được cấu hình cửa hàng.");
                }
            }
        }.execute();
        return panel;
    }

    private void luuCauHinh(final String store, final String address, final String phone, final BigDecimal rate) {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                CauHinhCuaHang value = new CauHinhCuaHang();
                value.setTenCuaHang(store);
                value.setDiaChi(address);
                value.setSoDienThoai(phone);
                value.setTyLeVat(rate);
                new SettingsService().luu(actor, value);
                return null;
            }
            @Override
            protected void done() {
                if (actor == null) return;
                try {
                    get();
                    status.setText("Đã lưu cấu hình; VAT áp dụng giao dịch tương lai.");
                } catch (Exception exception) {
                    showServiceError("Không lưu được cấu hình", exception);
                }
            }
        }.execute();
    }

    private DefaultListCellRenderer productRenderer() {
        return new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof SanPham) {
                    SanPham product = (SanPham) value;
                    setText(product.getMaSanPham() + " · " + product.getTenSanPham() + " · " + product.getGiaBan());
                }
                return this;
            }
        };
    }

    private DefaultListCellRenderer customerRenderer() {
        return new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value == null)
                    setText("Khách lẻ");
                else if (value instanceof KhachHang) {
                    KhachHang customer = (KhachHang) value;
                    setText(customer.getMaKhachHang() + " · " + customer.getHoTen());
                }
                return this;
            }
        };
    }

    private DefaultListCellRenderer promotionRenderer() {
        return new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof KhuyenMai) {
                    KhuyenMai promotion = (KhuyenMai) value;
                    setText(promotion.getMaKhuyenMai() + " · " + promotion.getTenKhuyenMai() + " · "
                            + promotion.getTyLeGiam() + "%");
                }
                return this;
            }
        };
    }

    private void findProducts(final String keyword, final JComboBox<SanPham> products, final boolean notify) {
        findProducts(keyword, products, notify, null);
    }

    private void findProducts(final String keyword, final JComboBox<SanPham> products, final boolean notify,
            final JTextField source) {
        new SwingWorker<List<SanPham>, Void>() {
            @Override
            protected List<SanPham> doInBackground() throws Exception {
                return new MasterDataService().timSanPham(actor, keyword, null, "DANG_KINH_DOANH", null, null);
            }
            @Override
            protected void done() {
                if (actor == null) return;
                if (source != null && !source.getText().trim().equals(keyword)) return;
                try {
                    List<SanPham> found = get();
                    String previousKey = null;
                    Object previous = products.getSelectedItem();
                    if (previous instanceof SanPham)
                        previousKey = ((SanPham) previous).getMaSanPham();
                    products.removeAllItems();
                    for (SanPham product : found) products.addItem(product);
                    int select = -1;
                    if (previousKey != null) {
                        for (int i = 0; i < products.getItemCount(); i++) {
                            SanPham item = products.getItemAt(i);
                            if (item != null && previousKey.equals(item.getMaSanPham())) {
                                select = i;
                                break;
                            }
                        }
                    }
                    if (select < 0)
                        select = found.isEmpty() ? -1 : 0;
                    products.setSelectedIndex(select);
                    if (source != null && !keyword.isEmpty() && products.getItemCount() > 0)
                        products.setPopupVisible(true);
                    if (notify && found.isEmpty())
                        JOptionPane.showMessageDialog(PosFrame.this, "Không tìm thấy sản phẩm đang kinh doanh.");
                } catch (Exception exception) {
                    if (notify)
                        JOptionPane.showMessageDialog(PosFrame.this, "Không tải được sản phẩm.");
                }
            }
        }.execute();
    }

    private void findCustomers(final String keyword, final JComboBox<KhachHang> customers,
            final JLabel loyalty, final Runnable afterLoad) {
        findCustomers(keyword, customers, loyalty, afterLoad, null);
    }

    private void findCustomers(final String keyword, final JComboBox<KhachHang> customers,
            final JLabel loyalty, final Runnable afterLoad, final JTextField source) {
        new SwingWorker<List<KhachHang>, Void>() {
            @Override
            protected List<KhachHang> doInBackground() throws Exception {
                return new MasterDataService().timKhachHang(actor, keyword, "HOAT_DONG");
            }
            @Override
            protected void done() {
                if (actor == null) return;
                if (source != null && !source.getText().trim().equals(keyword)) return;
                try {
                    List<KhachHang> found = get();
                    String previousKey = null;
                    Object previous = customers.getSelectedItem();
                    if (previous instanceof KhachHang)
                        previousKey = ((KhachHang) previous).getMaKhachHang();
                    customers.removeAllItems();
                    customers.addItem(null);
                    for (KhachHang customer : found) customers.addItem(customer);
                    int select = 0;
                    if (previousKey != null) {
                        for (int i = 1; i < customers.getItemCount(); i++) {
                            KhachHang item = customers.getItemAt(i);
                            if (item != null && previousKey.equals(item.getMaKhachHang())) {
                                select = i;
                                break;
                            }
                        }
                    } else if (found.size() == 1 || (keyword.length() > 0 && !found.isEmpty())) {
                        select = 1;
                    }
                    customers.setSelectedIndex(select);
                    if (source != null && !keyword.isEmpty() && customers.getItemCount() > 1)
                        customers.setPopupVisible(true);
                    showLoyalty((KhachHang) customers.getSelectedItem(), loyalty);
                    if (afterLoad != null) afterLoad.run();
                } catch (Exception exception) {
                    JOptionPane.showMessageDialog(PosFrame.this, "Không tải được khách hàng.");
                }
            }
        }.execute();
    }

    private static void showLoyalty(KhachHang customer, JLabel loyalty) {
        if (customer == null) {
            loyalty.setText("Khách lẻ · không dùng điểm");
            return;
        }
        long points = customer.getDiemTichLuy();
        String tier = points >= 1000000 ? "Vàng 10%"
                : points >= 200000      ? "Bạc 5%"
                : points >= 50000       ? "Đồng 3%"
                                        : "Chưa xếp hạng 0%";
        loyalty.setText("Hạng " + tier + " · điểm thưởng " + UiFormat.money(customer.getDiemHienCo()));
    }

    private void addCartLine(DefaultTableModel cart, SanPham product, int quantity) {
        for (int row = 0; row < cart.getRowCount(); row++) {
            if (product.getMaSanPham().equals(cart.getValueAt(row, 0))) {
                int amount = ((Number) cart.getValueAt(row, 2)).intValue() + quantity;
                cart.setValueAt(Integer.valueOf(amount), row, 2);
                cart.setValueAt(product.getGiaBan().multiply(BigDecimal.valueOf(amount)), row, 5);
                return;
            }
        }
        cart.addRow(new Object[] {product.getMaSanPham(), product.getTenSanPham(), Integer.valueOf(quantity),
                product.getGiaBan(), BigDecimal.ZERO, product.getGiaBan().multiply(BigDecimal.valueOf(quantity))});
    }

    private void editQuantity(DefaultTableModel cart, JTable table, final Runnable onApplied) {
        int row = table.getSelectedRow();
        if (row < 0)
            return;
        JSpinner quantity =
                new JSpinner(new SpinnerNumberModel(((Number) cart.getValueAt(row, 2)).intValue(), 1, 9999, 1));
        if (JOptionPane.showConfirmDialog(this, quantity, "Số lượng", JOptionPane.OK_CANCEL_OPTION)
                != JOptionPane.OK_OPTION)
            return;
        final int amount = ((Number) quantity.getValue()).intValue();
        final String productId = String.valueOf(cart.getValueAt(row, 0));
        final java.util.Map<String, Integer> needed = cartQuantities(cart);
        needed.put(productId, Integer.valueOf(amount));
        checkStock(needed, new Runnable() {
            @Override
            public void run() {
                for (int r = 0; r < cart.getRowCount(); r++) {
                    if (productId.equals(String.valueOf(cart.getValueAt(r, 0)))) {
                        cart.setValueAt(Integer.valueOf(amount), r, 2);
                        cart.setValueAt(((BigDecimal) cart.getValueAt(r, 3))
                                .multiply(BigDecimal.valueOf(amount)), r, 5);
                        break;
                    }
                }
                if (onApplied != null)
                    onApplied.run();
            }
        });
    }

    private java.util.Map<String, Integer> cartQuantities(DefaultTableModel cart) {
        java.util.Map<String, Integer> quantities = new java.util.LinkedHashMap<String, Integer>();
        for (int row = 0; row < cart.getRowCount(); row++) {
            String id = String.valueOf(cart.getValueAt(row, 0));
            int qty = ((Number) cart.getValueAt(row, 2)).intValue();
            Integer current = quantities.get(id);
            quantities.put(id, Integer.valueOf((current == null ? 0 : current.intValue()) + qty));
        }
        return quantities;
    }

    private void checkStock(final java.util.Map<String, Integer> needed, final Runnable onOk) {
        new SwingWorker<java.util.Map<String, Integer>, Void>() {
            @Override
            protected java.util.Map<String, Integer> doInBackground() throws Exception {
                java.util.Map<String, Integer> shortage = new java.util.LinkedHashMap<String, Integer>();
                SaleService sales = new SaleService();
                for (java.util.Map.Entry<String, Integer> entry : needed.entrySet()) {
                    int available = sales.tonKho(actor, entry.getKey());
                    if (entry.getValue().intValue() > available)
                        shortage.put(entry.getKey(), Integer.valueOf(available));
                }
                return shortage;
            }

            @Override
            protected void done() {
                if (actor == null) return;
                try {
                    java.util.Map<String, Integer> shortage = get();
                    if (!shortage.isEmpty()) {
                        StringBuilder message = new StringBuilder("Sản phẩm trong kho không đủ:\n");
                        for (java.util.Map.Entry<String, Integer> entry : shortage.entrySet())
                            message.append(entry.getKey()).append(" (tồn: ").append(entry.getValue())
                                    .append(")\n");
                        JOptionPane.showMessageDialog(PosFrame.this, message.toString(),
                                "Không đủ tồn kho", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    onOk.run();
                } catch (Exception exception) {
                    JOptionPane.showMessageDialog(PosFrame.this, "Không kiểm tra được tồn kho.");
                }
            }
        }.execute();
    }

    private void loadPromotions(DefaultTableModel cart, final JComboBox<KhuyenMai> promotions,
            final Runnable afterLoad) {
        final java.util.Set<String> productIds = new LinkedHashSet<String>();
        java.math.BigDecimal subtotal = java.math.BigDecimal.ZERO;
        for (int row = 0; row < cart.getRowCount(); row++) {
            productIds.add(String.valueOf(cart.getValueAt(row, 0)));
            subtotal = subtotal.add(((java.math.BigDecimal) cart.getValueAt(row, 3))
                    .multiply(java.math.BigDecimal.valueOf(((Number) cart.getValueAt(row, 2)).intValue())));
        }
        final java.math.BigDecimal cartSubtotal = subtotal;
        new SwingWorker<List<KhuyenMai>, Void>() {
            @Override
            protected List<KhuyenMai> doInBackground() throws Exception {
                return new SaleService().activePromotions(actor, productIds, cartSubtotal, LocalDateTime.now());
            }
            @Override
            protected void done() {
                if (actor == null) return;
                try {
                    promotions.removeAllItems();
                    for (KhuyenMai promotion : get()) promotions.addItem(promotion);
                    if (afterLoad != null)
                        afterLoad.run();
                } catch (Exception exception) {
                    JOptionPane.showMessageDialog(PosFrame.this, "Không tải được khuyến mãi đủ điều kiện.");
                }
            }
        }.execute();
    }

    private YeuCauThanhToan paymentRequest(DefaultTableModel cart, KhachHang customer, KhuyenMai promotion,
            boolean tier, boolean event, String rewards, String method, String tender, boolean electronicSuccess) {
        return paymentRequest(cart, customer, promotion, tier, event, rewards, method, tender,
                electronicSuccess, false);
    }

    private YeuCauThanhToan paymentRequest(DefaultTableModel cart, KhachHang customer, KhuyenMai promotion,
            boolean tier, boolean event, String rewards, String method, String tender, boolean electronicSuccess,
            boolean quiet) {
        if (cart.getRowCount() == 0) {
            if (!quiet)
                JOptionPane.showMessageDialog(this, "Giỏ hàng chưa có sản phẩm.");
            return null;
        }
        final long rewardPoints;
        try {
            String digits = rewards.replaceAll("[.,\\s]", "");
            rewardPoints = Long.parseLong(digits.length() == 0 ? "0" : digits);
        } catch (NumberFormatException exception) {
            if (!quiet)
                JOptionPane.showMessageDialog(this, "Điểm thưởng phải là số nguyên không âm.");
            return null;
        }
        if (rewardPoints < 0 || rewardPoints % 1000 != 0) {
            if (!quiet)
                JOptionPane.showMessageDialog(this, "Điểm thưởng phải là bội số của 1000 (1000 điểm = 1000đ).");
            return null;
        }
        if (rewardPoints > 0) {
            if (customer == null) {
                if (!quiet)
                    JOptionPane.showMessageDialog(this, "Khách lẻ không dùng điểm thưởng. Vui lòng chọn khách hàng.");
                return null;
            }
            if (rewardPoints > customer.getDiemHienCo()) {
                if (!quiet)
                    JOptionPane.showMessageDialog(this, "Điểm thưởng vượt quá số dư (còn "
                            + customer.getDiemHienCo() + " điểm).");
                return null;
            }
        }
        if ((tier && customer == null) || (event && promotion == null)) {
            if (!quiet)
                JOptionPane.showMessageDialog(this, "Chọn giảm giá hợp lệ và khách hàng khi dùng hạng.");
            return null;
        }
        YeuCauThanhToan request = new YeuCauThanhToan();
        request.setMaKhachHang(customer == null ? SaleService.KHACH_LE : customer.getMaKhachHang());
        request.setLoaiGiamGia(event ? "KHUYEN_MAI" : tier ? "HANG_THANH_VIEN" : "KHONG");
        request.setMaKhuyenMai(event ? promotion.getMaKhuyenMai() : null);
        request.setDiemThuongSuDung(rewardPoints);
        request.setPhuongThucThanhToan(method);
        if ("TIEN_MAT".equals(method)) {
            if (tender != null && tender.trim().length() > 0) {
                try {
                    request.setTienKhachDua(new BigDecimal(tender.replaceAll("[.\\s]", "")));
                } catch (NumberFormatException exception) {
                    if (!quiet)
                        JOptionPane.showMessageDialog(this, "Nhập tiền khách đưa hợp lệ.");
                    return null;
                }
            }
        } else {
            request.setThanhToanDienTuThanhCong(electronicSuccess);
            request.setMaThamChieuThanhToan("MOCK-" + method + "-" + (electronicSuccess ? "OK" : "FAIL"));
        }
        List<DongThanhToan> lines = new ArrayList<DongThanhToan>();
        for (int row = 0; row < cart.getRowCount(); row++) {
            DongThanhToan line = new DongThanhToan();
            line.setMaSanPham(String.valueOf(cart.getValueAt(row, 0)));
            line.setSoLuong(((Number) cart.getValueAt(row, 2)).intValue());
            lines.add(line);
        }
        request.setDongHang(lines);
        return request;
    }

    private void refreshQuote(DefaultTableModel cart, JComboBox<KhachHang> customers,
            JComboBox<KhuyenMai> promotions, JRadioButton noDiscount, JRadioButton tierDiscount,
            JRadioButton eventDiscount, JTextField rewards, JComboBox<String> method,
            JTextField tender, JComboBox<String> result, JLabel subtotal, JLabel discount,
            JLabel reward, JLabel vat, JLabel total, JLabel change) {
        if (cart.getRowCount() == 0)
            return;
        YeuCauThanhToan request = paymentRequest(cart, (KhachHang) customers.getSelectedItem(),
                (KhuyenMai) promotions.getSelectedItem(), tierDiscount.isSelected(),
                eventDiscount.isSelected(), rewards.getText().trim(),
                String.valueOf(method.getSelectedItem()), tender.getText().trim(),
                "THÀNH CÔNG".equals(result.getSelectedItem()), true);
        if (request != null)
            quote(request, cart, subtotal, discount, reward, vat, total, change);
    }

    private void quote(final YeuCauThanhToan request, final DefaultTableModel cart, final JLabel subtotal,
            final JLabel discount, final JLabel reward, final JLabel vat, final JLabel total, final JLabel change) {
        new SwingWorker<BaoGia, Void>() {
            @Override
            protected BaoGia doInBackground() throws Exception {
                return new SaleService().baoGia(actor, request);
            }
            @Override
            protected void done() {
                if (actor == null) return;
                try {
                    HoaDon invoice = get().getHoaDon();
                    int lines = Math.min(cart.getRowCount(), invoice.getChiTiet().size());
                    for (int row = 0; row < lines; row++) {
                        cart.setValueAt(invoice.getChiTiet().get(row).getTienGiamDong(), row, 4);
                        cart.setValueAt(invoice.getChiTiet().get(row).getThanhTien(), row, 5);
                    }
                    subtotal.setText(UiFormat.money(invoice.getTongGiaGoc()) + " đ");
                    discount.setText(UiFormat.money(invoice.getTienGiam()) + " đ");
                    reward.setText(UiFormat.money(invoice.getTienGiamDiem()) + " đ");
                    vat.setText(UiFormat.money(invoice.getTienVat()) + " đ");
                    total.setText(UiFormat.money(invoice.getTongThanhToan()) + " đ");
                    change.setText(UiFormat.money(invoice.getTienThua()) + " đ");
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        }.execute();
    }

    private void checkout(final YeuCauThanhToan request, final DefaultTableModel cart, final JTextField productSearch,
            final JComboBox<SanPham> products, final JSpinner quantity, final JTextField customerSearch,
            final JComboBox<KhachHang> customers, final JComboBox<KhuyenMai> promotions, final JRadioButton noDiscount,
            final JTextField rewards, final JTextField tender, final JComboBox<String> method,
            final JComboBox<String> result, final JLabel subtotal, final JLabel discount, final JLabel reward,
            final JLabel vat, final JLabel total, final JLabel change, final JLabel loyalty) {
        new SwingWorker<Object[], Void>() {
            @Override
            protected Object[] doInBackground() throws Exception {
                HoaDon paid = new SaleService().thanhToan(actor, request);
                HoaDon invoice = new SaleService().chiTietHoaDon(actor, paid.getMaHoaDon());
                CauHinhCuaHang store = new SettingsService().layChoHoaDon(actor);
                return new Object[] {invoice, store};
            }
            @Override
            protected void done() {
                if (actor == null) return;
                try {
                    Object[] checkoutResult = get();
                    HoaDon invoice = (HoaDon) checkoutResult[0];
                    resetSale(cart, productSearch, products, quantity, customerSearch, customers, promotions,
                            noDiscount, rewards, tender, method, result, subtotal, discount, reward, vat, total, change,
                            loyalty);
                    status.setText(
                            "Đã thanh toán " + invoice.getMaHoaDon() + ": " + invoice.getTongThanhToan() + " đ.");
                    JOptionPane.showMessageDialog(PosFrame.this,
                            "Thêm hóa đơn thành công: " + invoice.getMaHoaDon()
                                    + "\nTổng thanh toán: " + invoice.getTongThanhToan() + " đ.",
                            "Thanh toán thành công", JOptionPane.INFORMATION_MESSAGE);
                    previewReceipt(new ReceiptPrintable((CauHinhCuaHang) checkoutResult[1], invoice));
                } catch (Exception exception) {
                    showServiceError("Thanh toán không thành công", exception);
                }
            }
        }.execute();
    }

    private static void resetSale(DefaultTableModel cart, JTextField productSearch, JComboBox<SanPham> products,
            JSpinner quantity, JTextField customerSearch, JComboBox<KhachHang> customers,
            JComboBox<KhuyenMai> promotions, JRadioButton noDiscount, JTextField rewards, JTextField tender,
            JComboBox<String> method, JComboBox<String> result, JLabel subtotal, JLabel discount, JLabel reward,
            JLabel vat, JLabel total, JLabel change, JLabel loyalty) {
        cart.setRowCount(0);
        productSearch.setText("");
        products.removeAllItems();
        quantity.setValue(Integer.valueOf(1));
        customerSearch.setText("");
        customers.removeAllItems();
        customers.addItem(null);
        customers.setSelectedIndex(0);
        promotions.removeAllItems();
        noDiscount.setSelected(true);
        rewards.setText("0");
        tender.setText("");
        method.setSelectedItem("TIEN_MAT");
        result.setSelectedItem("THÀNH CÔNG");
        subtotal.setText("0");
        discount.setText("0");
        reward.setText("0");
        vat.setText("0");
        total.setText("0");
        change.setText("0");
        showLoyalty(null, loyalty);
    }

    private void previewReceipt(final ReceiptPrintable receipt) {
        JTextArea text = new JTextArea();
        text.setEditable(false);
        for (String line : receipt.lines()) text.append(line + "\n");
        JScrollPane scroll = new JScrollPane(text);
        scroll.setPreferredSize(DialogActions.PREVIEW_TEXT);
        if (DialogActions.confirm(this, scroll, "Xem trước hóa đơn"))
            exportReceiptPdf(receipt);
    }

    private void exportReceiptPdf(final ReceiptPrintable receipt) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Lưu hóa đơn PDF");
        String invoiceId = receipt.lines().size() > 3
                ? receipt.lines().get(3).replace("HÓA ĐƠN ", "") : "hoa-don";
        chooser.setSelectedFile(new File("hoa-don-" + invoiceId + ".pdf"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
            return;
        File selected = chooser.getSelectedFile();
        if (!selected.getName().toLowerCase().endsWith(".pdf"))
            selected = new File(selected.getParentFile(), selected.getName() + ".pdf");
        final File target = selected;
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                ReceiptPdfExporter.export(receipt, target);
                return null;
            }
            @Override
            protected void done() {
                if (actor == null) return;
                try {
                    get();
                    JOptionPane.showMessageDialog(PosFrame.this,
                            "Đã lưu hóa đơn PDF:\n" + target.getAbsolutePath(),
                            "Xuất hóa đơn thành công", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception exception) {
                    showServiceError("Xuất hóa đơn PDF không thành công", exception);
                }
            }
        }.execute();
    }

    private void showServiceError(String title, Exception exception) {
        Throwable cause = exception;
        while (cause.getCause() != null
                && (cause instanceof java.util.concurrent.ExecutionException
                    || cause instanceof java.util.concurrent.CompletionException))
            cause = cause.getCause();
        cause.printStackTrace();
        String message;
        if (cause instanceof IllegalArgumentException || cause instanceof IllegalStateException
                || cause instanceof SecurityException) {
            message = cause.getMessage();
        } else if (cause instanceof java.sql.SQLException) {
            message = "Không thể kết nối hoặc cập nhật dữ liệu. Vui lòng thử lại.";
        } else if (cause instanceof RuntimeException && cause.getMessage() != null
                && !cause.getMessage().trim().isEmpty()) {
            message = cause.getClass().getSimpleName() + ": " + cause.getMessage();
        } else {
            message = "Đã xảy ra lỗi khi xử lý. Vui lòng thử lại.";
        }
        if (message == null || message.trim().isEmpty())
            message = "Đã xảy ra lỗi khi xử lý. Vui lòng thử lại.";
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }

    private void addQuoteRefreshListener(final JTextField field, final Runnable refresh,
            final boolean integer) {
        final Timer debounce = new Timer(400, event -> refresh.run());
        debounce.setRepeats(false);
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                refreshIfValid();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                refreshIfValid();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                refreshIfValid();
            }

            private void refreshIfValid() {
                String value = field.getText().trim();
                if (value.isEmpty()) {
                    debounce.restart();
                    return;
                }
                try {
                    if (integer)
                        Long.parseLong(value.replaceAll("[.,\\s]", ""));
                    else
                        new BigDecimal(value.replaceAll("[.\\s]", ""));
                    debounce.restart();
                } catch (NumberFormatException ignored) {
                    // Keep the last valid quote while the user is entering a number.
                }
            }
        });
    }

    private JPanel page() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(UiTheme.BACKGROUND);
        return panel;
    }

    private void showPage(String key, JButton button) {
        cards.show(pages, key);
        title.setText(pageTitle(key));
        if (selectedNavigation != null)
            selectedNavigation.setBackground(UiTheme.NAVY);
        if (button != null && Boolean.TRUE.equals(button.getClientProperty("navigationButton"))) {
            selectedNavigation = button;
            button.setBackground(UiTheme.BLUE);
        }
        pages.revalidate();
        pages.repaint();
        status.setText("Sẵn sàng. Ctrl+F tìm kiếm · Ctrl+S lưu · Esc đóng hộp thoại.");
    }

    private String pageTitle(String key) {
        if ("sale".equals(key))
            return "Bán hàng tại quầy";
        if ("customers".equals(key))
            return "Khách hàng và CRM";
        if ("products".equals(key))
            return "Sản phẩm";
        if ("categories".equals(key))
            return "Loại sản phẩm";
        if ("stock".equals(key))
            return "Tồn kho";
        if ("invoices".equals(key))
            return "Hóa đơn";
        if ("staff".equals(key))
            return "Nhân viên";
        if ("accounts".equals(key))
            return "Tài khoản";
        if ("suppliers".equals(key))
            return "Nhà cung cấp";
        if ("purchases".equals(key))
            return "Phiếu nhập";
        if ("adjustments".equals(key))
            return "Phiếu điều chỉnh tồn kho";
        if ("promotions".equals(key))
            return "Khuyến mãi";
        if ("reports".equals(key))
            return "Báo cáo";
        if ("settings".equals(key))
            return "Cấu hình cửa hàng";
        return "Tổng quan";
    }

    private void bindShortcuts() {
        JComponent root = getRootPane();
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "sale", new Runnable() {
            public void run() {
                showPage("sale", null);
            }
        });
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "escape", new Runnable() {
            public void run() {
                closeDialog();
            }
        });
    }

    private void bind(JComponent root, KeyStroke key, String name, final Runnable action) {
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(key, name);
        root.getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                action.run();
            }
        });
    }

    private void closeDialog() {
        for (java.awt.Window window : java.awt.Window.getWindows())
            if (window instanceof javax.swing.JDialog && window.isShowing()) {
                window.dispose();
                return;
            }
    }
}
