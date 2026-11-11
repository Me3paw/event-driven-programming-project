package components;

import entity.CauHinhCuaHang;
import entity.ChiTietHoaDon;
import entity.HoaDon;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ReceiptPrintable implements Printable {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final List<String> lines = new ArrayList<String>();
    private final List<Item> items = new ArrayList<Item>();

    public ReceiptPrintable(CauHinhCuaHang store, HoaDon invoice) {
        if (store == null || invoice == null)
            throw new IllegalArgumentException("Thiếu dữ liệu xuất hóa đơn");
        lines.add(value(store.getTenCuaHang()));
        lines.add(value(store.getDiaChi()));
        lines.add("Điện thoại: " + value(store.getSoDienThoai()));
        lines.add("HÓA ĐƠN BÁN HÀNG");
        lines.add("Mã hóa đơn: " + value(invoice.getMaHoaDon()));
        lines.add("Ngày lập: " + (invoice.getThoiDiemLap() == null
                ? "" : DATE_TIME.format(invoice.getThoiDiemLap())));
        if (notBlank(invoice.getMaNhanVien()))
            lines.add("Nhân viên phục vụ: " + invoice.getMaNhanVien());
        String customerId = invoice.getMaKhachHang();
        boolean walkIn = !notBlank(customerId) || "KH000".equals(customerId.trim());
        lines.add(walkIn ? "Khách hàng: Khách lẻ" : "Mã khách hàng: " + customerId);
        lines.add("------------------------------------------------------------");
        lines.add("STT | Sản phẩm | SL | Đơn giá | Giảm | Thành tiền");
        if (invoice.getChiTiet() != null) {
            int index = 1;
            for (ChiTietHoaDon detail : invoice.getChiTiet()) {
                if (detail == null) continue;
                String name = notBlank(detail.getTenSanPham())
                        ? detail.getTenSanPham() : detail.getMaSanPham();
                items.add(new Item(index++, name, detail.getMaSanPham(), detail.getSoLuong(),
                        detail.getDonGiaBan(), detail.getTienGiamDong(), detail.getThanhTien()));
                lines.add(index - 1 + ". " + name + " | " + detail.getSoLuong() + " | "
                        + money(detail.getDonGiaBan()) + " | " + money(detail.getTienGiamDong())
                        + " | " + money(detail.getThanhTien()));
            }
        }
        lines.add("------------------------------------------------------------");
        lines.add("Tạm tính: " + money(invoice.getTongGiaGoc()));
        lines.add("Giảm giá: " + money(invoice.getTienGiam()));
        lines.add("Giảm bằng điểm: " + money(invoice.getTienGiamDiem()));
        lines.add("VAT: " + money(invoice.getTienVat()));
        lines.add("TỔNG THANH TOÁN: " + money(invoice.getTongThanhToan()));
        lines.add("Phương thức thanh toán: " + payment(invoice.getPhuongThucTt()));
        if (invoice.getTienKhachDua() != null)
            lines.add("Tiền khách đưa: " + money(invoice.getTienKhachDua()));
        if (invoice.getTienThua() != null)
            lines.add("Tiền thừa: " + money(invoice.getTienThua()));
        if (invoice.getDiemThuongKiem() > 0)
            lines.add("Điểm tích lũy lần này: " + invoice.getDiemThuongKiem());
        lines.add("Cảm ơn quý khách và hẹn gặp lại!");
    }

    public List<String> lines() {
        return Collections.unmodifiableList(lines);
    }

    public void render(Graphics2D graphics, int width, int height) {
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, width, height);
        graphics.setColor(Color.BLACK);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Font regular = new Font("SansSerif", Font.PLAIN, 17);
        Font bold = regular.deriveFont(Font.BOLD);
        int y = 40;
        int lineHeight = 27;
        for (int i = 0; i < 9 && i < lines.size(); i++) {
            graphics.setFont(i == 0 || i == 3 ? bold : regular);
            drawCentered(graphics, lines.get(i), width, y);
            y += lineHeight;
        }
        y += 6;
        graphics.setFont(bold);
        drawTableHeader(graphics, width, y);
        y += lineHeight;
        graphics.setFont(regular);
        for (Item item : items) {
            List<String> wrapped = wrap(item.name, graphics.getFontMetrics(), 300);
            for (int i = 0; i < wrapped.size(); i++) {
                drawItem(graphics, width, y, item, wrapped.get(i), i == 0);
                y += lineHeight;
            }
        }
        y += 8;
        graphics.setFont(regular);
        for (int i = 11 + items.size(); i < lines.size(); i++) {
            String line = lines.get(i);
            graphics.setFont(line.startsWith("TỔNG") || line.startsWith("Cảm ơn") ? bold : regular);
            if (line.startsWith("TỔNG") || line.startsWith("Tạm tính")
                    || line.startsWith("Giảm") || line.startsWith("VAT")) {
                drawRightLabel(graphics, line, width, y);
            } else {
                drawCentered(graphics, line, width, y);
            }
            y += lineHeight;
        }
    }

    public int renderedHeight(int width) {
        int wrappedItems = 0;
        Font font = new Font("SansSerif", Font.PLAIN, 17);
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(1, 1,
                java.awt.image.BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setFont(font);
            for (Item item : items)
                wrappedItems += wrap(item.name, g.getFontMetrics(), 300).size();
        } finally {
            g.dispose();
        }
        return 40 + (9 + 1 + wrappedItems + (lines.size() - 11 - items.size())) * 27 + 45;
    }

    @Override
    public int print(Graphics graphics, PageFormat format, int page) throws PrinterException {
        return page == 0 ? PAGE_EXISTS : NO_SUCH_PAGE;
    }

    private void drawCentered(Graphics2D g, String text, int width, int y) {
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(text, Math.max(24, (width - metrics.stringWidth(text)) / 2), y);
    }

    private void drawRightLabel(Graphics2D g, String text, int width, int y) {
        g.drawString(text, Math.max(24, width - g.getFontMetrics().stringWidth(text) - 48), y);
    }

    private void drawTableHeader(Graphics2D g, int width, int y) {
        g.drawString("STT", 30, y);
        g.drawString("SẢN PHẨM", 90, y);
        g.drawString("SL", 445, y);
        g.drawString("ĐƠN GIÁ", 510, y);
        g.drawString("GIẢM", 680, y);
        g.drawString("THÀNH TIỀN", 760, y);
        g.drawLine(24, y + 8, width - 24, y + 8);
    }

    private void drawItem(Graphics2D g, int width, int y, Item item, String name, boolean first) {
        g.drawString(first ? item.index + "." : "", 30, y);
        g.drawString(name, 90, y);
        if (first) {
            drawRight(g, String.valueOf(item.quantity), 485, y);
            drawRight(g, money(item.unitPrice), 650, y);
            drawRight(g, money(item.discount), 750, y);
            drawRight(g, money(item.total), width - 30, y);
        }
    }

    private void drawRight(Graphics2D g, String text, int x, int y) {
        g.drawString(text, x - g.getFontMetrics().stringWidth(text), y);
    }

    private List<String> wrap(String value, FontMetrics metrics, int maxWidth) {
        if (value == null || value.length() == 0)
            return Collections.singletonList("");
        List<String> result = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        for (String word : value.split(" ", -1)) {
            String candidate = current.length() == 0 ? word : current + " " + word;
            if (current.length() > 0 && metrics.stringWidth(candidate) > maxWidth) {
                result.add(current.toString());
                current.setLength(0);
            }
            current.append(word);
        }
        if (current.length() > 0) result.add(current.toString());
        return result;
    }

    private static String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String money(BigDecimal value) {
        return value == null ? "0 đ" : UiFormat.money(value) + " đ";
    }

    private static String payment(String value) {
        if ("TIEN_MAT".equals(value)) return "Tiền mặt";
        if ("THE".equals(value)) return "Thẻ";
        if ("QR".equals(value)) return "QR";
        return value(value);
    }

    private static boolean notBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static final class Item {
        private final int index;
        private final String name;
        private final int quantity;
        private final BigDecimal unitPrice;
        private final BigDecimal discount;
        private final BigDecimal total;

        private Item(int index, String name, String code, int quantity, BigDecimal unitPrice,
                BigDecimal discount, BigDecimal total) {
            this.index = index;
            this.name = name + (notBlank(code) && !code.equals(name) ? " (" + code + ")" : "");
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.discount = discount;
            this.total = total;
        }
    }
}
