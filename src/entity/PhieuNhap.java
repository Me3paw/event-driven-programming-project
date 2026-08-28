package entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PhieuNhap {
  private String maPhieuNhap;
  private String maNhaCungCap;
  private String maNhanVien;
  private LocalDateTime thoiDiemLap;
  private LocalDateTime thoiDiemHoanTat;
  private String trangThai;
  private BigDecimal tongTien;
  private List<ChiTietPhieuNhap> chiTiet = new ArrayList<ChiTietPhieuNhap>();

  public String getMaPhieuNhap() {
    return maPhieuNhap;
  }
  public void setMaPhieuNhap(String maPhieuNhap) {
    this.maPhieuNhap = maPhieuNhap;
  }
  public String getMaNhaCungCap() {
    return maNhaCungCap;
  }
  public void setMaNhaCungCap(String maNhaCungCap) {
    this.maNhaCungCap = maNhaCungCap;
  }
  public String getMaNhanVien() {
    return maNhanVien;
  }
  public void setMaNhanVien(String maNhanVien) {
    this.maNhanVien = maNhanVien;
  }
  public LocalDateTime getThoiDiemLap() {
    return thoiDiemLap;
  }
  public void setThoiDiemLap(LocalDateTime thoiDiemLap) {
    this.thoiDiemLap = thoiDiemLap;
  }
  public LocalDateTime getThoiDiemHoanTat() {
    return thoiDiemHoanTat;
  }
  public void setThoiDiemHoanTat(LocalDateTime thoiDiemHoanTat) {
    this.thoiDiemHoanTat = thoiDiemHoanTat;
  }
  public String getTrangThai() {
    return trangThai;
  }
  public void setTrangThai(String trangThai) {
    this.trangThai = trangThai;
  }
  public BigDecimal getTongTien() {
    return tongTien;
  }
  public void setTongTien(BigDecimal tongTien) {
    this.tongTien = tongTien;
  }
  public List<ChiTietPhieuNhap> getChiTiet() {
    return chiTiet;
  }
  public void setChiTiet(List<ChiTietPhieuNhap> chiTiet) {
    this.chiTiet = chiTiet;
  }
}
