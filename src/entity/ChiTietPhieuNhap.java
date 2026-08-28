package entity;

import java.math.BigDecimal;

public class ChiTietPhieuNhap {
  private String maSanPham;
  private int soLuong;
  private BigDecimal donGiaNhap;

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
  public BigDecimal getDonGiaNhap() {
    return donGiaNhap;
  }
  public void setDonGiaNhap(BigDecimal donGiaNhap) {
    this.donGiaNhap = donGiaNhap;
  }
}
