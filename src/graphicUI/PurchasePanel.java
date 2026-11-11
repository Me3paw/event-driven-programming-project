package graphicUI;

import components.UiFormat;
import components.UiTheme;
import components.DialogActions;
import components.FormValidator;
import entity.ChiTietPhieuNhap;
import entity.NhaCungCap;
import entity.PhieuNhap;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import service.Actor;
import service.MasterDataService;
import service.PurchaseService;

@SuppressWarnings("serial")
public final class PurchasePanel extends JPanel {
    private final Actor actor;
    private final JTextField key = new JTextField(12);
    private final JComboBox<String> state = new JComboBox<String>(
            new String[] {"Tất cả", "NHAP", "HOAN_TAT"});
    private final DefaultTableModel model =
            new DefaultTableModel(new String[] {"Mã", "Nhà cung cấp", "Ngày lập", "Trạng thái", "Tổng"}, 0) {
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
    private final JTable table = new JTable(model);

    public PurchasePanel(Actor actor) {
        super(new BorderLayout(0, 8));
        this.actor = actor;
        table.getColumnModel().getColumn(4).setCellRenderer(UiFormat.moneyRenderer());
        setBackground(UiTheme.BACKGROUND);
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bar.setBackground(Color.WHITE);
        bar.setBorder(UiTheme.CARD);
        addField(bar, "Từ khóa", key);
        addField(bar, "Trạng thái", state);
        JButton find = new JButton("Tìm");
        find.setMnemonic(KeyEvent.VK_T);
        find.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                load();
            }
        });
        bar.add(find);
        JButton draft = new JButton("Tạo phiếu nháp");
        draft.setMnemonic(KeyEvent.VK_A);
        draft.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                create();
            }
        });
        bar.add(draft);
        JButton detail = new JButton("Chi tiết");
        detail.setMnemonic(KeyEvent.VK_C);
        detail.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                detail();
            }
        });
        bar.add(detail);
        JButton edit = new JButton("Sửa nháp");
        edit.setMnemonic(KeyEvent.VK_S);
        edit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                editSelected();
            }
        });
        bar.add(edit);
        JButton delete = new JButton("Xóa nháp");
        delete.setMnemonic(KeyEvent.VK_X);
        delete.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                delete();
            }
        });
        bar.add(delete);
        JButton done = new JButton("Hoàn tất");
        done.setMnemonic(KeyEvent.VK_H);
        done.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                finish();
            }
        });
        bar.add(done);
        add(bar, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        load();
    }

    private void load() {
        final String keyword = key.getText().trim();
        final String status = "Tất cả".equals(state.getSelectedItem()) ? ""
                : (String) state.getSelectedItem();
        new SwingWorker<List<PhieuNhap>, Void>() {
            protected List<PhieuNhap> doInBackground() throws Exception {
                return new PurchaseService().tim(actor, keyword, status);
            }
            protected void done() {
                try {
                    model.setRowCount(0);
                    for (PhieuNhap purchase : get())
                        model.addRow(new Object[] {purchase.getMaPhieuNhap(), purchase.getMaNhaCungCap(),
                                purchase.getThoiDiemLap(), purchase.getTrangThai(), purchase.getTongTien()});
                } catch (Exception exception) {
                    message("Không tải được phiếu nhập.", exception);
                }
            }
        }.execute();
    }

    @SuppressWarnings("unchecked")
    private void create() {
        new SwingWorker<Object[], Void>() {
            @Override
            protected Object[] doInBackground() throws Exception {
                return new Object[] {new PurchaseService().maPhieuNhapTiepTheo(actor),
                        new MasterDataService().timNhaCungCap(actor, null, null)};
            }

            @Override
            protected void done() {
                try {
                    Object[] result = get();
                    editDraft(null, (String) result[0], (List<NhaCungCap>) result[1]);
                } catch (Exception exception) {
                    message("Không tạo được mã phiếu nhập.", exception);
                }
            }
        }.execute();
    }

    private String selected() {
        int row = table.getSelectedRow();
        return row < 0 ? null : String.valueOf(model.getValueAt(row, 0));
    }

    private String selectedStatus() {
        int row = table.getSelectedRow();
        return row < 0 ? null : String.valueOf(model.getValueAt(row, 3));
    }

    private void detail() {
        final String purchaseId = selected();
        if (purchaseId == null)
            return;
        new SwingWorker<PhieuNhap, Void>() {
            protected PhieuNhap doInBackground() throws Exception {
                return new PurchaseService().chiTiet(actor, purchaseId);
            }
            protected void done() {
                try {
                    PhieuNhap purchase = get();
                    JPanel form = new JPanel(new BorderLayout(0, 6));
                    JPanel header = new JPanel(new GridLayout(0, 2, 8, 4));
                    addField(header, "Mã phiếu", new JLabel(purchase.getMaPhieuNhap()));
                    addField(header, "Nhà cung cấp", new JLabel(purchase.getMaNhaCungCap()));
                    addField(header, "Nhân viên", new JLabel(purchase.getMaNhanVien()));
                    addField(header, "Ngày lập", new JLabel(String.valueOf(purchase.getThoiDiemLap())));
                    addField(header, "Trạng thái", new JLabel(purchase.getTrangThai()));
                    addField(header, "Tổng tiền", new JLabel(UiFormat.money(purchase.getTongTien()) + " đ"));
                    DefaultTableModel lines = new DefaultTableModel(
                            new String[] {"Mã sản phẩm", "Số lượng", "Đơn giá nhập"}, 0) {
                        public boolean isCellEditable(int row, int column) {
                            return false;
                        }
                    };
                    for (ChiTietPhieuNhap line : purchase.getChiTiet())
                        lines.addRow(new Object[] {line.getMaSanPham(), line.getSoLuong(),
                                line.getDonGiaNhap()});
                    form.add(header, BorderLayout.NORTH);
                    JTable linesTable = new JTable(lines);
                    linesTable.getColumnModel().getColumn(2).setCellRenderer(UiFormat.moneyRenderer());
                    JScrollPane linesScroll = new JScrollPane(linesTable);
                    linesScroll.setPreferredSize(DialogActions.DETAIL_TABLE);
                    form.add(linesScroll, BorderLayout.CENTER);
                    DialogActions.info(PurchasePanel.this, form, "Chi tiết phiếu nhập " + purchaseId);
                } catch (Exception exception) {
                    message("Không tải được chi tiết phiếu nhập.", exception);
                }
            }
        }.execute();
    }

    private void editSelected() {
        final String purchaseId = selected();
        final String status = selectedStatus();
        if (purchaseId == null)
            return;
        if (!"NHAP".equals(status)) {
            message("Chỉ sửa được phiếu nháp.");
            return;
        }
        new SwingWorker<Object[], Void>() {
            @Override
            @SuppressWarnings("unchecked")
            protected Object[] doInBackground() throws Exception {
                return new Object[] {new PurchaseService().chiTiet(actor, purchaseId),
                        new MasterDataService().timNhaCungCap(actor, null, null)};
            }
            @Override
            protected void done() {
                try {
                    Object[] result = get();
                    editDraft((PhieuNhap) result[0], null, (List<NhaCungCap>) result[1]);
                } catch (Exception exception) {
                    message("Không tải được chi tiết phiếu nhập.", exception);
                }
            }
        }.execute();
    }

    private void editDraft(PhieuNhap purchase, String generatedId, List<NhaCungCap> suppliers) {
        final boolean existing = purchase != null;
        final boolean editable = !existing || "NHAP".equals(purchase.getTrangThai());
        final JTextField id = new JTextField(existing ? purchase.getMaPhieuNhap() : generatedId);
        final JComboBox<String> supplier = new JComboBox<String>();
        for (NhaCungCap value : suppliers)
            supplier.addItem(UiFormat.joinIdName(value.getMaNhaCungCap(), value.getTenNhaCungCap()));
        if (existing) {
            String current = purchase.getMaNhaCungCap();
            boolean found = false;
            for (int i = 0; i < supplier.getItemCount(); i++)
                if (UiFormat.idOf(String.valueOf(supplier.getItemAt(i))).equals(current)) {
                    supplier.setSelectedIndex(i);
                    found = true;
                    break;
                }
            if (!found)
                supplier.addItem(current);
        }
        id.setEditable(false);
        supplier.setEnabled(!existing);
        final DraftLineModel lines = new DraftLineModel(existing ? purchase.getChiTiet() : null);
        final JTable lineTable = new JTable(lines);
        lineTable.getColumnModel().getColumn(3).setCellRenderer(UiFormat.moneyRenderer());
        JScrollPane linePane = new JScrollPane(lineTable);
        linePane.setPreferredSize(new Dimension(520, 180));
        JPanel lineButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton add = new JButton("Thêm dòng");
        add.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                editLine(lines, -1);
            }
        });
        JButton edit = new JButton("Sửa dòng");
        edit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                editLine(lines, lineTable.getSelectedRow());
            }
        });
        JButton remove = new JButton("Xóa dòng");
        remove.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                int row = lineTable.getSelectedRow();
                if (row >= 0)
                    lines.remove(row);
            }
        });
        add.setEnabled(editable);
        edit.setEnabled(editable);
        remove.setEnabled(editable);
        lineButtons.add(add);
        lineButtons.add(edit);
        lineButtons.add(remove);
        JPanel form = new JPanel(new BorderLayout(0, 6));
        JPanel header = FormValidator.form();
        FormValidator validator = new FormValidator();
        header.add(validator.row("Mã phiếu", id));
        header.add(validator.row("Nhà cung cấp", supplier));
        if (existing)
            header.add(new JLabel("Trạng thái: " + purchase.getTrangThai() + "    Tổng: "
                    + UiFormat.money(purchase.getTongTien()) + " đ"));
        validator.custom(supplier, () -> {
            String supplierId = UiFormat.idOf(String.valueOf(supplier.getSelectedItem()));
            if (supplierId.isEmpty())
                return "Mã nhà cung cấp không được để trống.";
            return supplierId.matches("NCC[0-9]{3,}") ? null
                    : "Mã nhà cung cấp sai định dạng, phải có dạng NCC + số (ví dụ: NCC001).";
        });
        form.add(header, BorderLayout.NORTH);
        form.add(linePane, BorderLayout.CENTER);
        form.add(lineButtons, BorderLayout.SOUTH);
        String title = existing && !editable ? "Phiếu đã hoàn tất (bất biến)"
                : existing                   ? "Sửa phiếu nhập nháp"
                                             : "Tạo phiếu nhập nháp";
        if (!editable) {
            JOptionPane.showMessageDialog(this, form, title, JOptionPane.PLAIN_MESSAGE);
            return;
        }
        if (!DialogActions.confirm(this, form, title, validator))
            return;
        final String purchaseId = id.getText().trim();
        final String supplierId = UiFormat.idOf(String.valueOf(supplier.getSelectedItem()));
        final List<ChiTietPhieuNhap> details = lines.details();
        new SwingWorker<Void, Void>() {
            protected Void doInBackground() throws Exception {
                PurchaseService service = new PurchaseService();
                if (existing)
                    service.capNhatChiTiet(actor, purchaseId, details);
                else {
                    for (PhieuNhap found : service.tim(actor, purchaseId, null))
                        if (purchaseId.equals(found.getMaPhieuNhap()))
                            throw new IllegalArgumentException("Mã phiếu nhập đã tồn tại");
                    PhieuNhap value = new PhieuNhap();
                    value.setMaPhieuNhap(purchaseId);
                    value.setMaNhaCungCap(supplierId);
                    value.setThoiDiemLap(LocalDateTime.now());
                    value.setChiTiet(details);
                    service.luuNhap(actor, value);
                }
                return null;
            }
            protected void done() {
                try {
                    get();
                    load();
                    message("Lưu phiếu nhập thành công.");
                } catch (Exception exception) {
                    message("Không lưu được phiếu nháp.", exception);
                }
            }
        }.execute();
    }

    private void editLine(DraftLineModel lines, int row) {
        if (row < -1)
            return;
        DraftLine old = row < 0 ? null : lines.get(row);
        JTextField product = new JTextField(old == null ? "" : old.productId);
        JTextField quantity = new JTextField(old == null ? "" : String.valueOf(old.quantity));
        JTextField cost = new JTextField(old == null ? "" : old.unitCost.toPlainString());
        FormValidator validator = new FormValidator();
        JPanel form = FormValidator.form();
        form.add(validator.row("Mã sản phẩm", product));
        form.add(validator.row("Số lượng", quantity));
        form.add(validator.row("Đơn giá nhập", cost));
        validator.require(product, "Mã sản phẩm không được để trống.");
        validator.matches(product, "893[0-9]{9}",
                "Mã sản phẩm sai định dạng, phải là mã vạch 12 chữ số bắt đầu bằng 893 (ví dụ: 893000000001).");
        validator.custom(product, () -> {
            String productId = product.getText().trim();
            if (!productId.matches("893[0-9]{9}"))
                return null;
            return lines.hasProduct(productId, row)
                    ? "Mỗi sản phẩm chỉ có một dòng trong phiếu nhập." : null;
        });
        validator.require(quantity, "Số lượng không được để trống.");
        validator.check(quantity, text -> text.matches("[0-9]+"),
                "Số lượng phải là số nguyên dương.");
        validator.custom(quantity, () -> {
            String text = quantity.getText().trim();
            if (!text.matches("[0-9]+"))
                return null;
            try {
                return Integer.parseInt(text) > 0 ? null : "Số lượng phải lớn hơn 0.";
            } catch (NumberFormatException exception) {
                return "Số lượng phải là số nguyên dương.";
            }
        });
        validator.require(cost, "Đơn giá nhập không được để trống.");
        validator.number(cost, "Đơn giá nhập phải là số.");
        validator.custom(cost, () -> {
            String text = cost.getText().trim();
            if (text.isEmpty())
                return null;
            try {
                return new BigDecimal(text).signum() > 0 ? null : "Đơn giá nhập phải lớn hơn 0.";
            } catch (NumberFormatException exception) {
                return null;
            }
        });
        if (!DialogActions.confirm(this, form, old == null ? "Thêm dòng hàng" : "Sửa dòng hàng",
                validator))
            return;
        final int amount = Integer.parseInt(quantity.getText().trim());
        final BigDecimal unitCost = new BigDecimal(cost.getText().trim());
        final String productId = product.getText().trim();
        lines.set(row, new DraftLine(productId, amount, unitCost));
    }

    private void delete() {
        final String purchaseId = selected();
        final String status = selectedStatus();
        if (purchaseId == null)
            return;
        if (!"NHAP".equals(status)) {
            message("Phiếu đã hoàn tất và bất biến.");
            return;
        }
        if (JOptionPane.showConfirmDialog(
                    this, "Xóa phiếu nháp " + purchaseId + "?", "Xác nhận xóa", JOptionPane.YES_NO_OPTION)
                != JOptionPane.YES_OPTION)
            return;
        new SwingWorker<Void, Void>() {
            protected Void doInBackground() throws Exception {
                new PurchaseService().xoaNhap(actor, purchaseId);
                return null;
            }
            protected void done() {
                try {
                    get();
                    load();
                    message("Xóa phiếu nháp thành công.");
                } catch (Exception exception) {
                    message("Không xóa được phiếu nháp.", exception);
                }
            }
        }.execute();
    }

    private void finish() {
        final String purchaseId = selected();
        final String status = selectedStatus();
        if (purchaseId == null)
            return;
        if (!"NHAP".equals(status)) {
            message("Chỉ có thể hoàn tất phiếu đang ở trạng thái nháp.");
            return;
        }
        if (JOptionPane.showConfirmDialog(
                    this, "Hoàn tất sẽ tạo lô FIFO và khóa nội dung. Tiếp tục?", "Xác nhận", JOptionPane.YES_NO_OPTION)
                != JOptionPane.YES_OPTION)
            return;
        new SwingWorker<Void, Void>() {
            protected Void doInBackground() throws Exception {
                new PurchaseService().hoanTat(actor, purchaseId);
                return null;
            }
            protected void done() {
                try {
                    get();
                    load();
                    message("Hoàn tất phiếu nhập thành công.");
                } catch (Exception exception) {
                    message("Không hoàn tất được phiếu.", exception);
                }
            }
        }.execute();
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

    private static final class DraftLine {
        private final String productId;
        private final int quantity;
        private final BigDecimal unitCost;

        private DraftLine(String productId, int quantity, BigDecimal unitCost) {
            this.productId = productId;
            this.quantity = quantity;
            this.unitCost = unitCost;
        }
    }

    private static final class DraftLineModel extends AbstractTableModel {
        private final List<DraftLine> rows = new ArrayList<DraftLine>();

        private DraftLineModel(List<ChiTietPhieuNhap> details) {
            if (details != null)
                for (ChiTietPhieuNhap detail : details)
                    rows.add(new DraftLine(detail.getMaSanPham(), detail.getSoLuong(), detail.getDonGiaNhap()));
        }

        public int getRowCount() {
            return rows.size();
        }
        public int getColumnCount() {
            return 4;
        }
        public String getColumnName(int column) {
            return new String[] {"Mã sản phẩm", "Số lượng", "Đơn giá nhập", "Thành tiền"}[column];
        }
        public Object getValueAt(int row, int column) {
            DraftLine line = rows.get(row);
            if (column == 0)
                return line.productId;
            if (column == 1)
                return Integer.valueOf(line.quantity);
            if (column == 2)
                return line.unitCost;
            return line.unitCost.multiply(BigDecimal.valueOf(line.quantity));
        }
        public boolean isCellEditable(int row, int column) {
            return false;
        }
        private DraftLine get(int row) {
            return rows.get(row);
        }
        private void set(int row, DraftLine line) {
            if (row < 0) {
                rows.add(line);
                fireTableRowsInserted(rows.size() - 1, rows.size() - 1);
            } else {
                rows.set(row, line);
                fireTableRowsUpdated(row, row);
            }
        }
        private void remove(int row) {
            rows.remove(row);
            fireTableRowsDeleted(row, row);
        }
        private boolean hasProduct(String productId, int ignoredRow) {
            for (int index = 0; index < rows.size(); index++)
                if (index != ignoredRow && productId.equals(rows.get(index).productId))
                    return true;
            return false;
        }
        private List<ChiTietPhieuNhap> details() {
            List<ChiTietPhieuNhap> details = new ArrayList<ChiTietPhieuNhap>();
            for (DraftLine line : rows) {
                ChiTietPhieuNhap detail = new ChiTietPhieuNhap();
                detail.setMaSanPham(line.productId);
                detail.setSoLuong(line.quantity);
                detail.setDonGiaNhap(line.unitCost);
                details.add(detail);
            }
            return details;
        }
    }
}
