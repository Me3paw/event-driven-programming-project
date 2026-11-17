package service;

import connectDB.ConnectDB;
import dao.CauHinhCuaHang_DAO;
import dao.HoaDon_DAO;
import dao.KhachHang_DAO;
import dao.KhuyenMai_DAO;
import dao.LoTonKho_DAO;
import dao.SanPham_DAO;
import entity.CauHinhCuaHang;
import entity.ChiTietHoaDon;
import entity.HoaDon;
import entity.KhachHang;
import entity.KhuyenMai;
import entity.LoTonKho;
import entity.LoaiGiamGia;
import entity.PhamViKhuyenMai;
import entity.PhanBoXuatLo;
import entity.PhuongThucThanhToan;
import entity.SanPham;
import entity.TrangThaiHoaDon;
import entity.TrangThaiHoatDong;
import entity.TrangThaiSanPham;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class DichVuBanHang {
  public static final String KHACH_LE = "KH000";
  private static final BigDecimal HUNDRED = new BigDecimal("100");
  private static final int MAX_CHECKOUT_ATTEMPTS = 3;
  private final SanPham_DAO sanPhamDAO = new SanPham_DAO();
  private final KhachHang_DAO khachHangDAO = new KhachHang_DAO();
  private final KhuyenMai_DAO khuyenMaiDAO = new KhuyenMai_DAO();
  private final CauHinhCuaHang_DAO cauHinhDAO = new CauHinhCuaHang_DAO();
  private final LoTonKho_DAO loTonKhoDAO = new LoTonKho_DAO();
  private final HoaDon_DAO hoaDonDAO = new HoaDon_DAO();
  private final DichVuDangNhap dichVuDangNhap = new DichVuDangNhap();

  public BaoGia baoGia(NguoiDung nguoiDung, YeuCauThanhToan yeuCau) throws SQLException {
    KiemTraQuyen.actor(nguoiDung);
    if (yeuCau == null) throw new IllegalArgumentException("Thiếu yêu cầu thanh toán");
    YeuCauThanhToan banSaoYeuCau = yeuCau.copy();
    banSaoYeuCau.setMaNhanVien(nguoiDung.getMaNhanVien());
    return baoGia(banSaoYeuCau);
  }

  public String maHoaDonTiepTheo(NguoiDung nguoiDung) throws SQLException {
    KiemTraQuyen.actor(nguoiDung);
    try (GiaoDichPhien transaction = new GiaoDichPhien(nguoiDung, false, false)) {
      String id = dao.Id_DAO.taoMaTiepTheo("HD", transaction.connection());
      transaction.commit();
      return id;
    }
  }

  public HoaDon thanhToan(NguoiDung nguoiDung, YeuCauThanhToan yeuCau) throws SQLException {
    KiemTraQuyen.actor(nguoiDung);
    if (yeuCau == null) throw new IllegalArgumentException("Thiếu yêu cầu thanh toán");
    YeuCauThanhToan banSaoYeuCau;
    synchronized (yeuCau) {
      String maHoaDon = KiemTraQuyen.blankToNull(yeuCau.getMaHoaDon());
      yeuCau.setMaHoaDon(maHoaDon == null ? maHoaDonTiepTheo(nguoiDung) : maHoaDon);
      banSaoYeuCau = yeuCau.copy();
    }
    kiemTraMaHoaDon(banSaoYeuCau.getMaHoaDon());
    banSaoYeuCau.setMaNhanVien(nguoiDung.getMaNhanVien());
    String dauVanTay = DauVanThanhToan.of(nguoiDung, banSaoYeuCau);
    boolean chuaRoKetQua = false;
    for (int lanThu = 1;; lanThu++) {
      try {
        return xuLyThanhToan(nguoiDung, banSaoYeuCau, dauVanTay);
      } catch (SQLException exception) {
        boolean ketQuaChuaRo = chuaRoKetQua || loiKetNoi(exception);
        chuaRoKetQua = ketQuaChuaRo;
        if (lanThu >= MAX_CHECKOUT_ATTEMPTS || (!ketQuaChuaRo && !loiTamThoi(exception))) {
          if (ketQuaChuaRo) throw new UnknownCheckoutStatusException(banSaoYeuCau.getMaHoaDon(), exception);
          throw exception;
        }
      } catch (RuntimeException exception) {
        if (chuaRoKetQua)
          throw new UnknownCheckoutStatusException(banSaoYeuCau.getMaHoaDon(), new SQLException(exception));
        throw exception;
      }
    }
  }

  private void kiemTraMaHoaDon(String id) {
    if (!QuyTacKiemTra.matches(id, QuyTacKiemTra.INVOICE_ID))
      throw new IllegalArgumentException("Mã hóa đơn phải là HD và đúng 13 chữ số (ví dụ: HD0000000000001)");
  }

  private boolean loiKetNoi(SQLException exception) {
    for (SQLException error = exception; error != null; error = error.getNextException())
      if (error.getSQLState() != null && error.getSQLState().startsWith("08")) return true;
    return false;
  }

  public List<KhuyenMai> khuyenMaiHieuLuc(NguoiDung nguoiDung, Set<String> danhSachMaSanPham, LocalDateTime at)
      throws SQLException {
    return khuyenMaiHieuLuc(nguoiDung, danhSachMaSanPham, null, at);
  }

  public List<KhuyenMai> khuyenMaiHieuLuc(NguoiDung nguoiDung, Set<String> danhSachMaSanPham, BigDecimal tongTienHang,
      LocalDateTime at) throws SQLException {
    KiemTraQuyen.actor(nguoiDung);
    Set<String> ids = new LinkedHashSet<String>();
    if (danhSachMaSanPham != null)
      for (String id : danhSachMaSanPham)
        if (hopLe(id))
          ids.add(id.trim());
    try (Connection connection = ConnectDB.open()) {
      List<KhuyenMai> danhSachKhuyenMai =
          khuyenMaiDAO.timHieuLucTheoSanPham(ids, at == null ? LocalDateTime.now() : at, connection);
      if (tongTienHang == null)
        return danhSachKhuyenMai;
      List<KhuyenMai> duDieuKien = new java.util.ArrayList<KhuyenMai>();
      for (KhuyenMai khuyenMai : danhSachKhuyenMai)
        if (!PhamViKhuyenMai.TOAN_DON.name().equals(khuyenMai.getPhamVi())
            || khuyenMai.getDonToiThieu() == null
            || tongTienHang.compareTo(khuyenMai.getDonToiThieu()) >= 0)
          duDieuKien.add(khuyenMai);
      return duDieuKien;
    }
  }

  public void huyHoaDon(NguoiDung nguoiDung, String maHoaDon, String lyDo, String tenDangNhapQuanLy,
      char[] matKhauQuanLy) throws SQLException {
    KiemTraQuyen.manager(nguoiDung);
    xuLyHuyHoaDon(nguoiDung, maHoaDon, lyDo, tenDangNhapQuanLy, matKhauQuanLy);
  }

  public List<HoaDon> timHoaDon(NguoiDung nguoiDung, String tuKhoa, String maNhanVien, String trangThai)
      throws SQLException {
    return timHoaDon(nguoiDung, tuKhoa, maNhanVien, null, trangThai, null, null, null);
  }
  public List<HoaDon> timHoaDon(NguoiDung nguoiDung, String tuKhoa, String maNhanVien, String maKhachHang,
      String trangThai, String phuongThuc, LocalDate from, LocalDate to) throws SQLException {
    KiemTraQuyen.actor(nguoiDung);
    if (vuotDoDai(tuKhoa, 100) || vuotDoDai(maNhanVien, 30) || vuotDoDai(maKhachHang, 30))
      throw new IllegalArgumentException("Thông tin tìm kiếm vượt quá độ dài cho phép");
    if (from != null && to != null && from.isAfter(to))
      throw new IllegalArgumentException("Khoảng ngày không hợp lệ");
    String status = KiemTraQuyen.blankToNull(trangThai);
    String phuongThucThanhToan = KiemTraQuyen.blankToNull(phuongThuc);
    if (status != null && !KiemTraQuyen.is(status, TrangThaiHoaDon.class))
      throw new IllegalArgumentException("Trạng thái hóa đơn không hợp lệ");
    if (phuongThucThanhToan != null && !KiemTraQuyen.is(phuongThucThanhToan, PhuongThucThanhToan.class))
      throw new IllegalArgumentException("Phương thức thanh toán không hợp lệ");
    return timHoaDon(KiemTraQuyen.blankToNull(tuKhoa),
        nguoiDung.isQuanLy() ? KiemTraQuyen.blankToNull(maNhanVien) : nguoiDung.getMaNhanVien(),
        KiemTraQuyen.blankToNull(maKhachHang), status, phuongThucThanhToan, from, to);
  }
  private boolean vuotDoDai(String value, int max) {
    return value != null && value.trim().length() > max;
  }

  public HoaDon chiTietHoaDon(NguoiDung nguoiDung, String maHoaDon) throws SQLException {
    KiemTraQuyen.actor(nguoiDung);
    HoaDon hoaDon = chiTietHoaDon(maHoaDon);
    if (hoaDon == null
        || (!nguoiDung.isQuanLy() && !nguoiDung.getMaNhanVien().equals(hoaDon.getMaNhanVien())))
      throw new SecurityException("Không có quyền xem hóa đơn");
    return hoaDon;
  }

  private BaoGia baoGia(YeuCauThanhToan yeuCau) throws SQLException {
    try (Connection connection = ConnectDB.open()) {
      return new BaoGia(tinh(yeuCau, connection, true, false));
    }
  }

  private HoaDon xuLyThanhToan(NguoiDung nguoiDung, YeuCauThanhToan yeuCau, String dauVanTay) throws SQLException {
    try (GiaoDichPhien transaction = new GiaoDichPhien(nguoiDung, false, false)) {
      Connection connection = transaction.connection();
      KiemTraQuyen.lock(connection, "POS_CHECKOUT_" + yeuCau.getMaHoaDon(), "Exclusive");
      HoaDon hoaDonDaCo = hoaDonDAO.timThanhToanTrung(yeuCau.getMaHoaDon(), nguoiDung.getTenDangNhap(),
          dauVanTay, connection);
      if (hoaDonDaCo != null) {
        transaction.commit();
        return hoaDonDaCo;
      }
      HoaDon hoaDon = tinh(yeuCau, connection, true, true);
      hoaDonDAO.luu(hoaDon, connection, nguoiDung.getTenDangNhap(), dauVanTay);
      for (PhanBoXuatLo phanBo : hoaDon.getPhanBo())
        loTonKhoDAO.thayDoiSoLuong(phanBo.getMaLo(), -phanBo.getSoLuongXuat(), connection);
      if (hoaDon.getMaKhachHang() != null)
        khachHangDAO.capNhatDiem(hoaDon.getMaKhachHang(),
            hoaDon.getDiemThuongKiem() - hoaDon.getDiemSuDung(), hoaDon.getDiemTichLuyKiem(), connection);
      try {
        transaction.commit();
      } catch (SQLException exception) {
        // A failed commit response can follow a successful database commit. Never allocate a new ID.
        throw new UnknownCheckoutStatusException(yeuCau.getMaHoaDon(), exception);
      }
      return hoaDon;
    }
  }

  private void xuLyHuyHoaDon(NguoiDung nguoiDung, String maHoaDon, String lyDo, String tenDangNhapQuanLy,
      char[] matKhauQuanLy) throws SQLException {
    if (!hopLe(maHoaDon) || maHoaDon.trim().length() > 30)
      throw new IllegalArgumentException("Mã hóa đơn không hợp lệ");
    if (lyDo == null || lyDo.trim().isEmpty() || lyDo.trim().length() > 255)
      throw new IllegalArgumentException("Lý do hủy phải từ 1 đến 255 ký tự");
    try (GiaoDichPhien transaction = new GiaoDichPhien(nguoiDung, true, false)) {
      Connection connection = transaction.connection();
      try {
        entity.TaiKhoan quanLy =
            dichVuDangNhap.xacThucQuanLy(tenDangNhapQuanLy, matKhauQuanLy, connection);
        HoaDon hoaDon = hoaDonDAO.khoa(maHoaDon, connection);
        if (hoaDon == null || !TrangThaiHoaDon.DA_THANH_TOAN.name().equals(hoaDon.getTrangThai()))
          throw new IllegalStateException("Hóa đơn không thể hủy");
        if (hoaDon.getMaKhachHang() != null && !KHACH_LE.equals(hoaDon.getMaKhachHang()))
          khachHangDAO.khoa(hoaDon.getMaKhachHang(), connection);
        for (PhanBoXuatLo phanBo : hoaDonDAO.danhSachPhanBo(maHoaDon, connection))
          loTonKhoDAO.thayDoiSoLuong(phanBo.getMaLo(), phanBo.getSoLuongXuat(), connection);
        if (hoaDon.getMaKhachHang() != null && !KHACH_LE.equals(hoaDon.getMaKhachHang()))
          khachHangDAO.capNhatDiem(hoaDon.getMaKhachHang(), hoaDon.getDiemSuDung(), 0, connection);
        hoaDonDAO.huy(maHoaDon, quanLy.getMaNhanVien(), lyDo.trim(), connection);
        transaction.commit();
      } catch (RuntimeException | SQLException exception) {
        connection.rollback();
        throw exception;
      }
    }
  }

  public ThongKeCa thongKeCa(NguoiDung nguoiDung) throws SQLException {
    KiemTraQuyen.actor(nguoiDung);
    try (Connection connection = ConnectDB.open()) {
      return hoaDonDAO.thongKeCa(nguoiDung.getMaNhanVien(), LocalDate.now(), connection);
    }
  }

  public int tonKho(NguoiDung nguoiDung, String maSanPham) throws SQLException {
    KiemTraQuyen.actor(nguoiDung);
    if (!hopLe(maSanPham))
      throw new IllegalArgumentException("Mã sản phẩm không hợp lệ");
    try (Connection connection = ConnectDB.open()) {
      return loTonKhoDAO.ton(maSanPham.trim(), connection);
    }
  }

  private List<HoaDon> timHoaDon(String tuKhoa, String maNhanVien, String maKhachHang,
      String trangThai, String phuongThuc, LocalDate from, LocalDate to) throws SQLException {
    try (Connection connection = ConnectDB.open()) {
      return hoaDonDAO.timKiem(
          tuKhoa, maNhanVien, maKhachHang, trangThai, phuongThuc, from, to, connection);
    }
  }
  private HoaDon chiTietHoaDon(String maHoaDon) throws SQLException {
    try (Connection connection = ConnectDB.open()) {
      return hoaDonDAO.chiTiet(maHoaDon, connection);
    }
  }

  private HoaDon tinh(YeuCauThanhToan yeuCau, Connection connection, boolean lock,
      boolean kiemTraThanhToan)
      throws SQLException {
    if (yeuCau == null || yeuCau.getDongHang() == null || yeuCau.getDongHang().isEmpty())
      throw new IllegalArgumentException("Hóa đơn phải có hàng");
    HoaDon hoaDon = new HoaDon();
    String maHoaDonYeuCau = KiemTraQuyen.blankToNull(yeuCau.getMaHoaDon());
    if (maHoaDonYeuCau != null) kiemTraMaHoaDon(maHoaDonYeuCau);
    hoaDon.setMaHoaDon(maHoaDonYeuCau);
    hoaDon.setMaNhanVien(yeuCau.getMaNhanVien());
    String maKhachHang = rongThanhNull(yeuCau.getMaKhachHang());
    if (KHACH_LE.equals(maKhachHang))
      maKhachHang = null;
    hoaDon.setMaKhachHang(maKhachHang);
    hoaDon.setThoiDiemLap(LocalDateTime.now());
    hoaDon.setTrangThai(TrangThaiHoaDon.DA_THANH_TOAN.name());
    KhachHang khachHang = null;
    if (maKhachHang != null) {
      khachHang = lock ? khachHangDAO.khoa(hoaDon.getMaKhachHang(), connection) : null;
      if (lock
          && (khachHang == null
              || !TrangThaiHoatDong.HOAT_DONG.name().equals(khachHang.getTrangThai())))
        throw new IllegalArgumentException("Khách hàng không hợp lệ");
    }
    Set<String> seen = new HashSet<String>();
    for (DongThanhToan dongHang : yeuCau.getDongHang()) {
      if (dongHang == null || !hopLe(dongHang.getMaSanPham()) || dongHang.getSoLuong() <= 0
          || !seen.add(dongHang.getMaSanPham()))
        throw new IllegalArgumentException("Dòng hàng không hợp lệ");
      SanPham sanPham = sanPhamDAO.timTheoMa(dongHang.getMaSanPham(), connection);
      if (sanPham == null
          || !TrangThaiSanPham.DANG_KINH_DOANH.name().equals(sanPham.getTrangThai()))
        throw new IllegalArgumentException("Sản phẩm không kinh doanh");
      ChiTietHoaDon chiTiet = new ChiTietHoaDon();
      chiTiet.setMaSanPham(sanPham.getMaSanPham());
      chiTiet.setTenSanPham(sanPham.getTenSanPham());
      chiTiet.setSoLuong(dongHang.getSoLuong());
      chiTiet.setDonGiaBan(sanPham.getGiaBan());
      chiTiet.setTienGiamDong(BigDecimal.ZERO);
      chiTiet.setThanhTien(sanPham.getGiaBan().multiply(BigDecimal.valueOf(dongHang.getSoLuong())));
      hoaDon.getChiTiet().add(chiTiet);
    }
    BigDecimal tongGiaGoc = BigDecimal.ZERO;
    for (ChiTietHoaDon chiTiet : hoaDon.getChiTiet()) tongGiaGoc = tongGiaGoc.add(chiTiet.getThanhTien());
    hoaDon.setTongGiaGoc(tongGiaGoc);
    KhuyenMai khuyenMai = null;
    String loaiGiamGia =
        yeuCau.getLoaiGiamGia() == null ? LoaiGiamGia.KHONG.name() : yeuCau.getLoaiGiamGia();
    if (!KiemTraQuyen.is(loaiGiamGia, LoaiGiamGia.class))
      throw new IllegalArgumentException("Loại giảm giá không hợp lệ");
    if (LoaiGiamGia.KHUYEN_MAI.name().equals(loaiGiamGia)) {
      if (!hopLe(yeuCau.getMaKhuyenMai()))
        throw new IllegalArgumentException("Thiếu khuyến mãi");
      khuyenMai =
          khuyenMaiDAO.timHieuLuc(yeuCau.getMaKhuyenMai(), hoaDon.getThoiDiemLap(), connection);
      if (khuyenMai == null)
        throw new IllegalArgumentException("Khuyến mãi không hiệu lực");
      hoaDon.setMaKhuyenMai(khuyenMai.getMaKhuyenMai());
    }
    if (LoaiGiamGia.HANG_THANH_VIEN.name().equals(loaiGiamGia) && khachHang == null && lock)
      throw new IllegalArgumentException("Giảm hạng cần khách hàng");
    BigDecimal rate = tyLeGiamGia(loaiGiamGia, khachHang, khuyenMai);
    hoaDon.setLoaiGiamGia(loaiGiamGia);
    hoaDon.setTyLeGiam(rate);
    BigDecimal tongTienDuDieuKien = BigDecimal.ZERO;
    int soDongDuDieuKien = 0;
    for (ChiTietHoaDon chiTiet : hoaDon.getChiTiet()) {
      boolean duDieuKien = LoaiGiamGia.HANG_THANH_VIEN.name().equals(loaiGiamGia)
          || (LoaiGiamGia.KHUYEN_MAI.name().equals(loaiGiamGia)
              && (PhamViKhuyenMai.TOAN_DON.name().equals(khuyenMai.getPhamVi())
                  || khuyenMai.getMaSanPham().contains(chiTiet.getMaSanPham())));
      if (duDieuKien) {
        tongTienDuDieuKien = tongTienDuDieuKien.add(chiTiet.getThanhTien());
        soDongDuDieuKien++;
      }
    }
    if (LoaiGiamGia.KHUYEN_MAI.name().equals(loaiGiamGia) && soDongDuDieuKien == 0)
      throw new IllegalArgumentException("Khuyến mãi không áp dụng cho giỏ hàng");
    if (LoaiGiamGia.KHUYEN_MAI.name().equals(loaiGiamGia)
        && PhamViKhuyenMai.TOAN_DON.name().equals(khuyenMai.getPhamVi())
        && khuyenMai.getDonToiThieu() != null
        && tongTienDuDieuKien.compareTo(khuyenMai.getDonToiThieu()) < 0)
      throw new IllegalArgumentException("Đơn chưa đạt giá trị tối thiểu " + khuyenMai.getDonToiThieu()
          + " của khuyến mãi " + khuyenMai.getMaKhuyenMai());
    BigDecimal tienGiam =
        tongTienDuDieuKien.multiply(rate).divide(HUNDRED, 0, RoundingMode.HALF_UP);
    BigDecimal daPhanBo = BigDecimal.ZERO;
    for (ChiTietHoaDon chiTiet : hoaDon.getChiTiet()) {
      boolean duDieuKien = LoaiGiamGia.HANG_THANH_VIEN.name().equals(loaiGiamGia)
          || (LoaiGiamGia.KHUYEN_MAI.name().equals(loaiGiamGia)
              && (PhamViKhuyenMai.TOAN_DON.name().equals(khuyenMai.getPhamVi())
                  || khuyenMai.getMaSanPham().contains(chiTiet.getMaSanPham())));
      BigDecimal tienGiamDong = duDieuKien && tongTienDuDieuKien.signum() != 0
          ? tienGiam.multiply(chiTiet.getThanhTien()).divide(tongTienDuDieuKien, 0, RoundingMode.DOWN)
          : BigDecimal.ZERO;
      chiTiet.setTienGiamDong(tienGiamDong);
      daPhanBo = daPhanBo.add(tienGiamDong);
    }
    int soDu = tienGiam.subtract(daPhanBo).intValueExact();
    for (ChiTietHoaDon chiTiet : hoaDon.getChiTiet())
      if (soDu > 0 && chiTiet.getThanhTien().signum() > 0
          && (LoaiGiamGia.HANG_THANH_VIEN.name().equals(loaiGiamGia)
              || (LoaiGiamGia.KHUYEN_MAI.name().equals(loaiGiamGia)
                  && (PhamViKhuyenMai.TOAN_DON.name().equals(khuyenMai.getPhamVi())
                      || khuyenMai.getMaSanPham().contains(chiTiet.getMaSanPham()))))) {
        chiTiet.setTienGiamDong(chiTiet.getTienGiamDong().add(BigDecimal.ONE));
        soDu--;
      }
    for (ChiTietHoaDon chiTiet : hoaDon.getChiTiet())
      chiTiet.setThanhTien(chiTiet.getThanhTien().subtract(chiTiet.getTienGiamDong()));
    hoaDon.setTienGiam(tienGiam);
    long diemSuDung = yeuCau.getDiemThuongSuDung();
    BigDecimal tienSauGiam = tongGiaGoc.subtract(tienGiam);
    if (diemSuDung < 0
        || BigDecimal.valueOf(diemSuDung).compareTo(tienSauGiam) > 0
        || (lock && khachHang != null && diemSuDung > khachHang.getDiemHienCo())
        || (diemSuDung > 0 && khachHang == null && lock))
      throw new IllegalArgumentException("Điểm thưởng phải là số nguyên không âm, không vượt số dư hoặc tiền sau giảm giá");
    hoaDon.setDiemSuDung(diemSuDung);
    hoaDon.setTienGiamDiem(BigDecimal.valueOf(diemSuDung));
    CauHinhCuaHang cauHinh = cauHinhDAO.layCauHinh(connection);
    BigDecimal tienTinhThue = tienSauGiam.subtract(BigDecimal.valueOf(diemSuDung)).max(BigDecimal.ZERO);
    BigDecimal tyLeVat = cauHinh.getTyLeVat();
    hoaDon.setTyLeVat(tyLeVat);
    hoaDon.setTienVat(tienTinhThue.multiply(tyLeVat).divide(HUNDRED, 0, RoundingMode.HALF_UP));
    hoaDon.setTongThanhToan(tienTinhThue.add(hoaDon.getTienVat()));
    hoaDon.setDiemTichLuyKiem(khachHang == null ? 0 : tongGiaGoc.longValueExact());
    hoaDon.setDiemThuongKiem(khachHang == null ? 0
                                              : tongGiaGoc.multiply(new BigDecimal("0.05"))
                                                    .setScale(0, RoundingMode.FLOOR)
                                                    .longValueExact());
    if (kiemTraThanhToan)
      kiemTraPhuongThucThanhToan(yeuCau, hoaDon);
    else {
      hoaDon.setPhuongThucTt(yeuCau.getPhuongThucThanhToan());
      if (PhuongThucThanhToan.TIEN_MAT.name().equals(yeuCau.getPhuongThucThanhToan())
          && yeuCau.getTienKhachDua() != null) {
        hoaDon.setTienKhachDua(yeuCau.getTienKhachDua());
        hoaDon.setTienThua(yeuCau.getTienKhachDua().subtract(hoaDon.getTongThanhToan())
            .max(BigDecimal.ZERO));
      } else {
        hoaDon.setTienThua(BigDecimal.ZERO);
      }
    }
    if (lock)
      phanBoLo(hoaDon, connection);
    return hoaDon;
  }

  private void phanBoLo(HoaDon hoaDon, Connection connection) throws SQLException {
    for (ChiTietHoaDon chiTiet : hoaDon.getChiTiet()) {
      int soLuongConLai = chiTiet.getSoLuong();
      List<LoTonKho> danhSachLo = loTonKhoDAO.khoaTheoFifo(chiTiet.getMaSanPham(), connection);
      for (LoTonKho lo : danhSachLo) {
        int soLuong = Math.min(soLuongConLai, lo.getSoLuongCon());
        if (soLuong > 0) {
          PhanBoXuatLo p = new PhanBoXuatLo();
          p.setMaSanPham(chiTiet.getMaSanPham());
          p.setMaLo(lo.getMaLo());
          p.setSoLuongXuat(soLuong);
          p.setDonGiaVon(lo.getDonGiaVon());
          hoaDon.getPhanBo().add(p);
          soLuongConLai -= soLuong;
        }
      }
      if (soLuongConLai != 0)
        throw new IllegalStateException("Không đủ tồn kho");
    }
  }
  private void kiemTraPhuongThucThanhToan(YeuCauThanhToan yeuCau, HoaDon hoaDon) {
    String phuongThuc = yeuCau.getPhuongThucThanhToan();
    if (!KiemTraQuyen.is(phuongThuc, PhuongThucThanhToan.class))
      throw new IllegalArgumentException("Phương thức thanh toán không hợp lệ");
    hoaDon.setPhuongThucTt(phuongThuc);
    if (PhuongThucThanhToan.TIEN_MAT.name().equals(phuongThuc)) {
      if (yeuCau.getTienKhachDua() == null
          || yeuCau.getTienKhachDua().compareTo(hoaDon.getTongThanhToan()) < 0)
        throw new IllegalArgumentException("Tiền khách đưa chưa đủ");
      hoaDon.setTienKhachDua(yeuCau.getTienKhachDua());
      hoaDon.setTienThua(yeuCau.getTienKhachDua().subtract(hoaDon.getTongThanhToan()));
    } else {
      if (!yeuCau.isThanhToanDienTuThanhCong() || !hopLe(yeuCau.getMaThamChieuThanhToan()))
        throw new IllegalArgumentException("Thanh toán điện tử thất bại");
      hoaDon.setMaThamChieuTt(yeuCau.getMaThamChieuThanhToan());
    }
  }
  private BigDecimal tyLeGiamGia(String type, KhachHang c, KhuyenMai p) {
    if (LoaiGiamGia.KHUYEN_MAI.name().equals(type))
      return p.getTyLeGiam();
    if (!LoaiGiamGia.HANG_THANH_VIEN.name().equals(type) || c == null)
      return BigDecimal.ZERO;
    long diemTichLuy = c.getDiemTichLuy();
    return diemTichLuy >= 1000000 ? new BigDecimal("10")
        : diemTichLuy >= 200000   ? new BigDecimal("5")
        : diemTichLuy >= 50000    ? new BigDecimal("3")
                             : BigDecimal.ZERO;
  }
  private boolean loiTamThoi(SQLException error) {
    for (SQLException e = error; e != null; e = e.getNextException()) {
      int code = e.getErrorCode();
      String state = e.getSQLState();
      if (code == 1020 || code == 1205 || code == 1213 || "40001".equals(state)
          || "41000".equals(state))
        return true;
    }
    return false;
  }
  private boolean hopLe(String value) {
    return value != null && !value.trim().isEmpty();
  }
  private String rongThanhNull(String value) {
    return hopLe(value) ? value : null;
  }
}
