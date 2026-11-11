package graphicUI;

import components.DialogActions;
import components.FormValidator;
import components.UiTheme;
import entity.TaiKhoan;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import service.Actor;
import service.MasterDataService;

@SuppressWarnings("serial")
public final class AccountPanel extends JPanel {
    private final Actor actor;
    private final JTextField key = new JTextField(14);
    private final JComboBox<String> state = new JComboBox<String>(
            new String[] {"Tất cả", "HOAT_DONG", "KHOA"});
    private final List<TaiKhoan> rows = new ArrayList<TaiKhoan>();
    private final DefaultTableModel model = new DefaultTableModel(
            new String[] {"Tên đăng nhập", "Mã nhân viên", "Họ tên", "Chức vụ", "Vai trò", "Trạng thái"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);

    public AccountPanel(Actor actor) {
        super(new BorderLayout(0, 8));
        this.actor = actor;
        setBackground(UiTheme.BACKGROUND);
        table.setAutoCreateRowSorter(true);
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bar.setBackground(Color.WHITE);
        bar.setBorder(UiTheme.CARD);
        addField(bar, "Từ khóa", key);
        addField(bar, "Trạng thái", state);
        button(bar, "Tìm", this::load);
        button(bar, "Chi tiết", this::detail);
        button(bar, "Thêm tài khoản", () -> edit(null));
        button(bar, "Sửa", () -> {
            TaiKhoan value = selected();
            if (value == null) { showError("Hãy chọn tài khoản."); return; }
            edit(value);
        });
        button(bar, "Đổi trạng thái", this::changeState);
        add(bar, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        load();
    }

    private void load() {
        final String keyword = key.getText().trim();
        final String filter = "Tất cả".equals(state.getSelectedItem()) ? ""
                : (String) state.getSelectedItem();
        new SwingWorker<List<TaiKhoan>, Void>() {
            @Override protected List<TaiKhoan> doInBackground() throws Exception {
                return new MasterDataService().timTaiKhoan(actor, keyword, "", filter);
            }
            @Override protected void done() {
                try {
                    rows.clear();
                    rows.addAll(get());
                    model.setRowCount(0);
                    for (TaiKhoan value : rows)
                        model.addRow(new Object[] {value.getTenDangNhap(), value.getMaNhanVien(),
                                value.getTenNhanVien(), value.getChucVuNhanVien(),
                                value.getVaiTro(), value.getTrangThai()});
                } catch (Exception exception) {
                    showError("Không tải được tài khoản.", exception);
                }
            }
        }.execute();
    }

    private TaiKhoan selected() {
        int row = table.getSelectedRow();
        return row < 0 ? null : rows.get(table.convertRowIndexToModel(row));
    }

    private void detail() {
        TaiKhoan value = selected();
        if (value == null) { showError("Hãy chọn tài khoản."); return; }
        JPanel details = new JPanel(new GridLayout(0, 2, 8, 4));
        addDetail(details, "Tên đăng nhập", value.getTenDangNhap());
        addDetail(details, "Mã nhân viên", value.getMaNhanVien());
        addDetail(details, "Họ tên", value.getTenNhanVien());
        addDetail(details, "Chức vụ", value.getChucVuNhanVien());
        addDetail(details, "Vai trò", value.getVaiTro());
        addDetail(details, "Trạng thái", value.getTrangThai());
        DialogActions.info(this, details, "Chi tiết tài khoản");
    }

    private void edit(TaiKhoan existing) {
        JTextField username = new JTextField(existing == null ? "" : existing.getTenDangNhap());
        JTextField employee = new JTextField(existing == null ? "" : existing.getMaNhanVien());
        JComboBox<String> role = new JComboBox<String>(new String[] {"QUAN_LY", "THU_NGAN"});
        role.setSelectedItem(existing == null ? "THU_NGAN" : existing.getVaiTro());
        JPasswordField password = new JPasswordField();
        username.setEditable(existing == null);
        FormValidator validator = new FormValidator();
        JPanel form = FormValidator.form();
        form.add(validator.row("Tên đăng nhập", username));
        form.add(validator.row("Mã nhân viên", employee));
        form.add(validator.row("Vai trò", role));
        form.add(validator.row(existing == null ? "Mật khẩu" : "Mật khẩu mới", password));
        validator.require(username, "Tên đăng nhập không được để trống.");
        validator.require(employee, "Mã nhân viên không được để trống.");
        validator.matches(employee, "NV[0-9]{3}",
                "Mã nhân viên sai định dạng, phải có dạng NV + đúng 3 số (ví dụ: NV001).");
        if (existing == null)
            validator.require(password, "Mật khẩu không được để trống.");
        boolean confirmed = DialogActions.confirm(this, form,
                existing == null ? "Tạo tài khoản" : "Đặt lại tài khoản", validator);
        final char[] secret = password.getPassword();
        password.setText("");
        if (!confirmed) { Arrays.fill(secret, '\0'); return; }
        final String user = username.getText().trim();
        final String employeeId = employee.getText().trim();
        TaiKhoan value = new TaiKhoan();
        value.setTenDangNhap(user);
        value.setMaNhanVien(employeeId);
        value.setVaiTro((String) role.getSelectedItem());
        value.setTrangThai(existing == null ? "HOAT_DONG" : existing.getTrangThai());
        final boolean isNew = existing == null;
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                MasterDataService service = new MasterDataService();
                if (isNew) {
                    for (TaiKhoan found : service.timTaiKhoan(actor, value.getTenDangNhap(), null, null))
                        if (value.getTenDangNhap().equals(found.getTenDangNhap()))
                            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại");
                }
                service.luuTaiKhoan(actor, value, secret);
                return null;
            }
            @Override protected void done() {
                Arrays.fill(secret, '\0');
                try { get(); load(); JOptionPane.showMessageDialog(AccountPanel.this, "Lưu tài khoản thành công."); }
                catch (Exception exception) { showError("Không lưu được tài khoản.", exception); }
            }
        }.execute();
    }

