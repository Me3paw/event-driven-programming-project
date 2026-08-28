package graphicUI;

import components.BarChart;
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
import service.Actor;
import service.CustomerTierCount;
import service.DailyRevenue;
import service.LowStockProduct;
import service.ProductSales;
import service.ReportService;
import service.ReportSummary;

public final class ReportPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final String ALL_TIERS = "Tất cả";
    private static final String[] TIERS = {"CHUA_XEP_HANG", "DONG", "BAC", "VANG"};

    private static final class ReportData {
        private final ReportSummary summary;
        private final List<DailyRevenue> daily;
        private final List<ProductSales> top;
        private final List<LowStockProduct> low;
        private final List<CustomerTierCount> tiers;
        private final Map<String, List<KhachHang>> customersByTier;
        private final List<KhachHang> allCustomers;

        private ReportData(ReportService service, Actor actor, LocalDate from, LocalDate to) throws Exception {
            summary = service.summary(actor, from, to);
            daily = service.dailyRevenue(actor, from, to);
            top = service.topProducts(actor, from, to);
            low = service.lowStock(actor);
            tiers = service.customerTierCounts(actor);
            customersByTier = new LinkedHashMap<String, List<KhachHang>>();
            allCustomers = new ArrayList<KhachHang>();
            for (String tier : TIERS) {
                List<KhachHang> customers = service.customersByTier(actor, tier);
                customersByTier.put(tier, customers);
                allCustomers.addAll(customers);
            }
        }

        private List<KhachHang> customers(String selectedTier) {
            return ALL_TIERS.equals(selectedTier) ? allCustomers : customersByTier.get(selectedTier);
        }
    }

    private final Actor actor;
    private final JTextField from = new JTextField(LocalDate.now().minusDays(6).toString(), 10);
    private final JTextField to = new JTextField(LocalDate.now().toString(), 10);
    private final JComboBox<String> customerTier = new JComboBox<String>(
            new String[] {ALL_TIERS, "CHUA_XEP_HANG", "DONG", "BAC", "VANG"});
    private final JLabel kpi = new JLabel();
    private final DefaultTableModel daily = readonlyModel(new String[] {"Ngày", "Doanh thu", "Hóa đơn"});
    private final DefaultTableModel top = readonlyModel(new String[] {"Mã", "Sản phẩm", "SL", "Doanh thu"});
    private final DefaultTableModel low = readonlyModel(new String[] {"Mã", "Sản phẩm", "Tồn", "Ngưỡng"});
    private final DefaultTableModel tier = readonlyModel(new String[] {"Hạng", "Số khách"});
    private final DefaultTableModel customers = readonlyModel(new String[] {"Mã", "Tên", "Điện thoại", "Điểm"});
    private final JPanel chart = new JPanel(new BorderLayout());
    private ReportData report;

    public ReportPanel(Actor actor) {
        super(new BorderLayout(0, 8));
        this.actor = actor;
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addField(controls, "Từ ngày (yyyy-MM-dd)", from);
        addField(controls, "Đến ngày (yyyy-MM-dd)", to);
        addField(controls, "Hạng khách", customerTier);
        JButton load = new JButton("Tải báo cáo");
        load.addActionListener(event -> load());
        controls.add(load);
        add(controls, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.add("Doanh thu ngày", new JScrollPane(new JTable(daily)));
        tabs.add("Bán chạy", new JScrollPane(new JTable(top)));
        tabs.add("Tồn thấp", new JScrollPane(new JTable(low)));
        tabs.add("Hạng", new JScrollPane(new JTable(tier)));
        tabs.add("Khách", new JScrollPane(new JTable(customers)));
        tabs.add("Biểu đồ", chart);
        add(tabs, BorderLayout.CENTER);
        add(kpi, BorderLayout.SOUTH);
        customerTier.addActionListener(event -> setCustomers());
        load();
    }

    private static DefaultTableModel readonlyModel(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private void addField(JPanel panel, String text, JComponent field) {
        JLabel label = new JLabel(text);
        label.setLabelFor(field);
        panel.add(label);
        panel.add(field);
    }

    private void load() {
        final LocalDate start;
        final LocalDate end;
        try {
            start = LocalDate.parse(from.getText().trim());
            end = LocalDate.parse(to.getText().trim());
        } catch (DateTimeParseException exception) {
            showError("Ngày phải theo định dạng yyyy-MM-dd.");
            return;
        }
        if (start.isAfter(end)) {
            showError("Từ ngày không được sau đến ngày.");
            return;
        }
        new SwingWorker<ReportData, Void>() {
            @Override
            protected ReportData doInBackground() throws Exception {
                return new ReportData(new ReportService(), actor, start, end);
            }

            @Override
            protected void done() {
                try {
                    report = get();
                    ReportSummary summary = report.summary;
                    kpi.setText("Doanh thu: " + summary.getRevenue() + " · Hóa đơn: " + summary.getInvoiceCount()
                            + " · Tiền nhập: " + summary.getPurchaseSpend() + " · Giá vốn FIFO: " + summary.getCogs()
                            + " · Lợi nhuận gộp: " + summary.getGrossProfit());
                    setDaily(report.daily);
                    setTop(report.top);
                    setLow(report.low);
                    setTier(report.tiers);
                    setCustomers();
                } catch (Exception exception) {
                    showError("Không tải được báo cáo.");
                }
            }
        }.execute();
    }

    private void setDaily(List<DailyRevenue> values) {
        daily.setRowCount(0);
        String[] labels = new String[values.size()];
        BigDecimal[] revenues = new BigDecimal[values.size()];
        for (int i = 0; i < values.size(); i++) {
            DailyRevenue value = values.get(i);
            daily.addRow(new Object[] {value.getDate(), value.getRevenue(), value.getInvoiceCount()});
            labels[i] = value.getDate().toString();
            revenues[i] = value.getRevenue();
        }
        chart.removeAll();
        chart.add(new BarChart(labels, revenues), BorderLayout.CENTER);
        chart.revalidate();
        chart.repaint();
    }

    private void setTop(List<ProductSales> values) {
        top.setRowCount(0);
        for (ProductSales value : values) {
            top.addRow(new Object[] {value.getProductId(), value.getProductName(), value.getQuantity(),
                    value.getRevenue()});
        }
    }

    private void setLow(List<LowStockProduct> values) {
        low.setRowCount(0);
        for (LowStockProduct value : values) {
            low.addRow(new Object[] {value.getProductId(), value.getProductName(), value.getStock(),
                    value.getThreshold()});
        }
    }

    private void setTier(List<CustomerTierCount> values) {
        tier.setRowCount(0);
        for (CustomerTierCount value : values) {
            tier.addRow(new Object[] {value.getTier(), value.getCount()});
        }
    }

    private void setCustomers() {
        customers.setRowCount(0);
        if (report == null) return;
        List<KhachHang> values = report.customers((String) customerTier.getSelectedItem());
        if (values == null) return;
        for (KhachHang value : values) {
            customers.addRow(new Object[] {value.getMaKhachHang(), value.getHoTen(), value.getSoDienThoai(),
                    value.getDiemTichLuy()});
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message);
    }
}
