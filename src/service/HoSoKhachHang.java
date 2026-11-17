package service;
import entity.HoaDon;
import entity.KhachHang;
import java.math.BigDecimal;
import java.util.List;
public final class HoSoKhachHang {
  private final KhachHang khachHang;
  private final BigDecimal actualSpend;
  private final String hangThanhVien;
  private final List<HoaDon> history;
  public HoSoKhachHang(
      KhachHang khachHang, BigDecimal actualSpend, String hangThanhVien, List<HoaDon> history) {
    this.khachHang = khachHang;
    this.actualSpend = actualSpend;
    this.hangThanhVien = hangThanhVien;
    this.history = history;
  }
  public KhachHang getKhachHang() {
    return khachHang;
  }
  public BigDecimal getTongChiTieu() {
    return actualSpend;
  }
  public String getHang() {
    return hangThanhVien;
  }
  public List<HoaDon> getLichSu() {
    return history;
  }
}
