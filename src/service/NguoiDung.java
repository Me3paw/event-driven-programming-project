package service;

import entity.TaiKhoan;
import entity.VaiTro;

public final class NguoiDung {

  private long accountVersion;
  private long employeeVersion;
  public long getPhienBanTaiKhoan() { return accountVersion; }
  public long getPhienBanNhanVien() { return employeeVersion; }
  private volatile boolean invalid;
  public boolean isInvalid() { return invalid; }
  private Runnable invalidationHandler;
  public void onInvalidated(Runnable handler) { invalidationHandler = handler; }
  void invalidate() {
    if (invalid) return;
    invalid = true;
    Runnable handler = invalidationHandler;
    if (handler != null) handler.run();
  }
  private final String tenDangNhap;
  private final String maNhanVien;
  private final String vaiTro;
  private final String tenNhanVien;
  private final String chucVuNhanVien;

  NguoiDung(TaiKhoan taiKhoan) {
    if (taiKhoan == null || rong(taiKhoan.getTenDangNhap())
        || rong(taiKhoan.getMaNhanVien()) || !vaiTroHopLe(taiKhoan.getVaiTro()))
      throw new IllegalArgumentException("Thông tin phiên đăng nhập không hợp lệ");
    accountVersion = taiKhoan.getPhienBanTaiKhoan();
    employeeVersion = taiKhoan.getPhienBanNhanVien();
    tenDangNhap = taiKhoan.getTenDangNhap();
    maNhanVien = taiKhoan.getMaNhanVien();
    vaiTro = taiKhoan.getVaiTro();
    tenNhanVien = taiKhoan.getTenNhanVien();
    chucVuNhanVien = taiKhoan.getChucVuNhanVien();
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
  public String getTenNhanVien() {
    return tenNhanVien;
  }
  public String getChucVuNhanVien() {
    return chucVuNhanVien;
  }
  public boolean isQuanLy() {
    return VaiTro.QUAN_LY.name().equals(vaiTro);
  }

  private static boolean rong(String value) {
    return value == null || value.trim().isEmpty();
  }

  private static boolean vaiTroHopLe(String value) {
    try {
      VaiTro.valueOf(value);
      return true;
    } catch (RuntimeException exception) {
      return false;
    }
  }
}
