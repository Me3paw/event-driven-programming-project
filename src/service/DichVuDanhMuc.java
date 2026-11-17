package service;

import connectDB.ConnectDB;
import dao.HoaDon_DAO;
import dao.KhachHang_DAO;
import dao.LoaiSanPham_DAO;
import dao.NhaCungCap_DAO;
import dao.NhanVien_DAO;
import dao.SanPham_DAO;
import dao.TaiKhoan_DAO;
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

public class DichVuDanhMuc {
  private final NhanVien_DAO nhanVienDAO = new NhanVien_DAO();
  private final TaiKhoan_DAO taiKhoanDAO = new TaiKhoan_DAO();
  private final LoaiSanPham_DAO loaiDAO = new LoaiSanPham_DAO();
  private final SanPham_DAO sanPhamDAO = new SanPham_DAO();
  private final KhachHang_DAO khachHangDAO = new KhachHang_DAO();
  private final HoaDon_DAO hoaDonDAO = new HoaDon_DAO();
  private final NhaCungCap_DAO nhaCungCapDAO = new NhaCungCap_DAO();
  public void luuNhanVien(NguoiDung a, NhanVien v) throws SQLException {
    KiemTraQuyen.manager(a);
    xuLyLuuNhanVien(a, v);
  }
  public void luuTaiKhoan(NguoiDung a, TaiKhoan v, char[] p) throws SQLException {
    KiemTraQuyen.manager(a);
    xuLyLuuTaiKhoan(a, v, p);
  }
  public void luuLoai(NguoiDung a, LoaiSanPham v) throws SQLException {
    KiemTraQuyen.manager(a);
    xuLyLuuLoai(a, v);
  }
  public void luuSanPham(NguoiDung a, SanPham v) throws SQLException {
    KiemTraQuyen.manager(a);
    xuLyLuuSanPham(a, v);
  }
  public void luuKhachHang(NguoiDung a, KhachHang v) throws SQLException {
    KiemTraQuyen.actor(a);
    xuLyLuuKhachHang(a, v);
  }
  public void luuNhaCungCap(NguoiDung a, NhaCungCap v) throws SQLException {
    KiemTraQuyen.manager(a);
    xuLyLuuNhaCungCap(a, v);
  }
  public String maNhanVienTiepTheo(NguoiDung a) throws SQLException {
    KiemTraQuyen.manager(a);
    try (Connection c = ConnectDB.open()) {
      return nhanVienDAO.maTiepTheo(c);
    }
  }
  public String maNhaCungCapTiepTheo(NguoiDung a) throws SQLException {
    KiemTraQuyen.manager(a);
    try (Connection c = ConnectDB.open()) {
      return nhaCungCapDAO.maTiepTheo(c);
    }
  }
  public String maKhachHangTiepTheo(NguoiDung a) throws SQLException {
    KiemTraQuyen.actor(a);
    try (Connection c = ConnectDB.open()) {
      return khachHangDAO.maTiepTheo(c);
    }
  }
  public String maLoaiTiepTheo(NguoiDung a) throws SQLException {
    KiemTraQuyen.manager(a);
    try (Connection c = ConnectDB.open()) {
      return loaiDAO.maTiepTheo(c);
    }
  }
  public List<NhanVien> timNhanVien(NguoiDung a, String k, String s) throws SQLException {
    return timNhanVien(a, k, s, null);
  }
  public List<NhanVien> timNhanVien(NguoiDung a, String k, String s, String chucVu)
      throws SQLException {
    KiemTraQuyen.manager(a);
    String trangThai = KiemTraQuyen.blankToNull(s);
    if (trangThai != null && !KiemTraQuyen.is(trangThai, TrangThaiNhanVien.class))
      throw new IllegalArgumentException("Trạng thái nhân viên không hợp lệ");
    return timNhanVien(KiemTraQuyen.blankToNull(k), trangThai, KiemTraQuyen.blankToNull(chucVu));
  }
  public List<TaiKhoan> timTaiKhoan(NguoiDung a, String k, String r, String s) throws SQLException {
    KiemTraQuyen.manager(a);
    String vaiTro = KiemTraQuyen.blankToNull(r);
    String trangThai = KiemTraQuyen.blankToNull(s);
    if (vaiTro != null && !KiemTraQuyen.is(vaiTro, VaiTro.class))
      throw new IllegalArgumentException("Vai trò không hợp lệ");
    if (trangThai != null && !KiemTraQuyen.is(trangThai, TrangThaiTaiKhoan.class))
      throw new IllegalArgumentException("Trạng thái tài khoản không hợp lệ");
    return timTaiKhoan(KiemTraQuyen.blankToNull(k), vaiTro, trangThai);
  }
  public List<LoaiSanPham> timLoai(NguoiDung a, String k) throws SQLException {
    KiemTraQuyen.actor(a);
    return timLoai(KiemTraQuyen.blankToNull(k));
  }
  public List<SanPham> timSanPham(NguoiDung a, String k, String l, String s, BigDecimal t, BigDecimal d)
      throws SQLException {
    KiemTraQuyen.actor(a);
    String trangThai = KiemTraQuyen.blankToNull(s);
    if (trangThai != null && !KiemTraQuyen.is(trangThai, TrangThaiSanPham.class))
      throw new IllegalArgumentException("Trạng thái sản phẩm không hợp lệ");
    return timSanPham(KiemTraQuyen.blankToNull(k), KiemTraQuyen.blankToNull(l), trangThai, t, d);
  }
  public List<KhachHang> timKhachHang(NguoiDung a, String k, String s) throws SQLException {
    KiemTraQuyen.actor(a);
    String trangThai = KiemTraQuyen.blankToNull(s);
    if (trangThai != null && !KiemTraQuyen.is(trangThai, TrangThaiHoatDong.class))
      throw new IllegalArgumentException("Trạng thái khách hàng không hợp lệ");
    return timKhachHang(KiemTraQuyen.blankToNull(k), trangThai);
  }
  public HoSoKhachHang chiTietKhachHang(NguoiDung a, String id) throws SQLException {
    KiemTraQuyen.actor(a);
    try (Connection c = ConnectDB.open()) {
      KhachHang khachHang = khachHangDAO.timTheoMa(id, c);
      if (khachHang == null)
        throw new IllegalArgumentException("Không tìm thấy khách hàng");
      return new HoSoKhachHang(khachHang, khachHangDAO.tongChiTieu(id, c),
          hangThanhVien(khachHang.getDiemTichLuy()), hoaDonDAO.timTheoKhachHang(id, c));
    }
  }
  public List<KhachHang> khachHangTheoHang(NguoiDung a, String hangThanhVien) throws SQLException {
    KiemTraQuyen.actor(a);
    if (!KiemTraQuyen.is(hangThanhVien, HangThanhVien.class))
      throw new IllegalArgumentException("Hạng khách hàng không hợp lệ");
    try (Connection c = ConnectDB.open()) {
      return khachHangDAO.timTheoHang(hangThanhVien, c);
    }
  }
  public List<NhaCungCap> timNhaCungCap(NguoiDung a, String k) throws SQLException {
    return timNhaCungCap(a, k, null);
  }
  public List<NhaCungCap> timNhaCungCap(NguoiDung a, String k, String s) throws SQLException {
    KiemTraQuyen.manager(a);
    String status = KiemTraQuyen.blankToNull(s);
    if (status != null && !KiemTraQuyen.is(status, TrangThaiHoatDong.class))
      throw new IllegalArgumentException("Trạng thái nhà cung cấp không hợp lệ");
    return timNhaCungCap(KiemTraQuyen.blankToNull(k), status);
  }
  public void xoaNhanVien(NguoiDung a, String id) throws SQLException {
    KiemTraQuyen.manager(a);
    xuLyXoaNhanVien(a, id);
  }
  public void xoaTaiKhoan(NguoiDung a, String id) throws SQLException {
    KiemTraQuyen.manager(a);
    xuLyXoaTaiKhoan(a, id);
  }
  public void xoaLoai(NguoiDung a, String id) throws SQLException {
    KiemTraQuyen.manager(a);
    xuLyXoaLoai(a, id);
  }
  public void xoaSanPham(NguoiDung a, String id) throws SQLException {
    KiemTraQuyen.manager(a);
    xuLyXoaSanPham(a, id);
  }
  public void xoaKhachHang(NguoiDung a, String id) throws SQLException {
    KiemTraQuyen.actor(a);
    xuLyXoaKhachHang(a, id);
  }
  public void xoaNhaCungCap(NguoiDung a, String id) throws SQLException {
    KiemTraQuyen.manager(a);
    xuLyXoaNhaCungCap(a, id);
  }
  public void capNhatTrangThaiNhanVien(NguoiDung a, String id, String s) throws SQLException {
    KiemTraQuyen.manager(a);
    xuLyCapNhatTrangThaiNhanVien(a, id, s);
  }
  public void capNhatTrangThaiTaiKhoan(NguoiDung a, String id, String s) throws SQLException {
    KiemTraQuyen.manager(a);
    xuLyCapNhatTrangThaiTaiKhoan(a, id, s);
  }
  private void xuLyLuuNhanVien(NguoiDung a, NhanVien v) throws SQLException {
    if (v == null)
      throw new IllegalArgumentException("Thiếu nhân viên");
    v.setMaNhanVien(kiemTraMa(v.getMaNhanVien(), "Mã nhân viên", QuyTacKiemTra.EMPLOYEE_ID, "NV001"));
    v.setHoTen(kiemTraTen(v.getHoTen(), "Họ tên nhân viên"));
    v.setSoDienThoai(kiemTraSoDienThoaiVietNam(v.getSoDienThoai(), "Số điện thoại nhân viên"));
    v.setChucVu(khongBatBuoc(v.getChucVu(), "Chức vụ", 30));
    v.setTrangThai(kiemTraGiaTriEnum(v.getTrangThai(), TrangThaiNhanVien.class, "Trạng thái nhân viên"));
    try (GiaoDichPhien transaction = new GiaoDichPhien(a, true, true)) {
      Connection c = transaction.connection();
      if (!"DANG_LAM".equals(v.getTrangThai())) baoVeNhanVien(a, v.getMaNhanVien(), c);

      nhanVienDAO.luu(v, c);
      transaction.commit();
    }
  }
  private void xuLyLuuTaiKhoan(NguoiDung a, TaiKhoan v, char[] password) throws SQLException {
    if (v == null)
      throw new IllegalArgumentException("Thiếu tài khoản");
    String tenDangNhap = batBuoc(v.getTenDangNhap(), "Tên đăng nhập", 50);
    if (!QuyTacKiemTra.matches(tenDangNhap, QuyTacKiemTra.USERNAME))
      throw new IllegalArgumentException(
          "Tên đăng nhập chỉ gồm chữ cái, số, dấu _ . - và dài 3-50 ký tự");
    v.setTenDangNhap(tenDangNhap);
    v.setMaNhanVien(kiemTraMa(v.getMaNhanVien(), "Mã nhân viên", QuyTacKiemTra.EMPLOYEE_ID, "NV001"));
    v.setVaiTro(kiemTraGiaTriEnum(v.getVaiTro(), VaiTro.class, "Vai trò"));
    v.setTrangThai(kiemTraGiaTriEnum(v.getTrangThai(), TrangThaiTaiKhoan.class, "Trạng thái tài khoản"));
    try (GiaoDichPhien transaction = new GiaoDichPhien(a, true, true)) {
      Connection c = transaction.connection();
      TaiKhoan existing = taiKhoanDAO.timTheoMa(v.getTenDangNhap(), c);
      if (existing != null && (!"HOAT_DONG".equals(v.getTrangThai())
          || !existing.getVaiTro().equals(v.getVaiTro())
          || !existing.getMaNhanVien().equals(v.getMaNhanVien()))) baoVeTaiKhoan(a, v.getTenDangNhap(), c);

      TaiKhoan old = taiKhoanDAO.timTheoMa(v.getTenDangNhap(), c);
      if (password == null || password.length == 0) {
        if (old == null)
          throw new IllegalArgumentException("Mật khẩu tài khoản mới không được để trống");
        v.setMatKhauBam(old.getMatKhauBam());
      } else {
        if (password.length < 6)
          throw new IllegalArgumentException("Mật khẩu phải có ít nhất 6 ký tự");
        v.setMatKhauBam(new DichVuDangNhap().hash(password));
      }
      taiKhoanDAO.luu(v, c);
      transaction.commit();
    } finally {
      if (password != null)
        Arrays.fill(password, '\0');
    }
  }
  private void xuLyLuuLoai(NguoiDung a, LoaiSanPham v) throws SQLException {
    if (v == null)
      throw new IllegalArgumentException("Thiếu loại sản phẩm");
    v.setMaLoai(kiemTraMa(v.getMaLoai(), "Mã loại", QuyTacKiemTra.CATEGORY_ID, "LSP001"));
    v.setTenLoai(batBuoc(v.getTenLoai(), "Tên loại", 100));
    v.setMoTa(khongBatBuoc(v.getMoTa(), "Mô tả", 255));
    v.setTrangThai(kiemTraGiaTriEnum(v.getTrangThai(), TrangThaiSanPham.class, "Trạng thái loại"));
    try (GiaoDichPhien transaction = new GiaoDichPhien(a, true, false)) {
      Connection c = transaction.connection();
      loaiDAO.luu(v, c);
      transaction.commit();
    }
  }
  private void xuLyLuuSanPham(NguoiDung a, SanPham v) throws SQLException {
    if (v == null)
      throw new IllegalArgumentException("Thiếu sản phẩm");
    v.setMaSanPham(kiemTraMa(v.getMaSanPham(), "Mã sản phẩm", QuyTacKiemTra.PRODUCT_ID, "893000000001"));
    v.setMaLoai(kiemTraMa(v.getMaLoai(), "Mã loại", QuyTacKiemTra.CATEGORY_ID, "LSP001"));
    v.setTenSanPham(batBuoc(v.getTenSanPham(), "Tên sản phẩm", 150));
    v.setDonViTinh(batBuoc(v.getDonViTinh(), "Đơn vị tính", 30));
    if (v.getGiaBan() == null || v.getGiaBan().signum() < 0)
      throw new IllegalArgumentException("Giá bán không được âm");
    if (v.getNguongTon() < 0)
      throw new IllegalArgumentException("Ngưỡng tồn không được âm");
    v.setTrangThai(kiemTraGiaTriEnum(v.getTrangThai(), TrangThaiSanPham.class, "Trạng thái sản phẩm"));
    try (GiaoDichPhien transaction = new GiaoDichPhien(a, true, false)) {
      Connection c = transaction.connection();
      sanPhamDAO.luu(v, c);
      transaction.commit();
    }
  }
  private void xuLyLuuKhachHang(NguoiDung a, KhachHang v) throws SQLException {
    if (v == null)
      throw new IllegalArgumentException("Thiếu khách hàng");
    v.setMaKhachHang(kiemTraMa(v.getMaKhachHang(), "Mã khách hàng", QuyTacKiemTra.CUSTOMER_ID, "KH001"));
    v.setHoTen(kiemTraTen(v.getHoTen(), "Họ tên khách hàng"));
    v.setSoDienThoai(kiemTraSoDienThoaiVietNam(v.getSoDienThoai(), "Số điện thoại khách hàng"));
    v.setTrangThai(kiemTraGiaTriEnum(v.getTrangThai(), TrangThaiHoatDong.class, "Trạng thái khách hàng"));
    try (GiaoDichPhien transaction = new GiaoDichPhien(a, false, false)) {
      Connection c = transaction.connection();
      khachHangDAO.luu(v, c);
      transaction.commit();
    }
  }
  private void xuLyLuuNhaCungCap(NguoiDung a, NhaCungCap v) throws SQLException {
    if (v == null)
      throw new IllegalArgumentException("Thiếu nhà cung cấp");
    v.setMaNhaCungCap(kiemTraMa(v.getMaNhaCungCap(), "Mã nhà cung cấp", QuyTacKiemTra.SUPPLIER_ID, "NCC001"));
    v.setTenNhaCungCap(batBuoc(v.getTenNhaCungCap(), "Tên nhà cung cấp", 150));
    v.setSoDienThoai(kiemTraSoDienThoaiVietNam(v.getSoDienThoai(), "Số điện thoại nhà cung cấp"));
    v.setDiaChi(khongBatBuoc(v.getDiaChi(), "Địa chỉ", 255));
    v.setTrangThai(kiemTraGiaTriEnum(v.getTrangThai(), TrangThaiHoatDong.class, "Trạng thái nhà cung cấp"));
    try (GiaoDichPhien transaction = new GiaoDichPhien(a, true, false)) {
      Connection c = transaction.connection();
      nhaCungCapDAO.luu(v, c);
      transaction.commit();
    }
  }
  private List<NhanVien> timNhanVien(String tuKhoa, String trangThai, String chucVu)
      throws SQLException {
    try (Connection c = ConnectDB.open()) {
      return nhanVienDAO.timKiem(tuKhoa, trangThai, chucVu, c);
    }
  }
  private List<TaiKhoan> timTaiKhoan(String tuKhoa, String vaiTro, String trangThai)
      throws SQLException {
    try (Connection c = ConnectDB.open()) {
      return taiKhoanDAO.timKiem(tuKhoa, vaiTro, trangThai, c);
    }
  }
  private List<LoaiSanPham> timLoai(String tuKhoa) throws SQLException {
    try (Connection c = ConnectDB.open()) {
      return loaiDAO.timKiem(tuKhoa, c);
    }
  }
  private List<SanPham> timSanPham(String tuKhoa, String maLoai, String trangThai,
      BigDecimal tuGia, BigDecimal denGia) throws SQLException {
    try (Connection c = ConnectDB.open()) {
      return sanPhamDAO.timKiem(tuKhoa, maLoai, trangThai, tuGia, denGia, c);
    }
  }
  private List<KhachHang> timKhachHang(String tuKhoa, String trangThai) throws SQLException {
    try (Connection c = ConnectDB.open()) {
      return khachHangDAO.timKiem(tuKhoa, trangThai, c);
    }
  }
  private List<NhaCungCap> timNhaCungCap(String tuKhoa, String trangThai) throws SQLException {
    try (Connection c = ConnectDB.open()) {
      return nhaCungCapDAO.timKiem(tuKhoa, trangThai, c);
    }
  }
  private void xuLyXoaNhanVien(NguoiDung a, String id) throws SQLException {
    try (GiaoDichPhien transaction = new GiaoDichPhien(a, true, true)) {
      Connection c = transaction.connection();
      baoVeNhanVien(a, id, c);

      nhanVienDAO.xoa(id, c);
      transaction.commit();
    } catch (SQLException exception) {
      if (exception.getErrorCode() == 547)
        throw new IllegalStateException("Bản ghi đã được tham chiếu; hãy đổi trạng thái thay vì xóa.", exception);
      throw exception;
    }
  }
  private void xuLyXoaTaiKhoan(NguoiDung a, String id) throws SQLException {
    try (GiaoDichPhien transaction = new GiaoDichPhien(a, true, true)) {
      Connection c = transaction.connection();
      baoVeTaiKhoan(a, id, c);

      taiKhoanDAO.xoa(id, c);
      transaction.commit();
    } catch (SQLException exception) {
      if (exception.getErrorCode() == 547)
        throw new IllegalStateException("Bản ghi đã được tham chiếu; hãy đổi trạng thái thay vì xóa.", exception);
      throw exception;
    }
  }
  private void xuLyXoaLoai(NguoiDung a, String id) throws SQLException {
    try (GiaoDichPhien transaction = new GiaoDichPhien(a, true, false)) {
      Connection c = transaction.connection();
      loaiDAO.xoa(id, c);
      transaction.commit();
    } catch (SQLException exception) {
      if (exception.getErrorCode() == 547)
        throw new IllegalStateException("Bản ghi đã được tham chiếu; hãy đổi trạng thái thay vì xóa.", exception);
      throw exception;
    }
  }
  private void xuLyXoaSanPham(NguoiDung a, String id) throws SQLException {
    try (GiaoDichPhien transaction = new GiaoDichPhien(a, true, false)) {
      Connection c = transaction.connection();
      sanPhamDAO.xoa(id, c);
      transaction.commit();
    } catch (SQLException exception) {
      if (exception.getErrorCode() == 547)
        throw new IllegalStateException("Bản ghi đã được tham chiếu; hãy đổi trạng thái thay vì xóa.", exception);
      throw exception;
    }
  }
  private void xuLyXoaKhachHang(NguoiDung a, String id) throws SQLException {
    try (GiaoDichPhien transaction = new GiaoDichPhien(a, false, false)) {
      Connection c = transaction.connection();
      khachHangDAO.xoa(id, c);
      transaction.commit();
    } catch (SQLException exception) {
      if (exception.getErrorCode() == 547)
        throw new IllegalStateException("Bản ghi đã được tham chiếu; hãy đổi trạng thái thay vì xóa.", exception);
      throw exception;
    }
  }
  private void xuLyXoaNhaCungCap(NguoiDung a, String id) throws SQLException {
    try (GiaoDichPhien transaction = new GiaoDichPhien(a, true, false)) {
      Connection c = transaction.connection();
      nhaCungCapDAO.xoa(id, c);
      transaction.commit();
    } catch (SQLException exception) {
      if (exception.getErrorCode() == 547)
        throw new IllegalStateException("Bản ghi đã được tham chiếu; hãy đổi trạng thái thay vì xóa.", exception);
      throw exception;
    }
  }
  private void xuLyCapNhatTrangThaiNhanVien(NguoiDung a, String maNhanVien, String trangThai) throws SQLException {
    if (!KiemTraQuyen.is(trangThai, TrangThaiNhanVien.class))
      throw new IllegalArgumentException("Trạng thái nhân viên không hợp lệ");
    try (GiaoDichPhien transaction = new GiaoDichPhien(a, true, true)) {
      Connection c = transaction.connection();
      if (!"DANG_LAM".equals(trangThai)) baoVeNhanVien(a, maNhanVien, c);

      nhanVienDAO.capNhatTrangThai(maNhanVien, trangThai, c);
      transaction.commit();
    }
  }
  private void xuLyCapNhatTrangThaiTaiKhoan(NguoiDung a, String tenDangNhap, String trangThai) throws SQLException {
    if (!KiemTraQuyen.is(trangThai, TrangThaiTaiKhoan.class))
      throw new IllegalArgumentException("Trạng thái tài khoản không hợp lệ");
    try (GiaoDichPhien transaction = new GiaoDichPhien(a, true, true)) {
      Connection c = transaction.connection();
      if (!"HOAT_DONG".equals(trangThai)) baoVeTaiKhoan(a, tenDangNhap, c);

      taiKhoanDAO.capNhatTrangThai(tenDangNhap, trangThai, c);
      transaction.commit();
    }
  }
  private void baoVeTaiKhoan(NguoiDung nguoiDung, String tenDangNhap, Connection connection) throws SQLException {
    if (nguoiDung.getTenDangNhap().equals(tenDangNhap))
      throw new IllegalStateException("Không thể xóa, khóa hoặc hạ quyền tài khoản đang đăng nhập");
    TaiKhoan taiKhoan = taiKhoanDAO.timTheoMa(tenDangNhap, connection);
    if (taiKhoan != null && "QUAN_LY".equals(taiKhoan.getVaiTro())
        && "HOAT_DONG".equals(taiKhoan.getTrangThai())
        && "DANG_LAM".equals(taiKhoan.getTrangThaiNhanVien())) {
      if (taiKhoanDAO.demQuanLyHoatDong(connection) <= 1)
        throw new IllegalStateException("Phải giữ ít nhất một quản lý đang hoạt động");
    }
  }

