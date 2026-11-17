package testsupport;

import connectDB.DBConnection;
import entity.*;
import service.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/** Exact fixture ownership and a full row snapshot make cleanup failures visible. */
final class TestFixtures implements AutoCloseable {
  final Actor manager;
  final MasterDataService masters = new MasterDataService();
  final SaleService sales = new SaleService();
  final PurchaseService purchases = new PurchaseService();
  private final Map<String, List<String>> owned = new LinkedHashMap<String, List<String>>();
  private final Map<String, List<String>> baseline;
  private final CauHinhCuaHang settings;
  private String category;
  private String supplier;
  static int checks;

  TestFixtures() throws Exception {
    TestDatabase.requireIsolated();
    baseline = snapshot();
    manager = new AuthService().dangNhapActor("quanly", "MatKhau123".toCharArray());
    settings = new SettingsService().lay(manager);
  }

  String track(String table, String id) {
    owned.computeIfAbsent(table, key -> new ArrayList<String>()).add(id);
    return id;
  }
  String category() throws Exception {
    if (category == null) {
      LoaiSanPham value = new LoaiSanPham();
      category = track("LOAI_SAN_PHAM", masters.maLoaiTiepTheo(manager));
      value.setMaLoai(category); value.setTenLoai("Danh mục kiểm thử");
      value.setTrangThai("DANG_KINH_DOANH"); masters.luuLoai(manager, value);
    }
    return category;
  }
  String supplier() throws Exception {
    if (supplier == null) {
      NhaCungCap value = new NhaCungCap();
      supplier = track("NHA_CUNG_CAP", masters.maNhaCungCapTiepTheo(manager));
      value.setMaNhaCungCap(supplier); value.setTenNhaCungCap("Nhà cung cấp kiểm thử");
      value.setTrangThai("HOAT_DONG"); masters.luuNhaCungCap(manager, value);
    }
    return supplier;
  }
  SanPham product(long price) throws Exception {
    String id;
    do { id = "893" + String.format(Locale.ROOT, "%09d", 100000000 + new Random().nextInt(900000000)); }
    while (number("SELECT COUNT(*) FROM SAN_PHAM WHERE ma_san_pham=?", id) > 0);
    SanPham value = new SanPham();
    value.setMaSanPham(track("SAN_PHAM", id)); value.setMaLoai(category());
    value.setTenSanPham("Sản phẩm kiểm thử " + id); value.setDonViTinh("Cái");
    value.setGiaBan(BigDecimal.valueOf(price)); value.setTrangThai("DANG_KINH_DOANH");
    masters.luuSanPham(manager, value); return value;
  }
  String customer(long balance, long loyalty) throws Exception {
    KhachHang value = new KhachHang();
    String id = track("KHACH_HANG", masters.maKhachHangTiepTheo(manager));
    value.setMaKhachHang(id); value.setHoTen("Khách Kiểm Thử");
    value.setSoDienThoai("09" + String.format(Locale.ROOT, "%08d", new Random().nextInt(100000000)));
    value.setTrangThai("HOAT_DONG"); masters.luuKhachHang(manager, value);
    sql("UPDATE KHACH_HANG SET diem_hien_co=?,diem_tich_luy=? WHERE ma_khach_hang=?", balance, loyalty, id);
    return id;
  }
  TaiKhoan account(String role) throws Exception {
    NhanVien employee = new NhanVien();
    String id = track("NHAN_VIEN", masters.maNhanVienTiepTheo(manager));
    employee.setMaNhanVien(id); employee.setHoTen("Nhân Viên Kiểm Thử");
    employee.setTrangThai("DANG_LAM"); masters.luuNhanVien(manager, employee);
    TaiKhoan account = new TaiKhoan();
    account.setTenDangNhap(track("TAI_KHOAN", "test_" + UUID.randomUUID().toString().substring(0, 12)));
    account.setMaNhanVien(id); account.setVaiTro(role); account.setTrangThai("HOAT_DONG");
    masters.luuTaiKhoan(manager, account, "MatKhau123".toCharArray()); return account;
  }
  Actor login(TaiKhoan account) throws Exception {
    return new AuthService().dangNhapActor(account.getTenDangNhap(), "MatKhau123".toCharArray());
  }
  PhieuNhap purchase(SanPham product, int quantity, long cost, boolean complete) throws Exception {
    PhieuNhap value = new PhieuNhap();
    value.setMaPhieuNhap(track("PHIEU_NHAP", purchases.maPhieuNhapTiepTheo(manager)));
    value.setMaNhaCungCap(supplier()); value.setThoiDiemLap(LocalDateTime.now()); value.setTrangThai("NHAP");
    ChiTietPhieuNhap line = new ChiTietPhieuNhap(); line.setMaSanPham(product.getMaSanPham());
    line.setSoLuong(quantity); line.setDonGiaNhap(BigDecimal.valueOf(cost)); value.getChiTiet().add(line);
    purchases.luuNhap(manager, value);
    if (complete) purchases.hoanTat(manager, value.getMaPhieuNhap());
    return value;
  }
  KhuyenMai promotion(String rate, Long minimum, String... products) throws Exception {
    KhuyenMai value = new KhuyenMai();
    value.setMaKhuyenMai(track("KHUYEN_MAI", new PromotionService().maKhuyenMaiTiepTheo(manager)));
    value.setTenKhuyenMai("Khuyến mãi giảm " + rate.replace('.', ' ') + "%");
    value.setTyLeGiam(new BigDecimal(rate)); value.setPhamVi(products.length == 0 ? "TOAN_DON" : "THEO_SAN_PHAM");
    value.setDonToiThieu(minimum == null ? null : BigDecimal.valueOf(minimum));
    value.setBatDau(LocalDateTime.now().minusDays(1)); value.setKetThuc(LocalDateTime.now().plusDays(1));
    value.setTrangThai("DANG_HOAT_DONG"); value.setMaSanPham(new LinkedHashSet<String>(Arrays.asList(products)));
    new PromotionService().luu(manager, value); return value;
  }
  YeuCauThanhToan request(SanPham product, int quantity) throws Exception {
    YeuCauThanhToan request = new YeuCauThanhToan();
    request.setMaHoaDon(track("HOA_DON", sales.maHoaDonTiepTheo(manager)));
    request.setPhuongThucThanhToan("QR"); request.setThanhToanDienTuThanhCong(true);
    request.setMaThamChieuThanhToan("TEST-" + UUID.randomUUID());
    DongThanhToan line = new DongThanhToan(); line.setMaSanPham(product.getMaSanPham());
    line.setSoLuong(quantity); request.getDongHang().add(line); return request;
  }
  void vat(String rate) throws Exception { sql("UPDATE CAU_HINH_CUA_HANG SET ty_le_vat=? WHERE id=1", new BigDecimal(rate)); }
  void cancel(HoaDon invoice) throws Exception {
    sales.huyHoaDon(manager, invoice.getMaHoaDon(), "Kiểm thử", "quanly", "MatKhau123".toCharArray());
  }
  int stock(SanPham product) throws Exception { return sales.tonKho(manager, product.getMaSanPham()); }

