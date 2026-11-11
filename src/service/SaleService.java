package service;

import connectDB.DBConnection;
import dao.CauHinhCuaHangDAO;
import dao.HoaDonDAO;
import dao.KhachHangDAO;
import dao.KhuyenMaiDAO;
import dao.LoTonKhoDAO;
import dao.SanPhamDAO;
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

public class SaleService {
  public static final String KHACH_LE = "KH000";
  private static final BigDecimal HUNDRED = new BigDecimal("100");
  private static final long DIEM_SU_DUNG_BUOC = 1000;
  private static final int MAX_CHECKOUT_ATTEMPTS = 3;
  private final SanPhamDAO sanPhamDAO = new SanPhamDAO();
  private final KhachHangDAO khachHangDAO = new KhachHangDAO();
  private final KhuyenMaiDAO khuyenMaiDAO = new KhuyenMaiDAO();
  private final CauHinhCuaHangDAO cauHinhDAO = new CauHinhCuaHangDAO();
  private final LoTonKhoDAO loTonKhoDAO = new LoTonKhoDAO();
  private final HoaDonDAO hoaDonDAO = new HoaDonDAO();
  private final AuthService authService = new AuthService();

  public BaoGia baoGia(Actor actor, YeuCauThanhToan request) throws SQLException {
    Access.actor(actor);
    request.setMaNhanVien(actor.getMaNhanVien());
    return baoGia(request);
  }

  public HoaDon thanhToan(Actor actor, YeuCauThanhToan request) throws SQLException {
    Access.actor(actor);
    request.setMaNhanVien(actor.getMaNhanVien());
    return thanhToan(request);
  }

  public List<KhuyenMai> activePromotions(Actor actor, Set<String> productIds, LocalDateTime at)
      throws SQLException {
    return activePromotions(actor, productIds, null, at);
  }

  public List<KhuyenMai> activePromotions(Actor actor, Set<String> productIds, BigDecimal subtotal,
      LocalDateTime at) throws SQLException {
    Access.actor(actor);
    Set<String> ids = new LinkedHashSet<String>();
    if (productIds != null)
      for (String id : productIds)
        if (valid(id))
          ids.add(id.trim());
    try (Connection connection = DBConnection.open()) {
      List<KhuyenMai> promotions =
          khuyenMaiDAO.activeForProducts(ids, at == null ? LocalDateTime.now() : at, connection);
      if (subtotal == null)
        return promotions;
      List<KhuyenMai> eligible = new java.util.ArrayList<KhuyenMai>();
      for (KhuyenMai promotion : promotions)
        if (!PhamViKhuyenMai.TOAN_DON.name().equals(promotion.getPhamVi())
            || promotion.getDonToiThieu() == null
            || subtotal.compareTo(promotion.getDonToiThieu()) >= 0)
          eligible.add(promotion);
      return eligible;
    }
  }

  public void huyHoaDon(Actor actor, String maHoaDon, String lyDo, String tenDangNhapQuanLy,
      char[] matKhauQuanLy) throws SQLException {
    Access.manager(actor);
    huyHoaDon(maHoaDon, lyDo, tenDangNhapQuanLy, matKhauQuanLy);
  }

