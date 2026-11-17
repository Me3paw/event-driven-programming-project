package graphicUI;

import components.ReceiptPrintable;
import components.ReceiptPrinting;
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
import service.NguoiDung;
import service.DichVuBanHang;
import service.DichVuCauHinh;

@SuppressWarnings("serial")
public final class HoaDonPanel extends JPanel {
    private final NguoiDung actor;
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

    public HoaDonPanel(NguoiDung actor) {
        super(new BorderLayout(0, 8));
        this.actor = actor;
        table.getColumnModel().getColumn(5).setCellRenderer(UiFormat.moneyRenderer());
        setBackground(UiTheme.BACKGROUND);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bar.setBackground(Color.WHITE);
        bar.setBorder(UiTheme.CARD);
        themBoLoc(bar, "Mã/từ khóa", key);
        if (actor.isQuanLy()) themBoLoc(bar, "Nhân viên", employee);
        themBoLoc(bar, "Khách hàng", customer);
        themBoLoc(bar, "Trạng thái", state);
        themBoLoc(bar, "Thanh toán", paymentMethod);
        themBoLoc(bar, "Từ ngày", from);
        themBoLoc(bar, "Đến ngày", to);
        themNut(bar, "Tìm", new Runnable() { @Override public void run() { taiDanhSach(); } });
        themNut(bar, "Chi tiết", new Runnable() { @Override public void run() { xemChiTiet(); } });
        themNut(bar, "In", new Runnable() { @Override public void run() { inHoaDon(false); } });
        themNut(bar, "Xuất PDF", new Runnable() { @Override public void run() { inHoaDon(true); } });
        if (actor.isQuanLy()) themNut(bar, "Hủy", new Runnable() { @Override public void run() { huyHoaDon(); } });
        add(bar, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        taiDanhSach();
    }

    private void themBoLoc(JPanel panel, String text, JComponent field) {
        JLabel label = new JLabel(text);
        label.setLabelFor(field);
        panel.add(label);
        panel.add(field);
    }

    private void themNut(JPanel panel, String text, final Runnable action) {
        JButton button = new JButton(text);
        button.addActionListener(event -> action.run());
        panel.add(button);
    }

    private void taiDanhSach() {
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
            start = docNgay(from.getText().trim());
            end = docNgay(to.getText().trim());
        } catch (DateTimeParseException exception) {
            baoLoi("Ngày phải theo định dạng yyyy-MM-dd.");
            return;
        }
        if (start != null && end != null && start.isAfter(end)) {
            baoLoi("Từ ngày không được sau đến ngày.");
            return;
        }
        table.clearSelection();
        new SwingWorker<List<HoaDon>, Void>() {
            @Override
            protected List<HoaDon> doInBackground() throws Exception {
                return new DichVuBanHang().timHoaDon(actor, keyword, employeeId, customerId, status, method, start, end);
            }

            @Override
            protected void done() {
                try {
                    rows.clear();
                    rows.addAll(get());
                    model.setRowCount(0);
                    for (HoaDon invoice : rows) {
                        model.addRow(new Object[] {invoice.getMaHoaDon(), invoice.getThoiDiemLap(),
                                invoice.getMaNhanVien(), nhanKhachHang(invoice), invoice.getTrangThai(),
                                invoice.getTongThanhToan(), invoice.getPhuongThucTt(), invoice.getMaThamChieuTt()});
                    }
                } catch (Exception exception) {
                    baoLoi("Không tải được hóa đơn.", exception);
                }
            }
        }.execute();
    }

    private LocalDate docNgay(String value) {
        return value.isEmpty() ? null : LocalDate.parse(value);
    }

    private HoaDon hoaDonDaChon() {
        int row = table.getSelectedRow();
        return row < 0 ? null : rows.get(table.convertRowIndexToModel(row));
    }

