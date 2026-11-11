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
  public String maNhanVienTiepTheo(Actor a) throws SQLException {
    Access.manager(a);
    try (Connection c = DBConnection.open()) {
      return nhanVienDAO.nextId(c);
    }
  }
  public String maNhaCungCapTiepTheo(Actor a) throws SQLException {
    Access.manager(a);
    try (Connection c = DBConnection.open()) {
      return nhaCungCapDAO.nextId(c);
    }
  }
  public String maKhachHangTiepTheo(Actor a) throws SQLException {
    Access.actor(a);
    try (Connection c = DBConnection.open()) {
      return khachHangDAO.nextId(c);
    }
  }
  public String maLoaiTiepTheo(Actor a) throws SQLException {
    Access.manager(a);
    try (Connection c = DBConnection.open()) {
      return loaiDAO.nextId(c);
    }
  }
  public List<NhanVien> timNhanVien(Actor a, String k, String s) throws SQLException {
    return timNhanVien(a, k, s, null);
  }
  public List<NhanVien> timNhanVien(Actor a, String k, String s, String chucVu)
      throws SQLException {
    Access.manager(a);
    String trangThai = Access.blankToNull(s);
    if (trangThai != null && !Access.is(trangThai, TrangThaiNhanVien.class))
      throw new IllegalArgumentException("Trạng thái nhân viên không hợp lệ");
    return timNhanVien(Access.blankToNull(k), trangThai, Access.blankToNull(chucVu));
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
    return timNhaCungCap(a, k, null);
  }
  public List<NhaCungCap> timNhaCungCap(Actor a, String k, String s) throws SQLException {
    Access.manager(a);
    String status = Access.blankToNull(s);
    if (status != null && !Access.is(status, TrangThaiHoatDong.class))
      throw new IllegalArgumentException("Trạng thái nhà cung cấp không hợp lệ");
    return timNhaCungCap(Access.blankToNull(k), status);
  }
  public void xoaNhanVien(Actor a, String id) throws SQLException {
    Access.manager(a);
    throw new IllegalStateException("Không xóa nhân viên; hãy đổi trạng thái nhân viên");
  }
  public void xoaTaiKhoan(Actor a, String id) throws SQLException {
    Access.manager(a);
    throw new IllegalStateException("Không xóa tài khoản; hãy đổi trạng thái tài khoản");
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
    throw new IllegalStateException("Không xóa khách hàng; hãy đổi trạng thái khách hàng");
  }
  public void xoaNhaCungCap(Actor a, String id) throws SQLException {
    Access.manager(a);
    throw new IllegalStateException("Không xóa nhà cung cấp; hãy đổi trạng thái nhà cung cấp");
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
    v.setMaNhanVien(code(v.getMaNhanVien(), "Mã nhân viên", ValidationRules.EMPLOYEE_ID, "NV001"));
    v.setHoTen(personName(v.getHoTen(), "Họ tên nhân viên"));
    v.setSoDienThoai(vnPhone(v.getSoDienThoai(), "Số điện thoại nhân viên"));
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
    v.setMaNhanVien(code(v.getMaNhanVien(), "Mã nhân viên", ValidationRules.EMPLOYEE_ID, "NV001"));
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
    v.setMaLoai(code(v.getMaLoai(), "Mã loại", ValidationRules.CATEGORY_ID, "LSP001"));
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
    v.setMaSanPham(code(v.getMaSanPham(), "Mã sản phẩm", ValidationRules.PRODUCT_ID, "893000000001"));
    v.setMaLoai(code(v.getMaLoai(), "Mã loại", ValidationRules.CATEGORY_ID, "LSP001"));
    v.setTenSanPham(required(v.getTenSanPham(), "Tên sản phẩm", 150));
    v.setDonViTinh(required(v.getDonViTinh(), "Đơn vị tính", 30));
    if (v.getGiaBan() == null || v.getGiaBan().signum() <= 0)
      throw new IllegalArgumentException("Giá bán phải lớn hơn 0");
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
    v.setMaKhachHang(code(v.getMaKhachHang(), "Mã khách hàng", ValidationRules.CUSTOMER_ID, "KH001"));
    v.setHoTen(personName(v.getHoTen(), "Họ tên khách hàng"));
    v.setSoDienThoai(vnPhone(v.getSoDienThoai(), "Số điện thoại khách hàng"));
    v.setTrangThai(enumValue(v.getTrangThai(), TrangThaiHoatDong.class, "Trạng thái khách hàng"));
    try (Connection c = DBConnection.open()) {
      khachHangDAO.save(v, c);
    }
  }
  private void luuNhaCungCap(NhaCungCap v) throws SQLException {
    if (v == null)
      throw new IllegalArgumentException("Thiếu nhà cung cấp");
    v.setMaNhaCungCap(code(v.getMaNhaCungCap(), "Mã nhà cung cấp", ValidationRules.SUPPLIER_ID, "NCC001"));
    v.setTenNhaCungCap(required(v.getTenNhaCungCap(), "Tên nhà cung cấp", 150));
    v.setSoDienThoai(phone(v.getSoDienThoai(), "Số điện thoại nhà cung cấp"));
    v.setDiaChi(optional(v.getDiaChi(), "Địa chỉ", 255));
    v.setTrangThai(enumValue(v.getTrangThai(), TrangThaiHoatDong.class, "Trạng thái nhà cung cấp"));
    try (Connection c = DBConnection.open()) {
      nhaCungCapDAO.save(v, c);
    }
  }
  private List<NhanVien> timNhanVien(String keyword, String trangThai, String chucVu)
      throws SQLException {
    try (Connection c = DBConnection.open()) {
      return nhanVienDAO.search(keyword, trangThai, chucVu, c);
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
  private List<NhaCungCap> timNhaCungCap(String keyword, String trangThai) throws SQLException {
    try (Connection c = DBConnection.open()) {
      return nhaCungCapDAO.search(keyword, trangThai, c);
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
  private String code(String value, String label, String regex, String example) {
    String trimmed = required(value, label, 20);
    if (!ValidationRules.matches(trimmed, regex))
      throw new IllegalArgumentException(label + " sai định dạng, phải có dạng " + example);
    return trimmed;
  }
  private String phone(String value, String label) {
    String trimmed = optional(value, label, 20);
    if (trimmed != null && !trimmed.matches(ValidationRules.PHONE))
      throw new IllegalArgumentException(label + " không hợp lệ, chỉ gồm số, dấu +, khoảng trắng, (), . và -");
    return trimmed;
  }
  private String vnPhone(String value, String label) {
    String trimmed = optional(value, label, 20);
    if (trimmed != null && !ValidationRules.matches(trimmed, ValidationRules.VN_PHONE))
      throw new IllegalArgumentException(label + " sai định dạng, phải có dạng 0xxxxxxxxx (10 số, đầu số 3/5/7/8/9, ví dụ: 0901234567)");
    return trimmed;
  }
  private String personName(String value, String label) {
    String trimmed = required(value, label, 100);
    if (!ValidationRules.matches(trimmed, ValidationRules.PERSON_NAME))
      throw new IllegalArgumentException(label + " sai định dạng, chỉ được chứa chữ cái và khoảng trắng (ví dụ: Nguyen Van A)");
    return trimmed;
  }
  private <E extends Enum<E>> String enumValue(String value, Class<E> type, String label) {
    String trimmed = required(value, label, 20);
    if (!Access.is(trimmed, type))
      throw new IllegalArgumentException(label + " không hợp lệ");
    return trimmed;
  }
}