  public List<HoaDon> timHoaDon(Actor actor, String keyword, String maNhanVien, String trangThai)
      throws SQLException {
    return timHoaDon(actor, keyword, maNhanVien, null, trangThai, null, null, null);
  }
  public List<HoaDon> timHoaDon(Actor actor, String keyword, String maNhanVien, String maKhachHang,
      String trangThai, String phuongThuc, LocalDate from, LocalDate to) throws SQLException {
    Access.actor(actor);
    if (lengthOver(keyword, 100) || lengthOver(maNhanVien, 30) || lengthOver(maKhachHang, 30))
      throw new IllegalArgumentException("Thông tin tìm kiếm vượt quá độ dài cho phép");
    if (from != null && to != null && from.isAfter(to))
      throw new IllegalArgumentException("Khoảng ngày không hợp lệ");
    String status = Access.blankToNull(trangThai);
    String paymentMethod = Access.blankToNull(phuongThuc);
    if (status != null && !Access.is(status, TrangThaiHoaDon.class))
      throw new IllegalArgumentException("Trạng thái hóa đơn không hợp lệ");
    if (paymentMethod != null && !Access.is(paymentMethod, PhuongThucThanhToan.class))
      throw new IllegalArgumentException("Phương thức thanh toán không hợp lệ");
    return timHoaDon(Access.blankToNull(keyword),
        actor.isQuanLy() ? Access.blankToNull(maNhanVien) : actor.getMaNhanVien(),
        Access.blankToNull(maKhachHang), status, paymentMethod, from, to);
  }
  private boolean lengthOver(String value, int max) {
    return value != null && value.trim().length() > max;
  }

  public HoaDon chiTietHoaDon(Actor actor, String maHoaDon) throws SQLException {
    Access.actor(actor);
    HoaDon hoaDon = chiTietHoaDon(maHoaDon);
    if (hoaDon == null
        || (!actor.isQuanLy() && !actor.getMaNhanVien().equals(hoaDon.getMaNhanVien())))
      throw new SecurityException("Không có quyền xem hóa đơn");
    return hoaDon;
  }

  private BaoGia baoGia(YeuCauThanhToan request) throws SQLException {
    try (Connection connection = DBConnection.open()) {
      return new BaoGia(tinh(request, connection, true, false));
    }
  }

  private HoaDon thanhToan(YeuCauThanhToan request) throws SQLException {
    for (int attempt = 1;; attempt++) {
      try {
        return checkout(request);
      } catch (SQLException exception) {
        if (attempt >= MAX_CHECKOUT_ATTEMPTS || !transientFailure(exception))
          throw exception;
      }
    }
  }

  private HoaDon checkout(YeuCauThanhToan request) throws SQLException {
    try (Connection connection = DBConnection.open()) {
      connection.setAutoCommit(false);
      try {
        HoaDon hoaDon = tinh(request, connection, true, true);
        hoaDonDAO.save(hoaDon, connection);
        for (PhanBoXuatLo allocation : hoaDon.getPhanBo())
          loTonKhoDAO.change(allocation.getMaLo(), -allocation.getSoLuongXuat(), connection);
        if (hoaDon.getMaKhachHang() != null && !KHACH_LE.equals(hoaDon.getMaKhachHang()))
          khachHangDAO.updatePoints(hoaDon.getMaKhachHang(),
              hoaDon.getDiemThuongKiem() - hoaDon.getDiemSuDung(), hoaDon.getDiemTichLuyKiem(),
              connection);
        connection.commit();
        return hoaDon;
      } catch (RuntimeException | SQLException exception) {
        connection.rollback();
        throw exception;
      } finally {
        connection.setAutoCommit(true);
      }
    }
  }