    private void xemChiTiet() {
        HoaDon selected = hoaDonDaChon();
        if (selected == null) {
            baoLoi("Hãy chọn hóa đơn.");
            return;
        }
        final String invoiceId = selected.getMaHoaDon();
        new SwingWorker<HoaDon, Void>() {
            @Override
            protected HoaDon doInBackground() throws Exception {
                return new DichVuBanHang().chiTietHoaDon(actor, invoiceId);
            }

            @Override
            protected void done() {
                try {
                    hienChiTiet(get());
                } catch (Exception exception) {
                    baoLoi("Không tải được chi tiết.", exception);
                }
            }
        }.execute();
    }

    private void hienChiTiet(HoaDon invoice) {
        JPanel summary = new JPanel(new GridLayout(0, 2, 8, 4));
        themTongKet(summary, "Mã hóa đơn", invoice.getMaHoaDon());
        themTongKet(summary, "Lập lúc", invoice.getThoiDiemLap());
        themTongKet(summary, "Nhân viên", invoice.getMaNhanVien());
        themTongKet(summary, "Khách hàng", nhanKhachHang(invoice));
        themTongKet(summary, "Khuyến mãi", invoice.getMaKhuyenMai());
        themTongKet(summary, "Trạng thái", invoice.getTrangThai());
        themTongKet(summary, "Tổng gốc", dinhDangTien(invoice.getTongGiaGoc()));
        themTongKet(summary, "Loại giảm giá", invoice.getLoaiGiamGia());
        themTongKet(summary, "Tỷ lệ giảm", invoice.getTyLeGiam());
        themTongKet(summary, "Tiền giảm", dinhDangTien(invoice.getTienGiam()));
        themTongKet(summary, "Điểm sử dụng", invoice.getDiemSuDung());
        themTongKet(summary, "Tiền giảm điểm", dinhDangTien(invoice.getTienGiamDiem()));
        themTongKet(summary, "Điểm tích lũy kiếm", invoice.getDiemTichLuyKiem());
        themTongKet(summary, "Điểm thưởng kiếm", invoice.getDiemThuongKiem());
        themTongKet(summary, "Tỷ lệ VAT", invoice.getTyLeVat());
        themTongKet(summary, "Tiền VAT", dinhDangTien(invoice.getTienVat()));
        themTongKet(summary, "Tổng thanh toán", dinhDangTien(invoice.getTongThanhToan()));
        themTongKet(summary, "Phương thức", invoice.getPhuongThucTt());
        themTongKet(summary, "Tham chiếu", invoice.getMaThamChieuTt());
        themTongKet(summary, "Tiền khách đưa", dinhDangTien(invoice.getTienKhachDua()));
        themTongKet(summary, "Tiền thừa", dinhDangTien(invoice.getTienThua()));
        themTongKet(summary, "Nhân viên hủy", invoice.getMaNhanVienHuy());
        themTongKet(summary, "Hủy lúc", invoice.getThoiDiemHuy());
        themTongKet(summary, "Lý do hủy", invoice.getLyDoHuy());

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Dòng hàng", new JScrollPane(bangChiTiet(invoice.getChiTiet())));
        tabs.addTab("Phân bổ FIFO", new JScrollPane(bangPhanBo(invoice.getPhanBo())));
        tabs.setPreferredSize(DialogActions.DETAIL_TABLE);
        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.add(summary, BorderLayout.NORTH);
        content.add(tabs, BorderLayout.CENTER);
        DialogActions.info(this, content, "Hóa đơn bất biến " + invoice.getMaHoaDon());
    }

