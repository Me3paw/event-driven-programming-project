package graphicUI;

import components.ReceiptPrintable;
import components.UiTheme;
import entity.HoaDon;
import entity.CauHinhCuaHang;
import entity.KhachHang;
import entity.KhuyenMai;
import entity.SanPham;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.print.PrinterJob;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
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
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import service.AuthService;
import service.Actor;
import service.BaoGia;
import service.DongThanhToan;
import service.SaleService;
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
    private JTextField globalSearch;
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
        JLabel caption = new JLabel("Bán hàng nội bộ · Java Swing · MariaDB");
        caption.setForeground(new Color(191, 219, 254));
        intro.add(caption, BorderLayout.SOUTH);
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
        form.add(labeled("Mật khẩu", password));
        JLabel note = new JLabel("AuthService sẽ xác thực tài khoản và trạng thái.");
        note.setForeground(UiTheme.MUTED);
        form.add(note);
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
                            JOptionPane.showMessageDialog(PosFrame.this,
                                    "Đăng nhập không hợp lệ hoặc không kết nối được cơ sở dữ liệu.", "Đăng nhập",
                                    JOptionPane.ERROR_MESSAGE);
                        }
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
            navigation(nav, "staff", "Nhân viên / tài khoản");
            navigation(nav, "suppliers", "Nhà cung cấp");
            navigation(nav, "purchases", "Phiếu nhập");
            navigation(nav, "adjustments", "Điều chỉnh tồn");
            navigation(nav, "promotions", "Khuyến mãi");
            navigation(nav, "reports", "Báo cáo");
            navigation(nav, "settings", "Cấu hình");
        }
        JScrollPane scroll = new JScrollPane(nav);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        side.add(scroll, BorderLayout.CENTER);
        JButton logout = new JButton("Đăng xuất");
        logout.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                actor = null;
                role = CASHIER;
                showLogin();
            }
        });
        side.add(logout, BorderLayout.SOUTH);
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
        UiTheme.title(title);
        top.add(title, BorderLayout.WEST);
        globalSearch = new JTextField();
        globalSearch.setToolTipText("Ctrl+F để tìm trong màn hình đang mở");
        globalSearch.getAccessibleContext().setAccessibleName("Tìm trong màn hình đang mở");
        globalSearch.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                searchActivePage();
            }
        });
        top.add(globalSearch, BorderLayout.CENTER);
        JLabel user = new JLabel(MANAGER.equals(role) ? "Quản lý" : "Thu ngân");
        user.setForeground(UiTheme.MUTED);
        top.add(user, BorderLayout.EAST);
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
            pages.add(new SupplierPanel(actor), "suppliers");
            pages.add(new PurchasePanel(actor), "purchases");
            pages.add(new InventoryPanel(actor, true), "adjustments");
            pages.add(new PromotionPanel(actor), "promotions");
            pages.add(new ReportPanel(actor), "reports");
            pages.add(settings(), "settings");
        }
    }

    private JPanel dashboard() {
        if (MANAGER.equals(role))
            return new ReportPanel(actor);
        JPanel panel = page();
        JPanel kpis = new JPanel(new GridLayout(1, 4, 12, 0));
        kpis.setOpaque(false);
        kpis.add(kpi("Bán hàng", "Sẵn sàng"));
        kpis.add(kpi("Khách hàng", "Tra cứu"));
        kpis.add(kpi("Sản phẩm", "Xem tồn"));
        kpis.add(kpi("Hóa đơn", "Của tôi"));
        panel.add(kpis, BorderLayout.NORTH);
        panel.add(new JLabel("Dùng điều hướng để bán hàng, tra cứu khách hàng, tồn kho và hóa đơn."),
                BorderLayout.CENTER);
        return panel;
    }

    private JPanel kpi(String text, String value) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(Color.WHITE);
        card.setBorder(UiTheme.CARD);
        JLabel label = new JLabel(text);
        label.setForeground(UiTheme.MUTED);
        JLabel number = new JLabel(value);
        number.setFont(number.getFont().deriveFont(Font.BOLD, 22f));
        card.add(label, BorderLayout.NORTH);
        card.add(number, BorderLayout.CENTER);
        return card;
    }

    private JPanel sale() {
        JPanel panel = page();
        final DefaultTableModel cart =
                new DefaultTableModel(new String[] {"Mã", "Sản phẩm", "SL", "Đơn giá", "Giảm dòng", "Thành tiền"}, 0) {
                    @Override
                    public boolean isCellEditable(int row, int column) {
                        return false;
                    }
                };
        final JComboBox<SanPham> products = new JComboBox<SanPham>();
        final JComboBox<KhachHang> customers = new JComboBox<KhachHang>();
        final JComboBox<KhuyenMai> promotions = new JComboBox<KhuyenMai>();
        products.setRenderer(productRenderer());
        customers.setRenderer(customerRenderer());
        customers.addItem(null);
        promotions.setRenderer(promotionRenderer());
        promotions.getAccessibleContext().setAccessibleName("Khuyến mãi áp dụng");
        JPanel form = new JPanel(new GridLayout(2, 1, 8, 8));
        form.setBackground(Color.WHITE);
        form.setBorder(UiTheme.CARD);
        JPanel productForm = new JPanel(new FlowLayout(FlowLayout.LEFT));
        productForm.setOpaque(false);
        final JTextField product = new JTextField(14);
        final JSpinner quantity = new JSpinner(new SpinnerNumberModel(1, 1, 9999, 1));
        JLabel productLabel = new JLabel("Mã / tên sản phẩm");
        productLabel.setLabelFor(product);
        productLabel.setDisplayedMnemonic(KeyEvent.VK_M);
        productForm.add(productLabel);
        productForm.add(product);
        JButton findProduct = new JButton("Tìm sản phẩm");
        findProduct.setMnemonic(KeyEvent.VK_T);
        product.getAccessibleContext().setAccessibleName("Mã hoặc tên sản phẩm");
        findProduct.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                findProducts(product.getText().trim(), products);
            }
        });
        productForm.add(findProduct);
        products.getAccessibleContext().setAccessibleName("Sản phẩm tìm được");
        productForm.add(products);
        JLabel quantityLabel = new JLabel("Số lượng");
        quantityLabel.setLabelFor(quantity);
        quantityLabel.setDisplayedMnemonic(KeyEvent.VK_S);
        productForm.add(quantityLabel);
        productForm.add(quantity);
        quantity.getAccessibleContext().setAccessibleName("Số lượng sản phẩm");
        JButton add = new JButton("Thêm vào giỏ");
        add.setMnemonic(KeyEvent.VK_A);
        add.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                SanPham selected = (SanPham) products.getSelectedItem();
                if (selected == null) {
                    JOptionPane.showMessageDialog(PosFrame.this, "Tìm và chọn sản phẩm đang kinh doanh.");
                    return;
                }
                addCartLine(cart, selected, ((Number) quantity.getValue()).intValue());
                product.setText("");
                loadPromotions(cart, promotions);
            }
        });
        productForm.add(add);
        form.add(productForm);
        JPanel customerForm = new JPanel(new FlowLayout(FlowLayout.LEFT));
        customerForm.setOpaque(false);
        final JTextField customer = new JTextField(14);
        final JLabel loyalty = new JLabel("Khách lẻ · không dùng điểm");
        JLabel customerLabel = new JLabel("Khách hàng");
        customerLabel.setLabelFor(customer);
        customerLabel.setDisplayedMnemonic(KeyEvent.VK_K);
        customerForm.add(customerLabel);
        customerForm.add(customer);
        JButton findCustomer = new JButton("Tìm khách");
        findCustomer.setMnemonic(KeyEvent.VK_K);
        customer.getAccessibleContext().setAccessibleName("Mã hoặc tên khách hàng");
        findCustomer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                findCustomers(customer.getText().trim(), customers, loyalty);
            }
        });
        customerForm.add(findCustomer);
        customers.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                showLoyalty((KhachHang) customers.getSelectedItem(), loyalty);
            }
        });
        customers.getAccessibleContext().setAccessibleName("Khách hàng tìm được");
        customerForm.add(customers);
        customerForm.add(loyalty);
        form.add(customerForm);
        panel.add(form, BorderLayout.NORTH);
        JTable table = new JTable(cart);
        table.setRowHeight(28);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JPanel cartBox = new JPanel(new BorderLayout(0, 8));
        cartBox.setBackground(Color.WHITE);
        cartBox.setBorder(UiTheme.CARD);
        cartBox.add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel payment = new JPanel(new GridLayout(3, 1, 0, 4));
        payment.setOpaque(false);
        JPanel cartActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        cartActions.setOpaque(false);
        JPanel paymentActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        paymentActions.setOpaque(false);
        JPanel totals = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        totals.setOpaque(false);
        JButton editQuantity = new JButton("Sửa SL");
        editQuantity.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                editQuantity(cart, table);
                loadPromotions(cart, promotions);
            }
        });
        JButton removeLine = new JButton("Bỏ dòng");
        removeLine.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                int row = table.getSelectedRow();
                if (row >= 0) {
                    cart.removeRow(row);
                    loadPromotions(cart, promotions);
                }
            }
        });
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
        final JLabel subtotal = new JLabel("Tạm tính: 0"), discount = new JLabel("Giảm: 0"),
                     reward = new JLabel("Điểm: 0"), vat = new JLabel("VAT: 0"), total = new JLabel("Tổng: 0"),
                     change = new JLabel("Thừa: 0");
        JButton quote = new JButton("Báo giá");
        quote.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                YeuCauThanhToan request = paymentRequest(cart, (KhachHang) customers.getSelectedItem(),
                        (KhuyenMai) promotions.getSelectedItem(), tierDiscount.isSelected(), eventDiscount.isSelected(),
                        rewards.getText().trim(), String.valueOf(method.getSelectedItem()), tender.getText().trim(),
                        "THÀNH CÔNG".equals(result.getSelectedItem()));
                if (request != null)
                    quote(request, cart, subtotal, discount, reward, vat, total, change);
            }
        });
        JButton checkout = new JButton("Thanh toán");
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
        cartActions.add(editQuantity);
        cartActions.add(removeLine);
        cartActions.add(noDiscount);
        cartActions.add(tierDiscount);
        cartActions.add(eventDiscount);
        cartActions.add(promotions);
        JLabel rewardsLabel = new JLabel("Điểm dùng");
        rewardsLabel.setLabelFor(rewards);
        rewardsLabel.setDisplayedMnemonic(KeyEvent.VK_D);
        rewards.getAccessibleContext().setAccessibleName("Điểm thưởng sử dụng");
        cartActions.add(rewardsLabel);
        cartActions.add(rewards);
        JLabel methodLabel = new JLabel("Phương thức");
        methodLabel.setLabelFor(method);
        methodLabel.setDisplayedMnemonic(KeyEvent.VK_P);
        method.getAccessibleContext().setAccessibleName("Phương thức thanh toán");
        paymentActions.add(methodLabel);
        paymentActions.add(method);
        result.getAccessibleContext().setAccessibleName("Kết quả thanh toán điện tử");
        paymentActions.add(result);
        JLabel tenderLabel = new JLabel("Tiền khách đưa");
        tenderLabel.setLabelFor(tender);
        tenderLabel.setDisplayedMnemonic(KeyEvent.VK_I);
        tender.getAccessibleContext().setAccessibleName("Tiền khách đưa");
        paymentActions.add(tenderLabel);
        paymentActions.add(tender);
        paymentActions.add(quote);
        paymentActions.add(checkout);
        totals.add(subtotal);
        totals.add(discount);
        totals.add(reward);
        totals.add(vat);
        totals.add(total);
        totals.add(change);
        payment.add(cartActions);
        payment.add(paymentActions);
        payment.add(totals);
        cartBox.add(payment, BorderLayout.SOUTH);
        panel.add(cartBox, BorderLayout.CENTER);
        return panel;
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
                try {
                    get();
                    status.setText("Đã lưu cấu hình; VAT áp dụng giao dịch tương lai.");
                } catch (Exception exception) {
                    JOptionPane.showMessageDialog(
                            PosFrame.this, "Không lưu được cấu hình.", "Lỗi", JOptionPane.ERROR_MESSAGE);
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

    private void findProducts(final String keyword, final JComboBox<SanPham> products) {
        new SwingWorker<List<SanPham>, Void>() {
            @Override
            protected List<SanPham> doInBackground() throws Exception {
                return new MasterDataService().timSanPham(actor, keyword, null, "DANG_KINH_DOANH", null, null);
            }
            @Override
            protected void done() {
                try {
                    products.removeAllItems();
                    for (SanPham product : get()) products.addItem(product);
                    if (products.getItemCount() == 0)
                        JOptionPane.showMessageDialog(PosFrame.this, "Không tìm thấy sản phẩm đang kinh doanh.");
                } catch (Exception exception) {
                    JOptionPane.showMessageDialog(PosFrame.this, "Không tải được sản phẩm.");
                }
            }
        }.execute();
    }

    private void findCustomers(final String keyword, final JComboBox<KhachHang> customers, final JLabel loyalty) {
        new SwingWorker<List<KhachHang>, Void>() {
            @Override
            protected List<KhachHang> doInBackground() throws Exception {
                return new MasterDataService().timKhachHang(actor, keyword, "HOAT_DONG");
            }
            @Override
            protected void done() {
                try {
                    customers.removeAllItems();
                    customers.addItem(null);
                    for (KhachHang customer : get()) customers.addItem(customer);
                    customers.setSelectedIndex(0);
                    showLoyalty(null, loyalty);
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
        loyalty.setText("Hạng " + tier + " · điểm thưởng " + customer.getDiemHienCo());
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

    private void editQuantity(DefaultTableModel cart, JTable table) {
        int row = table.getSelectedRow();
        if (row < 0)
            return;
        JSpinner quantity =
                new JSpinner(new SpinnerNumberModel(((Number) cart.getValueAt(row, 2)).intValue(), 1, 9999, 1));
        if (JOptionPane.showConfirmDialog(this, quantity, "Số lượng", JOptionPane.OK_CANCEL_OPTION)
                != JOptionPane.OK_OPTION)
            return;
        int amount = ((Number) quantity.getValue()).intValue();
        cart.setValueAt(Integer.valueOf(amount), row, 2);
        cart.setValueAt(((BigDecimal) cart.getValueAt(row, 3)).multiply(BigDecimal.valueOf(amount)), row, 5);
    }

    private void loadPromotions(DefaultTableModel cart, final JComboBox<KhuyenMai> promotions) {
        final java.util.Set<String> productIds = new LinkedHashSet<String>();
        for (int row = 0; row < cart.getRowCount(); row++) productIds.add(String.valueOf(cart.getValueAt(row, 0)));
        new SwingWorker<List<KhuyenMai>, Void>() {
            @Override
            protected List<KhuyenMai> doInBackground() throws Exception {
                return new SaleService().activePromotions(actor, productIds, LocalDateTime.now());
            }
            @Override
            protected void done() {
                try {
                    promotions.removeAllItems();
                    for (KhuyenMai promotion : get()) promotions.addItem(promotion);
                } catch (Exception exception) {
                    JOptionPane.showMessageDialog(PosFrame.this, "Không tải được khuyến mãi đủ điều kiện.");
                }
            }
        }.execute();
    }

    private YeuCauThanhToan paymentRequest(DefaultTableModel cart, KhachHang customer, KhuyenMai promotion,
            boolean tier, boolean event, String rewards, String method, String tender, boolean electronicSuccess) {
        if (cart.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Giỏ hàng chưa có sản phẩm.");
            return null;
        }
        final long rewardPoints;
        try {
            rewardPoints = Long.parseLong(rewards.length() == 0 ? "0" : rewards);
        } catch (NumberFormatException exception) {
            JOptionPane.showMessageDialog(this, "Điểm thưởng phải là số nguyên không âm.");
            return null;
        }
        if (rewardPoints < 0 || (tier && customer == null) || (event && promotion == null)) {
            JOptionPane.showMessageDialog(this, "Chọn giảm giá hợp lệ và khách hàng khi dùng hạng.");
            return null;
        }
        YeuCauThanhToan request = new YeuCauThanhToan();
        request.setMaKhachHang(customer == null ? null : customer.getMaKhachHang());
        request.setLoaiGiamGia(event ? "KHUYEN_MAI" : tier ? "HANG_THANH_VIEN" : "KHONG");
        request.setMaKhuyenMai(event ? promotion.getMaKhuyenMai() : null);
        request.setDiemThuongSuDung(rewardPoints);
        request.setPhuongThucThanhToan(method);
        if ("TIEN_MAT".equals(method)) {
            try {
                request.setTienKhachDua(new BigDecimal(tender));
            } catch (NumberFormatException exception) {
                JOptionPane.showMessageDialog(this, "Nhập tiền khách đưa hợp lệ.");
                return null;
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

    private void quote(final YeuCauThanhToan request, final DefaultTableModel cart, final JLabel subtotal,
            final JLabel discount, final JLabel reward, final JLabel vat, final JLabel total, final JLabel change) {
        new SwingWorker<BaoGia, Void>() {
            @Override
            protected BaoGia doInBackground() throws Exception {
                return new SaleService().baoGia(actor, request);
            }
            @Override
            protected void done() {
                try {
                    HoaDon invoice = get().getHoaDon();
                    for (int row = 0; row < invoice.getChiTiet().size(); row++) {
                        cart.setValueAt(invoice.getChiTiet().get(row).getTienGiamDong(), row, 4);
                        cart.setValueAt(invoice.getChiTiet().get(row).getThanhTien(), row, 5);
                    }
                    subtotal.setText("Tạm tính: " + invoice.getTongGiaGoc());
                    discount.setText("Giảm: " + invoice.getTienGiam());
                    reward.setText("Điểm: " + invoice.getTienGiamDiem());
                    vat.setText("VAT: " + invoice.getTienVat());
                    total.setText("Tổng: " + invoice.getTongThanhToan());
                    change.setText("Thừa: " + invoice.getTienThua());
                } catch (Exception exception) {
                    showServiceError("Không báo giá được", exception);
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
                try {
                    Object[] checkoutResult = get();
                    HoaDon invoice = (HoaDon) checkoutResult[0];
                    resetSale(cart, productSearch, products, quantity, customerSearch, customers, promotions,
                            noDiscount, rewards, tender, method, result, subtotal, discount, reward, vat, total, change,
                            loyalty);
                    status.setText(
                            "Đã thanh toán " + invoice.getMaHoaDon() + ": " + invoice.getTongThanhToan() + " đ.");
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
        subtotal.setText("Tạm tính: 0");
        discount.setText("Giảm: 0");
        reward.setText("Điểm: 0");
        vat.setText("VAT: 0");
        total.setText("Tổng: 0");
        change.setText("Thừa: 0");
        showLoyalty(null, loyalty);
    }

    private void previewReceipt(final ReceiptPrintable receipt) {
        JTextArea text = new JTextArea();
        text.setEditable(false);
        for (String line : receipt.lines()) text.append(line + "\n");
        if (JOptionPane.showConfirmDialog(this, new JScrollPane(text), "Xem trước hóa đơn", JOptionPane.YES_NO_OPTION)
                == JOptionPane.YES_OPTION)
            printReceipt(receipt);
    }

    private void printReceipt(final ReceiptPrintable receipt) {
        final PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintable(receipt);
        if (!job.printDialog())
            return;
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                job.print();
                return null;
            }
            @Override
            protected void done() {
                try {
                    get();
                } catch (Exception exception) {
                    JOptionPane.showMessageDialog(PosFrame.this, "Không in được hóa đơn.");
                }
            }
        }.execute();
    }

    private void showServiceError(String title, Exception exception) {
        Throwable cause = exception.getCause() == null ? exception : exception.getCause();
        JOptionPane.showMessageDialog(this, cause.getMessage(), title, JOptionPane.ERROR_MESSAGE);
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
        if (button != null) {
            selectedNavigation = button;
            button.setBackground(UiTheme.BLUE);
        }
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
            return "Nhân viên và tài khoản";
        if ("suppliers".equals(key))
            return "Nhà cung cấp";
        if ("purchases".equals(key))
            return "Phiếu nhập";
        if ("adjustments".equals(key))
            return "Điều chỉnh tồn kho";
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
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK), "search", new Runnable() {
            public void run() {
                globalSearch.requestFocusInWindow();
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

    private void searchActivePage() {
        JComponent page = activePage();
        JTextField field = firstField(page);
        if (field == null)
            return;
        field.setText(globalSearch.getText().trim());
        clickActiveButton("Tìm");
    }

    private JComponent activePage() {
        for (java.awt.Component component : pages.getComponents())
            if (component.isVisible() && component instanceof JComponent)
                return (JComponent) component;
        return null;
    }

    private JTextField firstField(java.awt.Component component) {
        if (component instanceof JTextField)
            return (JTextField) component;
        if (component instanceof java.awt.Container)
            for (java.awt.Component child : ((java.awt.Container) component).getComponents()) {
                JTextField field = firstField(child);
                if (field != null)
                    return field;
            }
        return null;
    }

    private void clickActiveButton(String text) {
        JButton button = findButton(activePage(), text);
        if (button != null && button.isEnabled())
            button.doClick();
    }

    private JButton findButton(java.awt.Component component, String text) {
        if (component instanceof JButton && ((JButton) component).getText().startsWith(text))
            return (JButton) component;
        if (component instanceof java.awt.Container)
            for (java.awt.Component child : ((java.awt.Container) component).getComponents()) {
                JButton button = findButton(child, text);
                if (button != null)
                    return button;
            }
        return null;
    }

    private void closeDialog() {
        for (java.awt.Window window : java.awt.Window.getWindows())
            if (window instanceof javax.swing.JDialog && window.isShowing()) {
                window.dispose();
                return;
            }
    }
}
