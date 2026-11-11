package graphicUI;

import components.UiFormat;
import components.UiTheme;
import components.DialogActions;
import components.FormValidator;
import entity.KhuyenMai;
import entity.SanPham;
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
import service.MasterDataService;
import service.PromotionService;

@SuppressWarnings("serial")
public final class PromotionPanel extends JPanel {
    private final Actor actor;
    private final JTextField key = new JTextField(12);
    private final JComboBox<String> state = new JComboBox<String>(
            new String[] {"Tất cả", "DANG_HOAT_DONG", "NGUNG_HOAT_DONG"});
    private final List<KhuyenMai> rows = new ArrayList<KhuyenMai>();
    private final DefaultTableModel model = new DefaultTableModel(
            new String[] {"Mã", "Tên", "Phạm vi", "Tỷ lệ", "Giá trị tối thiểu", "Bắt đầu", "Kết thúc", "Trạng thái", "Sản phẩm"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);

    public PromotionPanel(Actor actor) {
        super(new BorderLayout(0, 8));
        this.actor = actor;
        table.getColumnModel().getColumn(4).setCellRenderer(UiFormat.moneyRenderer());
        setBackground(UiTheme.BACKGROUND);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bar.setBackground(Color.WHITE);
        bar.setBorder(UiTheme.CARD);
        addField(bar, "Từ khóa", key);
        addField(bar, "Trạng thái", state);
        addButton(bar, "Tìm", new Runnable() { @Override public void run() { load(); } });
        addButton(bar, "Chi tiết", new Runnable() { @Override public void run() { detail(); } });
        addButton(bar, "Thêm", new Runnable() { @Override public void run() { edit(null); } });
        addButton(bar, "Sửa", new Runnable() { @Override public void run() {
            KhuyenMai value = selected();
            if (value == null) { showError("Hãy chọn khuyến mãi."); return; }
            edit(value);
        } });
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
        final String filter = "Tất cả".equals(state.getSelectedItem()) ? ""
                : (String) state.getSelectedItem();
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
                                value.getPhamVi(), value.getTyLeGiam(), value.getDonToiThieu(), value.getBatDau(), value.getKetThuc(),
                                value.getTrangThai(), joinProducts(value.getMaSanPham())});
                    }
                } catch (Exception exception) {
                    showError("Không tải được khuyến mãi.", exception);
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
        addDetail(details, "Giá trị tối thiểu", value.getDonToiThieu() == null
                ? "" : UiFormat.money(value.getDonToiThieu()) + " đ");
        addDetail(details, "Bắt đầu", value.getBatDau());
        addDetail(details, "Kết thúc", value.getKetThuc());
        addDetail(details, "Trạng thái", value.getTrangThai());
        addDetail(details, "Sản phẩm áp dụng", joinProducts(value.getMaSanPham()));
        DialogActions.info(this, details, "Chi tiết khuyến mãi");
    }

    private void addDetail(JPanel panel, String label, Object value) {
        panel.add(new JLabel(label));
        panel.add(new JLabel(value == null ? "" : String.valueOf(value)));
    }

    @SuppressWarnings("unchecked")
    private void edit(final KhuyenMai existing) {
        new SwingWorker<Object[], Void>() {
            @Override
            protected Object[] doInBackground() throws Exception {
                String promotionId = existing == null
                        ? new PromotionService().maKhuyenMaiTiepTheo(actor) : existing.getMaKhuyenMai();
                List<SanPham> catalog = new MasterDataService().timSanPham(
                        actor, null, null, "DANG_KINH_DOANH", null, null);
                return new Object[] {promotionId, catalog};
            }

            @Override
            protected void done() {
                try {
                    Object[] result = get();
                    editForm((String) result[0], existing, (List<SanPham>) result[1]);
                } catch (Exception exception) {
                    showError("Không tải được dữ liệu khuyến mãi.", exception);
                }
            }
        }.execute();
    }

    private void editForm(String generatedId, KhuyenMai existing, List<SanPham> catalog) {
        JTextField id = new JTextField(existing == null ? generatedId : existing.getMaKhuyenMai());
        JTextField name = new JTextField(existing == null ? "" : existing.getTenKhuyenMai());
        JTextField rate = new JTextField(existing == null ? "" : existing.getTyLeGiam().toPlainString());
        DateTimeField start = new DateTimeField();
        DateTimeField end = new DateTimeField();
        if (existing != null) {
            start.setText(existing.getBatDau().toString());
            end.setText(existing.getKetThuc().toString());
        }
        JComboBox<String> scope = new JComboBox<String>(new String[] {"TOAN_DON", "THEO_SAN_PHAM"});
        JComboBox<String> status = new JComboBox<String>(new String[] {"DANG_HOAT_DONG", "NGUNG_HOAT_DONG"});
        JTextField products = new JTextField(existing == null ? "" : joinProducts(existing.getMaSanPham()));
        products.setColumns(18);
        JComboBox<String> picker = new JComboBox<String>();
        if (catalog != null)
            for (SanPham value : catalog)
                picker.addItem(UiFormat.joinIdName(value.getMaSanPham(), value.getTenSanPham()));
        picker.addActionListener(event -> {
            String productId = UiFormat.idOf(String.valueOf(picker.getSelectedItem()));
            if (!productId.isEmpty() && !productIds(products.getText()).contains(productId)) {
                String current = products.getText().trim();
                products.setText(current.isEmpty() ? productId : current + ", " + productId);
            }
            picker.setSelectedIndex(-1);
        });
        JTextField minOrder = new JTextField(existing == null || existing.getDonToiThieu() == null
                ? "" : existing.getDonToiThieu().toPlainString());
        scope.setSelectedItem(existing == null ? "TOAN_DON" : existing.getPhamVi());
        status.setSelectedItem(existing == null ? "DANG_HOAT_DONG" : existing.getTrangThai());
        id.setEditable(false);
        products.setEnabled("THEO_SAN_PHAM".equals(scope.getSelectedItem()));
        minOrder.setEnabled("TOAN_DON".equals(scope.getSelectedItem()));
        scope.addActionListener(event -> {
            products.setEnabled("THEO_SAN_PHAM".equals(scope.getSelectedItem()));
            minOrder.setEnabled("TOAN_DON".equals(scope.getSelectedItem()));
        });

        JPanel productRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        productRow.add(products);
        productRow.add(picker);
        FormValidator validator = new FormValidator();
        JPanel form = FormValidator.form();
        form.add(validator.row("Mã khuyến mãi", id));
        form.add(validator.row("Phạm vi", scope));
        form.add(validator.row("Giá trị tối thiểu (chỉ toàn đơn)", minOrder));
        form.add(validator.row("Sản phẩm áp dụng (có thể chọn nhiều)", productRow, products));
        form.add(validator.row("Tên", name));
        form.add(validator.row("Tỷ lệ %", rate));
        form.add(validator.row("Bắt đầu", start));
        form.add(validator.row("Kết thúc", end));
        form.add(validator.row("Trạng thái", status));
        validator.require(id, "Mã khuyến mãi không được để trống.");
        validator.matches(id, "KM[0-9]{3}",
                "Mã khuyến mãi sai định dạng, phải có dạng KM + đúng 3 số (ví dụ: KM001).");
        validator.require(name, "Tên khuyến mãi không được để trống.");
        validator.matches(name, "Khuyến mãi[\\p{L}0-9\\s-]*",
                "Tên khuyến mãi sai định dạng, phải bắt đầu bằng \"Khuyến mãi\" (ví dụ: Khuyến mãi giảm 10%).");
        validator.require(rate, "Tỷ lệ % không được để trống.");
        validator.number(rate, "Tỷ lệ % phải là số.");
        validator.check(rate, text -> {
            try {
                BigDecimal value = new BigDecimal(text);
                return value.signum() > 0 && value.compareTo(new BigDecimal("100")) <= 0;
            } catch (NumberFormatException exception) {
                return true;
            }
        }, "Tỷ lệ phải trong khoảng lớn hơn 0 đến 100.");
        validator.custom(start, () -> {
            String text = start.getText().trim();
            if (text.isEmpty())
                return "Thời gian bắt đầu không được để trống.";
            try {
                LocalDateTime.parse(text);
                return null;
            } catch (DateTimeParseException exception) {
                return "Ngày giờ phải theo yyyy-MM-ddTHH:mm.";
            }
        });
        validator.custom(end, () -> {
            String text = end.getText().trim();
            if (text.isEmpty())
                return "Thời gian kết thúc không được để trống.";
            try {
                LocalDateTime parsed = LocalDateTime.parse(text);
                String beginText = start.getText().trim();
                if (!beginText.isEmpty()) {
                    try {
                        if (!LocalDateTime.parse(beginText).isBefore(parsed))
                            return "Thời gian kết thúc phải sau thời gian bắt đầu.";
                    } catch (DateTimeParseException ignored) {
                    }
                }
                return null;
            } catch (DateTimeParseException exception) {
                return "Ngày giờ phải theo yyyy-MM-ddTHH:mm.";
            }
        });
        validator.custom(minOrder, () -> {
            if (!"TOAN_DON".equals(scope.getSelectedItem()))
                return null;
            String text = minOrder.getText().trim();
            if (text.isEmpty())
                return "Giá trị tối thiểu không được để trống cho phạm vi toàn đơn.";
            try {
                return new BigDecimal(text).signum() > 0 ? null
                        : "Giá trị tối thiểu phải lớn hơn 0.";
            } catch (NumberFormatException exception) {
                return "Giá trị tối thiểu phải là số.";
            }
        });
        validator.custom(products, () -> {
            if (!"THEO_SAN_PHAM".equals(scope.getSelectedItem()))
                return null;
            Set<String> ids = productIds(products.getText());
            if (ids.isEmpty())
                return "Phạm vi theo sản phẩm phải chọn ít nhất một sản phẩm.";
            for (String productId : ids)
                if (!productId.matches("893[0-9]{9}"))
                    return "Mã sản phẩm " + productId + " sai định dạng (893 + 9 số).";
            return null;
        });
        if (!DialogActions.confirm(this, form, existing == null ? "Thêm khuyến mãi" : "Sửa khuyến mãi",
                validator)) return;

        final String promotionId = id.getText().trim();
        final String promotionName = name.getText().trim();
        final String promotionScope = (String) scope.getSelectedItem();
        final String promotionStatus = (String) status.getSelectedItem();
        final BigDecimal promotionRate = new BigDecimal(rate.getText().trim());
        final BigDecimal promotionMinOrder = "TOAN_DON".equals(promotionScope)
                ? new BigDecimal(minOrder.getText().trim()) : null;
        final LocalDateTime begins = LocalDateTime.parse(start.getText().trim());
        final LocalDateTime ends = LocalDateTime.parse(end.getText().trim());
        final Set<String> productIds = "THEO_SAN_PHAM".equals(promotionScope)
                ? productIds(products.getText()) : new LinkedHashSet<String>();
        final KhuyenMai value = new KhuyenMai();
        value.setMaKhuyenMai(promotionId);
        value.setTenKhuyenMai(promotionName);
        value.setTyLeGiam(promotionRate);
        value.setDonToiThieu(promotionMinOrder);
        value.setBatDau(begins);
        value.setKetThuc(ends);
        value.setPhamVi(promotionScope);
        value.setTrangThai(promotionStatus);
        value.setMaSanPham(productIds);
        final boolean isNew = existing == null;
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                PromotionService service = new PromotionService();
                if (isNew) {
                    for (KhuyenMai found : service.tim(actor, value.getMaKhuyenMai(), null))
                        if (value.getMaKhuyenMai().equals(found.getMaKhuyenMai()))
                            throw new IllegalArgumentException("Mã khuyến mãi đã tồn tại");
                }
                service.luu(actor, value);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    load();
                    JOptionPane.showMessageDialog(PromotionPanel.this, "Lưu khuyến mãi thành công.", "Thành công",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception exception) {
                    showError("Không lưu được khuyến mãi.", exception);
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
                    JOptionPane.showMessageDialog(PromotionPanel.this, "Đổi trạng thái khuyến mãi thành công.", "Thành công",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception exception) {
                    showError("Không đổi được trạng thái.", exception);
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
                    JOptionPane.showMessageDialog(PromotionPanel.this, "Xóa khuyến mãi thành công.", "Thành công",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception exception) {
                    showError("Không thể xóa khuyến mãi đã tham chiếu.", exception);
                }
            }
        }.execute();
    }

    private void showError(String message) {
        showError(message, null);
    }

    private void showError(String message, Exception exception) {
        String text = message;
        if (exception != null) {
            Throwable cause = exception;
            while (cause.getCause() != null
                    && cause instanceof java.util.concurrent.ExecutionException)
                cause = cause.getCause();
            if (cause instanceof IllegalArgumentException || cause instanceof IllegalStateException)
                text = cause.getMessage();
        }
        JOptionPane.showMessageDialog(this, text == null ? message : text, "Lỗi", JOptionPane.ERROR_MESSAGE);
    }
}
