package graphicUI;

import components.BarChart;
import components.UiFormat;
import entity.KhachHang;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import service.NguoiDung;
import service.SoLuongTheoHangThanhVien;
import service.DoanhThuTheoNgay;
import service.SanPhamSapHetHang;
import service.DoanhSoSanPham;
import service.DichVuBaoCao;
import service.TongHopBaoCao;

public final class ThongKePanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final String ALL_TIERS = "Tất cả";
    private static final String[] TIERS = {"CHUA_XEP_HANG", "DONG", "BAC", "VANG"};

    private static final class ReportData {
        private final TongHopBaoCao summary;
        private final List<DoanhThuTheoNgay> daily;
        private final List<DoanhSoSanPham> top;
        private final List<SanPhamSapHetHang> low;
        private final List<SoLuongTheoHangThanhVien> tiers;
        private final Map<String, List<KhachHang>> customersByTier;
        private final List<KhachHang> allCustomers;

        private ReportData(DichVuBaoCao service, NguoiDung actor, LocalDate from, LocalDate to) throws Exception {
            summary = service.tongHop(actor, from, to);
            daily = service.doanhThuTheoNgay(actor, from, to);
            top = service.sanPhamBanChay(actor, from, to);
            low = service.sanPhamSapHetHang(actor);
            tiers = service.soLuongTheoHangThanhVien(actor);
            customersByTier = new LinkedHashMap<String, List<KhachHang>>();
            allCustomers = new ArrayList<KhachHang>();
            for (String tier : TIERS) {
                List<KhachHang> customers = service.khachHangTheoHang(actor, tier);
                customersByTier.put(tier, customers);
                allCustomers.addAll(customers);
            }
        }

        private List<KhachHang> layKhachHang(String selectedTier) {
            return ALL_TIERS.equals(selectedTier) ? allCustomers : customersByTier.get(selectedTier);
        }
    }

    private final NguoiDung actor;
    private final DateField from = new DateField();
    private final DateField to = new DateField();
    private final JComboBox<String> customerTier = new JComboBox<String>(
            new String[] {ALL_TIERS, "CHUA_XEP_HANG", "DONG", "BAC", "VANG"});
    private final JLabel kpi = new JLabel();
    private final DefaultTableModel daily = modelKhongChoSua(new String[] {"Ngày", "Doanh thu", "Hóa đơn"});
    private final DefaultTableModel top = modelKhongChoSua(new String[] {"Mã", "Sản phẩm", "SL", "Doanh thu"});
    private final DefaultTableModel low = modelKhongChoSua(new String[] {"Mã", "Sản phẩm", "Tồn", "Ngưỡng"});
    private final DefaultTableModel tier = modelKhongChoSua(new String[] {"Hạng", "Số khách"});
    private final DefaultTableModel customers = modelKhongChoSua(new String[] {"Mã", "Tên", "Điện thoại", "Điểm"});
    private final JPanel chart = new JPanel(new BorderLayout());
    private ReportData report;

    public ThongKePanel(NguoiDung actor) {
        super(new BorderLayout(0, 8));
        this.actor = actor;
        from.setDate(LocalDate.now().minusDays(6));
        to.setDate(LocalDate.now());
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        themTruong(controls, "Từ ngày", from);
        themTruong(controls, "Đến ngày", to);
        themTruong(controls, "Hạng khách", customerTier);
        JButton load = new JButton("Tải báo cáo");
        load.addActionListener(event -> taiDanhSach());
        controls.add(load);
        add(controls, BorderLayout.NORTH);

        JTable dailyTable = new JTable(daily);
        dailyTable.getColumnModel().getColumn(1).setCellRenderer(UiFormat.moneyRenderer());
        JTable topTable = new JTable(top);
        topTable.getColumnModel().getColumn(3).setCellRenderer(UiFormat.moneyRenderer());
        JTabbedPane tabs = new JTabbedPane();
        tabs.add("Doanh thu ngày", new JScrollPane(dailyTable));
        tabs.add("Bán chạy", new JScrollPane(topTable));
        tabs.add("Tồn thấp", new JScrollPane(new JTable(low)));
        tabs.add("Hạng", new JScrollPane(new JTable(tier)));
        tabs.add("Khách", new JScrollPane(new JTable(customers)));
        tabs.add("Biểu đồ", chart);
        add(tabs, BorderLayout.CENTER);
        add(kpi, BorderLayout.SOUTH);
        customerTier.addActionListener(event -> hienKhachHang());
        taiDanhSach();
    }

    private static DefaultTableModel modelKhongChoSua(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private void themTruong(JPanel panel, String text, JComponent field) {
        JLabel label = new JLabel(text);
        label.setLabelFor(field);
        panel.add(label);
        panel.add(field);
    }

    private void taiDanhSach() {
        final LocalDate start;
        final LocalDate end;
        try {
            start = LocalDate.parse(from.getText().trim());
            end = LocalDate.parse(to.getText().trim());
        } catch (DateTimeParseException exception) {
            baoLoi("Ngày phải theo định dạng yyyy-MM-dd.");
            return;
        }
        if (start.isAfter(end)) {
            baoLoi("Từ ngày không được sau đến ngày.");
            return;
        }
        report = null;
        daily.setRowCount(0);
        top.setRowCount(0);
        low.setRowCount(0);
        tier.setRowCount(0);
        customers.setRowCount(0);
        kpi.setText("Đang tải báo cáo...");
        new SwingWorker<ReportData, Void>() {
            @Override
            protected ReportData doInBackground() throws Exception {
                return new ReportData(new DichVuBaoCao(), actor, start, end);
            }

            @Override
            protected void done() {
                try {
                    report = get();
                    TongHopBaoCao summary = report.summary;
                    kpi.setText("Doanh thu: " + summary.getDoanhThu() + " · Hóa đơn: " + summary.getSoHoaDon()
                            + " · Tiền nhập: " + summary.getTienNhapHang() + " · Giá vốn FIFO: " + summary.getGiaVon()
                            + " · Lợi nhuận gộp: " + summary.getLoiNhuanGop());
                    hienDoanhThuTheoNgay(report.daily);
                    hienSanPhamBanChay(report.top);
                    hienSanPhamSapHet(report.low);
                    hienHangThanhVien(report.tiers);
                    hienKhachHang();
                } catch (Exception exception) {
                    kpi.setText("");
                    baoLoi("Không tải được báo cáo.", exception);
                }
            }
        }.execute();
    }

    private void hienDoanhThuTheoNgay(List<DoanhThuTheoNgay> values) {
        daily.setRowCount(0);
        String[] labels = new String[values.size()];
        BigDecimal[] revenues = new BigDecimal[values.size()];
        for (int i = 0; i < values.size(); i++) {
            DoanhThuTheoNgay value = values.get(i);
            daily.addRow(new Object[] {value.getNgay(), value.getDoanhThu(), value.getSoHoaDon()});
            labels[i] = value.getNgay().toString();
            revenues[i] = value.getDoanhThu();
        }
        chart.removeAll();
        chart.add(new BarChart(labels, revenues), BorderLayout.CENTER);
        chart.revalidate();
        chart.repaint();
    }

    private void hienSanPhamBanChay(List<DoanhSoSanPham> values) {
        top.setRowCount(0);
        for (DoanhSoSanPham value : values) {
            top.addRow(new Object[] {value.getMaSanPham(), value.getTenSanPham(), value.getSoLuong(),
                    value.getDoanhThu()});
        }
    }

    private void hienSanPhamSapHet(List<SanPhamSapHetHang> values) {
        low.setRowCount(0);
        for (SanPhamSapHetHang value : values) {
            low.addRow(new Object[] {value.getMaSanPham(), value.getTenSanPham(), value.getTonKho(),
                    value.getNguong()});
        }
    }

    private void hienHangThanhVien(List<SoLuongTheoHangThanhVien> values) {
        tier.setRowCount(0);
        for (SoLuongTheoHangThanhVien value : values) {
            tier.addRow(new Object[] {value.getHang(), value.getSoLuong()});
        }
    }

    private void hienKhachHang() {
        customers.setRowCount(0);
        if (report == null) return;
        List<KhachHang> values = report.layKhachHang((String) customerTier.getSelectedItem());
        if (values == null) return;
        for (KhachHang value : values) {
            customers.addRow(new Object[] {value.getMaKhachHang(), value.getHoTen(), value.getSoDienThoai(),
                    value.getDiemTichLuy()});
        }
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
        String message = cause instanceof IllegalArgumentException || cause instanceof SecurityException
                ? cause.getMessage() : fallback;
        baoLoi(message == null || message.trim().isEmpty() ? fallback : message);
    }
}
