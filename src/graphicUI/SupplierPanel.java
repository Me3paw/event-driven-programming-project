package graphicUI;

import components.DialogActions;
import entity.NhaCungCap;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
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
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import service.Actor;
import service.MasterDataService;

public final class SupplierPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final Actor actor;
    private final JTextField key = new JTextField(16);
    private final JTextField state = new JTextField(10);
    private final List<NhaCungCap> rows = new ArrayList<NhaCungCap>();
    private final DefaultTableModel model = new DefaultTableModel(
            new String[] {"Mã", "Tên", "Điện thoại", "Địa chỉ", "Trạng thái"}, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);

    public SupplierPanel(Actor actor) {
        super(new BorderLayout());
        this.actor = actor;
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addField(bar, "Từ khóa", key);
        addField(bar, "Trạng thái", state);
        button(bar, "Tìm", new Runnable() { @Override public void run() { load(); } });
        button(bar, "Chi tiết", new Runnable() { @Override public void run() { detail(); } });
        button(bar, "Thêm", new Runnable() { @Override public void run() { edit(null); } });
        button(bar, "Sửa", new Runnable() { @Override public void run() { editSelected(); } });
        button(bar, "Đổi trạng thái", new Runnable() { @Override public void run() { toggle(); } });
        button(bar, "Xóa", new Runnable() { @Override public void run() { remove(); } });
        add(bar, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        load();
    }

    private void button(JPanel panel, String text, final Runnable action) {
        JButton button = new JButton(text);
        button.addActionListener(event -> action.run());
        panel.add(button);
    }

    private void addField(JPanel panel, String text, JComponent field) {
        JLabel label = new JLabel(text);
        label.setLabelFor(field);
        field.getAccessibleContext().setAccessibleName(text);
        panel.add(label);
        panel.add(field);
    }

    private void load() {
        final String keyword = key.getText().trim();
        final String filter = state.getText().trim();
        table.clearSelection();
        new SwingWorker<List<NhaCungCap>, Void>() {
            @Override
            protected List<NhaCungCap> doInBackground() throws Exception {
                return new MasterDataService().timNhaCungCap(actor, keyword);
            }

            @Override
            protected void done() {
                try {
                    rows.clear();
                    for (NhaCungCap value : get()) {
                        if (filter.isEmpty() || filter.equals(value.getTrangThai())) rows.add(value);
                    }
                    model.setRowCount(0);
                    for (NhaCungCap value : rows) {
                        model.addRow(new Object[] {value.getMaNhaCungCap(), value.getTenNhaCungCap(),
                                value.getSoDienThoai(), value.getDiaChi(), value.getTrangThai()});
                    }
                } catch (Exception exception) {
                    message("Không tải được nhà cung cấp.");
                }
            }
        }.execute();
    }

    private NhaCungCap selected() {
        int row = table.getSelectedRow();
        return row < 0 ? null : rows.get(table.convertRowIndexToModel(row));
    }

    private void editSelected() {
        NhaCungCap value = selected();
        if (value == null) {
            message("Hãy chọn nhà cung cấp để sửa.");
            return;
        }
        edit(value);
    }

    private void detail() {
        NhaCungCap value = selected();
        if (value == null) return;
        JOptionPane.showMessageDialog(this, value.getMaNhaCungCap() + "\n" + value.getTenNhaCungCap() + "\n"
                + value.getSoDienThoai() + "\n" + value.getDiaChi() + "\n" + value.getTrangThai());
    }

    private void edit(NhaCungCap existing) {
        JTextField id = new JTextField(existing == null ? "" : existing.getMaNhaCungCap());
        JTextField name = new JTextField(existing == null ? "" : existing.getTenNhaCungCap());
        JTextField phone = new JTextField(existing == null ? "" : existing.getSoDienThoai());
        JTextField address = new JTextField(existing == null ? "" : existing.getDiaChi());
        id.setEditable(existing == null);
        JPanel form = new JPanel(new GridLayout(0, 1, 0, 4));
        addField(form, "Mã", id);
        addField(form, "Tên", name);
        addField(form, "Điện thoại", phone);
        addField(form, "Địa chỉ", address);
        if (!DialogActions.confirm(this, form, existing == null ? "Thêm nhà cung cấp" : "Sửa nhà cung cấp")) return;
        NhaCungCap value = new NhaCungCap();
        value.setMaNhaCungCap(id.getText().trim());
        value.setTenNhaCungCap(name.getText().trim());
        value.setSoDienThoai(phone.getText().trim());
        value.setDiaChi(address.getText().trim());
        value.setTrangThai(existing == null ? "HOAT_DONG" : existing.getTrangThai());
        save(value);
    }

    private void toggle() {
        NhaCungCap value = selected();
        if (value == null || JOptionPane.showConfirmDialog(this, "Xác nhận?", "Trạng thái",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        value.setTrangThai("HOAT_DONG".equals(value.getTrangThai()) ? "NGUNG_HOAT_DONG" : "HOAT_DONG");
        save(value);
    }

    private void save(final NhaCungCap value) {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                new MasterDataService().luuNhaCungCap(actor, value);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    load();
                } catch (Exception exception) {
                    message("Không lưu được nhà cung cấp.");
                }
            }
        }.execute();
    }

    private void remove() {
        final NhaCungCap value = selected();
        if (value == null || JOptionPane.showConfirmDialog(this, "Xóa?", "Xác nhận",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                new MasterDataService().xoaNhaCungCap(actor, value.getMaNhaCungCap());
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    load();
                } catch (Exception exception) {
                    message("Không thể xóa; hãy ngừng hoạt động.");
                }
            }
        }.execute();
    }

    private void message(String text) {
        JOptionPane.showMessageDialog(this, text);
    }
}
