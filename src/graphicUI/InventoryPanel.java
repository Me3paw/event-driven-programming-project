package graphicUI;

import components.UiTheme;
import components.DialogActions;
import entity.DieuChinhTon;
import entity.LoTonKho;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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
import service.InventoryService;
import service.LowStockProduct;
import service.ReportService;

@SuppressWarnings("serial")
public final class InventoryPanel extends JPanel {
    private final Actor actor;
    private final boolean history;
    private final JTextField key = new JTextField(16);
    private final JCheckBox lowOnly = new JCheckBox("Chỉ tồn thấp");
    private final DefaultTableModel stockModel = new DefaultTableModel(
            new String[] {"Lô", "Phiếu nhập", "Sản phẩm", "Nhập", "Còn", "Giá vốn", "Thời điểm"}, 0) {
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final DefaultTableModel adjustmentModel =
            new DefaultTableModel(new String[] {"Mã", "Thời điểm", "Quản lý", "Lý do", "Số lô"}, 0) {
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
    private final JTable stockTable = new JTable(stockModel);
    private final JTable adjustmentTable = new JTable(adjustmentModel);

    public InventoryPanel(Actor actor) {
        this(actor, false);
    }

    public InventoryPanel(Actor actor, boolean history) {
        super(new BorderLayout(0, 8));
        this.actor = actor;
        this.history = history;
        setBackground(UiTheme.BACKGROUND);
        if (history)
            adjustmentPage();
        else
            stockPage();
    }

    private void stockPage() {
        JPanel bar = bar("Sản phẩm / từ khóa");
        if (actor.isQuanLy()) {
            lowOnly.setOpaque(false);
            lowOnly.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    load();
                }
            });
            bar.add(lowOnly);
            JButton adjust = new JButton("Điều chỉnh giảm FIFO");
            adjust.setMnemonic(KeyEvent.VK_D);
            adjust.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    adjust();
                }
            });
            bar.add(adjust);
        }
        add(bar, BorderLayout.NORTH);
        add(new JScrollPane(stockTable), BorderLayout.CENTER);
        load();
    }

    private void adjustmentPage() {
        JPanel bar = bar("Mã điều chỉnh / lý do");
        JButton detail = new JButton("Chi tiết");
        detail.setMnemonic(KeyEvent.VK_C);
        detail.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                detail();
            }
        });
        bar.add(detail);
        JButton adjust = new JButton("Điều chỉnh giảm FIFO");
        adjust.setMnemonic(KeyEvent.VK_D);
        adjust.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                adjust();
            }
        });
        bar.add(adjust);
        add(bar, BorderLayout.NORTH);
        add(new JScrollPane(adjustmentTable), BorderLayout.CENTER);
        load();
    }

    private JPanel bar(String label) {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bar.setBackground(Color.WHITE);
        bar.setBorder(UiTheme.CARD);
        addField(bar, label, key);
        JButton find = new JButton("Tìm");
        find.setMnemonic(KeyEvent.VK_T);
        find.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                load();
            }
        });
        bar.add(find);
        return bar;
    }

    private void load() {
        if (history)
            loadHistory();
        else
            loadStock();
    }

    private void loadStock() {
        final String keyword = key.getText().trim();
        final boolean onlyLow = actor.isQuanLy() && lowOnly.isSelected();
        new SwingWorker<StockData, Void>() {
            protected StockData doInBackground() throws Exception {
                List<LoTonKho> lots = new InventoryService().tonKho(actor, keyword);
                Set<String> low = new HashSet<String>();
                if (onlyLow)
                    for (LowStockProduct product : new ReportService().lowStock(actor)) low.add(product.getProductId());
                return new StockData(lots, low);
            }
            protected void done() {
                try {
                    StockData data = get();
                    stockModel.setRowCount(0);
                    for (LoTonKho lot : data.lots)
                        if (!onlyLow || data.lowProducts.contains(lot.getMaSanPham()))
                            stockModel.addRow(new Object[] {lot.getMaLo(), lot.getMaPhieuNhap(), lot.getMaSanPham(),
                                    lot.getSoLuongNhap(), lot.getSoLuongCon(), lot.getDonGiaVon(),
                                    lot.getThoiDiemNhap()});
                } catch (Exception exception) {
                    message("Không tải được tồn kho.");
                }
            }
        }.execute();
    }

    private void loadHistory() {
        final String keyword = key.getText().trim();
        new SwingWorker<List<DieuChinhTon>, Void>() {
            protected List<DieuChinhTon> doInBackground() throws Exception {
                return new InventoryService().timDieuChinh(actor, keyword);
            }
            protected void done() {
                try {
                    adjustmentModel.setRowCount(0);
                    for (DieuChinhTon value : get())
                        adjustmentModel.addRow(
                                new Object[] {value.getMaDieuChinh(), value.getThoiDiemLap(), value.getMaNhanVien(),
                                        value.getLyDo(), Integer.valueOf(value.getSoLuongGiamTheoLo().size())});
                } catch (Exception exception) {
                    message("Không tải được lịch sử điều chỉnh.");
                }
            }
        }.execute();
    }

    private void adjust() {
        JTextField id = new JTextField("DC" + System.currentTimeMillis());
        JTextField product = new JTextField();
        JTextField quantity = new JTextField();
        JTextField reason = new JTextField();
        JPanel form = new JPanel(new GridLayout(0, 1));
        addField(form, "Mã điều chỉnh", id);
        addField(form, "Mã sản phẩm", product);
        addField(form, "Số lượng giảm", quantity);
        addField(form, "Lý do", reason);
        if (!DialogActions.confirm(this, form, "Điều chỉnh giảm FIFO"))
            return;
        final String adjustmentId = id.getText().trim();
        final String productId = product.getText().trim();
        final String adjustmentReason = reason.getText().trim();
        final int amount;
        try {
            amount = Integer.parseInt(quantity.getText().trim());
        } catch (NumberFormatException exception) {
            message("Số lượng giảm phải là số nguyên dương.");
            return;
        }
        if (adjustmentId.length() == 0 || productId.length() == 0 || adjustmentReason.length() == 0 || amount <= 0) {
            message("Mã điều chỉnh, sản phẩm, số lượng dương và lý do là bắt buộc.");
            return;
        }
        new SwingWorker<Void, Void>() {
            protected Void doInBackground() throws Exception {
                new InventoryService().giamTonFifo(actor, adjustmentId, productId, amount, adjustmentReason);
                return null;
            }
            protected void done() {
                try {
                    get();
                    load();
                } catch (Exception exception) {
                    message("Không điều chỉnh được tồn kho.");
                }
            }
        }.execute();
    }

    private void detail() {
        int row = adjustmentTable.getSelectedRow();
        if (row < 0)
            return;
        final String adjustmentId = String.valueOf(adjustmentModel.getValueAt(row, 0));
        new SwingWorker<DieuChinhTon, Void>() {
            protected DieuChinhTon doInBackground() throws Exception {
                return new InventoryService().chiTietDieuChinh(actor, adjustmentId);
            }
            protected void done() {
                try {
                    DieuChinhTon value = get();
                    DefaultTableModel details = new DefaultTableModel(new String[] {"Lô", "Số lượng giảm"}, 0) {
                        public boolean isCellEditable(int row, int column) {
                            return false;
                        }
                    };
                    for (Map.Entry<Long, Integer> line : value.getSoLuongGiamTheoLo().entrySet())
                        details.addRow(new Object[] {line.getKey(), line.getValue()});
                    JPanel form = new JPanel(new BorderLayout(0, 6));
                    form.add(new JLabel("Lý do: " + value.getLyDo() + "    Người lập: " + value.getMaNhanVien()),
                            BorderLayout.NORTH);
                    form.add(new JScrollPane(new JTable(details)), BorderLayout.CENTER);
                    JOptionPane.showMessageDialog(
                            InventoryPanel.this, form, "Chi tiết " + adjustmentId, JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception exception) {
                    message("Không tải được chi tiết điều chỉnh.");
                }
            }
        }.execute();
    }

    private void message(String text) {
        JOptionPane.showMessageDialog(this, text);
    }

    private void addField(JPanel panel, String text, JComponent field) {
        JLabel label = new JLabel(text);
        label.setLabelFor(field);
        label.setDisplayedMnemonic(text.charAt(0));
        field.getAccessibleContext().setAccessibleName(text);
        panel.add(label);
        panel.add(field);
    }

    private static final class StockData {
        private final List<LoTonKho> lots;
        private final Set<String> lowProducts;

        private StockData(List<LoTonKho> lots, Set<String> lowProducts) {
            this.lots = lots;
            this.lowProducts = lowProducts;
        }
    }
}
