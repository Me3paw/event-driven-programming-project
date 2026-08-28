package entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class LoTonKho {
  private long maLo;
  private String maPhieuNhap;
  private String maSanPham;
  private int soLuongNhap;
  private int soLuongCon;
  private BigDecimal donGiaVon;
  private LocalDateTime thoiDiemNhap;

  public long getMaLo() {
    return maLo;
  }
  public void setMaLo(long maLo) {
    this.maLo = maLo;
  }
  public String getMaPhieuNhap() {
    return maPhieuNhap;
  }
  public void setMaPhieuNhap(String maPhieuNhap) {
    this.maPhieuNhap = maPhieuNhap;
  }
  public String getMaSanPham() {
    return maSanPham;
  }
  public void setMaSanPham(String maSanPham) {
    this.maSanPham = maSanPham;
  }
  public int getSoLuongNhap() {
    return soLuongNhap;
  }
  public void setSoLuongNhap(int soLuongNhap) {
    this.soLuongNhap = soLuongNhap;
  }
  public int getSoLuongCon() {
    return soLuongCon;
  }
  public void setSoLuongCon(int soLuongCon) {
    this.soLuongCon = soLuongCon;
  }
  public BigDecimal getDonGiaVon() {
    return donGiaVon;
  }
  public void setDonGiaVon(BigDecimal donGiaVon) {
    this.donGiaVon = donGiaVon;
  }
  public LocalDateTime getThoiDiemNhap() {
    return thoiDiemNhap;
  }
  public void setThoiDiemNhap(LocalDateTime thoiDiemNhap) {
    this.thoiDiemNhap = thoiDiemNhap;
  }
}
