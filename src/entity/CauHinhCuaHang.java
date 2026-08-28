package entity;

import java.math.BigDecimal;

public class CauHinhCuaHang {
  private String tenCuaHang;
  private String diaChi;
  private String soDienThoai;
  private BigDecimal tyLeVat;

  public String getTenCuaHang() {
    return tenCuaHang;
  }
  public void setTenCuaHang(String tenCuaHang) {
    this.tenCuaHang = tenCuaHang;
  }
  public String getDiaChi() {
    return diaChi;
  }
  public void setDiaChi(String diaChi) {
    this.diaChi = diaChi;
  }
  public String getSoDienThoai() {
    return soDienThoai;
  }
  public void setSoDienThoai(String soDienThoai) {
    this.soDienThoai = soDienThoai;
  }
  public BigDecimal getTyLeVat() {
    return tyLeVat;
  }
  public void setTyLeVat(BigDecimal tyLeVat) {
    this.tyLeVat = tyLeVat;
  }
}
