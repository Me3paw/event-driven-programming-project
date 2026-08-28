package entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class HoaDon {
  private String maHoaDon;
  private String maNhanVien;
  private String maKhachHang;
  private String maKhuyenMai;
  private String maNhanVienHuy;
  private LocalDateTime thoiDiemLap;
  private LocalDateTime thoiDiemHuy;
  private String phuongThucTt;
  private String maThamChieuTt;
  private BigDecimal tongGiaGoc;
  private String loaiGiamGia;
  private BigDecimal tyLeGiam;
  private BigDecimal tienGiam;
  private long diemSuDung;
  private BigDecimal tienGiamDiem;
  private long diemTichLuyKiem;
  private long diemThuongKiem;
  private BigDecimal tyLeVat;
  private BigDecimal tienVat;
  private BigDecimal tongThanhToan;
  private BigDecimal tienKhachDua;
  private BigDecimal tienThua;
  private String trangThai;
  private String lyDoHuy;
  private List<ChiTietHoaDon> chiTiet = new ArrayList<ChiTietHoaDon>();
  private List<PhanBoXuatLo> phanBo = new ArrayList<PhanBoXuatLo>();

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
  public String getMaNhanVienHuy() {
    return maNhanVienHuy;
  }
  public void setMaNhanVienHuy(String maNhanVienHuy) {
    this.maNhanVienHuy = maNhanVienHuy;
  }
  public LocalDateTime getThoiDiemLap() {
    return thoiDiemLap;
  }
  public void setThoiDiemLap(LocalDateTime thoiDiemLap) {
    this.thoiDiemLap = thoiDiemLap;
  }
  public LocalDateTime getThoiDiemHuy() {
    return thoiDiemHuy;
  }
  public void setThoiDiemHuy(LocalDateTime thoiDiemHuy) {
    this.thoiDiemHuy = thoiDiemHuy;
  }
  public String getPhuongThucTt() {
    return phuongThucTt;
  }
  public void setPhuongThucTt(String phuongThucTt) {
    this.phuongThucTt = phuongThucTt;
  }
  public String getMaThamChieuTt() {
    return maThamChieuTt;
  }
  public void setMaThamChieuTt(String maThamChieuTt) {
    this.maThamChieuTt = maThamChieuTt;
  }
  public BigDecimal getTongGiaGoc() {
    return tongGiaGoc;
  }
  public void setTongGiaGoc(BigDecimal tongGiaGoc) {
    this.tongGiaGoc = tongGiaGoc;
  }
  public String getLoaiGiamGia() {
    return loaiGiamGia;
  }
  public void setLoaiGiamGia(String loaiGiamGia) {
    this.loaiGiamGia = loaiGiamGia;
  }
  public BigDecimal getTyLeGiam() {
    return tyLeGiam;
  }
  public void setTyLeGiam(BigDecimal tyLeGiam) {
    this.tyLeGiam = tyLeGiam;
  }
  public BigDecimal getTienGiam() {
    return tienGiam;
  }
  public void setTienGiam(BigDecimal tienGiam) {
    this.tienGiam = tienGiam;
  }
  public long getDiemSuDung() {
    return diemSuDung;
  }
  public void setDiemSuDung(long diemSuDung) {
    this.diemSuDung = diemSuDung;
  }
  public BigDecimal getTienGiamDiem() {
    return tienGiamDiem;
  }
  public void setTienGiamDiem(BigDecimal tienGiamDiem) {
    this.tienGiamDiem = tienGiamDiem;
  }
  public long getDiemTichLuyKiem() {
    return diemTichLuyKiem;
  }
  public void setDiemTichLuyKiem(long diemTichLuyKiem) {
    this.diemTichLuyKiem = diemTichLuyKiem;
  }
  public long getDiemThuongKiem() {
    return diemThuongKiem;
  }
  public void setDiemThuongKiem(long diemThuongKiem) {
    this.diemThuongKiem = diemThuongKiem;
  }
  public BigDecimal getTyLeVat() {
    return tyLeVat;
  }
  public void setTyLeVat(BigDecimal tyLeVat) {
    this.tyLeVat = tyLeVat;
  }
  public BigDecimal getTienVat() {
    return tienVat;
  }
  public void setTienVat(BigDecimal tienVat) {
    this.tienVat = tienVat;
  }
  public BigDecimal getTongThanhToan() {
    return tongThanhToan;
  }
  public void setTongThanhToan(BigDecimal tongThanhToan) {
    this.tongThanhToan = tongThanhToan;
  }
  public BigDecimal getTienKhachDua() {
    return tienKhachDua;
  }
  public void setTienKhachDua(BigDecimal tienKhachDua) {
    this.tienKhachDua = tienKhachDua;
  }
  public BigDecimal getTienThua() {
    return tienThua;
  }
  public void setTienThua(BigDecimal tienThua) {
    this.tienThua = tienThua;
  }
  public String getTrangThai() {
    return trangThai;
  }
  public void setTrangThai(String trangThai) {
    this.trangThai = trangThai;
  }
  public String getLyDoHuy() {
    return lyDoHuy;
  }
  public void setLyDoHuy(String lyDoHuy) {
    this.lyDoHuy = lyDoHuy;
  }
  public List<ChiTietHoaDon> getChiTiet() {
    return chiTiet;
  }
  public void setChiTiet(List<ChiTietHoaDon> chiTiet) {
    this.chiTiet = chiTiet;
  }
  public List<PhanBoXuatLo> getPhanBo() {
    return phanBo;
  }
  public void setPhanBo(List<PhanBoXuatLo> phanBo) {
    this.phanBo = phanBo;
  }
}