  private void huyHoaDon(String maHoaDon, String lyDo, String tenDangNhapQuanLy,
      char[] matKhauQuanLy) throws SQLException {
    if (!valid(maHoaDon) || maHoaDon.trim().length() > 30)
      throw new IllegalArgumentException("Mã hóa đơn không hợp lệ");
    if (lyDo == null || lyDo.trim().isEmpty() || lyDo.trim().length() > 255)
      throw new IllegalArgumentException("Lý do hủy phải từ 1 đến 255 ký tự");
    try (Connection connection = DBConnection.open()) {
      connection.setAutoCommit(false);
      try {
        entity.TaiKhoan manager =
            authService.xacThucQuanLy(tenDangNhapQuanLy, matKhauQuanLy, connection);
        HoaDon hoaDon = hoaDonDAO.lock(maHoaDon, connection);
        if (hoaDon == null || !TrangThaiHoaDon.DA_THANH_TOAN.name().equals(hoaDon.getTrangThai()))
          throw new IllegalStateException("Hóa đơn không thể hủy");
        if (hoaDon.getMaKhachHang() != null && !KHACH_LE.equals(hoaDon.getMaKhachHang()))
          khachHangDAO.lock(hoaDon.getMaKhachHang(), connection);
        for (PhanBoXuatLo allocation : hoaDonDAO.allocations(maHoaDon, connection))
          loTonKhoDAO.change(allocation.getMaLo(), allocation.getSoLuongXuat(), connection);
        if (hoaDon.getMaKhachHang() != null && !KHACH_LE.equals(hoaDon.getMaKhachHang()))
          khachHangDAO.updatePoints(hoaDon.getMaKhachHang(), hoaDon.getDiemSuDung(), 0, connection);
        hoaDonDAO.cancel(maHoaDon, manager.getMaNhanVien(), lyDo.trim(), connection);
        connection.commit();
      } catch (RuntimeException | SQLException exception) {
        connection.rollback();
        throw exception;
      } finally {
        connection.setAutoCommit(true);
      }
    }
  }

  public ThongKeCa thongKeCa(Actor actor) throws SQLException {
    Access.actor(actor);
    try (Connection connection = DBConnection.open()) {
      return hoaDonDAO.thongKeCa(actor.getMaNhanVien(), LocalDate.now(), connection);
    }
  }

  public int tonKho(Actor actor, String maSanPham) throws SQLException {
    Access.actor(actor);
    if (!valid(maSanPham))
      throw new IllegalArgumentException("Mã sản phẩm không hợp lệ");
    try (Connection connection = DBConnection.open()) {
      return loTonKhoDAO.ton(maSanPham.trim(), connection);
    }
  }

  private List<HoaDon> timHoaDon(String keyword, String maNhanVien, String maKhachHang,
      String trangThai, String phuongThuc, LocalDate from, LocalDate to) throws SQLException {
    try (Connection connection = DBConnection.open()) {
      return hoaDonDAO.search(
          keyword, maNhanVien, maKhachHang, trangThai, phuongThuc, from, to, connection);
    }
  }
  private HoaDon chiTietHoaDon(String maHoaDon) throws SQLException {
    try (Connection connection = DBConnection.open()) {
      return hoaDonDAO.detail(maHoaDon, connection);
    }
  }

