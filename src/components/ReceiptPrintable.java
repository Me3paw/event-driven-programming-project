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
import java.awt.image.BufferedImage;
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
    private final String invoiceId;

    public ReceiptPrintable(CauHinhCuaHang store, HoaDon invoice) {
        if (store == null || invoice == null)
            throw new IllegalArgumentException("Thiếu dữ liệu xuất hóa đơn");
        invoiceId = value(invoice.getMaHoaDon());
        lines.add(value(store.getTenCuaHang()));
        lines.add(value(store.getDiaChi()));
        lines.add("Điện thoại: " + value(store.getSoDienThoai()));
        lines.add("HÓA ĐƠN BÁN HÀNG");
        lines.add("Mã hóa đơn: " + invoiceId);
        lines.add("Ngày lập: " + (invoice.getThoiDiemLap() == null ? "" : DATE_TIME.format(invoice.getThoiDiemLap())));
        if (notBlank(invoice.getMaNhanVien())) lines.add("Nhân viên phục vụ: " + invoice.getMaNhanVien());
        String customerId = invoice.getMaKhachHang();
        boolean walkIn = !notBlank(customerId) || "KH000".equals(customerId.trim());
        lines.add(walkIn ? "Khách hàng: Khách lẻ" : "Mã khách hàng: " + customerId);
        lines.add("SẢN PHẨM / SỐ LƯỢNG / ĐƠN GIÁ / GIẢM / THÀNH TIỀN");
        int index = 1;
        if (invoice.getChiTiet() != null) for (ChiTietHoaDon detail : invoice.getChiTiet()) {
            if (detail == null) continue;
            String name = notBlank(detail.getTenSanPham()) ? detail.getTenSanPham() : detail.getMaSanPham();
            lines.add(index++ + ". " + name + " (" + detail.getMaSanPham() + ")");
            lines.add("SL: " + detail.getSoLuong() + " × " + money(detail.getDonGiaBan())
                    + " | Giảm: " + money(detail.getTienGiamDong()) + " | Thành tiền: " + money(detail.getThanhTien()));
        }
        lines.add("Tạm tính: " + money(invoice.getTongGiaGoc()));
        lines.add("Giảm giá: " + money(invoice.getTienGiam()));
        lines.add("Giảm bằng điểm: " + money(invoice.getTienGiamDiem()));
        lines.add("VAT: " + money(invoice.getTienVat()));
        lines.add("TỔNG THANH TOÁN: " + money(invoice.getTongThanhToan()));
        lines.add("Phương thức thanh toán: " + payment(invoice.getPhuongThucTt()));
        if (invoice.getTienKhachDua() != null) lines.add("Tiền khách đưa: " + money(invoice.getTienKhachDua()));
        if (invoice.getTienThua() != null) lines.add("Tiền thừa: " + money(invoice.getTienThua()));
        if (notBlank(invoice.getMaThamChieuTt())) lines.add("Tham chiếu: " + invoice.getMaThamChieuTt());
        if (!walkIn) {
            lines.add("Điểm tích lũy lần này: " + invoice.getDiemTichLuyKiem());
            lines.add("Điểm thưởng nhận được: " + invoice.getDiemThuongKiem());
        }
        lines.add("Cảm ơn quý khách và hẹn gặp lại!");
    }

    public String invoiceId() { return invoiceId; }
    public List<String> lines() { return Collections.unmodifiableList(lines); }

    public void render(Graphics2D graphics, int width, int height) {
        Graphics2D copy = (Graphics2D) graphics.create();
        try {
            copy.setColor(Color.WHITE);
            copy.fillRect(0, 0, width, height);
            draw(copy, layout(copy, width - 48, 17), 24, 24, 0, Integer.MAX_VALUE, 17);
        } finally { copy.dispose(); }
    }

    public int renderedHeight(int width) {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            return 48 + layout(graphics, width - 48, 17).size() * lineHeight(graphics, 17);
        } finally { graphics.dispose(); }
    }

    @Override
    public int print(Graphics graphics, PageFormat format, int page) throws PrinterException {
        if (page < 0) return NO_SUCH_PAGE;
        Graphics2D copy = (Graphics2D) graphics.create();
        try {
            int width = (int) Math.floor(format.getImageableWidth());
            int height = (int) Math.floor(format.getImageableHeight());
            int lineHeight = lineHeight(copy, 10);
            if (width < 24 || height < lineHeight) throw new PrinterException("Vùng in quá nhỏ");
            List<ReceiptLine> wrapped = layout(copy, width - 8, 10);
            int perPage = height / lineHeight;
            long start = (long) page * perPage;
            if (start >= wrapped.size()) return NO_SUCH_PAGE;
            copy.translate(format.getImageableX(), format.getImageableY());
            copy.clipRect(0, 0, width, height);
            draw(copy, wrapped, 4, 0, (int) start, Math.min(wrapped.size(), (int) start + perPage), 10);
            return PAGE_EXISTS;
        } finally { copy.dispose(); }
    }

    private List<ReceiptLine> layout(Graphics2D graphics, int width, int size) {
        if (width < 1) throw new IllegalArgumentException("Khổ hóa đơn quá nhỏ");
        List<ReceiptLine> result = new ArrayList<ReceiptLine>();
        for (int index = 0; index < lines.size(); index++) {
            String text = lines.get(index);
            boolean bold = index == 0 || text.startsWith("HÓA ĐƠN") || text.startsWith("TỔNG");
            graphics.setFont(new Font("SansSerif", bold ? Font.BOLD : Font.PLAIN, size));
            FontMetrics metrics = graphics.getFontMetrics();
            if (text.isEmpty()) { result.add(new ReceiptLine("", bold)); continue; }
            while (!text.isEmpty()) {
                int end = text.length();
                while (end > 1 && metrics.stringWidth(text.substring(0, end)) > width) end--;
                if (end < text.length()) {
                    int space = text.lastIndexOf(' ', end);
                    if (space > 0) end = space;
                }
                result.add(new ReceiptLine(text.substring(0, end), bold));
                text = text.substring(end).stripLeading();
            }
        }
        return result;
    }

    private void draw(Graphics2D graphics, List<ReceiptLine> wrapped, int x, int top,
            int start, int end, int size) {
        int step = lineHeight(graphics, size);
        graphics.setColor(Color.BLACK);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int y = top + step - 4;
        for (int index = start; index < end && index < wrapped.size(); index++) {
            ReceiptLine line = wrapped.get(index);
            graphics.setFont(new Font("SansSerif", line.bold ? Font.BOLD : Font.PLAIN, size));
            graphics.drawString(line.text, x, y);
            y += step;
        }
    }

    private int lineHeight(Graphics2D graphics, int size) {
        graphics.setFont(new Font("SansSerif", Font.BOLD, size));
        return graphics.getFontMetrics().getHeight() + 3;
    }

    private static final class ReceiptLine {
        private final String text;
        private final boolean bold;
        private ReceiptLine(String text, boolean bold) { this.text = text; this.bold = bold; }
    }
    private static String value(Object value) { return value == null ? "" : String.valueOf(value); }
    private static String money(BigDecimal value) { return value == null ? "0 đ" : UiFormat.money(value) + " đ"; }
    private static String payment(String value) {
        if ("TIEN_MAT".equals(value)) return "Tiền mặt";
        if ("THE".equals(value)) return "Thẻ";
        if ("QR".equals(value)) return "QR";
        return value(value);
    }
    private static boolean notBlank(String value) { return value != null && !value.trim().isEmpty(); }
}
