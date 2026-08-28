package service;

import entity.TaiKhoan;
import entity.VaiTro;

public final class Actor {
  private final String tenDangNhap;
  private final String maNhanVien;
  private final String vaiTro;

  Actor(TaiKhoan taiKhoan) {
    tenDangNhap = taiKhoan.getTenDangNhap();
    maNhanVien = taiKhoan.getMaNhanVien();
    vaiTro = taiKhoan.getVaiTro();
  }

  public String getTenDangNhap() {
    return tenDangNhap;
  }
  public String getMaNhanVien() {
    return maNhanVien;
  }
  public String getVaiTro() {
    return vaiTro;
  }
  public boolean isQuanLy() {
    return VaiTro.QUAN_LY.name().equals(vaiTro);
  }
}