  static void sql(String query, Object... args) throws SQLException {
    try (Connection connection = DBConnection.open(); PreparedStatement statement = connection.prepareStatement(query)) {
      bind(statement, args); statement.executeUpdate();
    }
  }
  static long number(String query, Object... args) throws SQLException {
    try (Connection connection = DBConnection.open(); PreparedStatement statement = connection.prepareStatement(query)) {
      bind(statement, args);
      try (ResultSet result = statement.executeQuery()) { result.next(); return result.getLong(1); }
    }
  }
  static BigDecimal decimal(String query, Object... args) throws SQLException {
    try (Connection connection = DBConnection.open(); PreparedStatement statement = connection.prepareStatement(query)) {
      bind(statement, args);
      try (ResultSet result = statement.executeQuery()) { result.next(); return result.getBigDecimal(1); }
    }
  }
  private static void bind(PreparedStatement statement, Object[] args) throws SQLException {
    for (int index = 0; index < args.length; index++) statement.setObject(index + 1, args[index]);
  }
  static void check(boolean condition, String description) {
    if (!condition) throw new AssertionError(description);
    checks++; System.out.println("PASS " + description);
  }
  interface Action { void run() throws Exception; }
  static void rejects(Class<? extends Throwable> expected, Action action, String description) throws Exception {
    try { action.run(); }
    catch (Throwable exception) {
      if (!expected.isInstance(exception)) throw new AssertionError(description + ": " + exception, exception);
      check(true, description); return;
    }
    throw new AssertionError("Expected rejection: " + description);
  }
  static Map<String, List<String>> snapshot() throws SQLException {
    Map<String, List<String>> snapshot = new TreeMap<String, List<String>>();
    try (Connection connection = DBConnection.open(); Statement statement = connection.createStatement()) {
      List<String> tables = new ArrayList<String>();
      try (ResultSet rows = statement.executeQuery("SELECT name FROM sys.tables WHERE is_ms_shipped=0")) {
        while (rows.next()) tables.add(rows.getString(1));
      }
      for (String table : tables) {
        List<String> data = new ArrayList<String>();
        try (ResultSet rows = statement.executeQuery("SELECT * FROM dbo.[" + table + "]")) {
          int count = rows.getMetaData().getColumnCount();
          while (rows.next()) {
            StringBuilder row = new StringBuilder();
            for (int index = 1; index <= count; index++) {
              String value = rows.getString(index); row.append(value == null ? -1 : value.length()).append(':').append(value).append(';');
            }
            data.add(row.toString());
          }
        }
        Collections.sort(data); snapshot.put(table, data);
      }
    }
    return snapshot;
  }
  @Override public void close() throws SQLException {
    // Close is deliberately fail-fast; a cleanup error cannot turn a suite green.
    try (Connection restored = openAndRestore()) { new dao.CauHinhCuaHangDAO().save(settings, restored); }
    try (Connection connection = DBConnection.open()) {
      connection.setAutoCommit(false);
      String[][] cleanup = {
          {"HOA_DON", "PHAN_BO_XUAT_LO", "ma_hoa_don"}, {"HOA_DON", "CHI_TIET_HOA_DON", "ma_hoa_don"},
          {"HOA_DON", "HOA_DON", "ma_hoa_don"},
          {"PHIEU_DIEU_CHINH_TON", "CHI_TIET_PHIEU_DIEU_CHINH", "ma_dieu_chinh"},
          {"PHIEU_DIEU_CHINH_TON", "PHIEU_DIEU_CHINH_TON", "ma_dieu_chinh"},
          {"PHIEU_NHAP", "LO_TON_KHO", "ma_phieu_nhap"}, {"PHIEU_NHAP", "CHI_TIET_PHIEU_NHAP", "ma_phieu_nhap"},
          {"PHIEU_NHAP", "PHIEU_NHAP", "ma_phieu_nhap"},
          {"KHUYEN_MAI", "KHUYEN_MAI_SAN_PHAM", "ma_khuyen_mai"}, {"KHUYEN_MAI", "KHUYEN_MAI", "ma_khuyen_mai"},
          {"SAN_PHAM", "SAN_PHAM", "ma_san_pham"}, {"LOAI_SAN_PHAM", "LOAI_SAN_PHAM", "ma_loai"},
          {"KHACH_HANG", "KHACH_HANG", "ma_khach_hang"}, {"TAI_KHOAN", "TAI_KHOAN", "ten_dang_nhap"},
          {"NHAN_VIEN", "NHAN_VIEN", "ma_nhan_vien"}, {"NHA_CUNG_CAP", "NHA_CUNG_CAP", "ma_nha_cung_cap"}};
      for (String[] target : cleanup) for (String id : owned.getOrDefault(target[0], Collections.emptyList())) {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM " + target[1] + " WHERE " + target[2] + "=?")) {
          statement.setString(1, id); statement.executeUpdate();
        }
      }
      connection.commit();
    }
    check(baseline.equals(snapshot()), "cleanup restores all 17 business tables exactly");
  }
  private Connection openAndRestore() throws SQLException {
    // A removed settings row is restored before normal UPDATE-based settings save.
    sql("IF NOT EXISTS(SELECT 1 FROM CAU_HINH_CUA_HANG WHERE id=1) INSERT INTO CAU_HINH_CUA_HANG "
        + "(id,ten_cua_hang,dia_chi,so_dien_thoai,ty_le_vat) VALUES (1,?,?,?,?)",
        settings.getTenCuaHang(), settings.getDiaChi(), settings.getSoDienThoai(), settings.getTyLeVat());
    return DBConnection.open();
  }
}
