package graphicUI;

import components.UiFormat;
import components.UiTheme;
import components.DialogActions;
import components.FormValidator;
import entity.LoaiSanPham;
import entity.SanPham;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
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
import javax.swing.table.DefaultTableModel;
import service.Actor;
import service.MasterDataService;

@SuppressWarnings("serial")
public final class ProductPanel extends JPanel {
    private final Actor actor;
    private final boolean editable;
    private final JTextField keyword = new JTextField(12), category = new JTextField(8),
                             minPrice = new JTextField(7), maxPrice = new JTextField(7);
    private final JComboBox<String> status = new JComboBox<String>(
            new String[] {"Tất cả", "DANG_KINH_DOANH", "NGUNG_KINH_DOANH"});
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
        table.getColumnModel().getColumn(4).setCellRenderer(UiFormat.moneyRenderer());
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
                         filter = "Tất cả".equals(status.getSelectedItem()) ? ""
                                 : (String) status.getSelectedItem();
            final BigDecimal low = number(minPrice.getText().trim()), high = number(maxPrice.getText().trim());
            if (low != null && high != null && low.compareTo(high) > 0) {
                JOptionPane.showMessageDialog(this, "Giá từ không được lớn hơn giá đến.", "Dữ liệu không hợp lệ",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
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
                            + "\nĐơn vị: " + value.getDonViTinh() + "\nGiá: " + UiFormat.money(value.getGiaBan()) + " đ"
                            + "\nNgưỡng tồn: " + value.getNguongTon() + "\nTrạng thái: " + value.getTrangThai(),
                    "Chi tiết sản phẩm", JOptionPane.INFORMATION_MESSAGE);
    }
    private void edit(final SanPham existing) {
        new SwingWorker<List<LoaiSanPham>, Void>() {
            @Override
            protected List<LoaiSanPham> doInBackground() throws Exception {
                return new MasterDataService().timLoai(actor, null);
            }

            @Override
            protected void done() {
                try {
                    editForm(get(), existing);
                } catch (Exception exception) {
                    error("Không tải được danh sách loại.", exception);
                }
            }
        }.execute();
    }

    private void editForm(List<LoaiSanPham> categories, SanPham existing) {
        JTextField id = new JTextField(existing == null ? sinhMaVach() : existing.getMaSanPham()),
                   name = new JTextField(existing == null ? "" : existing.getTenSanPham()),
                   unit = new JTextField(existing == null ? "" : existing.getDonViTinh()),
                   price = new JTextField(existing == null ? "" : existing.getGiaBan().toPlainString()),
                   minimum = new JTextField(existing == null ? "" : String.valueOf(existing.getNguongTon()));
        JComboBox<String> type = new JComboBox<String>();
        for (LoaiSanPham value : categories)
            type.addItem(UiFormat.joinIdName(value.getMaLoai(), value.getTenLoai()));
        if (existing != null) {
            String current = existing.getMaLoai();
            boolean found = false;
            for (int i = 0; i < type.getItemCount(); i++)
                if (UiFormat.idOf(String.valueOf(type.getItemAt(i))).equals(current)) {
                    type.setSelectedIndex(i);
                    found = true;
                    break;
                }
            if (!found)
                type.addItem(current);
        }
        id.setEditable(existing == null);
        FormValidator validator = new FormValidator();
        JPanel form = FormValidator.form();
        form.add(validator.row("Mã", id));
        form.add(validator.row("Tên", name));
        form.add(validator.row("Loại", type));
        form.add(validator.row("Đơn vị", unit));
        form.add(validator.row("Giá", price));
        form.add(validator.row("Ngưỡng tồn", minimum));
        validator.require(id, "Mã sản phẩm không được để trống.");
        validator.matches(id, "893[0-9]{9}",
                "Mã sản phẩm sai định dạng, phải là mã vạch 12 chữ số bắt đầu bằng 893 (ví dụ: 893000000001).");
        validator.require(name, "Tên sản phẩm không được để trống.");
        validator.require(unit, "Đơn vị tính không được để trống.");
        validator.require(price, "Giá bán không được để trống.");
        validator.number(price, "Giá bán phải là số.");
        validator.check(price, text -> {
            try {
                return new BigDecimal(text).signum() > 0;
            } catch (NumberFormatException exception) {
                return true;
            }
        }, "Giá bán phải lớn hơn 0.");
        validator.require(minimum, "Ngưỡng tồn không được để trống.");
        validator.check(minimum, text -> text.matches("\\d+"),
                "Ngưỡng tồn phải là số nguyên không âm.");
        validator.custom(type, () -> {
            String typeId = UiFormat.idOf(String.valueOf(type.getSelectedItem()));
            if (typeId.isEmpty())
                return "Loại không được để trống.";
            return typeId.matches("LSP[0-9]{3,}") ? null
                    : "Mã loại sai định dạng, phải có dạng LSP + số (ví dụ: LSP001).";
        });
        if (!DialogActions.confirm(this, form, existing == null ? "Thêm sản phẩm" : "Sửa sản phẩm",
                validator))
            return;
        final String typeId = UiFormat.idOf(String.valueOf(type.getSelectedItem()));
        final SanPham value = new SanPham();
        value.setMaSanPham(id.getText().trim());
        value.setTenSanPham(name.getText().trim());
        value.setMaLoai(typeId);
        value.setDonViTinh(unit.getText().trim());
        value.setGiaBan(new BigDecimal(price.getText().trim()));
        value.setNguongTon(Integer.parseInt(minimum.getText().trim()));
        value.setTrangThai(existing == null ? "DANG_KINH_DOANH" : existing.getTrangThai());
        save(value, existing == null);
    }
    private String sinhMaVach() {
        Random random = new Random();
        StringBuilder ma = new StringBuilder("893");
        for (int i = 0; i < 9; i++)
            ma.append(random.nextInt(10));
        return ma.toString();
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
        save(value, false);
    }
    private void save(final SanPham value, final boolean isNew) {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                MasterDataService service = new MasterDataService();
                if (isNew) {
                    for (SanPham found : service.timSanPham(actor, value.getMaSanPham(), null, null, null, null))
                        if (value.getMaSanPham().equals(found.getMaSanPham()))
                            throw new IllegalArgumentException("Mã sản phẩm đã tồn tại");
                }
                service.luuSanPham(actor, value);
                return null;
            }
            @Override
            protected void done() {
                try {
                    get();
                    load();
                    JOptionPane.showMessageDialog(ProductPanel.this, "Lưu sản phẩm thành công.", "Thành công",
                            JOptionPane.INFORMATION_MESSAGE);
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
                    JOptionPane.showMessageDialog(ProductPanel.this, "Xóa sản phẩm thành công.", "Thành công",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception exception) {
                    error("Không thể xóa sản phẩm có lịch sử. Hãy ngừng kinh doanh thay vì xóa.", exception);
                }
            }
        }.execute();
    }
    private void error(String message, Exception exception) {
        Throwable cause = exception;
        while (cause.getCause() != null
                && cause instanceof java.util.concurrent.ExecutionException)
            cause = cause.getCause();
        String text = cause instanceof IllegalArgumentException || cause instanceof IllegalStateException
                ? cause.getMessage()
                : message;
        JOptionPane.showMessageDialog(this, text == null ? message : text, "Lỗi", JOptionPane.ERROR_MESSAGE);
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