    private void changeState() {
        TaiKhoan value = selected();
        if (value == null) { showError("Hãy chọn tài khoản."); return; }
        String next = "HOAT_DONG".equals(value.getTrangThai()) ? "KHOA" : "HOAT_DONG";
        if (JOptionPane.showConfirmDialog(this, "Đổi trạng thái tài khoản " + value.getTenDangNhap() + "?",
                "Xác nhận", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                new MasterDataService().capNhatTrangThaiTaiKhoan(actor, value.getTenDangNhap(), next);
                return null;
            }
            @Override protected void done() {
                try { get(); load(); JOptionPane.showMessageDialog(AccountPanel.this, "Đổi trạng thái thành công."); }
                catch (Exception exception) { showError("Không đổi được trạng thái tài khoản.", exception); }
            }
        }.execute();
    }

    private void addField(JPanel panel, String label, JComponent component) {
        panel.add(new JLabel(label));
        panel.add(component);
    }
    private void button(JPanel panel, String text, Runnable action) {
        JButton button = new JButton(text);
        button.addActionListener(event -> action.run());
        panel.add(button);
    }
    private void addDetail(JPanel panel, String label, String value) {
        panel.add(new JLabel(label));
        panel.add(new JLabel(value == null ? "" : value));
    }
    private void showError(String message) { showError(message, null); }
    private void showError(String message, Exception exception) {
        String detail = null;
        if (exception != null) {
            Throwable cause = exception;
            while (cause.getCause() != null
                    && cause instanceof java.util.concurrent.ExecutionException)
                cause = cause.getCause();
            detail = cause.getMessage();
        }
        JOptionPane.showMessageDialog(this, detail == null ? message : message + "\n" + detail,
                "Lỗi", JOptionPane.ERROR_MESSAGE);
    }
}
