package graphicUI;

import components.UiTheme;
import components.DialogActions;
import entity.SanPham;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
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
import javax.swing.table.DefaultTableModel;
import service.Actor;
import service.MasterDataService;

@SuppressWarnings("serial")
public final class ProductPanel extends JPanel {
    private final Actor actor;
    private final boolean editable;
    private final JTextField keyword = new JTextField(12), category = new JTextField(8),
                             status = new JTextField("DANG_KINH_DOANH", 16), minPrice = new JTextField(7),
                             maxPrice = new JTextField(7);
    private final List<SanPham> rows = new ArrayList<SanPham>();
    private final DefaultTableModel model =
            new DefaultTableModel(new String[] {"Mã", "Tên", "Loại", "Đơn vị", "Giá", "Ngưỡng", "Trạng thái"}, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
    private final JTable table = new JTable(model);

    public ProductPanel(Actor actor, boolean editable) {
        super(new BorderLayout(0, 8));
        this.actor = actor;
        this.editable = editable;
        setBackground(UiTheme.BACKGROUND);
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bar.setBackground(Color.WHITE);
        bar.setBorder(UiTheme.CARD);
        addField(bar, "Từ khóa", keyword);
        addField(bar, "Loại", category);
        addField(bar, "Trạng thái", status);
        addField(bar, "Giá từ", minPrice);
        addField(bar, "đến", maxPrice);
        button(bar, "Tìm", KeyEvent.VK_T, () -> load());
        button(bar, "Chi tiết", KeyEvent.VK_C, () -> detail());
        if (editable) {
            button(bar, "Thêm", KeyEvent.VK_A, () -> edit(null));
            button(bar, "Sửa", KeyEvent.VK_S, () -> editSelected());
            button(bar, "Đổi trạng thái", KeyEvent.VK_D, () -> toggle());
            button(bar, "Xóa", KeyEvent.VK_X, () -> remove());
        }
        add(bar, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        load();
    }

    private void button(JPanel panel, String text, int mnemonic, Runnable action) {
        JButton button = new JButton(text);
        button.setMnemonic(mnemonic);
        button.addActionListener(event -> action.run());
        panel.add(button);
    }
    private BigDecimal number(String value) {
        return value.length() == 0 ? null : new BigDecimal(value);
    }
    private void load() {
        try {
            final String search = keyword.getText().trim(), type = category.getText().trim(),
                         filter = status.getText().trim();
            final BigDecimal low = number(minPrice.getText().trim()), high = number(maxPrice.getText().trim());
            new SwingWorker<List<SanPham>, Void>() {
                @Override
                protected List<SanPham> doInBackground() throws Exception {
                    return new MasterDataService().timSanPham(actor, search, type, filter, low, high);
                }
                @Override
                protected void done() {
                    try {
                        rows.clear();
                        rows.addAll(get());
                        model.setRowCount(0);
                        for (SanPham value : rows)
                            model.addRow(new Object[] {value.getMaSanPham(), value.getTenSanPham(), value.getMaLoai(),
                                    value.getDonViTinh(), value.getGiaBan(), value.getNguongTon(),
                                    value.getTrangThai()});
                    } catch (Exception exception) {
                        error("Không tải được sản phẩm.", exception);
                    }
                }
            }.execute();
        } catch (NumberFormatException exception) {
            JOptionPane.showMessageDialog(this, "Khoảng giá không hợp lệ.");
        }
    }
    private SanPham selected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Hãy chọn sản phẩm.");
            return null;
        }
        return rows.get(table.convertRowIndexToModel(row));
    }
    private void editSelected() {
        SanPham value = selected();
        if (value != null)
            edit(value);
    }
    private void detail() {
        SanPham value = selected();
        if (value != null)
            JOptionPane.showMessageDialog(this,
                    "Mã: " + value.getMaSanPham() + "\nTên: " + value.getTenSanPham() + "\nLoại: " + value.getMaLoai()
                            + "\nĐơn vị: " + value.getDonViTinh() + "\nGiá: " + value.getGiaBan()
                            + "\nNgưỡng tồn: " + value.getNguongTon() + "\nTrạng thái: " + value.getTrangThai(),
                    "Chi tiết sản phẩm", JOptionPane.INFORMATION_MESSAGE);
    }
    private void edit(SanPham existing) {
        JTextField id = new JTextField(existing == null ? "" : existing.getMaSanPham()),
                   name = new JTextField(existing == null ? "" : existing.getTenSanPham()),
                   type = new JTextField(existing == null ? "" : existing.getMaLoai()),
                   unit = new JTextField(existing == null ? "" : existing.getDonViTinh()),
                   price = new JTextField(existing == null ? "" : existing.getGiaBan().toPlainString()),
                   minimum = new JTextField(existing == null ? "" : String.valueOf(existing.getNguongTon()));
        id.setEditable(existing == null);
        JPanel form = new JPanel(new GridLayout(0, 1));
        addField(form, "Mã", id);
        addField(form, "Tên", name);
        addField(form, "Mã loại", type);
        addField(form, "Đơn vị", unit);
        addField(form, "Giá", price);
        addField(form, "Ngưỡng tồn", minimum);
        if (!DialogActions.confirm(this, form, existing == null ? "Thêm sản phẩm" : "Sửa sản phẩm"))
            return;
        try {
            final SanPham value = new SanPham();
            value.setMaSanPham(id.getText().trim());
            value.setTenSanPham(name.getText().trim());
            value.setMaLoai(type.getText().trim());
            value.setDonViTinh(unit.getText().trim());
            value.setGiaBan(new BigDecimal(price.getText().trim()));
            value.setNguongTon(Integer.parseInt(minimum.getText().trim()));
            value.setTrangThai(existing == null ? "DANG_KINH_DOANH" : existing.getTrangThai());
            save(value);
        } catch (NumberFormatException exception) {
            JOptionPane.showMessageDialog(this, "Giá hoặc ngưỡng tồn không hợp lệ.");
        }
    }
    private void toggle() {
        final SanPham value = selected();
        if (value == null)
            return;
        final String next = "DANG_KINH_DOANH".equals(value.getTrangThai()) ? "NGUNG_KINH_DOANH" : "DANG_KINH_DOANH";
        if (JOptionPane.showConfirmDialog(this,
                    "DANG_KINH_DOANH".equals(next) ? "Kích hoạt lại sản phẩm?" : "Ngừng kinh doanh sản phẩm?",
                    "Xác nhận", JOptionPane.YES_NO_OPTION)
                != JOptionPane.YES_OPTION)
            return;
        value.setTrangThai(next);
        save(value);
    }
    private void save(final SanPham value) {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                new MasterDataService().luuSanPham(actor, value);
                return null;
            }
            @Override
            protected void done() {
                try {
                    get();
                    load();
                } catch (Exception exception) {
                    error("Không lưu được sản phẩm.", exception);
                }
            }
        }.execute();
    }
    private void remove() {
        final SanPham value = selected();
        if (value == null
                || JOptionPane.showConfirmDialog(
                           this, "Xóa " + value.getMaSanPham() + "?", "Xác nhận", JOptionPane.YES_NO_OPTION)
                        != JOptionPane.YES_OPTION)
            return;
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                new MasterDataService().xoaSanPham(actor, value.getMaSanPham());
                return null;
            }
            @Override
            protected void done() {
                try {
                    get();
                    load();
                } catch (Exception exception) {
                    error("Không thể xóa sản phẩm có lịch sử. Hãy ngừng kinh doanh thay vì xóa.", exception);
                }
            }
        }.execute();
    }
    private void error(String message, Exception exception) {
        Throwable cause = exception.getCause() == null ? exception : exception.getCause();
        JOptionPane.showMessageDialog(this, cause.getMessage() == null ? message : message + "\n" + cause.getMessage(),
                "Lỗi", JOptionPane.ERROR_MESSAGE);
    }
    private void addField(JPanel panel, String text, JComponent field) {
        JLabel label = new JLabel(text);
        label.setLabelFor(field);
        label.setDisplayedMnemonic(text.charAt(0));
        field.getAccessibleContext().setAccessibleName(text);
        panel.add(label);
        panel.add(field);
    }
}
