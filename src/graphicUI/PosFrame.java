package graphicUI;

import components.DialogActions;
import components.FormValidator;
import components.ReceiptPrintable;
import components.ReceiptPrinting;
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
import service.DichVuDangNhap;
import service.NguoiDung;
import service.BaoGia;
import service.DongThanhToan;
import service.DichVuBanHang;
import service.ThongKeCa;
import service.YeuCauThanhToan;
import service.DichVuCauHinh;
import service.DichVuDanhMuc;

@SuppressWarnings("serial")
public final class PosFrame extends JFrame {
    private static final String MANAGER = "QUAN_LY";
    private static final String CASHIER = "THU_NGAN";
    private final java.awt.CardLayout cards = new java.awt.CardLayout();
    private final JPanel pages = new JPanel(cards);
    private final JLabel title = new JLabel();
    private final JLabel status = new JLabel("Sẵn sàng.");
    private String role = CASHIER;
    private NguoiDung actor;
    private JButton selectedNavigation;

    private final java.util.Map<String, SaleUiState> savedSales = new java.util.HashMap<String, SaleUiState>();
    private final java.util.Map<DefaultTableModel, SaleUiState> cartStates = new java.util.IdentityHashMap<DefaultTableModel, SaleUiState>();
    private SaleUiState saleState;

    private static final class SaleUiState {
        private long revision;
        private long promotionRevision;
        private boolean applyingQuote;
        private boolean quoted;
        private boolean busy;
        private boolean locked;
        private YeuCauThanhToan pending;
        private JPanel panel;
        private JButton btnThanhToan;
        private JLabel lblTongThanhToan;
        private Runnable refresh;
        private final java.util.Map<Component, Boolean> enabled = new java.util.IdentityHashMap<Component, Boolean>();
    }

    private static final class ScrollableFields extends JPanel implements javax.swing.Scrollable {
        private static final long serialVersionUID = 1L;
        private ScrollableFields() { setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) { return 20; }
        @Override public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) { return Math.max(20, visible.height - 20); }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    private void huyBaoGia(SaleUiState state) {
        if (state == null || state.applyingQuote) return;
        state.revision++;
        state.quoted = false;
        if (state.lblTongThanhToan != null) state.lblTongThanhToan.setText("Đang tính…");
        if (state.btnThanhToan != null && state.pending == null) state.btnThanhToan.setEnabled(false);
    }

    private void khoaBanHang(SaleUiState state, boolean locked) {
        if (state.locked != locked) {
            state.locked = locked;
            if (locked) luuTrangThaiKichHoat(state.panel, state);
            else {
                for (java.util.Map.Entry<Component, Boolean> entry : state.enabled.entrySet())
                    entry.getKey().setEnabled(entry.getValue().booleanValue());
                state.enabled.clear();
            }
        }
        if (state.btnThanhToan != null) {
            state.btnThanhToan.setText(state.pending != null && !state.busy ? "Kiểm tra lại thanh toán" : "Thanh toán");
            state.btnThanhToan.setEnabled(!state.busy && (state.pending != null || state.quoted));
        }
    }

    private void luuTrangThaiKichHoat(Component component, SaleUiState state) {
        state.enabled.put(component, Boolean.valueOf(component.isEnabled()));
        component.setEnabled(false);
        if (component instanceof java.awt.Container)
            for (Component child : ((java.awt.Container) component).getComponents()) luuTrangThaiKichHoat(child, state);
    }

    private void hetPhienDangNhap(NguoiDung expired) {
        if (actor != expired) return;
        if (saleState != null) { huyBaoGia(saleState); saleState.promotionRevision++; }
        actor = null;
        role = CASHIER;
        hienDangNhap();
        revalidate();
        repaint();
        JOptionPane.showMessageDialog(this,
                "Phiên đăng nhập đã hết hiệu lực. Giỏ hàng được giữ riêng cho tài khoản này. Hãy đăng nhập lại.");
    }

