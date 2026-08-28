package graphicUI;

import components.ReceiptPrintable;
import components.DialogActions;
import components.UiTheme;
import entity.CauHinhCuaHang;
import entity.ChiTietHoaDon;
import entity.HoaDon;
import entity.PhanBoXuatLo;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.print.PrinterJob;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JButton;
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
            new String[] {"", "DA_THANH_TOAN", "DA_HUY"});
    private final JComboBox<String> paymentMethod = new JComboBox<String>(
            new String[] {"", "TIEN_MAT", "THE", "QR"});
    private final JTextField from = new JTextField(9);
    private final JTextField to = new JTextField(9);
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
        addFilter(bar, "Từ ngày (yyyy-MM-dd)", from);
        addFilter(bar, "Đến ngày (yyyy-MM-dd)", to);
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
        final String status = (String) state.getSelectedItem();
        final String method = (String) paymentMethod.getSelectedItem();
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
                                invoice.getMaNhanVien(), invoice.getMaKhachHang(), invoice.getTrangThai(),
                                invoice.getTongThanhToan(), invoice.getPhuongThucTt(), invoice.getMaThamChieuTt()});
                    }
                } catch (Exception exception) {
                    showError("Không tải được hóa đơn.");
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
                    showError("Không tải được chi tiết.");
                }
            }
        }.execute();
    }

    private void showDetail(HoaDon invoice) {
        JPanel summary = new JPanel(new GridLayout(0, 2, 8, 4));
        addSummary(summary, "Mã hóa đơn", invoice.getMaHoaDon());
        addSummary(summary, "Lập lúc", invoice.getThoiDiemLap());
        addSummary(summary, "Nhân viên", invoice.getMaNhanVien());
        addSummary(summary, "Khách hàng", invoice.getMaKhachHang());
        addSummary(summary, "Khuyến mãi", invoice.getMaKhuyenMai());
        addSummary(summary, "Trạng thái", invoice.getTrangThai());
        addSummary(summary, "Tổng gốc", invoice.getTongGiaGoc());
        addSummary(summary, "Loại giảm giá", invoice.getLoaiGiamGia());
        addSummary(summary, "Tỷ lệ giảm", invoice.getTyLeGiam());
        addSummary(summary, "Tiền giảm", invoice.getTienGiam());
        addSummary(summary, "Điểm sử dụng", invoice.getDiemSuDung());
        addSummary(summary, "Tiền giảm điểm", invoice.getTienGiamDiem());
        addSummary(summary, "Điểm tích lũy kiếm", invoice.getDiemTichLuyKiem());
        addSummary(summary, "Điểm thưởng kiếm", invoice.getDiemThuongKiem());
        addSummary(summary, "Tỷ lệ VAT", invoice.getTyLeVat());
        addSummary(summary, "Tiền VAT", invoice.getTienVat());
        addSummary(summary, "Tổng thanh toán", invoice.getTongThanhToan());
        addSummary(summary, "Phương thức", invoice.getPhuongThucTt());
        addSummary(summary, "Tham chiếu", invoice.getMaThamChieuTt());
        addSummary(summary, "Tiền khách đưa", invoice.getTienKhachDua());
        addSummary(summary, "Tiền thừa", invoice.getTienThua());
        addSummary(summary, "Nhân viên hủy", invoice.getMaNhanVienHuy());
        addSummary(summary, "Hủy lúc", invoice.getThoiDiemHuy());
        addSummary(summary, "Lý do hủy", invoice.getLyDoHuy());

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Dòng hàng", new JScrollPane(lineTable(invoice.getChiTiet())));
        tabs.addTab("Phân bổ FIFO", new JScrollPane(allocationTable(invoice.getPhanBo())));
        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.add(summary, BorderLayout.NORTH);
        content.add(tabs, BorderLayout.CENTER);
        JOptionPane.showMessageDialog(this, content, "Hóa đơn bất biến " + invoice.getMaHoaDon(),
                JOptionPane.INFORMATION_MESSAGE);
    }

    private JTable lineTable(List<ChiTietHoaDon> lines) {
        DefaultTableModel lineModel = nonEditableModel(
                new String[] {"Sản phẩm", "Số lượng", "Đơn giá", "Giảm dòng", "Thành tiền"});
        for (ChiTietHoaDon line : lines) {
            lineModel.addRow(new Object[] {line.getMaSanPham(), line.getSoLuong(), line.getDonGiaBan(),
                    line.getTienGiamDong(), line.getThanhTien()});
        }
        return new JTable(lineModel);
    }

    private JTable allocationTable(List<PhanBoXuatLo> allocations) {
        DefaultTableModel allocationModel = nonEditableModel(
                new String[] {"Sản phẩm", "Mã lô", "Số lượng xuất", "Đơn giá vốn"});
        for (PhanBoXuatLo allocation : allocations) {
            allocationModel.addRow(new Object[] {allocation.getMaSanPham(), allocation.getMaLo(),
                    allocation.getSoLuongXuat(), allocation.getDonGiaVon()});
        }
        return new JTable(allocationModel);
    }

    private DefaultTableModel nonEditableModel(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
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
                    if (JOptionPane.showConfirmDialog(InvoicePanel.this, new JScrollPane(preview),
                            "Xem trước hóa đơn", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) print(receipt);
                } catch (Exception exception) {
                    showError("Không tải được hóa đơn để in.");
                }
            }
        }.execute();
    }

    private void print(final ReceiptPrintable receipt) {
        final PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintable(receipt);
        if (!job.printDialog()) return;
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
                    showError("Không in được hóa đơn.");
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
        JPanel form = new JPanel(new GridLayout(0, 1, 0, 4));
        addFilter(form, "Lý do", reason);
        addFilter(form, "Mật khẩu quản lý", password);
        if (!DialogActions.confirm(this, form, "Xác nhận hủy hóa đơn")) {
            password.setText("");
            return;
        }
        final String invoiceId = selected.getMaHoaDon();
        final String cancellationReason = reason.getText().trim();
        final char[] managerPassword = password.getPassword();
        password.setText("");
        if (cancellationReason.isEmpty() || managerPassword.length == 0) {
            Arrays.fill(managerPassword, '\0');
            showError("Cần lý do và mật khẩu quản lý.");
            return;
        }
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
                } catch (Exception exception) {
                    showError("Không hủy được hóa đơn.");
                }
            }
        }.execute();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message);
    }
}
