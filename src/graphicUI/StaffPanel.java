package graphicUI;

import components.UiTheme;
import components.DialogActions;
import components.FormValidator;
import entity.NhanVien;
import java.awt.BorderLayout;
import java.awt.Color;
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

@SuppressWarnings("serial")
public final class StaffPanel extends JPanel {
    private final Actor actor;
    private final JTextField key = new JTextField(12);
    private final JComboBox<String> state = new JComboBox<String>(
            new String[] {"Tất cả", "DANG_LAM", "NGHI_VIEC"});
    private final JComboBox<String> title = new JComboBox<String>(
            new String[] {"Tất cả", "Thu ngân", "Quản lý", "Nhân viên kho", "Bảo vệ", "Phục vụ"});
    private final List<NhanVien> rows = new ArrayList<NhanVien>();
    private final DefaultTableModel model = new DefaultTableModel(
            new String[] {"Mã", "Họ tên", "Điện thoại", "Chức vụ", "Trạng thái"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);

    public StaffPanel(Actor actor) {
        super(new BorderLayout(0, 8));
        this.actor = actor;
        title.setEditable(true);
        setBackground(UiTheme.BACKGROUND);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bar.setBackground(Color.WHITE);
        bar.setBorder(UiTheme.CARD);
        addField(bar, "Từ khóa", key);
        addField(bar, "Chức vụ", title);
        addField(bar, "Trạng thái", state);
        addButton(bar, "Tìm", new Runnable() { @Override public void run() { load(); } });
        addButton(bar, "Chi tiết", new Runnable() { @Override public void run() { detail(); } });
        addButton(bar, "Thêm", new Runnable() { @Override public void run() { edit(null); } });
        addButton(bar, "Sửa", new Runnable() { @Override public void run() {
            NhanVien value = selected();
            if (value == null) { showError("Hãy chọn nhân viên."); return; }
            edit(value);
        } });
        addButton(bar, "Đổi trạng thái", new Runnable() { @Override public void run() { toggleStatus(); } });
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
        final String position = "Tất cả".equals(title.getSelectedItem()) ? ""
                : String.valueOf(title.getEditor().getItem()).trim();
        table.clearSelection();
        new SwingWorker<List<NhanVien>, Void>() {
            @Override
            protected List<NhanVien> doInBackground() throws Exception {
                return new MasterDataService().timNhanVien(actor, keyword, filter, position);
            }

            @Override
            protected void done() {
                try {
                    rows.clear();
                    rows.addAll(get());
                    model.setRowCount(0);
                    for (NhanVien value : rows) {
                        model.addRow(new Object[] {value.getMaNhanVien(), value.getHoTen(),
                                value.getSoDienThoai(), value.getChucVu(), value.getTrangThai()});
                    }
                } catch (Exception exception) {
                    showError("Không tải được nhân viên.", exception);
                }
            }
        }.execute();
    }

    private NhanVien selected() {
        int row = table.getSelectedRow();
        return row < 0 ? null : rows.get(table.convertRowIndexToModel(row));
    }

    private void detail() {
        NhanVien value = selected();
        if (value == null) {
            showError("Hãy chọn nhân viên.");
            return;
        }
        JPanel details = new JPanel(new GridLayout(0, 2, 8, 4));
        addDetail(details, "Mã", value.getMaNhanVien());
        addDetail(details, "Họ tên", value.getHoTen());
        addDetail(details, "Điện thoại", value.getSoDienThoai());
        addDetail(details, "Chức vụ", value.getChucVu());
        addDetail(details, "Trạng thái", value.getTrangThai());
        DialogActions.info(this, details, "Chi tiết nhân viên");
    }

    private void addDetail(JPanel panel, String label, String value) {
        panel.add(new JLabel(label));
        panel.add(new JLabel(value == null ? "" : value));
    }

    private void edit(NhanVien existing) {
        if (existing != null) {
            editForm(existing.getMaNhanVien(), existing);
            return;
        }
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return new MasterDataService().maNhanVienTiepTheo(actor);
            }

            @Override
            protected void done() {
                try {
                    editForm(get(), null);
                } catch (Exception exception) {
                    showError("Không tạo được mã nhân viên.", exception);
                }
            }
        }.execute();
    }

    private void editForm(String generatedId, NhanVien existing) {
        JTextField id = new JTextField(existing == null ? generatedId : existing.getMaNhanVien());
        JTextField name = new JTextField(existing == null ? "" : existing.getHoTen());
        JTextField phone = new JTextField(existing == null ? "" : existing.getSoDienThoai());
        JComboBox<String> title = new JComboBox<String>(
                new String[] {"Thu ngân", "Quản lý", "Nhân viên kho", "Bảo vệ", "Phục vụ"});
        title.setEditable(true);
        title.setSelectedItem(existing == null ? "Thu ngân" : existing.getChucVu());
        id.setEditable(false);
        FormValidator validator = new FormValidator();
        JPanel form = FormValidator.form();
        form.add(validator.row("Mã", id));
        form.add(validator.row("Họ tên", name));
        form.add(validator.row("Điện thoại", phone));
        form.add(validator.row("Chức vụ", title));
        validator.require(name, "Họ tên nhân viên không được để trống.");
        validator.matches(name, "[\\p{L}\\s]+",
                "Họ tên chỉ được chứa chữ cái và khoảng trắng, không chứa số.");
        validator.require(phone, "Số điện thoại nhân viên không được để trống.");
        validator.matches(phone, "0[35789][0-9]{8}",
                "Số điện thoại sai định dạng, phải có dạng 0xxxxxxxxx (10 số).");
        validator.custom(title, () -> String.valueOf(title.getEditor().getItem()).trim().isEmpty()
                ? "Chức vụ không được để trống." : null);
        if (!DialogActions.confirm(this, form, existing == null ? "Thêm nhân viên" : "Sửa nhân viên",
                validator)) return;

        final String employeeId = id.getText().trim();
        final String employeeName = name.getText().trim();
        final String employeePhone = phone.getText().trim();
        final String employeeTitle = String.valueOf(title.getEditor().getItem()).trim();
        final NhanVien value = new NhanVien();
        value.setMaNhanVien(employeeId);
        value.setHoTen(employeeName);
        value.setSoDienThoai(employeePhone);
        value.setChucVu(employeeTitle);
        value.setTrangThai(existing == null ? "DANG_LAM" : existing.getTrangThai());
        final boolean isNew = existing == null;
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                MasterDataService service = new MasterDataService();
                if (isNew) {
                    for (NhanVien found : service.timNhanVien(actor, value.getMaNhanVien(), null))
                        if (value.getMaNhanVien().equals(found.getMaNhanVien()))
                            throw new IllegalArgumentException("Mã nhân viên đã tồn tại");
                }
                service.luuNhanVien(actor, value);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    load();
                    JOptionPane.showMessageDialog(StaffPanel.this, "Lưu nhân viên thành công.", "Thành công",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception exception) {
                    showError("Không lưu được nhân viên.", exception);
                }
            }
        }.execute();
    }

    private void toggleStatus() {
        NhanVien selected = selected();
        if (selected == null) {
            showError("Hãy chọn nhân viên.");
            return;
        }
        final String id = selected.getMaNhanVien();
        final String value = "DANG_LAM".equals(selected.getTrangThai()) ? "NGHI_VIEC" : "DANG_LAM";
        String message = ("DANG_LAM".equals(value) ? "Cho nhân viên " : "Cho nghỉ việc ") + id + "?";
        if (JOptionPane.showConfirmDialog(this, message, "Xác nhận", JOptionPane.YES_NO_OPTION)
                != JOptionPane.YES_OPTION) return;
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                new MasterDataService().capNhatTrangThaiNhanVien(actor, id, value);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    load();
                    JOptionPane.showMessageDialog(StaffPanel.this, "Đổi trạng thái nhân viên thành công.", "Thành công",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception exception) {
                    showError("Không đổi được trạng thái.", exception);
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
