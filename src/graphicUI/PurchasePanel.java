package graphicUI;

import components.UiTheme;
import components.DialogActions;
import entity.ChiTietPhieuNhap;
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
import service.PurchaseService;

@SuppressWarnings("serial")
public final class PurchasePanel extends JPanel {
    private final Actor actor;
    private final JTextField key = new JTextField(12);
    private final JTextField state = new JTextField("", 9);
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
        JButton detail = new JButton("Chi tiết / sửa nháp");
        detail.setMnemonic(KeyEvent.VK_C);
        detail.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                detail();
            }
        });
        bar.add(detail);
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
        final String status = state.getText().trim();
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
                    message("Không tải được phiếu nhập.");
                }
            }
        }.execute();
    }

    private void create() {
        editDraft(null);
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
                    editDraft(get());
                } catch (Exception exception) {
                    message("Không tải được chi tiết phiếu nhập.");
                }
            }
        }.execute();
    }

    private void editDraft(PhieuNhap purchase) {
        final boolean existing = purchase != null;
        final boolean editable = !existing || "NHAP".equals(purchase.getTrangThai());
        final JTextField id = new JTextField(existing ? purchase.getMaPhieuNhap() : "PN" + System.currentTimeMillis());
        final JTextField supplier = new JTextField(existing ? purchase.getMaNhaCungCap() : "");
        id.setEditable(!existing);
        supplier.setEditable(!existing);
        final DraftLineModel lines = new DraftLineModel(existing ? purchase.getChiTiet() : null);
        final JTable lineTable = new JTable(lines);
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
        JPanel header = new JPanel(new GridLayout(0, 1));
        addField(header, "Mã phiếu", id);
        addField(header, "Mã nhà cung cấp", supplier);
        if (existing)
            header.add(new JLabel("Trạng thái: " + purchase.getTrangThai() + "    Tổng: " + purchase.getTongTien()));
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
        if (!DialogActions.confirm(this, form, title))
            return;
        final String purchaseId = id.getText().trim();
        final String supplierId = supplier.getText().trim();
        final List<ChiTietPhieuNhap> details = lines.details();
        if (purchaseId.length() == 0 || supplierId.length() == 0) {
            message("Cần mã phiếu và mã nhà cung cấp.");
            return;
        }
        new SwingWorker<Void, Void>() {
            protected Void doInBackground() throws Exception {
                PurchaseService service = new PurchaseService();
                if (existing)
                    service.capNhatChiTiet(actor, purchaseId, details);
                else {
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
                } catch (Exception exception) {
                    message("Không lưu được phiếu nháp.");
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
        JPanel form = new JPanel(new GridLayout(0, 1));
        addField(form, "Mã sản phẩm", product);
        addField(form, "Số lượng", quantity);
        addField(form, "Đơn giá nhập", cost);
        if (!DialogActions.confirm(this, form, old == null ? "Thêm dòng hàng" : "Sửa dòng hàng"))
            return;
        final int amount;
        final BigDecimal unitCost;
        final String productId = product.getText().trim();
        try {
            amount = Integer.parseInt(quantity.getText().trim());
            unitCost = new BigDecimal(cost.getText().trim());
        } catch (NumberFormatException exception) {
            message("Số lượng và đơn giá nhập không hợp lệ.");
            return;
        }
        if (productId.length() == 0 || amount <= 0 || unitCost.signum() < 0) {
            message("Mã sản phẩm, số lượng dương và đơn giá không âm là bắt buộc.");
            return;
        }
        if (lines.hasProduct(productId, row)) {
            message("Mỗi sản phẩm chỉ có một dòng trong phiếu nhập.");
            return;
        }
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
                } catch (Exception exception) {
                    message("Không xóa được phiếu nháp.");
                }
            }
        }.execute();
    }

    private void finish() {
        final String purchaseId = selected();
        final String status = selectedStatus();
        if (purchaseId == null)
            return;
        if ("HOAN_TAT".equals(status)) {
            message("Phiếu đã hoàn tất và bất biến.");
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
                } catch (Exception exception) {
                    message("Không hoàn tất được phiếu.");
                }
            }
        }.execute();
    }

    private void message(String text) {
        JOptionPane.showMessageDialog(this, text);
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