  private HoaDon tinh(YeuCauThanhToan request, Connection connection, boolean lock,
      boolean validatePayment)
      throws SQLException {
    if (request == null || request.getDongHang() == null || request.getDongHang().isEmpty())
      throw new IllegalArgumentException("Hóa đơn phải có hàng");
    HoaDon hoaDon = new HoaDon();
    String requestedInvoiceId = Access.blankToNull(request.getMaHoaDon());
    if (requestedInvoiceId != null
        && (!ValidationRules.matches(requestedInvoiceId, ValidationRules.INVOICE_ID)
            || requestedInvoiceId.length() > 20))
      throw new IllegalArgumentException("Mã hóa đơn sai định dạng, phải có dạng HD + số (ví dụ: HD001)");
    hoaDon.setMaHoaDon(requestedInvoiceId == null ? "HD" + System.currentTimeMillis() : requestedInvoiceId);
    hoaDon.setMaNhanVien(request.getMaNhanVien());
    String maKhachHang = emptyToNull(request.getMaKhachHang());
    if (KHACH_LE.equals(maKhachHang))
      maKhachHang = null;
    hoaDon.setMaKhachHang(maKhachHang);
    hoaDon.setThoiDiemLap(LocalDateTime.now());
    hoaDon.setTrangThai(TrangThaiHoaDon.DA_THANH_TOAN.name());
    KhachHang customer = null;
    if (maKhachHang != null) {
      customer = lock ? khachHangDAO.lock(hoaDon.getMaKhachHang(), connection) : null;
      if (lock
          && (customer == null
              || !TrangThaiHoatDong.HOAT_DONG.name().equals(customer.getTrangThai())))
        throw new IllegalArgumentException("Khách hàng không hợp lệ");
    }
    Set<String> seen = new HashSet<String>();
    for (DongThanhToan line : request.getDongHang()) {
      if (line == null || !valid(line.getMaSanPham()) || line.getSoLuong() <= 0
          || !seen.add(line.getMaSanPham()))
        throw new IllegalArgumentException("Dòng hàng không hợp lệ");
      SanPham product = sanPhamDAO.find(line.getMaSanPham(), connection);
      if (product == null
          || !TrangThaiSanPham.DANG_KINH_DOANH.name().equals(product.getTrangThai()))
        throw new IllegalArgumentException("Sản phẩm không kinh doanh");
      ChiTietHoaDon detail = new ChiTietHoaDon();
      detail.setMaSanPham(product.getMaSanPham());
      detail.setTenSanPham(product.getTenSanPham());
      detail.setSoLuong(line.getSoLuong());
      detail.setDonGiaBan(product.getGiaBan());
      detail.setTienGiamDong(BigDecimal.ZERO);
      detail.setThanhTien(product.getGiaBan().multiply(BigDecimal.valueOf(line.getSoLuong())));
      hoaDon.getChiTiet().add(detail);
    }
    BigDecimal original = BigDecimal.ZERO;
    for (ChiTietHoaDon detail : hoaDon.getChiTiet()) original = original.add(detail.getThanhTien());
    hoaDon.setTongGiaGoc(original);
    KhuyenMai promotion = null;
    String discountType =
        request.getLoaiGiamGia() == null ? LoaiGiamGia.KHONG.name() : request.getLoaiGiamGia();
    if (!Access.is(discountType, LoaiGiamGia.class))
      throw new IllegalArgumentException("Loại giảm giá không hợp lệ");
    if (LoaiGiamGia.KHUYEN_MAI.name().equals(discountType)) {
      if (!valid(request.getMaKhuyenMai()))
        throw new IllegalArgumentException("Thiếu khuyến mãi");
      promotion =
          khuyenMaiDAO.active(request.getMaKhuyenMai(), hoaDon.getThoiDiemLap(), connection);
      if (promotion == null)
        throw new IllegalArgumentException("Khuyến mãi không hiệu lực");
      hoaDon.setMaKhuyenMai(promotion.getMaKhuyenMai());
    }
    if (LoaiGiamGia.HANG_THANH_VIEN.name().equals(discountType) && customer == null && lock)
      throw new IllegalArgumentException("Giảm hạng cần khách hàng");
    BigDecimal rate = rate(discountType, customer, promotion);
    hoaDon.setLoaiGiamGia(discountType);
    hoaDon.setTyLeGiam(rate);
    BigDecimal eligibleBase = BigDecimal.ZERO;
    for (ChiTietHoaDon detail : hoaDon.getChiTiet()) {
      boolean eligible = LoaiGiamGia.HANG_THANH_VIEN.name().equals(discountType)
          || (LoaiGiamGia.KHUYEN_MAI.name().equals(discountType)
              && (PhamViKhuyenMai.TOAN_DON.name().equals(promotion.getPhamVi())
                  || promotion.getMaSanPham().contains(detail.getMaSanPham())));
      if (eligible)
        eligibleBase = eligibleBase.add(detail.getThanhTien());
    }
    if (LoaiGiamGia.KHUYEN_MAI.name().equals(discountType) && eligibleBase.signum() == 0)
      throw new IllegalArgumentException("Khuyến mãi không áp dụng cho giỏ hàng");
    if (LoaiGiamGia.KHUYEN_MAI.name().equals(discountType)
        && PhamViKhuyenMai.TOAN_DON.name().equals(promotion.getPhamVi())
        && promotion.getDonToiThieu() != null
        && eligibleBase.compareTo(promotion.getDonToiThieu()) < 0)
      throw new IllegalArgumentException("Đơn chưa đạt giá trị tối thiểu " + promotion.getDonToiThieu()
          + " của khuyến mãi " + promotion.getMaKhuyenMai());
    BigDecimal discount =
        money(eligibleBase.multiply(rate).divide(HUNDRED, 2, RoundingMode.HALF_UP));
    BigDecimal allocated = BigDecimal.ZERO;
    for (ChiTietHoaDon detail : hoaDon.getChiTiet()) {
      boolean eligible = LoaiGiamGia.HANG_THANH_VIEN.name().equals(discountType)
          || (LoaiGiamGia.KHUYEN_MAI.name().equals(discountType)
              && (PhamViKhuyenMai.TOAN_DON.name().equals(promotion.getPhamVi())
                  || promotion.getMaSanPham().contains(detail.getMaSanPham())));
      BigDecimal lineDiscount = eligible && eligibleBase.signum() != 0
          ? discount.multiply(detail.getThanhTien()).divide(eligibleBase, 0, RoundingMode.DOWN)
          : BigDecimal.ZERO;
      detail.setTienGiamDong(lineDiscount);
      allocated = allocated.add(lineDiscount);
    }
    int remainder = discount.subtract(allocated).intValueExact();
    for (ChiTietHoaDon detail : hoaDon.getChiTiet())
      if (remainder > 0 && detail.getThanhTien().signum() > 0
          && (LoaiGiamGia.HANG_THANH_VIEN.name().equals(discountType)
              || (LoaiGiamGia.KHUYEN_MAI.name().equals(discountType)
                  && (PhamViKhuyenMai.TOAN_DON.name().equals(promotion.getPhamVi())
                      || promotion.getMaSanPham().contains(detail.getMaSanPham()))))) {
        detail.setTienGiamDong(detail.getTienGiamDong().add(BigDecimal.ONE));
        remainder--;
      }
    for (ChiTietHoaDon detail : hoaDon.getChiTiet())
      detail.setThanhTien(detail.getThanhTien().subtract(detail.getTienGiamDong()));
    hoaDon.setTienGiam(discount);
    long used = request.getDiemThuongSuDung();
    BigDecimal afterDiscount = original.subtract(discount);
    if (used < 0 || used % DIEM_SU_DUNG_BUOC != 0
        || BigDecimal.valueOf(used).compareTo(afterDiscount) > 0
        || (lock && customer != null && used > customer.getDiemHienCo())
        || (used > 0 && customer == null && lock))
      throw new IllegalArgumentException("Điểm thưởng không hợp lệ, phải là bội số của "
          + DIEM_SU_DUNG_BUOC + " và không vượt quá số dư");
    hoaDon.setDiemSuDung(used);
    hoaDon.setTienGiamDiem(BigDecimal.valueOf(used));
    CauHinhCuaHang config = cauHinhDAO.get(connection);
    BigDecimal taxable = afterDiscount.subtract(BigDecimal.valueOf(used)).max(BigDecimal.ZERO);
    BigDecimal vatRate = config.getTyLeVat();
    hoaDon.setTyLeVat(vatRate);
    hoaDon.setTienVat(money(taxable.multiply(vatRate).divide(HUNDRED, 2, RoundingMode.HALF_UP)));
    hoaDon.setTongThanhToan(taxable.add(hoaDon.getTienVat()));
    hoaDon.setDiemTichLuyKiem(customer == null ? 0 : original.longValueExact());
    hoaDon.setDiemThuongKiem(customer == null ? 0
                                              : original.multiply(new BigDecimal("0.05"))
                                                    .setScale(0, RoundingMode.FLOOR)
                                                    .longValueExact());
    if (validatePayment)
      payment(request, hoaDon);
    else {
      hoaDon.setPhuongThucTt(request.getPhuongThucThanhToan());
      if (PhuongThucThanhToan.TIEN_MAT.name().equals(request.getPhuongThucThanhToan())
          && request.getTienKhachDua() != null) {
        hoaDon.setTienKhachDua(request.getTienKhachDua());
        hoaDon.setTienThua(request.getTienKhachDua().subtract(hoaDon.getTongThanhToan())
            .max(BigDecimal.ZERO));
      } else {
        hoaDon.setTienThua(BigDecimal.ZERO);
      }
    }
    if (lock)
      allocate(hoaDon, connection);
    return hoaDon;
  }