  private void baoVeNhanVien(NguoiDung nguoiDung, String maNhanVien, Connection connection) throws SQLException {
    if (nguoiDung.getMaNhanVien().equals(maNhanVien))
      throw new IllegalStateException("Không thể xóa hoặc cho nghỉ việc nhân viên đang đăng nhập");
    for (TaiKhoan taiKhoan : taiKhoanDAO.timKiem(maNhanVien, null, null, connection))
      if (maNhanVien.equals(taiKhoan.getMaNhanVien())) baoVeTaiKhoan(nguoiDung, taiKhoan.getTenDangNhap(), connection);
  }

  private String hangThanhVien(long diem) {
    return diem >= 1000000 ? HangThanhVien.VANG.name()
        : diem >= 200000   ? HangThanhVien.BAC.name()
        : diem >= 50000    ? HangThanhVien.DONG.name()
                             : HangThanhVien.CHUA_XEP_HANG.name();
  }
  private String batBuoc(String value, String label, int maximum) {
    String trimmed = KiemTraQuyen.blankToNull(value);
    if (trimmed == null)
      throw new IllegalArgumentException(label + " không được để trống");
    if (trimmed.length() > maximum)
      throw new IllegalArgumentException(label + " quá dài");
    return trimmed;
  }
  private String khongBatBuoc(String value, String label, int maximum) {
    String trimmed = KiemTraQuyen.blankToNull(value);
    if (trimmed != null && trimmed.length() > maximum)
      throw new IllegalArgumentException(label + " quá dài");
    return trimmed;
  }
  private String kiemTraMa(String value, String label, String regex, String example) {
    String trimmed = batBuoc(value, label, 20);
    if (!QuyTacKiemTra.matches(trimmed, regex))
      throw new IllegalArgumentException(label + " sai định dạng, phải có dạng " + example);
    return trimmed;
  }
  private String kiemTraSoDienThoai(String value, String label) {
    String trimmed = khongBatBuoc(value, label, 20);
    if (trimmed != null && !trimmed.matches(QuyTacKiemTra.PHONE))
      throw new IllegalArgumentException(label + " không hợp lệ, chỉ gồm số, dấu +, khoảng trắng, (), . và -");
    return trimmed;
  }
  private String kiemTraSoDienThoaiVietNam(String value, String label) {
    String trimmed = khongBatBuoc(value, label, 20);
    if (trimmed != null && !QuyTacKiemTra.matches(trimmed, QuyTacKiemTra.VN_PHONE))
      throw new IllegalArgumentException(label + " sai định dạng, phải có dạng 0xxxxxxxxx (10 số, đầu số 3/5/7/8/9, ví dụ: 0901234567)");
    return trimmed;
  }
  private String kiemTraTen(String value, String label) {
    String trimmed = batBuoc(value, label, 100);
    if (!QuyTacKiemTra.matches(trimmed, QuyTacKiemTra.PERSON_NAME))
      throw new IllegalArgumentException(label + " sai định dạng, chỉ được chứa chữ cái và khoảng trắng (ví dụ: Nguyen Van A)");
    return trimmed;
  }
  private <E extends Enum<E>> String kiemTraGiaTriEnum(String value, Class<E> type, String label) {
    String trimmed = batBuoc(value, label, 20);
    if (!KiemTraQuyen.is(trimmed, type))
      throw new IllegalArgumentException(label + " không hợp lệ");
    return trimmed;
  }
}
