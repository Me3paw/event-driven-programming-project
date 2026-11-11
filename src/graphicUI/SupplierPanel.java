package graphicUI;

import components.DialogActions;
import components.FormValidator;
import entity.NhaCungCap;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
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
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import service.Actor;
import service.MasterDataService;

public final class SupplierPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final Actor actor;
    private final JTextField key = new JTextField(16);
    private final JComboBox<String> state = new JComboBox<String>(
            new String[] {"Tất cả", "HOAT_DONG", "NGUNG_HOAT_DONG"});
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
        final String filter = "Tất cả".equals(state.getSelectedItem()) ? ""
                : (String) state.getSelectedItem();
        table.clearSelection();
        new SwingWorker<List<NhaCungCap>, Void>() {
            @Override
            protected List<NhaCungCap> doInBackground() throws Exception {
                return new MasterDataService().timNhaCungCap(actor, keyword, filter);
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
                    message("Không tải được nhà cung cấp.", exception);
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
        if (existing != null) {
            editForm(existing.getMaNhaCungCap(), existing);
            return;
        }
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return new MasterDataService().maNhaCungCapTiepTheo(actor);
            }

            @Override
            protected void done() {
                try {
                    editForm(get(), null);
                } catch (Exception exception) {
                    message("Không tạo được mã nhà cung cấp.", exception);
                }
            }
        }.execute();
    }

    private void editForm(String generatedId, NhaCungCap existing) {
        JTextField id = new JTextField(existing == null ? generatedId : existing.getMaNhaCungCap());
        JTextField name = new JTextField(existing == null ? "" : existing.getTenNhaCungCap());
        JTextField phone = new JTextField(existing == null ? "" : existing.getSoDienThoai());
        JTextField address = new JTextField(existing == null ? "" : existing.getDiaChi());
        id.setEditable(false);
        FormValidator validator = new FormValidator();
        JPanel form = FormValidator.form();
        form.add(validator.row("Mã", id));
        form.add(validator.row("Tên", name));
        form.add(validator.row("Điện thoại", phone));
        form.add(validator.row("Địa chỉ", address));
        validator.require(name, "Tên nhà cung cấp không được để trống.");
        validator.check(name, text -> text.length() <= 150,
                "Tên nhà cung cấp không được quá 150 ký tự.");
        validator.require(phone, "Số điện thoại không được để trống.");
        validator.matches(phone, "0[35789][0-9]{8}",
                "Số điện thoại sai định dạng, phải có dạng 0xxxxxxxxx (10 số, đầu số 3/5/7/8/9, ví dụ: 0901234567).");
        validator.check(address, text -> text.length() <= 255,
                "Địa chỉ không được quá 255 ký tự.");
        if (!DialogActions.confirm(this, form, existing == null ? "Thêm nhà cung cấp" : "Sửa nhà cung cấp",
                validator)) return;
        NhaCungCap value = new NhaCungCap();
        value.setMaNhaCungCap(id.getText().trim());
        value.setTenNhaCungCap(name.getText().trim());
        value.setSoDienThoai(phone.getText().trim());
        value.setDiaChi(address.getText().trim());
        value.setTrangThai(existing == null ? "HOAT_DONG" : existing.getTrangThai());
        save(value, existing == null);
    }

    private void toggle() {
        NhaCungCap value = selected();
        if (value == null) return;
        String next = "HOAT_DONG".equals(value.getTrangThai()) ? "NGUNG_HOAT_DONG" : "HOAT_DONG";
        if (JOptionPane.showConfirmDialog(this, "Chuyển nhà cung cấp sang trạng thái " + next + "?",
                "Xác nhận", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        value.setTrangThai(next);
        save(value, false);
    }

    private void save(final NhaCungCap value, final boolean isNew) {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                MasterDataService service = new MasterDataService();
                if (isNew) {
                    for (NhaCungCap found : service.timNhaCungCap(actor, value.getMaNhaCungCap(), null))
                        if (value.getMaNhaCungCap().equals(found.getMaNhaCungCap()))
                            throw new IllegalArgumentException("Mã nhà cung cấp đã tồn tại");
                }
                service.luuNhaCungCap(actor, value);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    load();
                    message("Lưu nhà cung cấp thành công.");
                } catch (Exception exception) {
                    message("Không lưu được nhà cung cấp.", exception);
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
        JOptionPane.showMessageDialog(this, text == null ? fallback : text, "Lỗi", JOptionPane.ERROR_MESSAGE);
    }
}