  private void allocate(HoaDon hoaDon, Connection connection) throws SQLException {
    for (ChiTietHoaDon detail : hoaDon.getChiTiet()) {
      int remaining = detail.getSoLuong();
      List<LoTonKho> lots = loTonKhoDAO.lockFifo(detail.getMaSanPham(), connection);
      for (LoTonKho lot : lots) {
        int quantity = Math.min(remaining, lot.getSoLuongCon());
        if (quantity > 0) {
          PhanBoXuatLo p = new PhanBoXuatLo();
          p.setMaSanPham(detail.getMaSanPham());
          p.setMaLo(lot.getMaLo());
          p.setSoLuongXuat(quantity);
          p.setDonGiaVon(lot.getDonGiaVon());
          hoaDon.getPhanBo().add(p);
          remaining -= quantity;
        }
      }
      if (remaining != 0)
        throw new IllegalStateException("Không đủ tồn kho");
    }
  }
  private void payment(YeuCauThanhToan request, HoaDon hoaDon) {
    String method = request.getPhuongThucThanhToan();
    if (!Access.is(method, PhuongThucThanhToan.class))
      throw new IllegalArgumentException("Phương thức thanh toán không hợp lệ");
    hoaDon.setPhuongThucTt(method);
    if (PhuongThucThanhToan.TIEN_MAT.name().equals(method)) {
      if (request.getTienKhachDua() == null
          || request.getTienKhachDua().compareTo(hoaDon.getTongThanhToan()) < 0)
        throw new IllegalArgumentException("Tiền khách đưa chưa đủ");
      hoaDon.setTienKhachDua(request.getTienKhachDua());
      hoaDon.setTienThua(request.getTienKhachDua().subtract(hoaDon.getTongThanhToan()));
    } else {
      if (!request.isThanhToanDienTuThanhCong() || !valid(request.getMaThamChieuThanhToan()))
        throw new IllegalArgumentException("Thanh toán điện tử thất bại");
      hoaDon.setMaThamChieuTt(request.getMaThamChieuThanhToan());
    }
  }
  private BigDecimal rate(String type, KhachHang c, KhuyenMai p) {
    if (LoaiGiamGia.KHUYEN_MAI.name().equals(type))
      return p.getTyLeGiam();
    if (!LoaiGiamGia.HANG_THANH_VIEN.name().equals(type) || c == null)
      return BigDecimal.ZERO;
    long points = c.getDiemTichLuy();
    return points >= 1000000 ? new BigDecimal("10")
        : points >= 200000   ? new BigDecimal("5")
        : points >= 50000    ? new BigDecimal("3")
                             : BigDecimal.ZERO;
  }
  private BigDecimal money(BigDecimal value) {
    return value.setScale(0, RoundingMode.HALF_UP);
  }
  private boolean transientFailure(SQLException error) {
    for (SQLException e = error; e != null; e = e.getNextException()) {
      int code = e.getErrorCode();
      String state = e.getSQLState();
      if (code == 1020 || code == 1205 || code == 1213 || "40001".equals(state)
          || "41000".equals(state))
        return true;
    }
    return false;
  }
  private boolean valid(String value) {
    return value != null && !value.trim().isEmpty();
  }
  private String emptyToNull(String value) {
    return valid(value) ? value : null;
  }
}
