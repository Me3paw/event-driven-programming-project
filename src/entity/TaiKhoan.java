package entity;

public class TaiKhoan {
  private long accountVersion;
  private long employeeVersion;
  public long getAccountVersion() { return accountVersion; }
  public long getEmployeeVersion() { return employeeVersion; }
  public void setAccountVersion(long value) { accountVersion = value; }
  public void setEmployeeVersion(long value) { employeeVersion = value; }

  private String tenDangNhap;
  private String matKhauBam;
  private String vaiTro;
  private String trangThai;
  private String maNhanVien;
  private String trangThaiNhanVien;
  private String tenNhanVien;
  private String chucVuNhanVien;

  public String getTenDangNhap() {
    return tenDangNhap;
  }
  public void setTenDangNhap(String tenDangNhap) {
    this.tenDangNhap = tenDangNhap;
  }
  public String getMatKhauBam() {
    return matKhauBam;
  }
  public void setMatKhauBam(String matKhauBam) {
    this.matKhauBam = matKhauBam;
  }
  public String getVaiTro() {
    return vaiTro;
  }
  public void setVaiTro(String vaiTro) {
    this.vaiTro = vaiTro;
  }
  public String getTrangThai() {
    return trangThai;
  }
  public void setTrangThai(String trangThai) {
    this.trangThai = trangThai;
  }
  public String getMaNhanVien() {
    return maNhanVien;
  }
  public void setMaNhanVien(String maNhanVien) {
    this.maNhanVien = maNhanVien;
  }
  public String getTrangThaiNhanVien() {
    return trangThaiNhanVien;
  }
  public void setTrangThaiNhanVien(String trangThaiNhanVien) {
    this.trangThaiNhanVien = trangThaiNhanVien;
  }
  public String getTenNhanVien() {
    return tenNhanVien;
  }
  public void setTenNhanVien(String tenNhanVien) {
    this.tenNhanVien = tenNhanVien;
  }
  public String getChucVuNhanVien() {
    return chucVuNhanVien;
  }
  public void setChucVuNhanVien(String chucVuNhanVien) {
    this.chucVuNhanVien = chucVuNhanVien;
  }
}
