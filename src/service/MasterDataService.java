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
    luuNhanVienInternal(a, v);
  }
  public void luuTaiKhoan(Actor a, TaiKhoan v, char[] p) throws SQLException {
    Access.manager(a);
    luuTaiKhoanInternal(a, v, p);
  }
  public void luuLoai(Actor a, LoaiSanPham v) throws SQLException {
    Access.manager(a);
    luuLoaiInternal(a, v);
  }
  public void luuSanPham(Actor a, SanPham v) throws SQLException {
    Access.manager(a);
    luuSanPhamInternal(a, v);
  }
  public void luuKhachHang(Actor a, KhachHang v) throws SQLException {
    Access.actor(a);
    luuKhachHangInternal(a, v);
  }
  public void luuNhaCungCap(Actor a, NhaCungCap v) throws SQLException {
    Access.manager(a);
    luuNhaCungCapInternal(a, v);
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
    xoaNhanVienInternal(a, id);
  }
  public void xoaTaiKhoan(Actor a, String id) throws SQLException {
    Access.manager(a);
    xoaTaiKhoanInternal(a, id);
  }
  public void xoaLoai(Actor a, String id) throws SQLException {
    Access.manager(a);
    xoaLoaiInternal(a, id);
  }
  public void xoaSanPham(Actor a, String id) throws SQLException {
    Access.manager(a);
    xoaSanPhamInternal(a, id);
  }
  public void xoaKhachHang(Actor a, String id) throws SQLException {
    Access.actor(a);
    xoaKhachHangInternal(a, id);
  }
  public void xoaNhaCungCap(Actor a, String id) throws SQLException {
    Access.manager(a);
    xoaNhaCungCapInternal(a, id);
  }
  public void capNhatTrangThaiNhanVien(Actor a, String id, String s) throws SQLException {
    Access.manager(a);
    capNhatTrangThaiNhanVienInternal(a, id, s);
  }
  public void capNhatTrangThaiTaiKhoan(Actor a, String id, String s) throws SQLException {
    Access.manager(a);
    capNhatTrangThaiTaiKhoanInternal(a, id, s);
  }
  private void luuNhanVienInternal(Actor a, NhanVien v) throws SQLException {
    if (v == null)
      throw new IllegalArgumentException("Thiếu nhân viên");
    v.setMaNhanVien(code(v.getMaNhanVien(), "Mã nhân viên", ValidationRules.EMPLOYEE_ID, "NV001"));
    v.setHoTen(personName(v.getHoTen(), "Họ tên nhân viên"));
    v.setSoDienThoai(vnPhone(v.getSoDienThoai(), "Số điện thoại nhân viên"));
    v.setChucVu(optional(v.getChucVu(), "Chức vụ", 30));
    v.setTrangThai(enumValue(v.getTrangThai(), TrangThaiNhanVien.class, "Trạng thái nhân viên"));
    try (SessionTransaction transaction = new SessionTransaction(a, true, true)) {
      Connection c = transaction.connection();
      if (!"DANG_LAM".equals(v.getTrangThai())) protectEmployee(a, v.getMaNhanVien(), c);

      nhanVienDAO.save(v, c);
      transaction.commit();
    }
  }
  private void luuTaiKhoanInternal(Actor a, TaiKhoan v, char[] password) throws SQLException {
    if (v == null)
      throw new IllegalArgumentException("Thiếu tài khoản");
    String username = required(v.getTenDangNhap(), "Tên đăng nhập", 50);
    if (!ValidationRules.matches(username, ValidationRules.USERNAME))
      throw new IllegalArgumentException(
          "Tên đăng nhập chỉ gồm chữ cái, số, dấu _ . - và dài 3-50 ký tự");
    v.setTenDangNhap(username);
    v.setMaNhanVien(code(v.getMaNhanVien(), "Mã nhân viên", ValidationRules.EMPLOYEE_ID, "NV001"));
    v.setVaiTro(enumValue(v.getVaiTro(), VaiTro.class, "Vai trò"));
    v.setTrangThai(enumValue(v.getTrangThai(), TrangThaiTaiKhoan.class, "Trạng thái tài khoản"));
    try (SessionTransaction transaction = new SessionTransaction(a, true, true)) {
      Connection c = transaction.connection();
      TaiKhoan existing = taiKhoanDAO.find(v.getTenDangNhap(), c);
      if (existing != null && (!"HOAT_DONG".equals(v.getTrangThai())
          || !existing.getVaiTro().equals(v.getVaiTro())
          || !existing.getMaNhanVien().equals(v.getMaNhanVien()))) protectAccount(a, v.getTenDangNhap(), c);

      TaiKhoan old = taiKhoanDAO.find(v.getTenDangNhap(), c);
      if (password == null || password.length == 0) {
        if (old == null)
          throw new IllegalArgumentException("Mật khẩu tài khoản mới không được để trống");
        v.setMatKhauBam(old.getMatKhauBam());
      } else {
        if (password.length < 6)
          throw new IllegalArgumentException("Mật khẩu phải có ít nhất 6 ký tự");
        v.setMatKhauBam(new AuthService().hash(password));
      }
      taiKhoanDAO.save(v, c);
      transaction.commit();
    } finally {
      if (password != null)
        Arrays.fill(password, '\0');
    }
  }
  private void luuLoaiInternal(Actor a, LoaiSanPham v) throws SQLException {
    if (v == null)
      throw new IllegalArgumentException("Thiếu loại sản phẩm");
    v.setMaLoai(code(v.getMaLoai(), "Mã loại", ValidationRules.CATEGORY_ID, "LSP001"));
    v.setTenLoai(required(v.getTenLoai(), "Tên loại", 100));
    v.setMoTa(optional(v.getMoTa(), "Mô tả", 255));
    v.setTrangThai(enumValue(v.getTrangThai(), TrangThaiSanPham.class, "Trạng thái loại"));
    try (SessionTransaction transaction = new SessionTransaction(a, true, false)) {
      Connection c = transaction.connection();
      loaiDAO.save(v, c);
      transaction.commit();
    }
  }
  private void luuSanPhamInternal(Actor a, SanPham v) throws SQLException {
    if (v == null)
      throw new IllegalArgumentException("Thiếu sản phẩm");
    v.setMaSanPham(code(v.getMaSanPham(), "Mã sản phẩm", ValidationRules.PRODUCT_ID, "893000000001"));
    v.setMaLoai(code(v.getMaLoai(), "Mã loại", ValidationRules.CATEGORY_ID, "LSP001"));
    v.setTenSanPham(required(v.getTenSanPham(), "Tên sản phẩm", 150));
    v.setDonViTinh(required(v.getDonViTinh(), "Đơn vị tính", 30));
    if (v.getGiaBan() == null || v.getGiaBan().signum() < 0)
      throw new IllegalArgumentException("Giá bán không được âm");
    if (v.getNguongTon() < 0)
      throw new IllegalArgumentException("Ngưỡng tồn không được âm");
    v.setTrangThai(enumValue(v.getTrangThai(), TrangThaiSanPham.class, "Trạng thái sản phẩm"));
    try (SessionTransaction transaction = new SessionTransaction(a, true, false)) {
      Connection c = transaction.connection();
      sanPhamDAO.save(v, c);
      transaction.commit();
    }
  }
  private void luuKhachHangInternal(Actor a, KhachHang v) throws SQLException {
    if (v == null)
      throw new IllegalArgumentException("Thiếu khách hàng");
    v.setMaKhachHang(code(v.getMaKhachHang(), "Mã khách hàng", ValidationRules.CUSTOMER_ID, "KH001"));
    v.setHoTen(personName(v.getHoTen(), "Họ tên khách hàng"));
    v.setSoDienThoai(vnPhone(v.getSoDienThoai(), "Số điện thoại khách hàng"));
    v.setTrangThai(enumValue(v.getTrangThai(), TrangThaiHoatDong.class, "Trạng thái khách hàng"));
    try (SessionTransaction transaction = new SessionTransaction(a, false, false)) {
      Connection c = transaction.connection();
      khachHangDAO.save(v, c);
      transaction.commit();
    }
  }
  private void luuNhaCungCapInternal(Actor a, NhaCungCap v) throws SQLException {
    if (v == null)
      throw new IllegalArgumentException("Thiếu nhà cung cấp");
    v.setMaNhaCungCap(code(v.getMaNhaCungCap(), "Mã nhà cung cấp", ValidationRules.SUPPLIER_ID, "NCC001"));
    v.setTenNhaCungCap(required(v.getTenNhaCungCap(), "Tên nhà cung cấp", 150));
    v.setSoDienThoai(vnPhone(v.getSoDienThoai(), "Số điện thoại nhà cung cấp"));
    v.setDiaChi(optional(v.getDiaChi(), "Địa chỉ", 255));
    v.setTrangThai(enumValue(v.getTrangThai(), TrangThaiHoatDong.class, "Trạng thái nhà cung cấp"));
    try (SessionTransaction transaction = new SessionTransaction(a, true, false)) {
      Connection c = transaction.connection();
      nhaCungCapDAO.save(v, c);
      transaction.commit();
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
  private void xoaNhanVienInternal(Actor a, String id) throws SQLException {
    try (SessionTransaction transaction = new SessionTransaction(a, true, true)) {
      Connection c = transaction.connection();
      protectEmployee(a, id, c);

      nhanVienDAO.delete(id, c);
      transaction.commit();
    } catch (SQLException exception) {
      if (exception.getErrorCode() == 547)
        throw new IllegalStateException("Bản ghi đã được tham chiếu; hãy đổi trạng thái thay vì xóa.", exception);
      throw exception;
    }
  }
  private void xoaTaiKhoanInternal(Actor a, String id) throws SQLException {
    try (SessionTransaction transaction = new SessionTransaction(a, true, true)) {
      Connection c = transaction.connection();
      protectAccount(a, id, c);

      taiKhoanDAO.delete(id, c);
      transaction.commit();
    } catch (SQLException exception) {
      if (exception.getErrorCode() == 547)
        throw new IllegalStateException("Bản ghi đã được tham chiếu; hãy đổi trạng thái thay vì xóa.", exception);
      throw exception;
    }
  }
  private void xoaLoaiInternal(Actor a, String id) throws SQLException {
    try (SessionTransaction transaction = new SessionTransaction(a, true, false)) {
      Connection c = transaction.connection();
      loaiDAO.delete(id, c);
      transaction.commit();
    } catch (SQLException exception) {
      if (exception.getErrorCode() == 547)
        throw new IllegalStateException("Bản ghi đã được tham chiếu; hãy đổi trạng thái thay vì xóa.", exception);
      throw exception;
    }
  }
  private void xoaSanPhamInternal(Actor a, String id) throws SQLException {
    try (SessionTransaction transaction = new SessionTransaction(a, true, false)) {
      Connection c = transaction.connection();
      sanPhamDAO.delete(id, c);
      transaction.commit();
    } catch (SQLException exception) {
      if (exception.getErrorCode() == 547)
        throw new IllegalStateException("Bản ghi đã được tham chiếu; hãy đổi trạng thái thay vì xóa.", exception);
      throw exception;
    }
  }
  private void xoaKhachHangInternal(Actor a, String id) throws SQLException {
    try (SessionTransaction transaction = new SessionTransaction(a, false, false)) {
      Connection c = transaction.connection();
      khachHangDAO.delete(id, c);
      transaction.commit();
    } catch (SQLException exception) {
      if (exception.getErrorCode() == 547)
        throw new IllegalStateException("Bản ghi đã được tham chiếu; hãy đổi trạng thái thay vì xóa.", exception);
      throw exception;
    }
  }
  private void xoaNhaCungCapInternal(Actor a, String id) throws SQLException {
    try (SessionTransaction transaction = new SessionTransaction(a, true, false)) {
      Connection c = transaction.connection();
      nhaCungCapDAO.delete(id, c);
      transaction.commit();
    } catch (SQLException exception) {
      if (exception.getErrorCode() == 547)
        throw new IllegalStateException("Bản ghi đã được tham chiếu; hãy đổi trạng thái thay vì xóa.", exception);
      throw exception;
    }
  }
  private void capNhatTrangThaiNhanVienInternal(Actor a, String maNhanVien, String trangThai) throws SQLException {
    if (!Access.is(trangThai, TrangThaiNhanVien.class))
      throw new IllegalArgumentException("Trạng thái nhân viên không hợp lệ");
    try (SessionTransaction transaction = new SessionTransaction(a, true, true)) {
      Connection c = transaction.connection();
      if (!"DANG_LAM".equals(trangThai)) protectEmployee(a, maNhanVien, c);

      nhanVienDAO.updateStatus(maNhanVien, trangThai, c);
      transaction.commit();
    }
  }
  private void capNhatTrangThaiTaiKhoanInternal(Actor a, String tenDangNhap, String trangThai) throws SQLException {
    if (!Access.is(trangThai, TrangThaiTaiKhoan.class))
      throw new IllegalArgumentException("Trạng thái tài khoản không hợp lệ");
    try (SessionTransaction transaction = new SessionTransaction(a, true, true)) {
      Connection c = transaction.connection();
      if (!"HOAT_DONG".equals(trangThai)) protectAccount(a, tenDangNhap, c);

      taiKhoanDAO.updateStatus(tenDangNhap, trangThai, c);
      transaction.commit();
    }
  }
  private void protectAccount(Actor actor, String username, Connection connection) throws SQLException {
    if (actor.getTenDangNhap().equals(username))
      throw new IllegalStateException("Không thể xóa, khóa hoặc hạ quyền tài khoản đang đăng nhập");
    TaiKhoan account = taiKhoanDAO.find(username, connection);
    if (account != null && "QUAN_LY".equals(account.getVaiTro())
        && "HOAT_DONG".equals(account.getTrangThai())
        && "DANG_LAM".equals(account.getTrangThaiNhanVien())) {
      if (taiKhoanDAO.countActiveManagers(connection) <= 1)
        throw new IllegalStateException("Phải giữ ít nhất một quản lý đang hoạt động");
    }
  }

  private void protectEmployee(Actor actor, String employeeId, Connection connection) throws SQLException {
    if (actor.getMaNhanVien().equals(employeeId))
      throw new IllegalStateException("Không thể xóa hoặc cho nghỉ việc nhân viên đang đăng nhập");
    for (TaiKhoan account : taiKhoanDAO.search(employeeId, null, null, connection))
      if (employeeId.equals(account.getMaNhanVien())) protectAccount(actor, account.getTenDangNhap(), connection);
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