    public PosFrame() {
        super("POS Cửa Hàng Tiện Lợi");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1024, 680));
        setSize(1280, 780);
        setLocationByPlatform(true);
        hienDangNhap();
    }

    private void hienDangNhap() {
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
                new SwingWorker<NguoiDung, Void>() {
                    @Override
                    protected NguoiDung doInBackground() throws Exception {
                        return new DichVuDangNhap().dangNhapNguoiDung(loginName, loginPassword);
                    }
                    @Override
                    protected void done() {
                        login.setEnabled(true);
                        Arrays.fill(loginPassword, '\0');
                        password.setText("");
                        try {
                            actor = get();
                            final NguoiDung loggedIn = actor;
                            loggedIn.onInvalidated(() -> javax.swing.SwingUtilities.invokeLater(() -> hetPhienDangNhap(loggedIn)));
                            saleState = savedSales.computeIfAbsent(actor.getTenDangNhap(), key -> new SaleUiState());
                            huyBaoGia(saleState);
                            role = actor.getVaiTro();
                            showShell();
                        } catch (Exception exception) {
                            baoLoiDangNhap(exception);
                        }
                    }

                    private void baoLoiDangNhap(Exception exception) {
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
        getRootPane().setDefaultButton(null);
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
        JLabel user = new JLabel(tenNguoiDung());
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
                doiMatKhau();
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
                if (saleState != null && saleState.busy) return;
                huyBaoGia(saleState);
                actor = null;
                role = CASHIER;
                hienDangNhap();
                revalidate();
                repaint();
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
        pages.add(saleState.panel == null ? trangBanHang() : saleState.panel, "sale");
        if (saleState.refresh != null) saleState.refresh.run();
        pages.add(new KhachHangPanel(actor), "customers");
        pages.add(new SanPhamPanel(actor, MANAGER.equals(role)), "products");
        pages.add(new LoaiSanPhamPanel(actor, MANAGER.equals(role)), "categories");
        pages.add(new TonKhoPanel(actor), "stock");
        pages.add(new HoaDonPanel(actor), "invoices");
        if (MANAGER.equals(role)) {
            pages.add(new NhanVienPanel(actor), "staff");
            pages.add(new TaiKhoanPanel(actor), "accounts");
            pages.add(new NhaCungCapPanel(actor), "suppliers");
            pages.add(new PhieuNhapPanel(actor), "purchases");
            pages.add(new TonKhoPanel(actor, true), "adjustments");
            pages.add(new KhuyenMaiPanel(actor), "promotions");
            pages.add(trangCauHinh(), "settings");
        }
    }

    private JPanel dashboard() {
        if (MANAGER.equals(role))
            return new ThongKePanel(actor);
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
                return new DichVuBanHang().thongKeCa(actor);
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

    private void doiMatKhau() {
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
                new DichVuDangNhap().doiMatKhau(actor, oldPassword, newPassword);
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

    private String tenNguoiDung() {
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

    private JPanel trangBanHang() {
        final SaleUiState state = saleState;
        JPanel panel = page();
        state.panel = panel;
        final DefaultTableModel gioHang =
                new DefaultTableModel(new String[] {"Mã", "Sản phẩm", "SL", "Đơn giá", "Giảm dòng", "Thành tiền", "Thao tác"}, 0) {
                    @Override
                    public boolean isCellEditable(int row, int column) {
                        return false;
                    }
                };
        cartStates.put(gioHang, state);
        final JComboBox<SanPham> dsSanPham = new JComboBox<SanPham>();
        final JComboBox<KhachHang> dsKhachHang = new JComboBox<KhachHang>();
        final JComboBox<KhuyenMai> dsKhuyenMai = new JComboBox<KhuyenMai>();
        dsSanPham.setRenderer(rendererSanPham());
        dsSanPham.setPreferredSize(new Dimension(170, dsSanPham.getPreferredSize().height));
        dsKhachHang.setRenderer(rendererKhachHang());
        dsKhachHang.setPreferredSize(new Dimension(170, dsKhachHang.getPreferredSize().height));
        dsKhachHang.addItem(null);
        dsKhuyenMai.setRenderer(rendererKhuyenMai());
        dsKhuyenMai.getAccessibleContext().setAccessibleName("Khuyến mãi áp dụng");

        JPanel productForm = new JPanel(new GridBagLayout());
        productForm.setOpaque(false);
        final JTextField sanPham = new JTextField();
        final JSpinner soLuong = new JSpinner(new SpinnerNumberModel(1, 1, 9999, 1));
        JLabel productLabel = posLabel("Mã / tên sản phẩm");
        productLabel.setLabelFor(sanPham);
        productLabel.setDisplayedMnemonic(KeyEvent.VK_M);
        place(productForm, productLabel, 0, 0, 0, GridBagConstraints.NONE);
        sanPham.setPreferredSize(new Dimension(130, sanPham.getPreferredSize().height));
        placeWest(productForm, sanPham, 1, 0);
        sanPham.getAccessibleContext().setAccessibleName("Mã hoặc tên sản phẩm");
        sanPham.setToolTipText("Gõ để tìm ngay — kết quả tự hiện trong danh sách");
        final Timer timKiemSanPham = new Timer(300,
                event -> timSanPham(sanPham.getText().trim(), dsSanPham, false, sanPham));
        timKiemSanPham.setRepeats(false);
        sanPham.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { timKiemSanPham.restart(); }
            @Override public void removeUpdate(DocumentEvent event) { timKiemSanPham.restart(); }
            @Override public void changedUpdate(DocumentEvent event) { timKiemSanPham.restart(); }
        });
        dsSanPham.getAccessibleContext().setAccessibleName("Sản phẩm tìm được");
        place(productForm, dsSanPham, 2, 0, 1.0, GridBagConstraints.HORIZONTAL);
        JLabel quantityLabel = posLabel("Số lượng");
        quantityLabel.setLabelFor(soLuong);
        quantityLabel.setDisplayedMnemonic(KeyEvent.VK_S);
        place(productForm, quantityLabel, 0, 1, 0, GridBagConstraints.NONE);
        placeWest(productForm, soLuong, 1, 1);
        soLuong.getAccessibleContext().setAccessibleName("Số lượng sản phẩm");
        final JRadioButton khongGiamGia = new JRadioButton("Không giảm", true);
        final JRadioButton giamTheoHang = new JRadioButton("Giảm hạng");
        final JRadioButton giamKhuyenMai = new JRadioButton("Khuyến mãi sự kiện");
        ButtonGroup discounts = new ButtonGroup();
        discounts.add(khongGiamGia);
        discounts.add(giamTheoHang);
        discounts.add(giamKhuyenMai);
        final JTextField diemThuong = new JTextField("0", 6);
        final JComboBox<String> phuongThuc = new JComboBox<String>(new String[] {"TIEN_MAT", "THE", "QR"});
        final JComboBox<String> result = new JComboBox<String>(new String[] {"THÀNH CÔNG", "THẤT BẠI"});
        final JTextField tienKhachDua = new JTextField(10);
        final JLabel tamTinh = new JLabel("0"), tienGiam = new JLabel("0"),
                     tienGiamDiem = new JLabel("0"), vat = new JLabel("0"), tongThanhToan = new JLabel("0"),
                     tienThua = new JLabel("0");
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
                if (state.locked || state.busy) return;
                final SanPham selected = (SanPham) dsSanPham.getSelectedItem();
                if (selected == null) {
                    JOptionPane.showMessageDialog(PosFrame.this, "Tìm và chọn sản phẩm đang kinh doanh.");
                    return;
                }
                final int amount = ((Number) soLuong.getValue()).intValue();
                final java.util.Map<String, Integer> needed = cartQuantities(gioHang);
                final Integer previous = needed.get(selected.getMaSanPham());
                needed.put(selected.getMaSanPham(), Integer.valueOf((previous == null ? 0 : previous.intValue()) + amount));
                kiemTraTonKho(needed, new Runnable() {
                    @Override
                    public void run() {
                        if (state.locked || state.busy) return;
                        themDongGioHang(gioHang, selected, amount);
                        sanPham.setText("");
                        status.setText(previous == null
                                ? "Đã thêm " + selected.getTenSanPham() + " vào giỏ."
                                : selected.getTenSanPham() + " đã có trong giỏ — đã cộng dồn số lượng.");
                        capNhatBaoGia(gioHang, dsKhachHang, dsKhuyenMai, khongGiamGia, giamTheoHang, giamKhuyenMai,
                                diemThuong, phuongThuc, tienKhachDua, result, tamTinh, tienGiam, tienGiamDiem, vat, tongThanhToan, tienThua);
                        taiKhuyenMai(gioHang, dsKhuyenMai, new Runnable() {
                            @Override
                            public void run() {
                                capNhatBaoGia(gioHang, dsKhachHang, dsKhuyenMai, khongGiamGia, giamTheoHang, giamKhuyenMai,
                                        diemThuong, phuongThuc, tienKhachDua, result, tamTinh, tienGiam, tienGiamDiem, vat, tongThanhToan, tienThua);
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
        productForm.removeAll();
        place(productForm, productLabel, 0, 0, 1.0, GridBagConstraints.HORIZONTAL);
        place(productForm, sanPham, 0, 1, 1.0, GridBagConstraints.HORIZONTAL);
        place(productForm, dsSanPham, 0, 2, 1.0, GridBagConstraints.HORIZONTAL);
        place(productForm, quantityLabel, 0, 3, 1.0, GridBagConstraints.HORIZONTAL);
        place(productForm, soLuong, 0, 4, 1.0, GridBagConstraints.HORIZONTAL);
        place(productForm, add, 0, 5, 1.0, GridBagConstraints.HORIZONTAL);
        JPanel addCard = titledCard("Thêm sản phẩm", productForm);

        final Runnable refreshQuoteAction = new Runnable() {
            @Override
            public void run() {
                capNhatBaoGia(gioHang, dsKhachHang, dsKhuyenMai, khongGiamGia, giamTheoHang, giamKhuyenMai,
                        diemThuong, phuongThuc, tienKhachDua, result, tamTinh, tienGiam, tienGiamDiem, vat, tongThanhToan, tienThua);
            }
        };
        final Runnable syncRewards = new Runnable() {
            @Override
            public void run() {
                boolean hasCustomer = dsKhachHang.getSelectedItem() != null;
                if (!hasCustomer)
                    diemThuong.setText("0");
            }
        };
        final Runnable khachHangThayDoi = new Runnable() {
            @Override
            public void run() {
                syncRewards.run();
                refreshQuoteAction.run();
            }
        };
        final Runnable sauKhiDoiGioHang = new Runnable() {
            @Override
            public void run() {
                refreshQuoteAction.run();
                taiKhuyenMai(gioHang, dsKhuyenMai, new Runnable() {
                    @Override
                    public void run() {
                        refreshQuoteAction.run();
                    }
                });
            }
        };

        JPanel customerForm = new JPanel(new GridBagLayout());
        customerForm.setOpaque(false);
        final JTextField khachHang = new JTextField();
        final JLabel lblDiemThanhVien = new JLabel("Khách lẻ · không dùng điểm");
        lblDiemThanhVien.setForeground(UiTheme.MUTED);
        JLabel customerLabel = posLabel("Tìm khách hàng");
        customerLabel.setLabelFor(khachHang);
        customerLabel.setDisplayedMnemonic(KeyEvent.VK_K);
        place(customerForm, customerLabel, 0, 0, 0, GridBagConstraints.NONE);
        khachHang.setPreferredSize(new Dimension(150, khachHang.getPreferredSize().height));
        placeWest(customerForm, khachHang, 1, 0);
        khachHang.getAccessibleContext().setAccessibleName("Mã hoặc tên khách hàng");
        khachHang.setToolTipText("Gõ để tìm ngay — kết quả tự hiện trong danh sách");
        final Timer timKiemKhachHang = new Timer(300,
                event -> timKhachHang(khachHang.getText().trim(), dsKhachHang, lblDiemThanhVien, khachHangThayDoi, khachHang));
        timKiemKhachHang.setRepeats(false);
        khachHang.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { timKiemKhachHang.restart(); }
            @Override public void removeUpdate(DocumentEvent event) { timKiemKhachHang.restart(); }
            @Override public void changedUpdate(DocumentEvent event) { timKiemKhachHang.restart(); }
        });
        dsKhachHang.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                hienDiemThanhVien((KhachHang) dsKhachHang.getSelectedItem(), lblDiemThanhVien);
                syncRewards.run();
                capNhatBaoGia(gioHang, dsKhachHang, dsKhuyenMai, khongGiamGia, giamTheoHang, giamKhuyenMai,
                        diemThuong, phuongThuc, tienKhachDua, result, tamTinh, tienGiam, tienGiamDiem, vat, tongThanhToan, tienThua);
            }
        });
        dsKhachHang.getAccessibleContext().setAccessibleName("Khách hàng tìm được");
        place(customerForm, dsKhachHang, 2, 0, 1.0, GridBagConstraints.HORIZONTAL);
        GridBagConstraints loyaltyConstraints = new GridBagConstraints();
        loyaltyConstraints.gridx = 1;
        loyaltyConstraints.gridy = 1;
        loyaltyConstraints.gridwidth = 2;
        loyaltyConstraints.weightx = 1.0;
        loyaltyConstraints.fill = GridBagConstraints.HORIZONTAL;
        loyaltyConstraints.anchor = GridBagConstraints.WEST;
        loyaltyConstraints.insets = new Insets(2, 4, 2, 4);
        customerForm.add(lblDiemThanhVien, loyaltyConstraints);
        customerForm.removeAll();
        place(customerForm, customerLabel, 0, 0, 1.0, GridBagConstraints.HORIZONTAL);
        place(customerForm, khachHang, 0, 1, 1.0, GridBagConstraints.HORIZONTAL);
        place(customerForm, dsKhachHang, 0, 2, 1.0, GridBagConstraints.HORIZONTAL);
        place(customerForm, lblDiemThanhVien, 0, 3, 1.0, GridBagConstraints.HORIZONTAL);
        JPanel customerCard = titledCard("Thông tin khách hàng", customerForm);
        syncRewards.run();
        timSanPham("", dsSanPham, false);

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
            @Override public void actionPerformed(ActionEvent event) { khongGiamGia.doClick(); }
        });
        segTier.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent event) { giamTheoHang.doClick(); }
        });
        segEvent.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent event) { giamKhuyenMai.doClick(); }
        });
        segRow.add(segNone);
        segRow.add(segTier);
        segRow.add(segEvent);
        place(promoBox, segRow, 1, 0, 1.0, GridBagConstraints.HORIZONTAL);
        place(promoBox, dsKhuyenMai, 1, 1, 1.0, GridBagConstraints.HORIZONTAL);
        JLabel rewardsLabel = posLabel("Điểm dùng");
        rewardsLabel.setLabelFor(diemThuong);
        rewardsLabel.setDisplayedMnemonic(KeyEvent.VK_D);
        diemThuong.getAccessibleContext().setAccessibleName("Điểm thưởng sử dụng");
        place(promoBox, rewardsLabel, 0, 2, 0, GridBagConstraints.NONE);
        placeWest(promoBox, diemThuong, 1, 2);
        JLabel rewardsError = fieldError();
        place(promoBox, rewardsError, 1, 3, 1.0, GridBagConstraints.HORIZONTAL);
        validateOnBlur(diemThuong, rewardsError, () -> {
            String text = diemThuong.getText().trim();
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
        methodLabel.setLabelFor(phuongThuc);
        methodLabel.setDisplayedMnemonic(KeyEvent.VK_P);
        phuongThuc.getAccessibleContext().setAccessibleName("Phương thức thanh toán");
        JLabel tenderLabel = posLabel("Tiền khách đưa");
        tenderLabel.setLabelFor(tienKhachDua);
        tenderLabel.setDisplayedMnemonic(KeyEvent.VK_I);
        tienKhachDua.getAccessibleContext().setAccessibleName("Tiền khách đưa");
        JPanel payFields = new JPanel(new GridBagLayout());
        payFields.setOpaque(false);
        place(payFields, methodLabel, 0, 0, 0, GridBagConstraints.NONE);
        place(payFields, phuongThuc, 1, 0, 1.0, GridBagConstraints.HORIZONTAL);
        JLabel resultLabel = posLabel("Kết quả");
        resultLabel.setLabelFor(result);
        result.getAccessibleContext().setAccessibleName("Kết quả thanh toán điện tử");
        place(payFields, resultLabel, 0, 1, 0, GridBagConstraints.NONE);
        place(payFields, result, 1, 1, 1.0, GridBagConstraints.HORIZONTAL);
        place(payFields, tenderLabel, 0, 2, 0, GridBagConstraints.NONE);
        place(payFields, tienKhachDua, 1, 2, 1.0, GridBagConstraints.HORIZONTAL);
        JLabel tenderError = fieldError();
        place(payFields, tenderError, 1, 3, 1.0, GridBagConstraints.HORIZONTAL);
        validateOnBlur(tienKhachDua, tenderError, () -> {
            String text = tienKhachDua.getText().trim();
            if (text.isEmpty())
                return null;
            try {
                return new BigDecimal(text).signum() < 0 ? "Tiền khách đưa không được âm." : null;
            } catch (NumberFormatException exception) {
                return "Tiền khách đưa phải là số.";
            }
        });
        JButton btnThanhToan = new JButton("Thanh toán");
        state.btnThanhToan = btnThanhToan;
        state.lblTongThanhToan = tongThanhToan;
        btnThanhToan.setBackground(UiTheme.BLUE);
        btnThanhToan.setForeground(Color.WHITE);
        btnThanhToan.setOpaque(true);
        btnThanhToan.setBorderPainted(false);
        btnThanhToan.setFocusPainted(false);
        btnThanhToan.setFont(btnThanhToan.getFont().deriveFont(Font.BOLD, 16f));
        btnThanhToan.setMargin(new Insets(8, 0, 8, 0));
        btnThanhToan.setEnabled(false);
        btnThanhToan.setToolTipText("Giỏ hàng đang trống — hãy thêm sản phẩm");
        btnThanhToan.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "submitCheckout");
        btnThanhToan.getActionMap().put("submitCheckout", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) { btnThanhToan.doClick(); }
        });
        btnThanhToan.addActionListener(event -> {
            if (state.busy || actor == null || actor.isInvalid()) return;
            YeuCauThanhToan yeuCau = state.pending;
            if (yeuCau == null) {
                yeuCau = yeuCauThanhToan(gioHang, (KhachHang) dsKhachHang.getSelectedItem(),
                        (KhuyenMai) dsKhuyenMai.getSelectedItem(), giamTheoHang.isSelected(), giamKhuyenMai.isSelected(),
                        diemThuong.getText().trim(), String.valueOf(phuongThuc.getSelectedItem()), tienKhachDua.getText().trim(),
                        "THÀNH CÔNG".equals(result.getSelectedItem()));
                if (yeuCau == null) return;
                state.busy = true;
                khoaBanHang(state, true);
                if (JOptionPane.showConfirmDialog(PosFrame.this, "Xác nhận thanh toán?", "Thanh toán",
                        JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                    state.busy = false;
                    khoaBanHang(state, false);
                    return;
                }
            }
            thanhToan(yeuCau, gioHang, sanPham, dsSanPham, soLuong, khachHang, dsKhachHang, dsKhuyenMai, khongGiamGia,
                    diemThuong, tienKhachDua, phuongThuc, result, tamTinh, tienGiam, tienGiamDiem, vat, tongThanhToan, tienThua, lblDiemThanhVien);
        });
        JPanel payBody = new JPanel(new BorderLayout(0, 8));
        payBody.setOpaque(false);
        payBody.add(payFields, BorderLayout.CENTER);

        JPanel payCard = titledCard("Thanh toán", payBody);

        final Runnable refresh = new Runnable() {
            @Override
            public void run() {
                capNhatBaoGia(gioHang, dsKhachHang, dsKhuyenMai, khongGiamGia, giamTheoHang, giamKhuyenMai,
                        diemThuong, phuongThuc, tienKhachDua, result, tamTinh, tienGiam, tienGiamDiem, vat, tongThanhToan, tienThua);
            }
        };
        khongGiamGia.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                segNone.setSelected(true);
                refresh.run();
            }
        });
        giamTheoHang.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                segTier.setSelected(true);
                refresh.run();
            }
        });
        giamKhuyenMai.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                segEvent.setSelected(true);
                refresh.run();
            }
        });
        dsKhuyenMai.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                refresh.run();
            }
        });
        phuongThuc.addActionListener(new ActionListener() {
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
        themListenerCapNhatBaoGia(diemThuong, refresh, true);
        themListenerCapNhatBaoGia(tienKhachDua, refresh, false);
        tongThanhToan.setFont(tongThanhToan.getFont().deriveFont(Font.BOLD, 18f));
        tongThanhToan.setForeground(UiTheme.BLUE);
        JPanel totalsBox = new JPanel(new GridBagLayout());
        totalsBox.setOpaque(false);
        place(totalsBox, moneyRow("Tạm tính", tamTinh), 0, 0, 1.0, GridBagConstraints.HORIZONTAL);
        place(totalsBox, moneyRow("Giảm giá", tienGiam), 0, 1, 1.0, GridBagConstraints.HORIZONTAL);
        place(totalsBox, moneyRow("Điểm sử dụng", tienGiamDiem), 0, 2, 1.0, GridBagConstraints.HORIZONTAL);
        place(totalsBox, moneyRow("VAT", vat), 0, 3, 1.0, GridBagConstraints.HORIZONTAL);
        place(totalsBox, totalRow(tongThanhToan), 0, 4, 1.0, GridBagConstraints.HORIZONTAL);
        place(totalsBox, moneyRow("Tiền thừa", tienThua), 0, 5, 1.0, GridBagConstraints.HORIZONTAL);
        JPanel totalsCard = titledCard("Tổng tiền", totalsBox);

        JTable table = new JTable(gioHang);
        table.setRowHeight(30);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
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
                if (state.locked || state.busy) return;
                int viewRow = table.rowAtPoint(event.getPoint());
                int viewCol = table.columnAtPoint(event.getPoint());
                if (viewRow < 0 || viewCol != 6)
                    return;
                table.setRowSelectionInterval(viewRow, viewRow);
                Rectangle rect = table.getCellRect(viewRow, viewCol, false);
                if (event.getX() - rect.x < rect.width / 2)
                    suaSoLuong(gioHang, table, sauKhiDoiGioHang);
                else {
                    gioHang.removeRow(viewRow);
                    sauKhiDoiGioHang.run();
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

        gioHang.addTableModelListener(event -> {
            boolean empty = gioHang.getRowCount() == 0;
            tableCards.show(tableArea, empty ? "empty" : "table");
            summaryLabel.setText(gioHang.getRowCount() + " món");
            if (!state.applyingQuote && event.getColumn() != 4 && event.getColumn() != 5) {
                huyBaoGia(state);
                if (empty) {
                    for (JLabel label : new JLabel[] {tamTinh, tienGiam, tienGiamDiem, vat, tongThanhToan, tienThua}) label.setText("0");
                }
            }
            btnThanhToan.setEnabled(!state.busy && (state.pending != null || (!empty && state.quoted)));
            btnThanhToan.setToolTipText(empty ? "Giỏ hàng đang trống — hãy thêm sản phẩm" : null);
        });

        JPanel side = new ScrollableFields();
        side.setOpaque(false);
        side.add(addCard);
        side.add(customerCard);
        side.add(promoCard);
        side.add(payCard);

        JPanel rightWrap = new JPanel(new BorderLayout());
        rightWrap.setOpaque(false);
        JScrollPane fieldsScroll = new JScrollPane(side, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        fieldsScroll.setBorder(null);
        fieldsScroll.getVerticalScrollBar().setUnitIncrement(20);
        rightWrap.add(fieldsScroll, BorderLayout.CENTER);
        JPanel paymentFooter = new JPanel(new BorderLayout(0, 6));
        paymentFooter.setOpaque(false);
        paymentFooter.add(totalsCard, BorderLayout.CENTER);
        paymentFooter.add(btnThanhToan, BorderLayout.SOUTH);
        rightWrap.add(paymentFooter, BorderLayout.SOUTH);
        rightWrap.setPreferredSize(new Dimension(390, 1));
        rightWrap.setMinimumSize(new Dimension(330, 1));

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
        state.refresh = refresh;
        // Tab navigation scrolls off-screen inputs into view.
        java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("focusOwner", event -> {
            Object focused = event.getNewValue();
            if (focused instanceof JComponent && javax.swing.SwingUtilities.isDescendingFrom((JComponent) focused, side))
                ((JComponent) focused).scrollRectToVisible(new Rectangle(0, 0,
                        ((JComponent) focused).getWidth(), ((JComponent) focused).getHeight()));
        });
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

    private JPanel trangCauHinh() {
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
                return new DichVuCauHinh().lay(actor);
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
                new DichVuCauHinh().luu(actor, value);
                return null;
            }
            @Override
            protected void done() {
                if (actor == null) return;
                try {
                    get();
                    status.setText("Đã lưu cấu hình; VAT áp dụng giao dịch tương lai.");
                } catch (Exception exception) {
                    baoLoiDichVu("Không lưu được cấu hình", exception);
                }
            }
        }.execute();
    }

    private DefaultListCellRenderer rendererSanPham() {
        return new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof SanPham) {
                    SanPham sanPham = (SanPham) value;
                    setText(sanPham.getMaSanPham() + " · " + sanPham.getTenSanPham() + " · " + sanPham.getGiaBan());
                }
                return this;
            }
        };
    }

    private DefaultListCellRenderer rendererKhachHang() {
        return new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value == null)
                    setText("Khách lẻ");
                else if (value instanceof KhachHang) {
                    KhachHang khachHang = (KhachHang) value;
                    setText(khachHang.getMaKhachHang() + " · " + khachHang.getHoTen());
                }
                return this;
            }
        };
    }

    private DefaultListCellRenderer rendererKhuyenMai() {
        return new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof KhuyenMai) {
                    KhuyenMai khuyenMai = (KhuyenMai) value;
                    setText(khuyenMai.getMaKhuyenMai() + " · " + khuyenMai.getTenKhuyenMai() + " · "
                            + khuyenMai.getTyLeGiam() + "%");
                }
                return this;
            }
        };
    }

    private void timSanPham(final String keyword, final JComboBox<SanPham> dsSanPham, final boolean notify) {
        timSanPham(keyword, dsSanPham, notify, null);
    }

    private void timSanPham(final String keyword, final JComboBox<SanPham> dsSanPham, final boolean notify,
            final JTextField source) {
        final NguoiDung requestActor = actor;
        final SaleUiState requestState = saleState;
        new SwingWorker<List<SanPham>, Void>() {
            @Override
            protected List<SanPham> doInBackground() throws Exception {
                return new DichVuDanhMuc().timSanPham(requestActor, keyword, null, "DANG_KINH_DOANH", null, null);
            }
            @Override
            protected void done() {
                if (actor != requestActor || requestState != saleState || (requestState != null && requestState.locked)) return;
                if (source != null && !source.getText().trim().equals(keyword)) return;
                try {
                    List<SanPham> found = get();
                    String previousKey = null;
                    Object previous = dsSanPham.getSelectedItem();
                    if (previous instanceof SanPham)
                        previousKey = ((SanPham) previous).getMaSanPham();
                    dsSanPham.removeAllItems();
                    for (SanPham sanPham : found) dsSanPham.addItem(sanPham);
                    int select = -1;
                    if (previousKey != null) {
                        for (int i = 0; i < dsSanPham.getItemCount(); i++) {
                            SanPham item = dsSanPham.getItemAt(i);
                            if (item != null && previousKey.equals(item.getMaSanPham())) {
                                select = i;
                                break;
                            }
                        }
                    }
                    if (select < 0)
                        select = found.isEmpty() ? -1 : 0;
                    dsSanPham.setSelectedIndex(select);
                    if (source != null && !keyword.isEmpty() && dsSanPham.getItemCount() > 0)
                        dsSanPham.setPopupVisible(true);
                    if (notify && found.isEmpty())
                        JOptionPane.showMessageDialog(PosFrame.this, "Không tìm thấy sản phẩm đang kinh doanh.");
                } catch (Exception exception) {
                    if (notify)
                        JOptionPane.showMessageDialog(PosFrame.this, "Không tải được sản phẩm.");
                }
            }
        }.execute();
    }

    private void timKhachHang(final String keyword, final JComboBox<KhachHang> dsKhachHang,
            final JLabel lblDiemThanhVien, final Runnable afterLoad) {
        timKhachHang(keyword, dsKhachHang, lblDiemThanhVien, afterLoad, null);
    }

    private void timKhachHang(final String keyword, final JComboBox<KhachHang> dsKhachHang,
            final JLabel lblDiemThanhVien, final Runnable afterLoad, final JTextField source) {
        final NguoiDung requestActor = actor;
        final SaleUiState requestState = saleState;
        new SwingWorker<List<KhachHang>, Void>() {
            @Override
            protected List<KhachHang> doInBackground() throws Exception {
                return new DichVuDanhMuc().timKhachHang(requestActor, keyword, "HOAT_DONG");
            }
            @Override
            protected void done() {
                if (actor != requestActor || requestState != saleState || (requestState != null && requestState.locked)) return;
                if (source != null && !source.getText().trim().equals(keyword)) return;
                try {
                    List<KhachHang> found = get();
                    String previousKey = null;
                    Object previous = dsKhachHang.getSelectedItem();
                    if (previous instanceof KhachHang)
                        previousKey = ((KhachHang) previous).getMaKhachHang();
                    dsKhachHang.removeAllItems();
                    dsKhachHang.addItem(null);
                    for (KhachHang khachHang : found) dsKhachHang.addItem(khachHang);
                    int select = 0;
                    if (previousKey != null) {
                        for (int i = 1; i < dsKhachHang.getItemCount(); i++) {
                            KhachHang item = dsKhachHang.getItemAt(i);
                            if (item != null && previousKey.equals(item.getMaKhachHang())) {
                                select = i;
                                break;
                            }
                        }
                    } else if (found.size() == 1 || (keyword.length() > 0 && !found.isEmpty())) {
                        select = 1;
                    }
                    dsKhachHang.setSelectedIndex(select);
                    if (source != null && !keyword.isEmpty() && dsKhachHang.getItemCount() > 1)
                        dsKhachHang.setPopupVisible(true);
                    hienDiemThanhVien((KhachHang) dsKhachHang.getSelectedItem(), lblDiemThanhVien);
                    if (afterLoad != null) afterLoad.run();
                } catch (Exception exception) {
                    JOptionPane.showMessageDialog(PosFrame.this, "Không tải được khách hàng.");
                }
            }
        }.execute();
    }

    private static void hienDiemThanhVien(KhachHang khachHang, JLabel lblDiemThanhVien) {
        if (khachHang == null) {
            lblDiemThanhVien.setText("Khách lẻ · không dùng điểm");
            return;
        }
        long diem = khachHang.getDiemTichLuy();
        String hangThanhVien = diem >= 1000000 ? "Vàng 10%"
                : diem >= 200000      ? "Bạc 5%"
                : diem >= 50000       ? "Đồng 3%"
                                        : "Chưa xếp hạng 0%";
        lblDiemThanhVien.setText("Hạng " + hangThanhVien + " · điểm thưởng " + UiFormat.money(khachHang.getDiemHienCo()));
    }

    private void themDongGioHang(DefaultTableModel gioHang, SanPham sanPham, int soLuong) {
        for (int row = 0; row < gioHang.getRowCount(); row++) {
            if (sanPham.getMaSanPham().equals(gioHang.getValueAt(row, 0))) {
                int amount = ((Number) gioHang.getValueAt(row, 2)).intValue() + soLuong;
                gioHang.setValueAt(Integer.valueOf(amount), row, 2);
                gioHang.setValueAt(sanPham.getGiaBan().multiply(BigDecimal.valueOf(amount)), row, 5);
                return;
            }
        }
        gioHang.addRow(new Object[] {sanPham.getMaSanPham(), sanPham.getTenSanPham(), Integer.valueOf(soLuong),
                sanPham.getGiaBan(), BigDecimal.ZERO, sanPham.getGiaBan().multiply(BigDecimal.valueOf(soLuong))});
    }

    private void suaSoLuong(DefaultTableModel gioHang, JTable table, final Runnable onApplied) {
        int row = table.getSelectedRow();
        if (row < 0)
            return;
        JSpinner soLuong =
                new JSpinner(new SpinnerNumberModel(((Number) gioHang.getValueAt(row, 2)).intValue(), 1, 9999, 1));
        if (JOptionPane.showConfirmDialog(this, soLuong, "Số lượng", JOptionPane.OK_CANCEL_OPTION)
                != JOptionPane.OK_OPTION)
            return;
        final int amount = ((Number) soLuong.getValue()).intValue();
        final String maSanPham = String.valueOf(gioHang.getValueAt(row, 0));
        final java.util.Map<String, Integer> needed = cartQuantities(gioHang);
        needed.put(maSanPham, Integer.valueOf(amount));
        kiemTraTonKho(needed, new Runnable() {
            @Override
            public void run() {
                for (int r = 0; r < gioHang.getRowCount(); r++) {
                    if (maSanPham.equals(String.valueOf(gioHang.getValueAt(r, 0)))) {
                        gioHang.setValueAt(Integer.valueOf(amount), r, 2);
                        gioHang.setValueAt(((BigDecimal) gioHang.getValueAt(r, 3))
                                .multiply(BigDecimal.valueOf(amount)), r, 5);
                        break;
                    }
                }
                if (onApplied != null)
                    onApplied.run();
            }
        });
    }

    private java.util.Map<String, Integer> cartQuantities(DefaultTableModel gioHang) {
        java.util.Map<String, Integer> soLuongTheoMa = new java.util.LinkedHashMap<String, Integer>();
        for (int row = 0; row < gioHang.getRowCount(); row++) {
            String id = String.valueOf(gioHang.getValueAt(row, 0));
            int qty = ((Number) gioHang.getValueAt(row, 2)).intValue();
            Integer current = soLuongTheoMa.get(id);
            soLuongTheoMa.put(id, Integer.valueOf((current == null ? 0 : current.intValue()) + qty));
        }
        return soLuongTheoMa;
    }

    private void kiemTraTonKho(final java.util.Map<String, Integer> needed, final Runnable onOk) {
        final NguoiDung requestActor = actor;
        final SaleUiState requestState = saleState;
        new SwingWorker<java.util.Map<String, Integer>, Void>() {
            @Override
            protected java.util.Map<String, Integer> doInBackground() throws Exception {
                java.util.Map<String, Integer> shortage = new java.util.LinkedHashMap<String, Integer>();
                DichVuBanHang sales = new DichVuBanHang();
                for (java.util.Map.Entry<String, Integer> entry : needed.entrySet()) {
                    int available = sales.tonKho(requestActor, entry.getKey());
                    if (entry.getValue().intValue() > available)
                        shortage.put(entry.getKey(), Integer.valueOf(available));
                }
                return shortage;
            }

            @Override
            protected void done() {
                if (actor != requestActor || requestState != saleState || (requestState != null && requestState.locked)) return;
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

    private void taiKhuyenMai(DefaultTableModel gioHang, final JComboBox<KhuyenMai> dsKhuyenMai,
            final Runnable afterLoad) {
        final SaleUiState state = cartStates.get(gioHang);
        if (state == null || state.locked || state != saleState || actor == null) return;
        final NguoiDung requestActor = actor;
        final long revision = ++state.promotionRevision;
        final java.util.Map<String, Integer> soLuongTheoMa = cartQuantities(gioHang);
        final java.util.Set<String> dsMaSanPham = new LinkedHashSet<String>();
        java.math.BigDecimal tamTinh = java.math.BigDecimal.ZERO;
        for (int row = 0; row < gioHang.getRowCount(); row++) {
            dsMaSanPham.add(String.valueOf(gioHang.getValueAt(row, 0)));
            tamTinh = tamTinh.add(((java.math.BigDecimal) gioHang.getValueAt(row, 3))
                    .multiply(java.math.BigDecimal.valueOf(((Number) gioHang.getValueAt(row, 2)).intValue())));
        }
        final java.math.BigDecimal tamTinhGioHang = tamTinh;
        new SwingWorker<List<KhuyenMai>, Void>() {
            @Override
            protected List<KhuyenMai> doInBackground() throws Exception {
                return new DichVuBanHang().khuyenMaiHieuLuc(requestActor, dsMaSanPham, tamTinhGioHang, LocalDateTime.now());
            }
            @Override
            protected void done() {
                if (actor != requestActor || state != saleState || state.locked
                        || state.promotionRevision != revision || !soLuongTheoMa.equals(cartQuantities(gioHang))) return;
                try {
                    dsKhuyenMai.removeAllItems();
                    for (KhuyenMai khuyenMai : get()) dsKhuyenMai.addItem(khuyenMai);
                    if (afterLoad != null)
                        afterLoad.run();
                } catch (Exception exception) {
                    JOptionPane.showMessageDialog(PosFrame.this, "Không tải được khuyến mãi đủ điều kiện.");
                }
            }
        }.execute();
    }

    private YeuCauThanhToan yeuCauThanhToan(DefaultTableModel gioHang, KhachHang khachHang, KhuyenMai khuyenMai,
            boolean hangThanhVien, boolean event, String diemThuong, String phuongThuc, String tienKhachDua, boolean thanhToanDienTuThanhCong) {
        return yeuCauThanhToan(gioHang, khachHang, khuyenMai, hangThanhVien, event, diemThuong, phuongThuc, tienKhachDua,
                thanhToanDienTuThanhCong, false);
    }

    private YeuCauThanhToan yeuCauThanhToan(DefaultTableModel gioHang, KhachHang khachHang, KhuyenMai khuyenMai,
            boolean hangThanhVien, boolean event, String diemThuong, String phuongThuc, String tienKhachDua, boolean thanhToanDienTuThanhCong,
            boolean quiet) {
        if (gioHang.getRowCount() == 0) {
            if (!quiet)
                JOptionPane.showMessageDialog(this, "Giỏ hàng chưa có sản phẩm.");
            return null;
        }
        final long diemSuDung;
        try {
            String digits = diemThuong.trim();
            diemSuDung = Long.parseLong(digits.length() == 0 ? "0" : digits);
        } catch (NumberFormatException exception) {
            if (!quiet)
                JOptionPane.showMessageDialog(this, "Điểm thưởng phải là số nguyên không âm.");
            return null;
        }
        if (diemSuDung < 0) {
            if (!quiet)
                JOptionPane.showMessageDialog(this, "Điểm thưởng phải là số nguyên không âm (1 điểm = 1đ).");
            return null;
        }
        if (diemSuDung > 0) {
            if (khachHang == null) {
                if (!quiet)
                    JOptionPane.showMessageDialog(this, "Khách lẻ không dùng điểm thưởng. Vui lòng chọn khách hàng.");
                return null;
            }
            if (diemSuDung > khachHang.getDiemHienCo()) {
                if (!quiet)
                    JOptionPane.showMessageDialog(this, "Điểm thưởng vượt quá số dư (còn "
                            + khachHang.getDiemHienCo() + " điểm).");
                return null;
            }
        }
        if ((hangThanhVien && khachHang == null) || (event && khuyenMai == null)) {
            if (!quiet)
                JOptionPane.showMessageDialog(this, "Chọn giảm giá hợp lệ và khách hàng khi dùng hạng.");
            return null;
        }
        YeuCauThanhToan yeuCau = new YeuCauThanhToan();
        yeuCau.setMaKhachHang(khachHang == null ? DichVuBanHang.KHACH_LE : khachHang.getMaKhachHang());
        yeuCau.setLoaiGiamGia(event ? "KHUYEN_MAI" : hangThanhVien ? "HANG_THANH_VIEN" : "KHONG");
        yeuCau.setMaKhuyenMai(event ? khuyenMai.getMaKhuyenMai() : null);
        yeuCau.setDiemThuongSuDung(diemSuDung);
        yeuCau.setPhuongThucThanhToan(phuongThuc);
        if ("TIEN_MAT".equals(phuongThuc)) {
            if (tienKhachDua != null && tienKhachDua.trim().length() > 0) {
                try {
                    yeuCau.setTienKhachDua(new BigDecimal(tienKhachDua.replaceAll("[.\\s]", "")));
                } catch (NumberFormatException exception) {
                    if (!quiet)
                        JOptionPane.showMessageDialog(this, "Nhập tiền khách đưa hợp lệ.");
                    return null;
                }
            }
        } else {
            yeuCau.setThanhToanDienTuThanhCong(thanhToanDienTuThanhCong);
            yeuCau.setMaThamChieuThanhToan("MOCK-" + phuongThuc + "-" + (thanhToanDienTuThanhCong ? "OK" : "FAIL"));
        }
        List<DongThanhToan> lines = new ArrayList<DongThanhToan>();
        for (int row = 0; row < gioHang.getRowCount(); row++) {
            DongThanhToan line = new DongThanhToan();
            line.setMaSanPham(String.valueOf(gioHang.getValueAt(row, 0)));
            line.setSoLuong(((Number) gioHang.getValueAt(row, 2)).intValue());
            lines.add(line);
        }
        yeuCau.setDongHang(lines);
        return yeuCau;
    }

    private void capNhatBaoGia(DefaultTableModel gioHang, JComboBox<KhachHang> dsKhachHang,
            JComboBox<KhuyenMai> dsKhuyenMai, JRadioButton khongGiamGia, JRadioButton giamTheoHang,
            JRadioButton giamKhuyenMai, JTextField diemThuong, JComboBox<String> phuongThuc,
            JTextField tienKhachDua, JComboBox<String> result, JLabel tamTinh, JLabel tienGiam,
            JLabel tienGiamDiem, JLabel vat, JLabel tongThanhToan, JLabel tienThua) {
        SaleUiState state = cartStates.get(gioHang);
        if (state == null || state.locked || actor == null || state != saleState) return;
        huyBaoGia(state);
        if (gioHang.getRowCount() == 0) { state.lblTongThanhToan.setText("0"); return; }
        YeuCauThanhToan yeuCau = yeuCauThanhToan(gioHang, (KhachHang) dsKhachHang.getSelectedItem(),
                (KhuyenMai) dsKhuyenMai.getSelectedItem(), giamTheoHang.isSelected(),
                giamKhuyenMai.isSelected(), diemThuong.getText().trim(),
                String.valueOf(phuongThuc.getSelectedItem()), tienKhachDua.getText().trim(),
                "THÀNH CÔNG".equals(result.getSelectedItem()), true);
        if (yeuCau != null)
            tinhBaoGia(yeuCau, gioHang, tamTinh, tienGiam, tienGiamDiem, vat, tongThanhToan, tienThua);
    }

    private void tinhBaoGia(final YeuCauThanhToan yeuCau, final DefaultTableModel gioHang, final JLabel tamTinh,
            final JLabel tienGiam, final JLabel tienGiamDiem, final JLabel vat, final JLabel tongThanhToan, final JLabel tienThua) {
        final SaleUiState state = cartStates.get(gioHang);
        final long revision = state.revision;
        final NguoiDung quotedActor = actor;
        final YeuCauThanhToan snapshot = yeuCau.copy();
        new SwingWorker<BaoGia, Void>() {
            @Override protected BaoGia doInBackground() throws Exception {
                return new DichVuBanHang().baoGia(quotedActor, snapshot);
            }
            @Override protected void done() {
                if (actor != quotedActor || state != saleState || state.revision != revision || state.locked) return;
                try {
                    HoaDon hoaDon = get().getHoaDon();
                    java.util.Map<String, entity.ChiTietHoaDon> details = new java.util.HashMap<String, entity.ChiTietHoaDon>();
                    for (entity.ChiTietHoaDon detail : hoaDon.getChiTiet()) details.put(detail.getMaSanPham(), detail);
                    state.applyingQuote = true;
                    try {
                        for (int row = 0; row < gioHang.getRowCount(); row++) {
                            entity.ChiTietHoaDon detail = details.get(String.valueOf(gioHang.getValueAt(row, 0)));
                            if (detail == null) return;
                            gioHang.setValueAt(detail.getTienGiamDong(), row, 4);
                            gioHang.setValueAt(detail.getThanhTien(), row, 5);
                        }
                    } finally { state.applyingQuote = false; }
                    tamTinh.setText(UiFormat.money(hoaDon.getTongGiaGoc()) + " đ");
                    tienGiam.setText(UiFormat.money(hoaDon.getTienGiam()) + " đ");
                    tienGiamDiem.setText(UiFormat.money(hoaDon.getTienGiamDiem()) + " đ");
                    vat.setText(UiFormat.money(hoaDon.getTienVat()) + " đ");
                    tongThanhToan.setText(UiFormat.money(hoaDon.getTongThanhToan()) + " đ");
                    tienThua.setText(UiFormat.money(hoaDon.getTienThua()) + " đ");
                    state.quoted = true;
                    state.btnThanhToan.setEnabled(!state.busy);
                } catch (Exception exception) {
                    state.quoted = false;
                    tongThanhToan.setText("Chưa có tổng hợp lệ");
                    state.btnThanhToan.setEnabled(false);
                    status.setText("Chưa thể tính tổng. Kiểm tra giỏ hàng và dữ liệu thanh toán.");
                }
            }
        }.execute();
    }

    private void thanhToan(final YeuCauThanhToan yeuCau, final DefaultTableModel gioHang, final JTextField timKiemSanPham,
            final JComboBox<SanPham> dsSanPham, final JSpinner soLuong, final JTextField timKiemKhachHang,
            final JComboBox<KhachHang> dsKhachHang, final JComboBox<KhuyenMai> dsKhuyenMai, final JRadioButton khongGiamGia,
            final JTextField diemThuong, final JTextField tienKhachDua, final JComboBox<String> phuongThuc,
            final JComboBox<String> result, final JLabel tamTinh, final JLabel tienGiam, final JLabel tienGiamDiem,
            final JLabel vat, final JLabel tongThanhToan, final JLabel tienThua, final JLabel lblDiemThanhVien) {
        final SaleUiState state = cartStates.get(gioHang);
        final NguoiDung nguoiDangThanhToan = actor;
        final boolean resolving = state.pending != null;
        state.pending = yeuCau;
        state.busy = true;
        huyBaoGia(state);
        khoaBanHang(state, true);
        new SwingWorker<HoaDon, Void>() {
            @Override protected HoaDon doInBackground() throws Exception {
                return new DichVuBanHang().thanhToan(nguoiDangThanhToan, yeuCau);
            }
            @Override protected void done() {
                state.busy = false;
                try {
                    HoaDon hoaDon = get();
                    // A committed invoice completes the cart before any receipt operation.
                    state.pending = null;
                    xoaGiaoDich(gioHang, timKiemSanPham, dsSanPham, soLuong, timKiemKhachHang, dsKhachHang, dsKhuyenMai,
                            khongGiamGia, diemThuong, tienKhachDua, phuongThuc, result, tamTinh, tienGiam, tienGiamDiem, vat, tongThanhToan, tienThua, lblDiemThanhVien);
                    huyBaoGia(state);
                    tongThanhToan.setText("0");
                    khoaBanHang(state, false);
                    if (actor != nguoiDangThanhToan || state != saleState) return;
                    status.setText("Đã thanh toán " + hoaDon.getMaHoaDon() + ": " + hoaDon.getTongThanhToan() + " đ.");
                    JOptionPane.showMessageDialog(PosFrame.this,
                            "Thanh toán thành công: " + hoaDon.getMaHoaDon(), "Thanh toán", JOptionPane.INFORMATION_MESSAGE);
                    taiHoaDonDaThanhToan(nguoiDangThanhToan, hoaDon);
                } catch (Exception exception) {
                    Throwable cause = exception;
                    while (cause.getCause() != null && cause instanceof java.util.concurrent.ExecutionException) cause = cause.getCause();
                    boolean uncertain = resolving || cause instanceof service.UnknownCheckoutStatusException;
                    if (!uncertain) state.pending = null;
                    khoaBanHang(state, uncertain);
                    if (actor != nguoiDangThanhToan || state != saleState) return;
                    if (uncertain) {
                        JOptionPane.showMessageDialog(PosFrame.this, cause.getMessage(),
                                "Cần kiểm tra kết quả thanh toán", JOptionPane.WARNING_MESSAGE);
                    } else {
                        baoLoiDichVu("Thanh toán không thành công", exception);
                        if (state.refresh != null) state.refresh.run();
                    }
                }
            }
        }.execute();
    }

    private void taiHoaDonDaThanhToan(final NguoiDung nguoiThanhToan, final HoaDon hoaDon) {
        new SwingWorker<CauHinhCuaHang, Void>() {
            @Override protected CauHinhCuaHang doInBackground() throws Exception {
                return new DichVuCauHinh().layChoHoaDon(nguoiThanhToan);
            }
            @Override protected void done() {
                if (actor != nguoiThanhToan) return;
                try { xemTruocHoaDon(new ReceiptPrintable(get(), hoaDon)); }
                catch (Exception exception) {
                    int choice = JOptionPane.showConfirmDialog(PosFrame.this,
                            "Thanh toán " + hoaDon.getMaHoaDon() + " đã thành công. Không tải được dữ liệu in. Thử lại hóa đơn?",
                            "Thanh toán thành công — lỗi hóa đơn in", JOptionPane.YES_NO_OPTION);
                    if (choice == JOptionPane.YES_OPTION) taiHoaDonDaThanhToan(nguoiThanhToan, hoaDon);
                }
            }
        }.execute();
    }

    private static void xoaGiaoDich(DefaultTableModel gioHang, JTextField timKiemSanPham, JComboBox<SanPham> dsSanPham,
            JSpinner soLuong, JTextField timKiemKhachHang, JComboBox<KhachHang> dsKhachHang,
            JComboBox<KhuyenMai> dsKhuyenMai, JRadioButton khongGiamGia, JTextField diemThuong, JTextField tienKhachDua,
            JComboBox<String> phuongThuc, JComboBox<String> result, JLabel tamTinh, JLabel tienGiam, JLabel tienGiamDiem,
            JLabel vat, JLabel tongThanhToan, JLabel tienThua, JLabel lblDiemThanhVien) {
        gioHang.setRowCount(0);
        timKiemSanPham.setText("");
        dsSanPham.removeAllItems();
        soLuong.setValue(Integer.valueOf(1));
        timKiemKhachHang.setText("");
        dsKhachHang.removeAllItems();
        dsKhachHang.addItem(null);
        dsKhachHang.setSelectedIndex(0);
        dsKhuyenMai.removeAllItems();
        khongGiamGia.setSelected(true);
        diemThuong.setText("0");
        tienKhachDua.setText("");
        phuongThuc.setSelectedItem("TIEN_MAT");
        result.setSelectedItem("THÀNH CÔNG");
        tamTinh.setText("0");
        tienGiam.setText("0");
        tienGiamDiem.setText("0");
        vat.setText("0");
        tongThanhToan.setText("0");
        tienThua.setText("0");
        hienDiemThanhVien(null, lblDiemThanhVien);
    }

    private void xemTruocHoaDon(final ReceiptPrintable banInHoaDon) {
        JTextArea text = new JTextArea();
        text.setEditable(false);
        for (String line : banInHoaDon.lines()) text.append(line + "\n");
        JScrollPane scroll = new JScrollPane(text);
        scroll.setPreferredSize(DialogActions.PREVIEW_TEXT);
        Object[] choices = {"In", "Xuất PDF", "Đóng"};
        int choice = JOptionPane.showOptionDialog(this, scroll, "Xem trước hóa đơn — thanh toán đã thành công",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, choices, choices[2]);
        if (choice == 0) ReceiptPrinting.print(this, banInHoaDon);
        else if (choice == 1) xuatHoaDonPdf(banInHoaDon);
    }

    private void xuatHoaDonPdf(final ReceiptPrintable banInHoaDon) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Lưu hóa đơn PDF");
        String invoiceId = banInHoaDon.invoiceId();
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
                ReceiptPdfExporter.export(banInHoaDon, target);
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
                    baoLoiDichVu("Thanh toán đã thành công; xuất PDF thất bại. Có thể thử lại từ danh sách hóa đơn.", exception);
                }
            }
        }.execute();
    }

    private void baoLoiDichVu(String title, Exception exception) {
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

    private void themListenerCapNhatBaoGia(final JTextField field, final Runnable refresh,
            final boolean integer) {
        final Timer debounce = new Timer(400, event -> refresh.run());
        debounce.setRepeats(false);
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                capNhatNeuHopLe();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                capNhatNeuHopLe();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                capNhatNeuHopLe();
            }

            private void capNhatNeuHopLe() {
                huyBaoGia(saleState);
                String value = field.getText().trim();
                if (value.isEmpty()) {
                    debounce.restart();
                    return;
                }
                try {
                    if (integer)
                        Long.parseLong(value);
                    else
                        new BigDecimal(value.replaceAll("[.\\s]", ""));
                    debounce.restart();
                } catch (NumberFormatException ignored) {
                    // The visible quote is invalid until the input is valid again.
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
