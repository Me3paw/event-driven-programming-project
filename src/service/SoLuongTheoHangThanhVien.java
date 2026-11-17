package service;
public final class SoLuongTheoHangThanhVien {
  private final String hangThanhVien;
  private final long count;
  public SoLuongTheoHangThanhVien(String hangThanhVien, long count) {
    this.hangThanhVien = hangThanhVien;
    this.count = count;
  }
  public String getHang() {
    return hangThanhVien;
  }
  public long getSoLuong() {
    return count;
  }
}
