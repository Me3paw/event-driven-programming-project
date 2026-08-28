package service;

import entity.LoaiGiamGia;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class YeuCauThanhToan {
  private String maHoaDon;
  private String maNhanVien;
  private String maKhachHang;
  private String maKhuyenMai;
  private String loaiGiamGia = LoaiGiamGia.KHONG.name();
  private long diemThuongSuDung;
  private String phuongThucThanhToan;
  private BigDecimal tienKhachDua;
  private boolean thanhToanDienTuThanhCong;
  private String maThamChieuThanhToan;
  private List<DongThanhToan> dongHang = new ArrayList<DongThanhToan>();
  public String getMaHoaDon() {
    return maHoaDon;
  }
  public void setMaHoaDon(String maHoaDon) {
    this.maHoaDon = maHoaDon;
  }
  public String getMaNhanVien() {
    return maNhanVien;
  }
  public void setMaNhanVien(String maNhanVien) {
    this.maNhanVien = maNhanVien;
  }
  public String getMaKhachHang() {
    return maKhachHang;
  }
  public void setMaKhachHang(String maKhachHang) {
    this.maKhachHang = maKhachHang;
  }
  public String getMaKhuyenMai() {
    return maKhuyenMai;
  }
  public void setMaKhuyenMai(String maKhuyenMai) {
    this.maKhuyenMai = maKhuyenMai;
  }
  public String getLoaiGiamGia() {
    return loaiGiamGia;
  }
  public void setLoaiGiamGia(String loaiGiamGia) {
    this.loaiGiamGia = loaiGiamGia;
  }
  public long getDiemThuongSuDung() {
    return diemThuongSuDung;
  }
  public void setDiemThuongSuDung(long diemThuongSuDung) {
    this.diemThuongSuDung = diemThuongSuDung;
  }
  public String getPhuongThucThanhToan() {
    return phuongThucThanhToan;
  }
  public void setPhuongThucThanhToan(String phuongThucThanhToan) {
    this.phuongThucThanhToan = phuongThucThanhToan;
  }
  public BigDecimal getTienKhachDua() {
    return tienKhachDua;
  }
  public void setTienKhachDua(BigDecimal tienKhachDua) {
    this.tienKhachDua = tienKhachDua;
  }
  public boolean isThanhToanDienTuThanhCong() {
    return thanhToanDienTuThanhCong;
  }
  public void setThanhToanDienTuThanhCong(boolean value) {
    thanhToanDienTuThanhCong = value;
  }
  public String getMaThamChieuThanhToan() {
    return maThamChieuThanhToan;
  }
  public void setMaThamChieuThanhToan(String value) {
    maThamChieuThanhToan = value;
  }
  public List<DongThanhToan> getDongHang() {
    return dongHang;
  }
  public void setDongHang(List<DongThanhToan> value) {
    dongHang = value;
  }
}
