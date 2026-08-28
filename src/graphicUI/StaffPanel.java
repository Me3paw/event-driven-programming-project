package graphicUI;

import components.UiTheme;
import entity.NhanVien;
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
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import service.Actor;
import service.MasterDataService;

public final class StaffPanel extends JPanel {
    private final Actor actor;
    private final JTextField key = new JTextField(14);
    private final JTextField state = new JTextField("", 10);
    private final List<NhanVien> employeeRows = new ArrayList<NhanVien>();
    private final List<TaiKhoan> accountRows = new ArrayList<TaiKhoan>();
    private final DefaultTableModel employees = new DefaultTableModel(
            new String[] {"Mã", "Họ tên", "Điện thoại", "Chức vụ", "Trạng thái"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final DefaultTableModel accounts = new DefaultTableModel(
            new String[] {"Tài khoản", "Nhân viên", "Vai trò", "Trạng thái"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable employeeTable = new JTable(employees);
    private final JTable accountTable = new JTable(accounts);

    public StaffPanel(Actor actor) {
        super(new BorderLayout(0, 8));
        this.actor = actor;
        setBackground(UiTheme.BACKGROUND);
        linkSelections(employeeTable, accountTable);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.setBackground(Color.WHITE);
        buttons.setBorder(UiTheme.CARD);
        buttons.add(new JLabel("Từ khóa"));
        buttons.add(key);
        buttons.add(new JLabel("Trạng thái"));
        buttons.add(state);
        addButton(buttons, "Tìm", new Runnable() { @Override public void run() { load(); } });
        addButton(buttons, "Chi tiết", new Runnable() { @Override public void run() { detail(); } });
        addButton(buttons, "Thêm / sửa nhân viên", new Runnable() { @Override public void run() { editEmployee(); } });
        addButton(buttons, "Tạo / đặt lại tài khoản", new Runnable() { @Override public void run() { editAccount(); } });
        addButton(buttons, "Đổi trạng thái", new Runnable() { @Override public void run() { changeState(); } });
        addButton(buttons, "Xóa dòng chọn", new Runnable() { @Override public void run() { remove(); } });
        add(buttons, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(employeeTable), new JScrollPane(accountTable));
        split.setResizeWeight(.55);
        add(split, BorderLayout.CENTER);
        load();
    }

    static void linkSelections(final JTable first, final JTable second) {
        first.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        second.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        first.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && first.getSelectedRow() >= 0) second.clearSelection();
        });
        second.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && second.getSelectedRow() >= 0) first.clearSelection();
        });
    }

    private void addButton(JPanel panel, String text, final Runnable action) {
        JButton button = new JButton(text);
        button.addActionListener(event -> action.run());
        panel.add(button);
    }

    private void load() {
        final String keyword = key.getText().trim();
        final String filter = state.getText().trim();
        new SwingWorker<List<NhanVien>, Void>() {
            @Override
            protected List<NhanVien> doInBackground() throws Exception {
                return new MasterDataService().timNhanVien(actor, keyword, filter);
            }

            @Override
            protected void done() {
                try {
                    employeeRows.clear();
                    employeeRows.addAll(get());
                    employeeTable.clearSelection();
                    employees.setRowCount(0);
                    for (NhanVien value : employeeRows) {
                        employees.addRow(new Object[] {value.getMaNhanVien(), value.getHoTen(),
                                value.getSoDienThoai(), value.getChucVu(), value.getTrangThai()});
                    }
                } catch (Exception exception) {
                    showError("Không tải được nhân viên.");
                }
            }
        }.execute();
        new SwingWorker<List<TaiKhoan>, Void>() {
            @Override
            protected List<TaiKhoan> doInBackground() throws Exception {
                return new MasterDataService().timTaiKhoan(actor, keyword, "", filter);
            }

            @Override
            protected void done() {
                try {
                    accountRows.clear();
                    accountRows.addAll(get());
                    accountTable.clearSelection();
                    accounts.setRowCount(0);
                    for (TaiKhoan value : accountRows) {
                        accounts.addRow(new Object[] {value.getTenDangNhap(), value.getMaNhanVien(),
                                value.getVaiTro(), value.getTrangThai()});
                    }
                } catch (Exception exception) {
                    showError("Không tải được tài khoản.");
                }
            }
        }.execute();
    }

    private Selected selected() {
        int employeeRow = employeeTable.getSelectedRow();
        int accountRow = accountTable.getSelectedRow();
        if ((employeeRow < 0) == (accountRow < 0)) return null;
        if (employeeRow >= 0) {
            return Selected.employee(employeeRows.get(employeeTable.convertRowIndexToModel(employeeRow)));
        }
        return Selected.account(accountRows.get(accountTable.convertRowIndexToModel(accountRow)));
    }

    private void detail() {
        Selected selected = selected();
        if (selected == null) {
            showError("Hãy chọn nhân viên hoặc tài khoản.");
            return;
        }
        JPanel details = new JPanel(new GridLayout(0, 2, 8, 4));
        if (selected.isEmployee()) {
            NhanVien value = selected.employee;
            addDetail(details, "Mã", value.getMaNhanVien());
            addDetail(details, "Họ tên", value.getHoTen());
            addDetail(details, "Điện thoại", value.getSoDienThoai());
            addDetail(details, "Chức vụ", value.getChucVu());
            addDetail(details, "Trạng thái", value.getTrangThai());
        } else {
            TaiKhoan value = selected.account;
            addDetail(details, "Tên đăng nhập", value.getTenDangNhap());
            addDetail(details, "Mã nhân viên", value.getMaNhanVien());
            addDetail(details, "Vai trò", value.getVaiTro());
            addDetail(details, "Trạng thái", value.getTrangThai());
        }
        JOptionPane.showMessageDialog(this, details, "Chi tiết", JOptionPane.INFORMATION_MESSAGE);
    }

    private void addDetail(JPanel panel, String label, String value) {
        panel.add(new JLabel(label));
        panel.add(new JLabel(value == null ? "" : value));
    }

    private void editEmployee() {
        Selected selected = selected();
        NhanVien existing = selected != null && selected.isEmployee() ? selected.employee : null;
        JTextField id = new JTextField(existing == null ? "" : existing.getMaNhanVien());
        JTextField name = new JTextField(existing == null ? "" : existing.getHoTen());
        JTextField phone = new JTextField(existing == null ? "" : existing.getSoDienThoai());
        JTextField title = new JTextField(existing == null ? "" : existing.getChucVu());
        id.setEditable(existing == null);
        JPanel form = new JPanel(new GridLayout(0, 1, 0, 4));
        addField(form, "Mã", id);
        addField(form, "Họ tên", name);
        addField(form, "Điện thoại", phone);
        addField(form, "Chức vụ", title);
        if (JOptionPane.showConfirmDialog(this, form, existing == null ? "Thêm nhân viên" : "Sửa nhân viên",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;

        final String employeeId = id.getText().trim();
        final String employeeName = name.getText().trim();
        final String employeePhone = phone.getText().trim();
        final String employeeTitle = title.getText().trim();
        if (blank(employeeId) || blank(employeeName) || blank(employeePhone) || blank(employeeTitle)) {
            showError("Nhập đủ mã, họ tên, điện thoại và chức vụ.");
            return;
        }
        final NhanVien value = new NhanVien();
        value.setMaNhanVien(employeeId);
        value.setHoTen(employeeName);
        value.setSoDienThoai(employeePhone);
        value.setChucVu(employeeTitle);
        value.setTrangThai(existing == null ? "DANG_LAM" : existing.getTrangThai());
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                new MasterDataService().luuNhanVien(actor, value);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    load();
                } catch (Exception exception) {
                    showError("Không lưu được nhân viên.");
                }
            }
        }.execute();
    }

    private void editAccount() {
        Selected selected = selected();
        TaiKhoan existing = selected != null && !selected.isEmployee() ? selected.account : null;
        JTextField user = new JTextField(existing == null ? "" : existing.getTenDangNhap());
        JTextField employee = new JTextField(existing == null ? "" : existing.getMaNhanVien());
        JComboBox<String> role = new JComboBox<String>(new String[] {"QUAN_LY", "THU_NGAN"});
        role.setSelectedItem(existing == null ? "THU_NGAN" : existing.getVaiTro());
        JPasswordField password = new JPasswordField();
        user.setEditable(existing == null);
        JPanel form = new JPanel(new GridLayout(0, 1, 0, 4));
        addField(form, "Tên đăng nhập", user);
        addField(form, "Mã nhân viên", employee);
        addField(form, "Vai trò", role);
        addField(form, existing == null ? "Mật khẩu" : "Mật khẩu mới", password);
        int choice = JOptionPane.showConfirmDialog(this, form,
                existing == null ? "Tạo tài khoản" : "Đặt lại tài khoản", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        final char[] secret = password.getPassword();
        password.setText("");
        if (choice != JOptionPane.OK_OPTION) {
            Arrays.fill(secret, '\0');
            return;
        }

        final String username = user.getText().trim();
        final String employeeId = employee.getText().trim();
        final String accountRole = (String) role.getSelectedItem();
        if (blank(username) || blank(employeeId) || blank(accountRole)
                || (existing == null && secret.length == 0)) {
            Arrays.fill(secret, '\0');
            showError(existing == null ? "Nhập đủ thông tin và mật khẩu." : "Nhập đủ thông tin tài khoản.");
            return;
        }
        final TaiKhoan value = new TaiKhoan();
        value.setTenDangNhap(username);
        value.setMaNhanVien(employeeId);
        value.setVaiTro(accountRole);
        value.setTrangThai(existing == null ? "HOAT_DONG" : existing.getTrangThai());
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                new MasterDataService().luuTaiKhoan(actor, value, secret);
                return null;
            }

            @Override
            protected void done() {
                Arrays.fill(secret, '\0');
                try {
                    get();
                    load();
                } catch (Exception exception) {
                    showError("Không lưu được tài khoản.");
                }
            }
        }.execute();
    }

    private void changeState() {
        Selected selected = selected();
        if (selected == null) {
            showError("Hãy chọn nhân viên hoặc tài khoản.");
            return;
        }
        final boolean employee = selected.isEmployee();
        final String id = selected.id();
        final String value = selected.nextStatus();
        String message = ("DANG_LAM".equals(value) || "HOAT_DONG".equals(value)
                ? "Kích hoạt lại " : "Ngừng hoạt động ") + id + "?";
        if (JOptionPane.showConfirmDialog(this, message, "Xác nhận", JOptionPane.YES_NO_OPTION)
                != JOptionPane.YES_OPTION) return;
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                MasterDataService service = new MasterDataService();
                if (employee) service.capNhatTrangThaiNhanVien(actor, id, value);
                else service.capNhatTrangThaiTaiKhoan(actor, id, value);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    load();
                } catch (Exception exception) {
                    showError("Không đổi được trạng thái.");
                }
            }
        }.execute();
    }

    private void remove() {
        Selected selected = selected();
        if (selected == null) {
            showError("Hãy chọn nhân viên hoặc tài khoản.");
            return;
        }
        final boolean employee = selected.isEmployee();
        final String id = selected.id();
        if (JOptionPane.showConfirmDialog(this, "Xóa " + id + "?", "Xác nhận", JOptionPane.YES_NO_OPTION)
                != JOptionPane.YES_OPTION) return;
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                MasterDataService service = new MasterDataService();
                if (employee) service.xoaNhanVien(actor, id);
                else service.xoaTaiKhoan(actor, id);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    load();
                } catch (Exception exception) {
                    showError("Không thể xóa bản ghi có liên hệ lịch sử.");
                }
            }
        }.execute();
    }

    private void addField(JPanel panel, String text, JComponent field) {
        JLabel label = new JLabel(text);
        label.setLabelFor(field);
        panel.add(label);
        panel.add(field);
    }

    private boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    private static final class Selected {
        private final NhanVien employee;
        private final TaiKhoan account;

        private Selected(NhanVien employee, TaiKhoan account) {
            this.employee = employee;
            this.account = account;
        }

        private static Selected employee(NhanVien value) {
            return new Selected(value, null);
        }

        private static Selected account(TaiKhoan value) {
            return new Selected(null, value);
        }

        private boolean isEmployee() {
            return employee != null;
        }

        private String id() {
            return isEmployee() ? employee.getMaNhanVien() : account.getTenDangNhap();
        }

        private String nextStatus() {
            if (isEmployee()) return "DANG_LAM".equals(employee.getTrangThai()) ? "NGHI_VIEC" : "DANG_LAM";
            return "HOAT_DONG".equals(account.getTrangThai()) ? "KHOA" : "HOAT_DONG";
        }
    }
}
