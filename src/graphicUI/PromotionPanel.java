package graphicUI;

import components.UiTheme;
import components.DialogActions;
import entity.KhuyenMai;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import service.Actor;
import service.PromotionService;

@SuppressWarnings("serial")
public final class PromotionPanel extends JPanel {
    private final Actor actor;
    private final JTextField key = new JTextField(12);
    private final JTextField state = new JTextField("", 9);
    private final List<KhuyenMai> rows = new ArrayList<KhuyenMai>();
    private final DefaultTableModel model = new DefaultTableModel(
            new String[] {"Mã", "Tên", "Phạm vi", "Tỷ lệ", "Bắt đầu", "Kết thúc", "Trạng thái", "Sản phẩm"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);

    public PromotionPanel(Actor actor) {
        super(new BorderLayout(0, 8));
        this.actor = actor;
        setBackground(UiTheme.BACKGROUND);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bar.setBackground(Color.WHITE);
        bar.setBorder(UiTheme.CARD);
        addField(bar, "Từ khóa", key);
        addField(bar, "Trạng thái", state);
        addButton(bar, "Tìm", new Runnable() { @Override public void run() { load(); } });
        addButton(bar, "Chi tiết", new Runnable() { @Override public void run() { detail(); } });
        addButton(bar, "Thêm / sửa", new Runnable() { @Override public void run() { edit(); } });
        addButton(bar, "Đổi trạng thái", new Runnable() { @Override public void run() { toggleStatus(); } });
        addButton(bar, "Xóa", new Runnable() { @Override public void run() { remove(); } });
        add(bar, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        load();
    }

    private void addField(JPanel panel, String text, JComponent field) {
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
        final String filter = state.getText().trim();
        table.clearSelection();
        new SwingWorker<List<KhuyenMai>, Void>() {
            @Override
            protected List<KhuyenMai> doInBackground() throws Exception {
                return new PromotionService().tim(actor, keyword, filter);
            }

            @Override
            protected void done() {
                try {
                    rows.clear();
                    rows.addAll(get());
                    model.setRowCount(0);
                    for (KhuyenMai value : rows) {
                        model.addRow(new Object[] {value.getMaKhuyenMai(), value.getTenKhuyenMai(),
                                value.getPhamVi(), value.getTyLeGiam(), value.getBatDau(), value.getKetThuc(),
                                value.getTrangThai(), joinProducts(value.getMaSanPham())});
                    }
                } catch (Exception exception) {
                    showError("Không tải được khuyến mãi.");
                }
            }
        }.execute();
    }

    private KhuyenMai selected() {
        int row = table.getSelectedRow();
        return row < 0 ? null : rows.get(table.convertRowIndexToModel(row));
    }

    private void detail() {
        KhuyenMai value = selected();
        if (value == null) {
            showError("Hãy chọn khuyến mãi.");
            return;
        }
        JPanel details = new JPanel(new GridLayout(0, 2, 8, 4));
        addDetail(details, "Mã", value.getMaKhuyenMai());
        addDetail(details, "Tên", value.getTenKhuyenMai());
        addDetail(details, "Phạm vi", value.getPhamVi());
        addDetail(details, "Tỷ lệ %", value.getTyLeGiam());
        addDetail(details, "Bắt đầu", value.getBatDau());
        addDetail(details, "Kết thúc", value.getKetThuc());
        addDetail(details, "Trạng thái", value.getTrangThai());
        addDetail(details, "Sản phẩm áp dụng", joinProducts(value.getMaSanPham()));
        JOptionPane.showMessageDialog(this, details, "Chi tiết khuyến mãi", JOptionPane.INFORMATION_MESSAGE);
    }

    private void addDetail(JPanel panel, String label, Object value) {
        panel.add(new JLabel(label));
        panel.add(new JLabel(value == null ? "" : String.valueOf(value)));
    }

    private void edit() {
        KhuyenMai existing = selected();
        JTextField id = new JTextField(existing == null ? "" : existing.getMaKhuyenMai());
        JTextField name = new JTextField(existing == null ? "" : existing.getTenKhuyenMai());
        JTextField rate = new JTextField(existing == null ? "" : existing.getTyLeGiam().toPlainString());
        JTextField start = new JTextField(existing == null ? "" : existing.getBatDau().toString());
        JTextField end = new JTextField(existing == null ? "" : existing.getKetThuc().toString());
        JComboBox<String> scope = new JComboBox<String>(new String[] {"TOAN_DON", "THEO_SAN_PHAM"});
        JComboBox<String> status = new JComboBox<String>(new String[] {"DANG_HOAT_DONG", "NGUNG_HOAT_DONG"});
        JTextField products = new JTextField(existing == null ? "" : joinProducts(existing.getMaSanPham()));
        scope.setSelectedItem(existing == null ? "TOAN_DON" : existing.getPhamVi());
        status.setSelectedItem(existing == null ? "DANG_HOAT_DONG" : existing.getTrangThai());
        id.setEditable(existing == null);
        products.setEnabled("THEO_SAN_PHAM".equals(scope.getSelectedItem()));
        scope.addActionListener(event -> products.setEnabled("THEO_SAN_PHAM".equals(scope.getSelectedItem())));

        JPanel form = new JPanel(new GridLayout(0, 1, 0, 4));
        addField(form, "Mã", id);
        addField(form, "Tên", name);
        addField(form, "Tỷ lệ %", rate);
        addField(form, "Bắt đầu (yyyy-MM-ddTHH:mm)", start);
        addField(form, "Kết thúc (yyyy-MM-ddTHH:mm)", end);
        addField(form, "Phạm vi", scope);
        addField(form, "Trạng thái", status);
        addField(form, "Mã sản phẩm, cách nhau dấu phẩy", products);
        if (!DialogActions.confirm(this, form, existing == null ? "Thêm khuyến mãi" : "Sửa khuyến mãi")) return;

        final String promotionId = id.getText().trim();
        final String promotionName = name.getText().trim();
        final String promotionScope = (String) scope.getSelectedItem();
        final String promotionStatus = (String) status.getSelectedItem();
        final BigDecimal promotionRate;
        final LocalDateTime begins;
        final LocalDateTime ends;
        try {
            promotionRate = new BigDecimal(rate.getText().trim());
            begins = LocalDateTime.parse(start.getText().trim());
            ends = LocalDateTime.parse(end.getText().trim());
        } catch (NumberFormatException exception) {
            showError("Tỷ lệ và ngày giờ không hợp lệ.");
            return;
        } catch (DateTimeParseException exception) {
            showError("Ngày giờ phải theo yyyy-MM-ddTHH:mm.");
            return;
        }
        final Set<String> productIds = "THEO_SAN_PHAM".equals(promotionScope)
                ? productIds(products.getText()) : new LinkedHashSet<String>();
        if (promotionId.isEmpty() || promotionName.isEmpty() || promotionRate.signum() <= 0
                || promotionRate.compareTo(new BigDecimal("100")) > 0 || !begins.isBefore(ends)
                || ("THEO_SAN_PHAM".equals(promotionScope) && productIds.isEmpty())) {
            showError("Nhập đủ thông tin; tỷ lệ 0-100, thời gian hợp lệ và sản phẩm cho phạm vi theo sản phẩm.");
            return;
        }
        final KhuyenMai value = new KhuyenMai();
        value.setMaKhuyenMai(promotionId);
        value.setTenKhuyenMai(promotionName);
        value.setTyLeGiam(promotionRate);
        value.setBatDau(begins);
        value.setKetThuc(ends);
        value.setPhamVi(promotionScope);
        value.setTrangThai(promotionStatus);
        value.setMaSanPham(productIds);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                new PromotionService().luu(actor, value);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    load();
                } catch (Exception exception) {
                    showError("Không lưu được khuyến mãi.");
                }
            }
        }.execute();
    }

    private Set<String> productIds(String value) {
        Set<String> ids = new LinkedHashSet<String>();
        for (String id : value.split(",")) {
            String trimmed = id.trim();
            if (!trimmed.isEmpty()) ids.add(trimmed);
        }
        return ids;
    }

    private String joinProducts(Set<String> products) {
        StringBuilder result = new StringBuilder();
        if (products != null) {
            for (String product : products) {
                if (result.length() > 0) result.append(", ");
                result.append(product);
            }
        }
        return result.toString();
    }

    private void toggleStatus() {
        KhuyenMai selected = selected();
        if (selected == null) {
            showError("Hãy chọn khuyến mãi.");
            return;
        }
        final String id = selected.getMaKhuyenMai();
        final String value = "DANG_HOAT_DONG".equals(selected.getTrangThai())
                ? "NGUNG_HOAT_DONG" : "DANG_HOAT_DONG";
        String message = ("DANG_HOAT_DONG".equals(value) ? "Kích hoạt lại " : "Ngừng hoạt động ") + id + "?";
        if (JOptionPane.showConfirmDialog(this, message, "Xác nhận", JOptionPane.YES_NO_OPTION)
                != JOptionPane.YES_OPTION) return;
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                new PromotionService().capNhatTrangThai(actor, id, value);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    load();
                } catch (Exception exception) {
                    showError("Không đổi được trạng thái.");
                }
            }
        }.execute();
    }

    private void remove() {
        KhuyenMai selected = selected();
        if (selected == null) {
            showError("Hãy chọn khuyến mãi.");
            return;
        }
        final String id = selected.getMaKhuyenMai();
        if (JOptionPane.showConfirmDialog(this, "Xóa " + id + "?", "Xác nhận", JOptionPane.YES_NO_OPTION)
                != JOptionPane.YES_OPTION) return;
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                new PromotionService().xoa(actor, id);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    load();
                } catch (Exception exception) {
                    showError("Không thể xóa khuyến mãi đã tham chiếu.");
                }
            }
        }.execute();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message);
    }
}
