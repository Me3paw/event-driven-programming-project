package entity;

import java.math.BigDecimal;

public class ChiTietHoaDon {
  private String maSanPham;
  private int soLuong;
  private BigDecimal donGiaBan;
  private BigDecimal tienGiamDong;
  private BigDecimal thanhTien;

  public String getMaSanPham() {
    return maSanPham;
  }
  public void setMaSanPham(String maSanPham) {
    this.maSanPham = maSanPham;
  }
  public int getSoLuong() {
    return soLuong;
  }
  public void setSoLuong(int soLuong) {
    this.soLuong = soLuong;
  }
  public BigDecimal getDonGiaBan() {
    return donGiaBan;
  }
  public void setDonGiaBan(BigDecimal donGiaBan) {
    this.donGiaBan = donGiaBan;
  }
  public BigDecimal getTienGiamDong() {
    return tienGiamDong;
  }
  public void setTienGiamDong(BigDecimal tienGiamDong) {
    this.tienGiamDong = tienGiamDong;
  }
  public BigDecimal getThanhTien() {
    return thanhTien;
  }
  public void setThanhTien(BigDecimal thanhTien) {
    this.thanhTien = thanhTien;
  }
}
