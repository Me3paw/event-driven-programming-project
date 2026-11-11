package graphicUI;

import components.UiFormat;
import components.UiTheme;
import components.DialogActions;
import components.FormValidator;
import entity.PhieuDieuChinhTon;
import entity.LoTonKho;
import entity.SanPham;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.DefaultListCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import service.Actor;
import service.InventoryService;
import service.MasterDataService;
import service.LowStockProduct;
import service.ReportService;

@SuppressWarnings("serial")
public final class InventoryPanel extends JPanel {
    private final Actor actor;
    private final boolean history;
    private final JTextField key = new JTextField(16);
    private final JCheckBox lowOnly = new JCheckBox("Chỉ tồn thấp");
    private final DefaultTableModel stockModel = new DefaultTableModel(
            new String[] {"Lô", "Phiếu nhập", "Sản phẩm", "Nhập", "Còn", "Giá vốn", "Thời điểm"}, 0) {
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final DefaultTableModel adjustmentModel =
            new DefaultTableModel(new String[] {"Mã", "Thời điểm", "Quản lý", "Lý do", "Số lô"}, 0) {
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
    private final JTable stockTable = new JTable(stockModel);
    private final JTable adjustmentTable = new JTable(adjustmentModel);

    public InventoryPanel(Actor actor) {
        this(actor, false);
    }

    public InventoryPanel(Actor actor, boolean history) {
        super(new BorderLayout(0, 8));
        this.actor = actor;
        this.history = history;
        stockTable.getColumnModel().getColumn(5).setCellRenderer(UiFormat.moneyRenderer());
        setBackground(UiTheme.BACKGROUND);
        if (history)
            adjustmentPage();
        else
            stockPage();
    }

    private void stockPage() {
        JPanel bar = bar("Sản phẩm / từ khóa");
        if (actor.isQuanLy()) {
            lowOnly.setOpaque(false);
            lowOnly.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    load();
                }
            });
            bar.add(lowOnly);
            JButton adjust = new JButton("Điều chỉnh giảm FIFO");
            adjust.setMnemonic(KeyEvent.VK_D);
            adjust.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    adjust();
                }
            });
            bar.add(adjust);
        }
        add(bar, BorderLayout.NORTH);
        add(new JScrollPane(stockTable), BorderLayout.CENTER);
        load();
    }

    private void adjustmentPage() {
        JPanel bar = bar("Mã phiếu điều chỉnh / lý do");
        JButton detail = new JButton("Chi tiết");
        detail.setMnemonic(KeyEvent.VK_C);
        detail.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                detail();
            }
        });
        bar.add(detail);
        if (actor.isQuanLy()) {
            JButton adjust = new JButton("Điều chỉnh giảm FIFO");
            adjust.setMnemonic(KeyEvent.VK_D);
            adjust.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    adjust();
                }
            });
            bar.add(adjust);
        }
        add(bar, BorderLayout.NORTH);
        add(new JScrollPane(adjustmentTable), BorderLayout.CENTER);
        load();
    }

    private JPanel bar(String label) {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bar.setBackground(Color.WHITE);
        bar.setBorder(UiTheme.CARD);
        addField(bar, label, key);
        JButton find = new JButton("Tìm");
        find.setMnemonic(KeyEvent.VK_T);
        find.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                load();
            }
        });
        bar.add(find);
        return bar;
    }

    private void load() {
        if (history)
            loadHistory();
        else
            loadStock();
    }

    private void loadStock() {
        final String keyword = key.getText().trim();
        final boolean onlyLow = actor.isQuanLy() && lowOnly.isSelected();
        new SwingWorker<StockData, Void>() {
            protected StockData doInBackground() throws Exception {
                List<LoTonKho> lots = new InventoryService().tonKho(actor, keyword);
                Set<String> low = new HashSet<String>();
                if (onlyLow)
                    for (LowStockProduct product : new ReportService().lowStock(actor)) low.add(product.getProductId());
                return new StockData(lots, low);
            }
            protected void done() {
                try {
                    StockData data = get();
                    stockModel.setRowCount(0);
                    for (LoTonKho lot : data.lots)
                        if (!onlyLow || data.lowProducts.contains(lot.getMaSanPham()))
                            stockModel.addRow(new Object[] {lot.getMaLo(), lot.getMaPhieuNhap(), lot.getMaSanPham(),
                                    lot.getSoLuongNhap(), lot.getSoLuongCon(), lot.getDonGiaVon(),
                                    lot.getThoiDiemNhap()});
                } catch (Exception exception) {
                    message("Không tải được tồn kho.", exception);
                }
            }
        }.execute();
    }

    private void loadHistory() {
        final String keyword = key.getText().trim();
        new SwingWorker<List<PhieuDieuChinhTon>, Void>() {
            protected List<PhieuDieuChinhTon> doInBackground() throws Exception {
                return new InventoryService().timPhieuDieuChinh(actor, keyword);
            }
            protected void done() {
                try {
                    adjustmentModel.setRowCount(0);
                    for (PhieuDieuChinhTon value : get())
                        adjustmentModel.addRow(
                                new Object[] {value.getMaDieuChinh(), value.getThoiDiemLap(), value.getMaNhanVien(),
                                        value.getLyDo(), Integer.valueOf(value.getSoLuongGiamTheoLo().size())});
                } catch (Exception exception) {
                    message("Không tải được lịch sử điều chỉnh.", exception);
                }
            }
        }.execute();
    }

    @SuppressWarnings("unchecked")
    private void adjust() {
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return new InventoryService().maDieuChinhTiepTheo(actor);
            }

            @Override
            protected void done() {
                try {
                    adjustForm(get());
                } catch (Exception exception) {
                    message("Không tạo được mã phiếu điều chỉnh.", exception);
                }
            }
        }.execute();
    }

    private void adjustForm(String generatedId) {
        JTextField id = new JTextField(generatedId);
        id.setEditable(false);
        final JTextField productSearch = new JTextField();
        final JComboBox<SanPham> productBox = new JComboBox<SanPham>();
        productBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof SanPham) {
                    SanPham found = (SanPham) value;
                    setText(found.getMaSanPham() + " · " + found.getTenSanPham());
                }
                return this;
            }
        });
        JPanel productPicker = new JPanel(new BorderLayout(6, 0));
        productPicker.setOpaque(false);
        productSearch.setPreferredSize(new Dimension(140, productSearch.getPreferredSize().height));
        productPicker.add(productSearch, BorderLayout.WEST);
        productPicker.add(productBox, BorderLayout.CENTER);
        productSearch.setToolTipText("Gõ mã hoặc tên để tìm — kết quả tự hiện trong danh sách");
        productBox.getAccessibleContext().setAccessibleName("Sản phẩm tìm được");
        final Timer productTimer = new Timer(300,
                event -> findProducts(productSearch.getText().trim(), productBox, productSearch));
        productTimer.setRepeats(false);
        productSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { productTimer.restart(); }
            @Override public void removeUpdate(DocumentEvent event) { productTimer.restart(); }
            @Override public void changedUpdate(DocumentEvent event) { productTimer.restart(); }
        });
        findProducts("", productBox, productSearch);
        JTextField quantity = new JTextField();
        JTextField reason = new JTextField();
        JPanel form = FormValidator.form();
        FormValidator validator = new FormValidator();
        form.add(validator.row("Mã phiếu điều chỉnh", id));
        form.add(validator.row("Mã / tên sản phẩm", productPicker));
        form.add(validator.row("Số lượng giảm", quantity));
        form.add(validator.row("Lý do", reason));
        validator.custom(productPicker, () -> {
            String productId = selectedProductId(productBox, productSearch);
            if (productId.isEmpty())
                return "Mã sản phẩm không được để trống.";
            return productId.matches("893[0-9]{9}") ? null
                    : "Mã sản phẩm sai định dạng, phải là mã vạch 12 chữ số bắt đầu bằng 893 (ví dụ: 893000000001).";
        });
        validator.require(quantity, "Số lượng giảm không được để trống.");
        validator.check(quantity, text -> text.matches("[0-9]+"),
                "Số lượng giảm phải là số nguyên dương.");
        validator.custom(quantity, () -> {
            String text = quantity.getText().trim();
            if (!text.matches("[0-9]+"))
                return null;
            try {
                return Integer.parseInt(text) > 0 ? null : "Số lượng giảm phải lớn hơn 0.";
            } catch (NumberFormatException exception) {
                return "Số lượng giảm phải là số nguyên dương.";
            }
        });
        validator.require(reason, "Lý do điều chỉnh không được để trống.");
        if (!DialogActions.confirm(this, form, "Điều chỉnh giảm FIFO", validator))
            return;
        final String adjustmentId = id.getText().trim();
        final String productId = selectedProductId(productBox, productSearch);
        final String adjustmentReason = reason.getText().trim();
        final int amount = Integer.parseInt(quantity.getText().trim());
        new SwingWorker<Void, Void>() {
            protected Void doInBackground() throws Exception {
                new InventoryService().giamTonFifo(actor, adjustmentId, productId, amount, adjustmentReason);
                return null;
            }
            protected void done() {
                try {
                    get();
                    load();
                    message("Lập phiếu điều chỉnh tồn thành công.");
                } catch (Exception exception) {
                    message("Không điều chỉnh được tồn kho.", exception);
                }
            }
        }.execute();
    }

    private void detail() {
        int row = adjustmentTable.getSelectedRow();
        if (row < 0)
            return;
        final String adjustmentId = String.valueOf(adjustmentModel.getValueAt(row, 0));
        new SwingWorker<PhieuDieuChinhTon, Void>() {
            protected PhieuDieuChinhTon doInBackground() throws Exception {
                return new InventoryService().chiTietPhieuDieuChinh(actor, adjustmentId);
            }
            protected void done() {
                try {
                    PhieuDieuChinhTon value = get();
                    DefaultTableModel details = new DefaultTableModel(new String[] {"Lô", "Số lượng giảm"}, 0) {
                        public boolean isCellEditable(int row, int column) {
                            return false;
                        }
                    };
                    for (Map.Entry<Long, Integer> line : value.getSoLuongGiamTheoLo().entrySet())
                        details.addRow(new Object[] {line.getKey(), line.getValue()});
                    JPanel form = new JPanel(new BorderLayout(0, 6));
                    form.add(new JLabel("Lý do: " + value.getLyDo() + "    Người lập: " + value.getMaNhanVien()),
                            BorderLayout.NORTH);
                    JScrollPane detailScroll = new JScrollPane(new JTable(details));
                    detailScroll.setPreferredSize(DialogActions.DETAIL_TABLE);
                    form.add(detailScroll, BorderLayout.CENTER);
                    DialogActions.info(InventoryPanel.this, form, "Chi tiết " + adjustmentId);
                } catch (Exception exception) {
                    message("Không tải được chi tiết điều chỉnh.", exception);
                }
            }
        }.execute();
    }

    private void findProducts(final String keyword, final JComboBox<SanPham> box,
            final JTextField source) {
        new SwingWorker<List<SanPham>, Void>() {
            @Override
            protected List<SanPham> doInBackground() throws Exception {
                return new MasterDataService().timSanPham(actor, keyword, null, "DANG_KINH_DOANH", null, null);
            }
            @Override
            protected void done() {
                if (source != null && !source.getText().trim().equals(keyword))
                    return;
                try {
                    List<SanPham> found = get();
                    String previousKey = null;
                    Object previous = box.getSelectedItem();
                    if (previous instanceof SanPham)
                        previousKey = ((SanPham) previous).getMaSanPham();
                    box.removeAllItems();
                    for (SanPham value : found)
                        box.addItem(value);
                    int select = -1;
                    if (previousKey != null) {
                        for (int i = 0; i < box.getItemCount(); i++) {
                            SanPham item = box.getItemAt(i);
                            if (item != null && previousKey.equals(item.getMaSanPham())) {
                                select = i;
                                break;
                            }
                        }
                    }
                    if (select < 0)
                        select = found.isEmpty() ? -1 : 0;
                    box.setSelectedIndex(select);
                    if (source != null && !keyword.isEmpty() && box.getItemCount() > 0)
                        box.setPopupVisible(true);
                } catch (Exception ignored) {
                }
            }
        }.execute();
    }

    private String selectedProductId(JComboBox<SanPham> box, JTextField source) {
        Object selected = box.getSelectedItem();
        if (selected instanceof SanPham)
            return ((SanPham) selected).getMaSanPham();
        return source.getText().trim();
    }

    private void message(String text) {
        JOptionPane.showMessageDialog(this, text, "Thông báo", JOptionPane.INFORMATION_MESSAGE);
    }

    private void message(String fallback, Exception exception) {
        Throwable cause = exception;
        while (cause.getCause() != null
                && cause instanceof java.util.concurrent.ExecutionException)
            cause = cause.getCause();
        String text = cause instanceof IllegalArgumentException || cause instanceof IllegalStateException
                ? cause.getMessage()
                : fallback;
        JOptionPane.showMessageDialog(this, text == null ? fallback : text, "Lỗi",
                JOptionPane.ERROR_MESSAGE);
    }

    private void addField(JPanel panel, String text, JComponent field) {
        JLabel label = new JLabel(text);
        label.setLabelFor(field);
        label.setDisplayedMnemonic(text.charAt(0));
        field.getAccessibleContext().setAccessibleName(text);
        panel.add(label);
        panel.add(field);
    }

    private static final class StockData {
        private final List<LoTonKho> lots;
        private final Set<String> lowProducts;

        private StockData(List<LoTonKho> lots, Set<String> lowProducts) {
            this.lots = lots;
            this.lowProducts = lowProducts;
        }
    }
}
