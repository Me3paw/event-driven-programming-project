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
import service.NguoiDung;
import service.DichVuDanhMuc;

@SuppressWarnings("serial")
public final class NhanVienPanel extends JPanel {
    private final NguoiDung actor;
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

    public NhanVienPanel(NguoiDung actor) {
        super(new BorderLayout(0, 8));
        this.actor = actor;
        title.setEditable(true);
        setBackground(UiTheme.BACKGROUND);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bar.setBackground(Color.WHITE);
        bar.setBorder(UiTheme.CARD);
        themTruong(bar, "Từ khóa", key);
        themTruong(bar, "Chức vụ", title);
        themTruong(bar, "Trạng thái", state);
        themNut(bar, "Tìm", new Runnable() { @Override public void run() { taiDanhSach(); } });
        themNut(bar, "Chi tiết", new Runnable() { @Override public void run() { xemChiTiet(); } });
        themNut(bar, "Thêm", new Runnable() { @Override public void run() { sua(null); } });
        themNut(bar, "Sửa", new Runnable() { @Override public void run() {
            NhanVien value = mucDaChon();
            if (value == null) { baoLoi("Hãy chọn nhân viên."); return; }
            sua(value);
        } });
        themNut(bar, "Đổi trạng thái", new Runnable() { @Override public void run() { doiTrangThai(); } });
        add(bar, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        taiDanhSach();
    }

    private void themTruong(JPanel panel, String text, JComponent field) {
        JLabel label = new JLabel(text);
        label.setLabelFor(field);
        panel.add(label);
        panel.add(field);
    }

    private void themNut(JPanel panel, String text, final Runnable action) {
        JButton button = new JButton(text);
        button.addActionListener(event -> action.run());
        panel.add(button);
    }

    private void taiDanhSach() {
        final String keyword = key.getText().trim();
        final String filter = "Tất cả".equals(state.getSelectedItem()) ? ""
                : (String) state.getSelectedItem();
        final String position = "Tất cả".equals(title.getSelectedItem()) ? ""
                : String.valueOf(title.getEditor().getItem()).trim();
        table.clearSelection();
        new SwingWorker<List<NhanVien>, Void>() {
            @Override
            protected List<NhanVien> doInBackground() throws Exception {
                return new DichVuDanhMuc().timNhanVien(actor, keyword, filter, position);
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
                    baoLoi("Không tải được nhân viên.", exception);
                }
            }
        }.execute();
    }

    private NhanVien mucDaChon() {
        int row = table.getSelectedRow();
        return row < 0 ? null : rows.get(table.convertRowIndexToModel(row));
    }

    private void xemChiTiet() {
        NhanVien value = mucDaChon();
        if (value == null) {
            baoLoi("Hãy chọn nhân viên.");
            return;
        }
        JPanel details = new JPanel(new GridLayout(0, 2, 8, 4));
        themChiTiet(details, "Mã", value.getMaNhanVien());
        themChiTiet(details, "Họ tên", value.getHoTen());
        themChiTiet(details, "Điện thoại", value.getSoDienThoai());
        themChiTiet(details, "Chức vụ", value.getChucVu());
        themChiTiet(details, "Trạng thái", value.getTrangThai());
        DialogActions.info(this, details, "Chi tiết nhân viên");
    }

    private void themChiTiet(JPanel panel, String label, String value) {
        panel.add(new JLabel(label));
        panel.add(new JLabel(value == null ? "" : value));
    }

    private void sua(NhanVien existing) {
        if (existing != null) {
            moFormSua(existing.getMaNhanVien(), existing);
            return;
        }
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return new DichVuDanhMuc().maNhanVienTiepTheo(actor);
            }

            @Override
            protected void done() {
                try {
                    moFormSua(get(), null);
                } catch (Exception exception) {
                    baoLoi("Không tạo được mã nhân viên.", exception);
                }
            }
        }.execute();
    }

    private void moFormSua(String generatedId, NhanVien existing) {
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
                DichVuDanhMuc service = new DichVuDanhMuc();
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
                    taiDanhSach();
                    JOptionPane.showMessageDialog(NhanVienPanel.this, "Lưu nhân viên thành công.", "Thành công",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception exception) {
                    baoLoi("Không lưu được nhân viên.", exception);
                }
            }
        }.execute();
    }

    private void doiTrangThai() {
        NhanVien selected = mucDaChon();
        if (selected == null) {
            baoLoi("Hãy chọn nhân viên.");
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
                new DichVuDanhMuc().capNhatTrangThaiNhanVien(actor, id, value);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    taiDanhSach();
                    JOptionPane.showMessageDialog(NhanVienPanel.this, "Đổi trạng thái nhân viên thành công.", "Thành công",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception exception) {
                    baoLoi("Không đổi được trạng thái.", exception);
                }
            }
        }.execute();
    }

    private void baoLoi(String message) {
        baoLoi(message, null);
    }

    private void baoLoi(String message, Exception exception) {
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
