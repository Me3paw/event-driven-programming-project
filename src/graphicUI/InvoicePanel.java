package graphicUI;

import components.ReceiptPrintable;
import components.ReceiptPdfExporter;
import components.DialogActions;
import components.FormValidator;
import components.UiFormat;
import components.UiTheme;
import entity.CauHinhCuaHang;
import entity.ChiTietHoaDon;
import entity.HoaDon;
import entity.PhanBoXuatLo;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import service.Actor;
import service.SaleService;
import service.SettingsService;

@SuppressWarnings("serial")
public final class InvoicePanel extends JPanel {
    private final Actor actor;
    private final JTextField key = new JTextField(12);
    private final JTextField employee = new JTextField(9);
    private final JTextField customer = new JTextField(9);
    private final JComboBox<String> state = new JComboBox<String>(
            new String[] {"Tất cả", "DA_THANH_TOAN", "DA_HUY"});
    private final JComboBox<String> paymentMethod = new JComboBox<String>(
            new String[] {"Tất cả", "TIEN_MAT", "THE", "QR"});
    private final DateField from = new DateField();
    private final DateField to = new DateField();
    private final List<HoaDon> rows = new ArrayList<HoaDon>();
    private final DefaultTableModel model = new DefaultTableModel(
            new String[] {"Mã", "Thời điểm", "Nhân viên", "Khách", "TT", "Tổng", "PTTT", "Tham chiếu"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);

    public InvoicePanel(Actor actor) {
        super(new BorderLayout(0, 8));
        this.actor = actor;
        table.getColumnModel().getColumn(5).setCellRenderer(UiFormat.moneyRenderer());
        setBackground(UiTheme.BACKGROUND);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bar.setBackground(Color.WHITE);
        bar.setBorder(UiTheme.CARD);
        addFilter(bar, "Mã/từ khóa", key);
        if (actor.isQuanLy()) addFilter(bar, "Nhân viên", employee);
        addFilter(bar, "Khách hàng", customer);
        addFilter(bar, "Trạng thái", state);
        addFilter(bar, "Thanh toán", paymentMethod);
        addFilter(bar, "Từ ngày", from);
        addFilter(bar, "Đến ngày", to);
        addButton(bar, "Tìm", new Runnable() { @Override public void run() { load(); } });
        addButton(bar, "Chi tiết", new Runnable() { @Override public void run() { detail(); } });
        addButton(bar, "In", new Runnable() { @Override public void run() { print(); } });
        if (actor.isQuanLy()) addButton(bar, "Hủy", new Runnable() { @Override public void run() { cancel(); } });
        add(bar, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        load();
    }

    private void addFilter(JPanel panel, String text, JComponent field) {
        JLabel label = new JLabel(text);
        label.setLabelFor(field);
        panel.add(label);
        panel.add(field);
    }

    private void addButton(JPanel panel, String text, final Runnable action) {
        JButton button = new JButton(text);
        button.addActionListener(event -> action.run());
        panel.add(button);
    }

    private void load() {
        final String keyword = key.getText().trim();
        final String employeeId = actor.isQuanLy() ? employee.getText().trim() : null;
        final String customerId = customer.getText().trim();
        final String status = "Tất cả".equals(state.getSelectedItem()) ? null
                : (String) state.getSelectedItem();
        final String method = "Tất cả".equals(paymentMethod.getSelectedItem()) ? null
                : (String) paymentMethod.getSelectedItem();
        final LocalDate start;
        final LocalDate end;
        try {
            start = parseDate(from.getText().trim());
            end = parseDate(to.getText().trim());
        } catch (DateTimeParseException exception) {
            showError("Ngày phải theo định dạng yyyy-MM-dd.");
            return;
        }
        if (start != null && end != null && start.isAfter(end)) {
            showError("Từ ngày không được sau đến ngày.");
            return;
        }
        table.clearSelection();
        new SwingWorker<List<HoaDon>, Void>() {
            @Override
            protected List<HoaDon> doInBackground() throws Exception {
                return new SaleService().timHoaDon(actor, keyword, employeeId, customerId, status, method, start, end);
            }

            @Override
            protected void done() {
                try {
                    rows.clear();
                    rows.addAll(get());
                    model.setRowCount(0);
                    for (HoaDon invoice : rows) {
                        model.addRow(new Object[] {invoice.getMaHoaDon(), invoice.getThoiDiemLap(),
                                invoice.getMaNhanVien(), customerLabel(invoice), invoice.getTrangThai(),
                                invoice.getTongThanhToan(), invoice.getPhuongThucTt(), invoice.getMaThamChieuTt()});
                    }
                } catch (Exception exception) {
                    showError("Không tải được hóa đơn.", exception);
                }
            }
        }.execute();
    }

    private LocalDate parseDate(String value) {
        return value.isEmpty() ? null : LocalDate.parse(value);
    }

    private HoaDon selectedInvoice() {
        int row = table.getSelectedRow();
        return row < 0 ? null : rows.get(table.convertRowIndexToModel(row));
    }

    private void detail() {
        HoaDon selected = selectedInvoice();
        if (selected == null) {
            showError("Hãy chọn hóa đơn.");
            return;
        }
        final String invoiceId = selected.getMaHoaDon();
        new SwingWorker<HoaDon, Void>() {
            @Override
            protected HoaDon doInBackground() throws Exception {
                return new SaleService().chiTietHoaDon(actor, invoiceId);
            }

            @Override
            protected void done() {
                try {
                    showDetail(get());
                } catch (Exception exception) {
                    showError("Không tải được chi tiết.", exception);
                }
            }
        }.execute();
    }

    private void showDetail(HoaDon invoice) {
        JPanel summary = new JPanel(new GridLayout(0, 2, 8, 4));
        addSummary(summary, "Mã hóa đơn", invoice.getMaHoaDon());
        addSummary(summary, "Lập lúc", invoice.getThoiDiemLap());
        addSummary(summary, "Nhân viên", invoice.getMaNhanVien());
        addSummary(summary, "Khách hàng", customerLabel(invoice));
        addSummary(summary, "Khuyến mãi", invoice.getMaKhuyenMai());
        addSummary(summary, "Trạng thái", invoice.getTrangThai());
        addSummary(summary, "Tổng gốc", money(invoice.getTongGiaGoc()));
        addSummary(summary, "Loại giảm giá", invoice.getLoaiGiamGia());
        addSummary(summary, "Tỷ lệ giảm", invoice.getTyLeGiam());
        addSummary(summary, "Tiền giảm", money(invoice.getTienGiam()));
        addSummary(summary, "Điểm sử dụng", invoice.getDiemSuDung());
        addSummary(summary, "Tiền giảm điểm", money(invoice.getTienGiamDiem()));
        addSummary(summary, "Điểm tích lũy kiếm", invoice.getDiemTichLuyKiem());
        addSummary(summary, "Điểm thưởng kiếm", invoice.getDiemThuongKiem());
        addSummary(summary, "Tỷ lệ VAT", invoice.getTyLeVat());
        addSummary(summary, "Tiền VAT", money(invoice.getTienVat()));
        addSummary(summary, "Tổng thanh toán", money(invoice.getTongThanhToan()));
        addSummary(summary, "Phương thức", invoice.getPhuongThucTt());
        addSummary(summary, "Tham chiếu", invoice.getMaThamChieuTt());
        addSummary(summary, "Tiền khách đưa", money(invoice.getTienKhachDua()));
        addSummary(summary, "Tiền thừa", money(invoice.getTienThua()));
        addSummary(summary, "Nhân viên hủy", invoice.getMaNhanVienHuy());
        addSummary(summary, "Hủy lúc", invoice.getThoiDiemHuy());
        addSummary(summary, "Lý do hủy", invoice.getLyDoHuy());

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Dòng hàng", new JScrollPane(lineTable(invoice.getChiTiet())));
        tabs.addTab("Phân bổ FIFO", new JScrollPane(allocationTable(invoice.getPhanBo())));
        tabs.setPreferredSize(DialogActions.DETAIL_TABLE);
        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.add(summary, BorderLayout.NORTH);
        content.add(tabs, BorderLayout.CENTER);
        DialogActions.info(this, content, "Hóa đơn bất biến " + invoice.getMaHoaDon());
    }

    private JTable lineTable(List<ChiTietHoaDon> lines) {
        DefaultTableModel lineModel = nonEditableModel(
                new String[] {"Sản phẩm", "Số lượng", "Đơn giá", "Giảm dòng", "Thành tiền"});
        for (ChiTietHoaDon line : lines) {
            lineModel.addRow(new Object[] {line.getMaSanPham(), line.getSoLuong(), line.getDonGiaBan(),
                    line.getTienGiamDong(), line.getThanhTien()});
        }
        JTable lineTable = new JTable(lineModel);
        for (int column = 2; column <= 4; column++)
            lineTable.getColumnModel().getColumn(column).setCellRenderer(UiFormat.moneyRenderer());
        return lineTable;
    }

    private JTable allocationTable(List<PhanBoXuatLo> allocations) {
        DefaultTableModel allocationModel = nonEditableModel(
                new String[] {"Sản phẩm", "Mã lô", "Số lượng xuất", "Đơn giá vốn"});
        for (PhanBoXuatLo allocation : allocations) {
            allocationModel.addRow(new Object[] {allocation.getMaSanPham(), allocation.getMaLo(),
                    allocation.getSoLuongXuat(), allocation.getDonGiaVon()});
        }
        JTable allocationTable = new JTable(allocationModel);
        allocationTable.getColumnModel().getColumn(3).setCellRenderer(UiFormat.moneyRenderer());
        return allocationTable;
    }

    private DefaultTableModel nonEditableModel(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private static String customerLabel(HoaDon invoice) {
        String id = invoice.getMaKhachHang();
        return id == null || id.trim().isEmpty() || SaleService.KHACH_LE.equals(id) ? "Khách lẻ" : id;
    }

    private String money(java.math.BigDecimal value) {
        return value == null ? "" : UiFormat.money(value) + " đ";
    }

    private void addSummary(JPanel panel, String label, Object value) {
        panel.add(new JLabel(label));
        panel.add(new JLabel(value == null ? "" : String.valueOf(value)));
    }

    private void print() {
        HoaDon selected = selectedInvoice();
        if (selected == null) {
            showError("Hãy chọn hóa đơn.");
            return;
        }
        final String invoiceId = selected.getMaHoaDon();
        new SwingWorker<Object[], Void>() {
            @Override
            protected Object[] doInBackground() throws Exception {
                return new Object[] {new SaleService().chiTietHoaDon(actor, invoiceId),
                        new SettingsService().layChoHoaDon(actor)};
            }

            @Override
            protected void done() {
                try {
                    Object[] result = get();
                    ReceiptPrintable receipt = new ReceiptPrintable((CauHinhCuaHang) result[1], (HoaDon) result[0]);
                    JTextArea preview = new JTextArea();
                    preview.setEditable(false);
                    for (String line : receipt.lines()) preview.append(line + "\n");
                    JScrollPane scroll = new JScrollPane(preview);
                    scroll.setPreferredSize(DialogActions.PREVIEW_TEXT);
                    if (DialogActions.confirm(InvoicePanel.this, scroll,
                            "Xem trước hóa đơn"))
                        exportPdf(receipt, invoiceId);
                } catch (Exception exception) {
                    showError("Không tải được hóa đơn để in.", exception);
                }
            }
        }.execute();
    }

    private void exportPdf(final ReceiptPrintable receipt, String invoiceId) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Lưu hóa đơn PDF");
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
                try {
                    get();
                    JOptionPane.showMessageDialog(InvoicePanel.this,
                            "Đã lưu hóa đơn PDF:\n" + target.getAbsolutePath(), "Xuất hóa đơn thành công",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception exception) {
                    showError("Không xuất được hóa đơn PDF.", exception);
                }
            }
        }.execute();
    }

    private void cancel() {
        HoaDon selected = selectedInvoice();
        if (selected == null) {
            showError("Hãy chọn hóa đơn.");
            return;
        }
        if ("DA_HUY".equals(selected.getTrangThai())) {
            showError("Hóa đơn đã hủy.");
            return;
        }
        JTextField reason = new JTextField();
        JPasswordField password = new JPasswordField();
        JPanel form = FormValidator.form();
        FormValidator validator = new FormValidator();
        form.add(validator.row("Lý do", reason));
        form.add(validator.row("Mật khẩu quản lý", password));
        validator.require(reason, "Cần nhập lý do hủy hóa đơn.");
        validator.require(password, "Cần nhập mật khẩu quản lý.");
        if (!DialogActions.confirm(this, form, "Xác nhận hủy hóa đơn", validator)) {
            password.setText("");
            return;
        }
        final String invoiceId = selected.getMaHoaDon();
        final String cancellationReason = reason.getText().trim();
        final char[] managerPassword = password.getPassword();
        password.setText("");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                new SaleService().huyHoaDon(actor, invoiceId, cancellationReason, actor.getTenDangNhap(),
                        managerPassword);
                return null;
            }

            @Override
            protected void done() {
                Arrays.fill(managerPassword, '\0');
                try {
                    get();
                    load();
                    JOptionPane.showMessageDialog(InvoicePanel.this, "Hủy hóa đơn thành công.", "Thành công",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception exception) {
                    showError("Không hủy được hóa đơn.", exception);
                }
            }
        }.execute();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Lỗi", JOptionPane.ERROR_MESSAGE);
    }

    private void showError(String fallback, Exception exception) {
        Throwable cause = exception;
        while (cause.getCause() != null
                && (cause instanceof java.util.concurrent.ExecutionException
                    || cause instanceof java.lang.reflect.InvocationTargetException))
            cause = cause.getCause();
        String message = cause instanceof IllegalArgumentException || cause instanceof IllegalStateException
                || cause instanceof SecurityException ? cause.getMessage() : fallback;
        showError(message == null || message.trim().isEmpty() ? fallback : message);
    }
}
