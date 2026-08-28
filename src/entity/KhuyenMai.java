package entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

public class KhuyenMai {
  private String maKhuyenMai;
  private String tenKhuyenMai;
  private String phamVi;
  private BigDecimal tyLeGiam;
  private LocalDateTime batDau;
  private LocalDateTime ketThuc;
  private String trangThai;
  private Set<String> maSanPham = new LinkedHashSet<String>();

  public String getMaKhuyenMai() {
    return maKhuyenMai;
  }
  public void setMaKhuyenMai(String maKhuyenMai) {
    this.maKhuyenMai = maKhuyenMai;
  }
  public String getTenKhuyenMai() {
    return tenKhuyenMai;
  }
  public void setTenKhuyenMai(String tenKhuyenMai) {
    this.tenKhuyenMai = tenKhuyenMai;
  }
  public String getPhamVi() {
    return phamVi;
  }
  public void setPhamVi(String phamVi) {
    this.phamVi = phamVi;
  }
  public BigDecimal getTyLeGiam() {
    return tyLeGiam;
  }
  public void setTyLeGiam(BigDecimal tyLeGiam) {
    this.tyLeGiam = tyLeGiam;
  }
  public LocalDateTime getBatDau() {
    return batDau;
  }
  public void setBatDau(LocalDateTime batDau) {
    this.batDau = batDau;
  }
  public LocalDateTime getKetThuc() {
    return ketThuc;
  }
  public void setKetThuc(LocalDateTime ketThuc) {
    this.ketThuc = ketThuc;
  }
  public String getTrangThai() {
    return trangThai;
  }
  public void setTrangThai(String trangThai) {
    this.trangThai = trangThai;
  }
  public Set<String> getMaSanPham() {
    return maSanPham;
  }
  public void setMaSanPham(Set<String> maSanPham) {
    this.maSanPham = maSanPham;
  }
}
