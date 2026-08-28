package components;

import entity.CauHinhCuaHang;
import entity.ChiTietHoaDon;
import entity.HoaDon;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ReceiptPrintable implements Printable {
    private final List<String> lines;

    public ReceiptPrintable(CauHinhCuaHang store, HoaDon invoice) {
        lines = new ArrayList<String>();
        lines.add(store.getTenCuaHang());
        lines.add(store.getDiaChi());
        lines.add("ĐT: " + store.getSoDienThoai());
        lines.add("HÓA ĐƠN " + invoice.getMaHoaDon());
        lines.add("NV: " + invoice.getMaNhanVien() + "  " + invoice.getThoiDiemLap());
        if (invoice.getMaKhachHang() != null) lines.add("Khách: " + invoice.getMaKhachHang());
        lines.add("--------------------------------");
        for (ChiTietHoaDon line : invoice.getChiTiet()) {
            lines.add(line.getMaSanPham() + " x" + line.getSoLuong());
            lines.add("  " + line.getDonGiaBan() + "  -" + line.getTienGiamDong() + "  = " + line.getThanhTien());
        }
        lines.add("Tạm tính: " + invoice.getTongGiaGoc());
        lines.add("Giảm giá: " + invoice.getTienGiam());
        lines.add("Điểm thưởng: " + invoice.getTienGiamDiem());
        lines.add("VAT: " + invoice.getTienVat());
        lines.add("TỔNG: " + invoice.getTongThanhToan());
        lines.add("Thanh toán: " + invoice.getPhuongThucTt());
        if (invoice.getMaThamChieuTt() != null) lines.add("Tham chiếu: " + invoice.getMaThamChieuTt());
        if (invoice.getTienThua() != null) lines.add("Tiền thừa: " + invoice.getTienThua());
    }

    public List<String> lines() {
        return Collections.unmodifiableList(lines);
    }

    @Override
    public int print(Graphics graphics, PageFormat format, int page) throws PrinterException {
        if (page > 0) return NO_SUCH_PAGE;
        Graphics2D g = (Graphics2D) graphics.create();
        g.translate(format.getImageableX(), format.getImageableY());
        g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));
        for (int i = 0; i < lines.size(); i++) g.drawString(lines.get(i), 0, 16 + i * 14);
        g.dispose();
        return PAGE_EXISTS;
    }
}
