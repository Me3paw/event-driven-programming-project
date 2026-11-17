package service;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

final class CheckoutFingerprint {
  private CheckoutFingerprint() {}
  static String of(Actor actor, YeuCauThanhToan request) {
    if (request.getDongHang() == null || request.getDongHang().isEmpty()
        || request.getDongHang().contains(null))
      throw new IllegalArgumentException("Hóa đơn phải có dòng hàng hợp lệ");
    try {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      DataOutputStream data = new DataOutputStream(bytes);
      field(data, actor.getTenDangNhap());
      field(data, actor.getMaNhanVien());
      field(data, SaleService.KHACH_LE.equals(request.getMaKhachHang()) ? null : request.getMaKhachHang());
      field(data, request.getLoaiGiamGia() == null ? "KHONG" : request.getLoaiGiamGia());
      field(data, request.getMaKhuyenMai());
      data.writeLong(request.getDiemThuongSuDung());
      field(data, request.getPhuongThucThanhToan());
      field(data, request.getTienKhachDua() == null ? null
          : request.getTienKhachDua().stripTrailingZeros().toPlainString());
      data.writeBoolean(request.isThanhToanDienTuThanhCong());
      field(data, request.getMaThamChieuThanhToan());
      List<DongThanhToan> items = new ArrayList<DongThanhToan>(request.getDongHang());
      items.sort(Comparator.comparing(DongThanhToan::getMaSanPham,
          Comparator.nullsFirst(Comparator.naturalOrder())));
      data.writeInt(items.size());
      for (DongThanhToan item : items) { field(data, item.getMaSanPham()); data.writeInt(item.getSoLuong()); }
      data.flush();
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
    } catch (java.io.IOException | java.security.NoSuchAlgorithmException exception) {
      throw new IllegalStateException("Không thể xác nhận nội dung thanh toán", exception);
    }
  }
  private static void field(DataOutputStream data, String value) throws java.io.IOException {
    if (value == null) { data.writeInt(-1); return; }
    byte[] encoded = value.getBytes(StandardCharsets.UTF_8);
    data.writeInt(encoded.length);
    data.write(encoded);
  }
}
