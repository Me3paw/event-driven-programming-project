package service;

import connectDB.DBConnection;
import dao.HoaDonDAO;
import dao.KhachHangDAO;
import dao.LoaiSanPhamDAO;
import dao.NhaCungCapDAO;
import dao.NhanVienDAO;
import dao.SanPhamDAO;
import dao.TaiKhoanDAO;
import entity.HangThanhVien;
import entity.KhachHang;
import entity.LoaiSanPham;
import entity.NhaCungCap;
import entity.NhanVien;
import entity.SanPham;
import entity.TaiKhoan;
import entity.TrangThaiHoatDong;
import entity.TrangThaiNhanVien;
import entity.TrangThaiSanPham;
import entity.TrangThaiTaiKhoan;
import entity.VaiTro;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public class MasterDataService {
  private final NhanVienDAO nhanVienDAO = new NhanVienDAO();
  private final TaiKhoanDAO taiKhoanDAO = new TaiKhoanDAO();
  private final LoaiSanPhamDAO loaiDAO = new LoaiSanPhamDAO();
  private final SanPhamDAO sanPhamDAO = new SanPhamDAO();
  private final KhachHangDAO khachHangDAO = new KhachHangDAO();
  private final HoaDonDAO hoaDonDAO = new HoaDonDAO();
  private final NhaCungCapDAO nhaCungCapDAO = new NhaCungCapDAO();
  public void luuNhanVien(Actor a, NhanVien v) throws SQLException {
    Access.manager(a);
    luuNhanVien(v);
  }
  public void luuTaiKhoan(Actor a, TaiKhoan v, char[] p) throws SQLException {
    Access.manager(a);
    luuTaiKhoan(v, p);
  }
  public void luuLoai(Actor a, LoaiSanPham v) throws SQLException {
    Access.manager(a);
    luuLoai(v);
  }
  public void luuSanPham(Actor a, SanPham v) throws SQLException {
    Access.manager(a);
    luuSanPham(v);
  }
  public void luuKhachHang(Actor a, KhachHang v) throws SQLException {
    Access.actor(a);
    luuKhachHang(v);
  }
  public void luuNhaCungCap(Actor a, NhaCungCap v) throws SQLException {
    Access.manager(a);
    luuNhaCungCap(v);
  }
  public List<NhanVien> timNhanVien(Actor a, String k, String s) throws SQLException {
    Access.manager(a);
    String trangThai = Access.blankToNull(s);
    if (trangThai != null && !Access.is(trangThai, TrangThaiNhanVien.class))
      throw new IllegalArgumentException("Trạng thái nhân viên không hợp lệ");
    return timNhanVien(Access.blankToNull(k), trangThai);
  }
  public List<TaiKhoan> timTaiKhoan(Actor a, String k, String r, String s) throws SQLException {
    Access.manager(a);
    String vaiTro = Access.blankToNull(r);
    String trangThai = Access.blankToNull(s);
    if (vaiTro != null && !Access.is(vaiTro, VaiTro.class))
      throw new IllegalArgumentException("Vai trò không hợp lệ");
    if (trangThai != null && !Access.is(trangThai, TrangThaiTaiKhoan.class))
      throw new IllegalArgumentException("Trạng thái tài khoản không hợp lệ");
    return timTaiKhoan(Access.blankToNull(k), vaiTro, trangThai);
  }
  public List<LoaiSanPham> timLoai(Actor a, String k) throws SQLException {
    Access.actor(a);
    return timLoai(Access.blankToNull(k));
  }
  public List<SanPham> timSanPham(Actor a, String k, String l, String s, BigDecimal t, BigDecimal d)
      throws SQLException {
    Access.actor(a);
    String trangThai = Access.blankToNull(s);
    if (trangThai != null && !Access.is(trangThai, TrangThaiSanPham.class))
      throw new IllegalArgumentException("Trạng thái sản phẩm không hợp lệ");
    return timSanPham(Access.blankToNull(k), Access.blankToNull(l), trangThai, t, d);
  }
  public List<KhachHang> timKhachHang(Actor a, String k, String s) throws SQLException {
    Access.actor(a);
    String trangThai = Access.blankToNull(s);
    if (trangThai != null && !Access.is(trangThai, TrangThaiHoatDong.class))
      throw new IllegalArgumentException("Trạng thái khách hàng không hợp lệ");
    return timKhachHang(Access.blankToNull(k), trangThai);
  }
  public CustomerProfile chiTietKhachHang(Actor a, String id) throws SQLException {
    Access.actor(a);
    try (Connection c = DBConnection.open()) {
      KhachHang customer = khachHangDAO.find(id, c);
      if (customer == null)
        throw new IllegalArgumentException("Không tìm thấy khách hàng");
      return new CustomerProfile(customer, khachHangDAO.actualSpend(id, c),
          tier(customer.getDiemTichLuy()), hoaDonDAO.byCustomer(id, c));
    }
  }
  public List<KhachHang> khachHangTheoHang(Actor a, String tier) throws SQLException {
    Access.actor(a);
    if (!Access.is(tier, HangThanhVien.class))
      throw new IllegalArgumentException("Hạng khách hàng không hợp lệ");
    try (Connection c = DBConnection.open()) {
      return khachHangDAO.byTier(tier, c);
    }
  }
  public List<NhaCungCap> timNhaCungCap(Actor a, String k) throws SQLException {
    Access.manager(a);
    return timNhaCungCap(Access.blankToNull(k));
  }
  public void xoaNhanVien(Actor a, String id) throws SQLException {
    Access.manager(a);
    xoaNhanVien(id);
  }
  public void xoaTaiKhoan(Actor a, String id) throws SQLException {
    Access.manager(a);
    xoaTaiKhoan(id);
  }
  public void xoaLoai(Actor a, String id) throws SQLException {
    Access.manager(a);
    xoaLoai(id);
  }
  public void xoaSanPham(Actor a, String id) throws SQLException {
    Access.manager(a);
    xoaSanPham(id);
  }
  public void xoaKhachHang(Actor a, String id) throws SQLException {
    Access.actor(a);
    xoaKhachHang(id);
  }
  public void xoaNhaCungCap(Actor a, String id) throws SQLException {
    Access.manager(a);
    xoaNhaCungCap(id);
  }
  public void capNhatTrangThaiNhanVien(Actor a, String id, String s) throws SQLException {
    Access.manager(a);
    capNhatTrangThaiNhanVien(id, s);
  }
  public void capNhatTrangThaiTaiKhoan(Actor a, String id, String s) throws SQLException {
    Access.manager(a);
    capNhatTrangThaiTaiKhoan(id, s);
  }
  private void luuNhanVien(NhanVien v) throws SQLException {
    if (v == null)
      throw new IllegalArgumentException("Thiếu nhân viên");
    v.setMaNhanVien(required(v.getMaNhanVien(), "Mã nhân viên", 20));
    v.setHoTen(required(v.getHoTen(), "Họ tên nhân viên", 100));
    v.setSoDienThoai(phone(v.getSoDienThoai(), "Số điện thoại nhân viên"));
    v.setChucVu(optional(v.getChucVu(), "Chức vụ", 30));
    v.setTrangThai(enumValue(v.getTrangThai(), TrangThaiNhanVien.class, "Trạng thái nhân viên"));
    try (Connection c = DBConnection.open()) {
      nhanVienDAO.save(v, c);
    }
  }
  private void luuTaiKhoan(TaiKhoan v, char[] password) throws SQLException {
    if (v == null)
      throw new IllegalArgumentException("Thiếu tài khoản");
    v.setTenDangNhap(required(v.getTenDangNhap(), "Tên đăng nhập", 50));
    v.setMaNhanVien(required(v.getMaNhanVien(), "Mã nhân viên", 20));
    v.setVaiTro(enumValue(v.getVaiTro(), VaiTro.class, "Vai trò"));
    v.setTrangThai(enumValue(v.getTrangThai(), TrangThaiTaiKhoan.class, "Trạng thái tài khoản"));
    try (Connection c = DBConnection.open()) {
      TaiKhoan old = taiKhoanDAO.find(v.getTenDangNhap(), c);
      if (password == null || password.length == 0) {
        if (old == null)
          throw new IllegalArgumentException("Mật khẩu tài khoản mới không được để trống");
        v.setMatKhauBam(old.getMatKhauBam());
      } else
        v.setMatKhauBam(new AuthService().hash(password));
      taiKhoanDAO.save(v, c);
    } finally {
      if (password != null)
        Arrays.fill(password, '\0');
    }
  }
  private void luuLoai(LoaiSanPham v) throws SQLException {
    if (v == null)
      throw new IllegalArgumentException("Thiếu loại sản phẩm");
    v.setMaLoai(required(v.getMaLoai(), "Mã loại", 20));
    v.setTenLoai(required(v.getTenLoai(), "Tên loại", 100));
    v.setMoTa(optional(v.getMoTa(), "Mô tả", 255));
    v.setTrangThai(enumValue(v.getTrangThai(), TrangThaiSanPham.class, "Trạng thái loại"));
    try (Connection c = DBConnection.open()) {
      loaiDAO.save(v, c);
    }
  }
  private void luuSanPham(SanPham v) throws SQLException {
    if (v == null)
      throw new IllegalArgumentException("Thiếu sản phẩm");
    v.setMaSanPham(required(v.getMaSanPham(), "Mã sản phẩm", 20));
    v.setMaLoai(required(v.getMaLoai(), "Mã loại", 20));
    v.setTenSanPham(required(v.getTenSanPham(), "Tên sản phẩm", 150));
    v.setDonViTinh(required(v.getDonViTinh(), "Đơn vị tính", 30));
    if (v.getGiaBan() == null || v.getGiaBan().signum() < 0)
      throw new IllegalArgumentException("Giá bán không được âm");
    if (v.getNguongTon() < 0)
      throw new IllegalArgumentException("Ngưỡng tồn không được âm");
    v.setTrangThai(enumValue(v.getTrangThai(), TrangThaiSanPham.class, "Trạng thái sản phẩm"));
    try (Connection c = DBConnection.open()) {
      sanPhamDAO.save(v, c);
    }
  }
  private void luuKhachHang(KhachHang v) throws SQLException {
    if (v == null)
      throw new IllegalArgumentException("Thiếu khách hàng");
    v.setMaKhachHang(required(v.getMaKhachHang(), "Mã khách hàng", 20));
    v.setHoTen(required(v.getHoTen(), "Họ tên khách hàng", 100));
    v.setSoDienThoai(phone(v.getSoDienThoai(), "Số điện thoại khách hàng"));
    v.setTrangThai(enumValue(v.getTrangThai(), TrangThaiHoatDong.class, "Trạng thái khách hàng"));
    try (Connection c = DBConnection.open()) {
      khachHangDAO.save(v, c);
    }
  }
  private void luuNhaCungCap(NhaCungCap v) throws SQLException {
    if (v == null)
      throw new IllegalArgumentException("Thiếu nhà cung cấp");
    v.setMaNhaCungCap(required(v.getMaNhaCungCap(), "Mã nhà cung cấp", 20));
    v.setTenNhaCungCap(required(v.getTenNhaCungCap(), "Tên nhà cung cấp", 150));
    v.setSoDienThoai(phone(v.getSoDienThoai(), "Số điện thoại nhà cung cấp"));
    v.setDiaChi(optional(v.getDiaChi(), "Địa chỉ", 255));
    v.setTrangThai(enumValue(v.getTrangThai(), TrangThaiHoatDong.class, "Trạng thái nhà cung cấp"));
    try (Connection c = DBConnection.open()) {
      nhaCungCapDAO.save(v, c);
    }
  }
  private List<NhanVien> timNhanVien(String keyword, String trangThai) throws SQLException {
    try (Connection c = DBConnection.open()) {
      return nhanVienDAO.search(keyword, trangThai, c);
    }
  }
  private List<TaiKhoan> timTaiKhoan(String keyword, String vaiTro, String trangThai)
      throws SQLException {
    try (Connection c = DBConnection.open()) {
      return taiKhoanDAO.search(keyword, vaiTro, trangThai, c);
    }
  }
  private List<LoaiSanPham> timLoai(String keyword) throws SQLException {
    try (Connection c = DBConnection.open()) {
      return loaiDAO.search(keyword, c);
    }
  }
  private List<SanPham> timSanPham(String keyword, String maLoai, String trangThai,
      BigDecimal tuGia, BigDecimal denGia) throws SQLException {
    try (Connection c = DBConnection.open()) {
      return sanPhamDAO.search(keyword, maLoai, trangThai, tuGia, denGia, c);
    }
  }
  private List<KhachHang> timKhachHang(String keyword, String trangThai) throws SQLException {
    try (Connection c = DBConnection.open()) {
      return khachHangDAO.search(keyword, trangThai, c);
    }
  }
  private List<NhaCungCap> timNhaCungCap(String keyword) throws SQLException {
    try (Connection c = DBConnection.open()) {
      return nhaCungCapDAO.search(keyword, c);
    }
  }
  private void xoaNhanVien(String id) throws SQLException {
    try (Connection c = DBConnection.open()) {
      nhanVienDAO.delete(id, c);
    }
  }
  private void xoaTaiKhoan(String id) throws SQLException {
    try (Connection c = DBConnection.open()) {
      taiKhoanDAO.delete(id, c);
    }
  }
  private void xoaLoai(String id) throws SQLException {
    try (Connection c = DBConnection.open()) {
      loaiDAO.delete(id, c);
    }
  }
  private void xoaSanPham(String id) throws SQLException {
    try (Connection c = DBConnection.open()) {
      sanPhamDAO.delete(id, c);
    }
  }
  private void xoaKhachHang(String id) throws SQLException {
    try (Connection c = DBConnection.open()) {
      khachHangDAO.delete(id, c);
    }
  }
  private void xoaNhaCungCap(String id) throws SQLException {
    try (Connection c = DBConnection.open()) {
      nhaCungCapDAO.delete(id, c);
    }
  }
  private void capNhatTrangThaiNhanVien(String maNhanVien, String trangThai) throws SQLException {
    if (!Access.is(trangThai, TrangThaiNhanVien.class))
      throw new IllegalArgumentException("Trạng thái nhân viên không hợp lệ");
    try (Connection c = DBConnection.open()) {
      nhanVienDAO.updateStatus(maNhanVien, trangThai, c);
    }
  }
  private void capNhatTrangThaiTaiKhoan(String tenDangNhap, String trangThai) throws SQLException {
    if (!Access.is(trangThai, TrangThaiTaiKhoan.class))
      throw new IllegalArgumentException("Trạng thái tài khoản không hợp lệ");
    try (Connection c = DBConnection.open()) {
      taiKhoanDAO.updateStatus(tenDangNhap, trangThai, c);
    }
  }
  private String tier(long points) {
    return points >= 1000000 ? HangThanhVien.VANG.name()
        : points >= 200000   ? HangThanhVien.BAC.name()
        : points >= 50000    ? HangThanhVien.DONG.name()
                             : HangThanhVien.CHUA_XEP_HANG.name();
  }
  private String required(String value, String label, int maximum) {
    String trimmed = Access.blankToNull(value);
    if (trimmed == null)
      throw new IllegalArgumentException(label + " không được để trống");
    if (trimmed.length() > maximum)
      throw new IllegalArgumentException(label + " quá dài");
    return trimmed;
  }
  private String optional(String value, String label, int maximum) {
    String trimmed = Access.blankToNull(value);
    if (trimmed != null && trimmed.length() > maximum)
      throw new IllegalArgumentException(label + " quá dài");
    return trimmed;
  }
  private String phone(String value, String label) {
    String trimmed = optional(value, label, 20);
    if (trimmed != null && !trimmed.matches("\\+?[0-9][0-9 .()\\-]{5,19}"))
      throw new IllegalArgumentException(label + " không hợp lệ");
    return trimmed;
  }
  private <E extends Enum<E>> String enumValue(String value, Class<E> type, String label) {
    String trimmed = required(value, label, 20);
    if (!Access.is(trimmed, type))
      throw new IllegalArgumentException(label + " không hợp lệ");
    return trimmed;
  }
}
