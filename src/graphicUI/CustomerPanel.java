package graphicUI;

import components.DialogActions;
import entity.HoaDon;
import entity.KhachHang;
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
import service.CustomerProfile;
import service.MasterDataService;

public final class CustomerPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final Actor actor;
    private final JTextField key = new JTextField(18);
    private final JTextField state = new JTextField("HOAT_DONG", 12);
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
                                value.getDiemHienCo(), value.getDiemTichLuy(), value.getTrangThai()});
                    }
                } catch (Exception exception) {
                    message("Không tải được khách hàng.");
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
                                    + profile.getActualSpend() + " · Hạng: " + profile.getTier() + " · Điểm thưởng: "
                                    + value.getDiemHienCo() + " · Điểm tích lũy: " + value.getDiemTichLuy()),
                            BorderLayout.NORTH);
                    content.add(new JScrollPane(new JTable(history)), BorderLayout.CENTER);
                    JOptionPane.showMessageDialog(CustomerPanel.this, content, "CRM",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception exception) {
                    message("Không tải được hồ sơ khách hàng.");
                }
            }
        }.execute();
    }

    private void edit(KhachHang existing) {
        JTextField id = new JTextField(existing == null ? "" : existing.getMaKhachHang());
        JTextField name = new JTextField(existing == null ? "" : existing.getHoTen());
        JTextField phone = new JTextField(existing == null ? "" : existing.getSoDienThoai());
        id.setEditable(existing == null);
        JPanel form = new JPanel(new GridLayout(0, 1, 0, 4));
        addField(form, "Mã", id);
        addField(form, "Họ tên", name);
        addField(form, "Điện thoại", phone);
        if (!DialogActions.confirm(this, form, existing == null ? "Thêm khách hàng" : "Sửa khách hàng")) return;
        KhachHang value = new KhachHang();
        value.setMaKhachHang(id.getText().trim());
        value.setHoTen(name.getText().trim());
        value.setSoDienThoai(phone.getText().trim());
        value.setTrangThai(existing == null ? "HOAT_DONG" : existing.getTrangThai());
        save(value);
    }

    private void toggle() {
        KhachHang value = selected();
        if (value == null) return;
        value.setTrangThai("HOAT_DONG".equals(value.getTrangThai()) ? "NGUNG_HOAT_DONG" : "HOAT_DONG");
        save(value);
    }

    private void save(final KhachHang value) {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                new MasterDataService().luuKhachHang(actor, value);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    load();
                } catch (Exception exception) {
                    message("Không lưu được khách hàng.");
                }
            }
        }.execute();
    }

    private void remove() {
        final KhachHang value = selected();
        if (value == null || JOptionPane.showConfirmDialog(this, "Xóa khách hàng?", "Xác nhận",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                new MasterDataService().xoaKhachHang(actor, value.getMaKhachHang());
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    load();
                } catch (Exception exception) {
                    message("Không thể xóa khách hàng có lịch sử.");
                }
            }
        }.execute();
    }

    private void message(String text) {
        JOptionPane.showMessageDialog(this, text);
    }
}
