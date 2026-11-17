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
import service.NguoiDung;
import service.DichVuDanhMuc;

public final class NhaCungCapPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final NguoiDung actor;
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

    public NhaCungCapPanel(NguoiDung actor) {
        super(new BorderLayout());
        this.actor = actor;
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        themTruong(bar, "Từ khóa", key);
        themTruong(bar, "Trạng thái", state);
        themNut(bar, "Tìm", new Runnable() { @Override public void run() { taiDanhSach(); } });
        themNut(bar, "Chi tiết", new Runnable() { @Override public void run() { xemChiTiet(); } });
        themNut(bar, "Thêm", new Runnable() { @Override public void run() { sua(null); } });
        themNut(bar, "Sửa", new Runnable() { @Override public void run() { suaMucDaChon(); } });
        themNut(bar, "Đổi trạng thái", new Runnable() { @Override public void run() { doiTrangThai(); } });
        add(bar, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        taiDanhSach();
    }

    private void themNut(JPanel panel, String text, final Runnable action) {
        JButton button = new JButton(text);
        button.addActionListener(event -> action.run());
        panel.add(button);
    }

    private void themTruong(JPanel panel, String text, JComponent field) {
        JLabel label = new JLabel(text);
        label.setLabelFor(field);
        field.getAccessibleContext().setAccessibleName(text);
        panel.add(label);
        panel.add(field);
    }

    private void taiDanhSach() {
        final String keyword = key.getText().trim();
        final String filter = "Tất cả".equals(state.getSelectedItem()) ? ""
                : (String) state.getSelectedItem();
        table.clearSelection();
        new SwingWorker<List<NhaCungCap>, Void>() {
            @Override
            protected List<NhaCungCap> doInBackground() throws Exception {
                return new DichVuDanhMuc().timNhaCungCap(actor, keyword, filter);
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
                    thongBao("Không tải được nhà cung cấp.", exception);
                }
            }
        }.execute();
    }

    private NhaCungCap mucDaChon() {
        int row = table.getSelectedRow();
        return row < 0 ? null : rows.get(table.convertRowIndexToModel(row));
    }

    private void suaMucDaChon() {
        NhaCungCap value = mucDaChon();
        if (value == null) {
            thongBao("Hãy chọn nhà cung cấp để sửa.");
            return;
        }
        sua(value);
    }

    private void xemChiTiet() {
        NhaCungCap value = mucDaChon();
        if (value == null) return;
        JOptionPane.showMessageDialog(this, value.getMaNhaCungCap() + "\n" + value.getTenNhaCungCap() + "\n"
                + value.getSoDienThoai() + "\n" + value.getDiaChi() + "\n" + value.getTrangThai());
    }

    private void sua(NhaCungCap existing) {
        if (existing != null) {
            moFormSua(existing.getMaNhaCungCap(), existing);
            return;
        }
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return new DichVuDanhMuc().maNhaCungCapTiepTheo(actor);
            }

            @Override
            protected void done() {
                try {
                    moFormSua(get(), null);
                } catch (Exception exception) {
                    thongBao("Không tạo được mã nhà cung cấp.", exception);
                }
            }
        }.execute();
    }

    private void moFormSua(String generatedId, NhaCungCap existing) {
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
        luu(value, existing == null);
    }

    private void doiTrangThai() {
        NhaCungCap value = mucDaChon();
        if (value == null) return;
        String next = "HOAT_DONG".equals(value.getTrangThai()) ? "NGUNG_HOAT_DONG" : "HOAT_DONG";
        if (JOptionPane.showConfirmDialog(this, "Chuyển nhà cung cấp sang trạng thái " + next + "?",
                "Xác nhận", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        value.setTrangThai(next);
        luu(value, false);
    }

    private void luu(final NhaCungCap value, final boolean isNew) {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                DichVuDanhMuc service = new DichVuDanhMuc();
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
                    taiDanhSach();
                    thongBao("Lưu nhà cung cấp thành công.");
                } catch (Exception exception) {
                    thongBao("Không lưu được nhà cung cấp.", exception);
                }
            }
        }.execute();
    }

    private void thongBao(String text) {
        JOptionPane.showMessageDialog(this, text, "Thông báo", JOptionPane.INFORMATION_MESSAGE);
    }

    private void thongBao(String fallback, Exception exception) {
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
