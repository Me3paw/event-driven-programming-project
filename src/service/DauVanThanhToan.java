package service;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

final class DauVanThanhToan {
  private DauVanThanhToan() {}
  static String of(NguoiDung nguoiDung, YeuCauThanhToan yeuCau) {
    if (yeuCau.getDongHang() == null || yeuCau.getDongHang().isEmpty()
        || yeuCau.getDongHang().contains(null))
      throw new IllegalArgumentException("Hóa đơn phải có dòng hàng hợp lệ");
    try {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      DataOutputStream data = new DataOutputStream(bytes);
      field(data, nguoiDung.getTenDangNhap());
      field(data, nguoiDung.getMaNhanVien());
      field(data, DichVuBanHang.KHACH_LE.equals(yeuCau.getMaKhachHang()) ? null : yeuCau.getMaKhachHang());
      field(data, yeuCau.getLoaiGiamGia() == null ? "KHONG" : yeuCau.getLoaiGiamGia());
      field(data, yeuCau.getMaKhuyenMai());
      data.writeLong(yeuCau.getDiemThuongSuDung());
      field(data, yeuCau.getPhuongThucThanhToan());
      field(data, yeuCau.getTienKhachDua() == null ? null
          : yeuCau.getTienKhachDua().stripTrailingZeros().toPlainString());
      data.writeBoolean(yeuCau.isThanhToanDienTuThanhCong());
      field(data, yeuCau.getMaThamChieuThanhToan());
      List<DongThanhToan> danhSachDongHang = new ArrayList<DongThanhToan>(yeuCau.getDongHang());
      danhSachDongHang.sort(Comparator.comparing(DongThanhToan::getMaSanPham,
          Comparator.nullsFirst(Comparator.naturalOrder())));
      data.writeInt(danhSachDongHang.size());
      for (DongThanhToan dongHang : danhSachDongHang) { field(data, dongHang.getMaSanPham()); data.writeInt(dongHang.getSoLuong()); }
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
