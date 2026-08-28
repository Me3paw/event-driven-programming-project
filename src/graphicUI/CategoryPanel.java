package graphicUI;

import components.UiTheme;
import components.DialogActions;
import entity.LoaiSanPham;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
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
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import service.Actor;
import service.MasterDataService;

@SuppressWarnings("serial")
public final class CategoryPanel extends JPanel {
    private final Actor actor;
    private final boolean editable;
    private final JTextField keyword = new JTextField(18);
    private final List<LoaiSanPham> rows = new ArrayList<LoaiSanPham>();
    private final DefaultTableModel model =
            new DefaultTableModel(new String[] {"Mã", "Tên", "Mô tả", "Trạng thái"}, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
    private final JTable table = new JTable(model);

    public CategoryPanel(Actor actor, boolean editable) {
        super(new BorderLayout(0, 8));
        this.actor = actor;
        this.editable = editable;
        setBackground(UiTheme.BACKGROUND);
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bar.setBackground(Color.WHITE);
        bar.setBorder(UiTheme.CARD);
        addField(bar, "Từ khóa", keyword);
        button(bar, "Tìm", KeyEvent.VK_T, new Runnable() {
            @Override
            public void run() {
                load();
            }
        });
        button(bar, "Chi tiết", KeyEvent.VK_C, new Runnable() {
            @Override
            public void run() {
                detail();
            }
        });
        if (editable) {
            button(bar, "Thêm", KeyEvent.VK_A, new Runnable() {
                @Override
                public void run() {
                    edit(null);
                }
            });
            button(bar, "Sửa", KeyEvent.VK_S, new Runnable() {
                @Override
                public void run() {
                    editSelected();
                }
            });
            button(bar, "Đổi trạng thái", KeyEvent.VK_D, new Runnable() {
                @Override
                public void run() {
                    toggle();
                }
            });
            button(bar, "Xóa", KeyEvent.VK_X, new Runnable() {
                @Override
                public void run() {
                    remove();
                }
            });
        }
        add(bar, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        load();
    }

    private void button(JPanel panel, String text, int mnemonic, final Runnable action) {
        JButton button = new JButton(text);
        button.setMnemonic(mnemonic);
        button.addActionListener(event -> action.run());
        panel.add(button);
    }

    private void load() {
        final String search = keyword.getText().trim();
        new SwingWorker<List<LoaiSanPham>, Void>() {
            @Override
            protected List<LoaiSanPham> doInBackground() throws Exception {
                return new MasterDataService().timLoai(actor, search);
            }
            @Override
            protected void done() {
                try {
                    rows.clear();
                    rows.addAll(get());
                    model.setRowCount(0);
                    for (LoaiSanPham value : rows)
                        model.addRow(new Object[] {
                                value.getMaLoai(), value.getTenLoai(), value.getMoTa(), value.getTrangThai()});
                } catch (Exception exception) {
                    error("Không tải được loại sản phẩm.", exception);
                }
            }
        }.execute();
    }

    private LoaiSanPham selected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Hãy chọn một loại sản phẩm.");
            return null;
        }
        return rows.get(table.convertRowIndexToModel(row));
    }

    private void detail() {
        LoaiSanPham value = selected();
        if (value != null)
            JOptionPane.showMessageDialog(this,
                    "Mã: " + value.getMaLoai() + "\nTên: " + value.getTenLoai() + "\nMô tả: " + value.getMoTa()
                            + "\nTrạng thái: " + value.getTrangThai(),
                    "Chi tiết loại", JOptionPane.INFORMATION_MESSAGE);
    }

    private void edit(LoaiSanPham existing) {
        JTextField id = new JTextField(existing == null ? "" : existing.getMaLoai());
        JTextField name = new JTextField(existing == null ? "" : existing.getTenLoai());
        JTextField note = new JTextField(existing == null ? "" : existing.getMoTa());
        id.setEditable(existing == null);
        JPanel form = new JPanel(new GridLayout(0, 1));
        addField(form, "Mã", id);
        addField(form, "Tên", name);
        addField(form, "Mô tả", note);
        if (!DialogActions.confirm(this, form, existing == null ? "Thêm loại" : "Sửa loại"))
            return;
        final LoaiSanPham value = new LoaiSanPham();
        value.setMaLoai(id.getText().trim());
        value.setTenLoai(name.getText().trim());
        value.setMoTa(note.getText().trim());
        value.setTrangThai(existing == null ? "DANG_KINH_DOANH" : existing.getTrangThai());
        save(value);
    }

    private void editSelected() {
        LoaiSanPham value = selected();
        if (value != null)
            edit(value);
    }

    private void toggle() {
        final LoaiSanPham value = selected();
        if (value == null)
            return;
        final String next = "DANG_KINH_DOANH".equals(value.getTrangThai()) ? "NGUNG_KINH_DOANH" : "DANG_KINH_DOANH";
        if (JOptionPane.showConfirmDialog(this,
                    "DANG_KINH_DOANH".equals(next) ? "Kích hoạt lại loại này?" : "Ngừng kinh doanh loại này?",
                    "Xác nhận", JOptionPane.YES_NO_OPTION)
                != JOptionPane.YES_OPTION)
            return;
        value.setTrangThai(next);
        save(value);
    }

    private void save(final LoaiSanPham value) {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                new MasterDataService().luuLoai(actor, value);
                return null;
            }
            @Override
            protected void done() {
                try {
                    get();
                    load();
                } catch (Exception exception) {
                    error("Không lưu được loại sản phẩm.", exception);
                }
            }
        }.execute();
    }

    private void remove() {
        final LoaiSanPham value = selected();
        if (value == null
                || JOptionPane.showConfirmDialog(
                           this, "Xóa " + value.getMaLoai() + "?", "Xác nhận", JOptionPane.YES_NO_OPTION)
                        != JOptionPane.YES_OPTION)
            return;
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                new MasterDataService().xoaLoai(actor, value.getMaLoai());
                return null;
            }
            @Override
            protected void done() {
                try {
                    get();
                    load();
                } catch (Exception exception) {
                    error("Không thể xóa loại đã được tham chiếu. Hãy ngừng kinh doanh thay vì xóa.", exception);
                }
            }
        }.execute();
    }

    private void error(String message, Exception exception) {
        Throwable cause = exception.getCause() == null ? exception : exception.getCause();
        JOptionPane.showMessageDialog(this, cause.getMessage() == null ? message : message + "\n" + cause.getMessage(),
                "Lỗi", JOptionPane.ERROR_MESSAGE);
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
