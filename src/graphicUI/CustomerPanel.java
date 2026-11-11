package graphicUI;

import components.DialogActions;
import components.FormValidator;
import components.UiFormat;
import entity.HoaDon;
import entity.KhachHang;
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
import service.CustomerProfile;
import service.MasterDataService;

public final class CustomerPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final Actor actor;
    private final JTextField key = new JTextField(18);
    private final JComboBox<String> state = new JComboBox<String>(
            new String[] {"Đang hoạt động", "Ngừng hoạt động", "Tất cả"});
    private final List<KhachHang> rows = new ArrayList<KhachHang>();
    private final DefaultTableModel model = new DefaultTableModel(
            new String[] {"Mã", "Họ tên", "Điện thoại", "Điểm thưởng", "Điểm tích lũy", "Trạng thái"}, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);

    public CustomerPanel(Actor actor) {
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
        final String selected = (String) state.getSelectedItem();
        final String filter = "Tất cả".equals(selected) ? ""
                : "Ngừng hoạt động".equals(selected) ? "NGUNG_HOAT_DONG" : "HOAT_DONG";
        table.clearSelection();
        new SwingWorker<List<KhachHang>, Void>() {
            @Override
            protected List<KhachHang> doInBackground() throws Exception {
                return new MasterDataService().timKhachHang(actor, keyword, filter);
            }

            @Override
            protected void done() {
                try {
                    rows.clear();
                    rows.addAll(get());
                    model.setRowCount(0);
                    for (KhachHang value : rows) {
                        model.addRow(new Object[] {value.getMaKhachHang(), value.getHoTen(), value.getSoDienThoai(),
                                value.getDiemHienCo(), value.getDiemTichLuy(), statusLabel(value.getTrangThai())});
                    }
                } catch (Exception exception) {
                    message("Không tải được khách hàng.", exception);
                }
            }
        }.execute();
    }

    private KhachHang selected() {
        int row = table.getSelectedRow();
        return row < 0 ? null : rows.get(table.convertRowIndexToModel(row));
    }

    private void editSelected() {
        KhachHang value = selected();
        if (value == null) {
            message("Hãy chọn khách hàng để sửa.");
            return;
        }
        edit(value);
    }

    private void detail() {
        KhachHang customer = selected();
        if (customer == null) return;
        final String id = customer.getMaKhachHang();
        new SwingWorker<CustomerProfile, Void>() {
            @Override
            protected CustomerProfile doInBackground() throws Exception {
                return new MasterDataService().chiTietKhachHang(actor, id);
            }

            @Override
            protected void done() {
                try {
                    CustomerProfile profile = get();
                    KhachHang value = profile.getCustomer();
                    DefaultTableModel history = new DefaultTableModel(
                            new String[] {"Hóa đơn", "Thời điểm", "Trạng thái", "Tổng"}, 0) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public boolean isCellEditable(int row, int column) {
                            return false;
                        }
                    };
                    for (HoaDon invoice : profile.getHistory()) {
                        history.addRow(new Object[] {invoice.getMaHoaDon(), invoice.getThoiDiemLap(),
                                invoice.getTrangThai(), invoice.getTongThanhToan()});
                    }
                    JPanel content = new JPanel(new BorderLayout(0, 8));
                    content.add(
                            new JLabel(value.getMaKhachHang() + " · " + value.getHoTen() + " · "
                                    + value.getSoDienThoai() + " · " + value.getTrangThai() + " · Chi tiêu: "
                                    + UiFormat.money(profile.getActualSpend()) + " đ · Hạng: " + profile.getTier()
                                    + " · Điểm thưởng: " + UiFormat.money(value.getDiemHienCo())
                                    + " · Điểm tích lũy: " + UiFormat.money(value.getDiemTichLuy())),
                            BorderLayout.NORTH);
                    JTable historyTable = new JTable(history);
                    historyTable.getColumnModel().getColumn(3).setCellRenderer(UiFormat.moneyRenderer());
                    JScrollPane historyScroll = new JScrollPane(historyTable);
                    historyScroll.setPreferredSize(DialogActions.DETAIL_TABLE);
                    content.add(historyScroll, BorderLayout.CENTER);
                    DialogActions.info(CustomerPanel.this, content, "CRM");
                } catch (Exception exception) {
                    message("Không tải được hồ sơ khách hàng.", exception);
                }
            }
        }.execute();
    }

    private void edit(KhachHang existing) {
        if (existing != null) {
            editForm(existing.getMaKhachHang(), existing);
            return;
        }
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return new MasterDataService().maKhachHangTiepTheo(actor);
            }

            @Override
            protected void done() {
                try {
                    editForm(get(), null);
                } catch (Exception exception) {
                    message("Không tạo được mã khách hàng.", exception);
                }
            }
        }.execute();
    }

    private void editForm(String generatedId, KhachHang existing) {
        JTextField id = new JTextField(existing == null ? generatedId : existing.getMaKhachHang());
        JTextField name = new JTextField(existing == null ? "" : existing.getHoTen());
        JTextField phone = new JTextField(existing == null ? "" : existing.getSoDienThoai());
        id.setEditable(false);
        FormValidator validator = new FormValidator();
        JPanel form = FormValidator.form();
        form.add(validator.row("Mã", id));
        form.add(validator.row("Họ tên", name));
        form.add(validator.row("Điện thoại", phone));
        validator.require(name, "Họ tên khách hàng không được để trống.");
        validator.matches(name, "[\\p{L}\\s]+",
                "Họ tên chỉ được chứa chữ cái và khoảng trắng, không chứa số.");
        validator.matches(phone, "0[35789][0-9]{8}",
                "Số điện thoại sai định dạng, phải có dạng 0xxxxxxxxx (10 số, ví dụ: 0901234567).");
        if (!DialogActions.confirm(this, form, existing == null ? "Thêm khách hàng" : "Sửa khách hàng",
                validator)) return;
        KhachHang value = new KhachHang();
        value.setMaKhachHang(id.getText().trim());
        value.setHoTen(name.getText().trim());
        value.setSoDienThoai(phone.getText().trim());
        value.setTrangThai(existing == null ? "HOAT_DONG" : existing.getTrangThai());
        save(value, existing == null);
    }

    private void toggle() {
        KhachHang value = selected();
        if (value == null) return;
        String next = "HOAT_DONG".equals(value.getTrangThai()) ? "NGUNG_HOAT_DONG" : "HOAT_DONG";
        if (JOptionPane.showConfirmDialog(this,
                "Chuyển khách hàng sang trạng thái " + statusLabel(next)
                        + "? Khách hàng sẽ không bán hàng được nữa nhưng hóa đơn cũ vẫn giữ nguyên.",
                "Xác nhận", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
            return;
        value.setTrangThai(next);
        save(value, false);
    }

    private void save(final KhachHang value, final boolean isNew) {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                MasterDataService service = new MasterDataService();
                if (isNew) {
                    for (KhachHang found : service.timKhachHang(actor, value.getMaKhachHang(), null))
                        if (value.getMaKhachHang().equals(found.getMaKhachHang()))
                            throw new IllegalArgumentException("Mã khách hàng đã tồn tại");
                }
                service.luuKhachHang(actor, value);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    load();
                    message("Lưu khách hàng thành công.");
                } catch (Exception exception) {
                    message("Không lưu được khách hàng.", exception);
                }
            }
        }.execute();
    }

    private static String statusLabel(String status) {
        return "NGUNG_HOAT_DONG".equals(status) ? "Ngừng hoạt động" : "Đang hoạt động";
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
