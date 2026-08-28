package entity;

import java.math.BigDecimal;

public class SanPham {
  private String maSanPham;
  private String maLoai;
  private String tenSanPham;
  private String donViTinh;
  private BigDecimal giaBan;
  private int nguongTon;
  private String trangThai;

  public String getMaSanPham() {
    return maSanPham;
  }
  public void setMaSanPham(String maSanPham) {
    this.maSanPham = maSanPham;
  }
  public String getMaLoai() {
    return maLoai;
  }
  public void setMaLoai(String maLoai) {
    this.maLoai = maLoai;
  }
  public String getTenSanPham() {
    return tenSanPham;
  }
  public void setTenSanPham(String tenSanPham) {
    this.tenSanPham = tenSanPham;
  }
  public String getDonViTinh() {
    return donViTinh;
  }
  public void setDonViTinh(String donViTinh) {
    this.donViTinh = donViTinh;
  }
  public BigDecimal getGiaBan() {
    return giaBan;
  }
  public void setGiaBan(BigDecimal giaBan) {
    this.giaBan = giaBan;
  }
  public int getNguongTon() {
    return nguongTon;
  }
  public void setNguongTon(int nguongTon) {
    this.nguongTon = nguongTon;
  }
  public String getTrangThai() {
    return trangThai;
  }
  public void setTrangThai(String trangThai) {
    this.trangThai = trangThai;
  }
}
