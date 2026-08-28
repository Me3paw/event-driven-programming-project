package entity;

import java.math.BigDecimal;

public class PhanBoXuatLo {
  private String maSanPham;
  private long maLo;
  private int soLuongXuat;
  private BigDecimal donGiaVon;

  public String getMaSanPham() {
    return maSanPham;
  }
  public void setMaSanPham(String maSanPham) {
    this.maSanPham = maSanPham;
  }
  public long getMaLo() {
    return maLo;
  }
  public void setMaLo(long maLo) {
    this.maLo = maLo;
  }
  public int getSoLuongXuat() {
    return soLuongXuat;
  }
  public void setSoLuongXuat(int soLuongXuat) {
    this.soLuongXuat = soLuongXuat;
  }
  public BigDecimal getDonGiaVon() {
    return donGiaVon;
  }
  public void setDonGiaVon(BigDecimal donGiaVon) {
    this.donGiaVon = donGiaVon;
  }
}
