package service;
import java.math.BigDecimal;
public final class DoanhSoSanPham {
  private final String maSanPham, tenSanPham;
  private final long soLuong;
  private final BigDecimal doanhThu;
  public DoanhSoSanPham(String maSanPham, String tenSanPham, long soLuong, BigDecimal doanhThu) {
    this.maSanPham = maSanPham;
    this.tenSanPham = tenSanPham;
    this.soLuong = soLuong;
    this.doanhThu = doanhThu;
  }
  public String getMaSanPham() {
    return maSanPham;
  }
  public String getTenSanPham() {
    return tenSanPham;
  }
  public long getSoLuong() {
    return soLuong;
  }
  public BigDecimal getDoanhThu() {
    return doanhThu;
  }
}