    private JTable bangChiTiet(List<ChiTietHoaDon> lines) {
        DefaultTableModel lineModel = modelKhongChoSua(
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

    private JTable bangPhanBo(List<PhanBoXuatLo> allocations) {
        DefaultTableModel allocationModel = modelKhongChoSua(
                new String[] {"Sản phẩm", "Mã lô", "Số lượng xuất", "Đơn giá vốn"});
        for (PhanBoXuatLo allocation : allocations) {
            allocationModel.addRow(new Object[] {allocation.getMaSanPham(), allocation.getMaLo(),
                    allocation.getSoLuongXuat(), allocation.getDonGiaVon()});
        }
        JTable allocationTable = new JTable(allocationModel);
        allocationTable.getColumnModel().getColumn(3).setCellRenderer(UiFormat.moneyRenderer());
        return allocationTable;
    }

    private DefaultTableModel modelKhongChoSua(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private static String nhanKhachHang(HoaDon invoice) {
        String id = invoice.getMaKhachHang();
        return id == null || id.trim().isEmpty() || DichVuBanHang.KHACH_LE.equals(id) ? "Khách lẻ" : id;
    }

    private String dinhDangTien(java.math.BigDecimal value) {
        return value == null ? "" : UiFormat.money(value) + " đ";
    }

    private void themTongKet(JPanel panel, String label, Object value) {
        panel.add(new JLabel(label));
        panel.add(new JLabel(value == null ? "" : String.valueOf(value)));
    }

    private void inHoaDon(final boolean pdf) {
        HoaDon selected = hoaDonDaChon();
        if (selected == null) {
            baoLoi("Hãy chọn hóa đơn.");
            return;
        }
        final String invoiceId = selected.getMaHoaDon();
        new SwingWorker<Object[], Void>() {
            @Override
            protected Object[] doInBackground() throws Exception {
                return new Object[] {new DichVuBanHang().chiTietHoaDon(actor, invoiceId),
                        new DichVuCauHinh().layChoHoaDon(actor)};
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
                    if (DialogActions.confirm(HoaDonPanel.this, scroll, "Xem trước hóa đơn")) {
                        if (pdf) xuatPdf(receipt, invoiceId);
                        else ReceiptPrinting.print(HoaDonPanel.this, receipt);
                    }
                } catch (Exception exception) {
                    baoLoi("Không tải được hóa đơn để in.", exception);
                }
            }
        }.execute();
    }

    private void xuatPdf(final ReceiptPrintable receipt, String invoiceId) {
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
                    JOptionPane.showMessageDialog(HoaDonPanel.this,
                            "Đã lưu hóa đơn PDF:\n" + target.getAbsolutePath(), "Xuất hóa đơn thành công",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception exception) {
                    baoLoi("Không xuất được hóa đơn PDF.", exception);
                }
            }
        }.execute();
    }

    private void huyHoaDon() {
        HoaDon selected = hoaDonDaChon();
        if (selected == null) {
            baoLoi("Hãy chọn hóa đơn.");
            return;
        }
        if ("DA_HUY".equals(selected.getTrangThai())) {
            baoLoi("Hóa đơn đã hủy.");
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
                new DichVuBanHang().huyHoaDon(actor, invoiceId, cancellationReason, actor.getTenDangNhap(),
                        managerPassword);
                return null;
            }

            @Override
            protected void done() {
                Arrays.fill(managerPassword, '\0');
                try {
                    get();
                    taiDanhSach();
                    JOptionPane.showMessageDialog(HoaDonPanel.this, "Hủy hóa đơn thành công.", "Thành công",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception exception) {
                    baoLoi("Không hủy được hóa đơn.", exception);
                }
            }
        }.execute();
    }

    private void baoLoi(String message) {
        JOptionPane.showMessageDialog(this, message, "Lỗi", JOptionPane.ERROR_MESSAGE);
    }

    private void baoLoi(String fallback, Exception exception) {
        Throwable cause = exception;
        while (cause.getCause() != null
                && (cause instanceof java.util.concurrent.ExecutionException
                    || cause instanceof java.lang.reflect.InvocationTargetException))
            cause = cause.getCause();
        String message = cause instanceof IllegalArgumentException || cause instanceof IllegalStateException
                || cause instanceof SecurityException ? cause.getMessage() : fallback;
        baoLoi(message == null || message.trim().isEmpty() ? fallback : message);
    }
}
