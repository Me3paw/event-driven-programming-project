package service;
public final class SanPhamSapHetHang {
  private final String maSanPham, tenSanPham;
  private final int tonKho, nguong;
  public SanPhamSapHetHang(String maSanPham, String tenSanPham, int tonKho, int nguong) {
    this.maSanPham = maSanPham;
    this.tenSanPham = tenSanPham;
    this.tonKho = tonKho;
    this.nguong = nguong;
  }
  public String getMaSanPham() {
    return maSanPham;
  }
  public String getTenSanPham() {
    return tenSanPham;
  }
  public int getTonKho() {
    return tonKho;
  }
  public int getNguong() {
    return nguong;
  }
}
