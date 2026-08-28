package testsupport;

import connectDB.DBConnection;
import dao.LoTonKhoDAO;
import entity.ChiTietHoaDon;
import entity.ChiTietPhieuNhap;
import entity.HoaDon;
import entity.LoTonKho;
import entity.LoaiGiamGia;
import entity.PhieuNhap;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import service.Actor;
import service.AuthService;
import service.DongThanhToan;
import service.InventoryService;
import service.PurchaseService;
import service.ReportService;
import service.ReportSummary;
import service.SaleService;
import service.YeuCauThanhToan;

public final class BackendTransactionSmoke {
  private BackendTransactionSmoke() {}

  public static void main(String[] args) throws Exception {
    AuthService auth = new AuthService();
    Actor manager = auth.dangNhapActor("quanly", "MatKhau123".toCharArray());
    Actor cashier = auth.dangNhapActor("thungan", "MatKhau123".toCharArray());
    String token = Long.toString(System.nanoTime());
    try {
      reconciliation(manager);
      discount(manager, token);
      fifoCompletion(manager, token);
      concurrency(manager, cashier, token);
    } finally {
      cleanup(token);
    }
  }

  private static void discount(Actor manager, String token) throws Exception {
    String first = "D1" + token, second = "D2" + token, promotion = "K" + token;
    product(first, BigDecimal.valueOf(5));
    product(second, BigDecimal.valueOf(5));
    stock(first, 1, token);
    stock(second, 1, token);
    rejectPromotion(manager, request("X" + token, "KM001", first, 1, null, 0));
    try (Connection connection = DBConnection.open();
        PreparedStatement statement = connection.prepareStatement("INSERT INTO KHUYEN_MAI "
            + "(ma_khuyen_mai,ten_khuyen_mai,pham_vi,ty_le_giam,bat_dau,ket_thuc,trang_thai) "
            + "VALUES (?,?, 'TOAN_DON',10,?,?, 'DANG_HOAT_DONG')")) {
      statement.setString(1, promotion);
      statement.setString(2, promotion);
      statement.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now().minusMinutes(1)));
      statement.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now().plusMinutes(1)));
      statement.executeUpdate();
    }
    SaleService sales = new SaleService();
    HoaDon invoice = sales.thanhToan(manager, request("H" + token, promotion, first, 1, second, 1));
    BigDecimal lines = BigDecimal.ZERO;
    for (ChiTietHoaDon line : invoice.getChiTiet()) lines = lines.add(line.getTienGiamDong());
    if (BigDecimal.ONE.compareTo(invoice.getTienGiam()) != 0
        || BigDecimal.ONE.compareTo(lines) != 0)
      throw new AssertionError("Phân bổ giảm giá không khớp header");
    sales.huyHoaDon(
        manager, invoice.getMaHoaDon(), "Kiem thu", "quanly", "MatKhau123".toCharArray());
  }

  private static void reconciliation(Actor manager) throws Exception {
    ReportSummary summary =
        new ReportService().summary(manager, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
    if (BigDecimal.valueOf(13200).compareTo(summary.getRevenue()) != 0
        || summary.getInvoiceCount() != 1L
        || BigDecimal.valueOf(800000).compareTo(summary.getPurchaseSpend()) != 0
        || BigDecimal.valueOf(32000).compareTo(summary.getCogs()) != 0)
      throw new AssertionError("SQL báo cáo không khớp fixture");
  }
  private static void rejectPromotion(Actor actor, YeuCauThanhToan request) throws Exception {
    try {
      new SaleService().baoGia(actor, request);
    } catch (IllegalArgumentException expected) {
      return;
    }
    throw new AssertionError("Khuyến mãi không liên quan không bị chặn");
  }

  private static void fifoCompletion(Actor manager, String token) throws Exception {
    String product = "F" + token, oldId = "O" + token, newId = "N" + token;
    product(product, BigDecimal.ONE);
    PurchaseService purchases = new PurchaseService();
    purchases.luuNhap(manager, purchase(oldId, product, LocalDateTime.now().minusDays(2)));
    purchases.luuNhap(manager, purchase(newId, product, LocalDateTime.now().minusDays(1)));
    purchases.hoanTat(manager, newId);
    Thread.sleep(1100L);
    purchases.hoanTat(manager, oldId);
    List<LoTonKho> lots = new InventoryService().tonKho(manager, product);
    if (lots.size() != 2 || !newId.equals(lots.get(0).getMaPhieuNhap()))
      throw new AssertionError("FIFO phải theo thời điểm hoàn tất");
  }

  private static void concurrency(final Actor manager, final Actor cashier, String token)
      throws Exception {
    final String product = "C" + token;
    product(product, BigDecimal.ONE);
    stock(product, 95, token);
    final SaleService sales = new SaleService();
    final CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
    ExecutorService pool = Executors.newFixedThreadPool(2);
    try {
      Future<HoaDon> first = pool.submit(
          checkout(cashier, sales, request("A" + token, null, product, 40, null, 0), ready, start));
      Future<HoaDon> second = pool.submit(
          checkout(cashier, sales, request("B" + token, null, product, 40, null, 0), ready, start));
      ready.await();
      start.countDown();
      HoaDon one = first.get(), two = second.get();
      if (stock(product) != 15)
        throw new AssertionError("Hai hóa đơn đồng thời phải còn 15");
      sales.huyHoaDon(manager, one.getMaHoaDon(), "Kiem thu", "quanly", "MatKhau123".toCharArray());
      sales.huyHoaDon(manager, two.getMaHoaDon(), "Kiem thu", "quanly", "MatKhau123".toCharArray());
      if (stock(product) != 95)
        throw new AssertionError("Hủy phải khôi phục 95");
    } finally {
      pool.shutdownNow();
    }
  }

  private static Callable<HoaDon> checkout(final Actor actor, final SaleService sales,
      final YeuCauThanhToan request, final CountDownLatch ready, final CountDownLatch start) {
    return new Callable<HoaDon>() {
      public HoaDon call() throws Exception {
        ready.countDown();
        start.await();
        return sales.thanhToan(actor, request);
      }
    };
  }

  private static YeuCauThanhToan request(String id, String promotion, String first,
      int firstQuantity, String second, int secondQuantity) {
    YeuCauThanhToan request = new YeuCauThanhToan();
    request.setMaHoaDon(id);
    request.setLoaiGiamGia(
        promotion == null ? LoaiGiamGia.KHONG.name() : LoaiGiamGia.KHUYEN_MAI.name());
    request.setMaKhuyenMai(promotion);
    request.setPhuongThucThanhToan("QR");
    request.setThanhToanDienTuThanhCong(true);
    request.setMaThamChieuThanhToan("R" + id);
    line(request, first, firstQuantity);
    if (second != null)
      line(request, second, secondQuantity);
    return request;
  }
  private static void line(YeuCauThanhToan request, String product, int quantity) {
    DongThanhToan line = new DongThanhToan();
    line.setMaSanPham(product);
    line.setSoLuong(quantity);
    request.getDongHang().add(line);
  }
  private static PhieuNhap purchase(String id, String product, LocalDateTime created) {
    PhieuNhap value = new PhieuNhap();
    value.setMaPhieuNhap(id);
    value.setMaNhaCungCap("NCC001");
    value.setThoiDiemLap(created);
    ChiTietPhieuNhap line = new ChiTietPhieuNhap();
    line.setMaSanPham(product);
    line.setSoLuong(1);
    line.setDonGiaNhap(BigDecimal.ONE);
    value.getChiTiet().add(line);
    return value;
  }
  private static void product(String id, BigDecimal price) throws Exception {
    try (Connection connection = DBConnection.open();
        PreparedStatement statement = connection.prepareStatement("INSERT INTO SAN_PHAM "
            + "(ma_san_pham,ma_loai,ten_san_pham,don_vi_tinh,gia_ban,nguong_ton,trang_thai) VALUES "
            + "(?, 'LSP001', ?, 'Cai', ?, 0, 'DANG_KINH_DOANH')")) {
      statement.setString(1, id);
      statement.setString(2, id);
      statement.setBigDecimal(3, price);
      statement.executeUpdate();
    }
  }
  private static void stock(String product, int quantity, String token) throws Exception {
    String purchase = "P" + product;
    try (Connection connection = DBConnection.open();
        PreparedStatement header = connection.prepareStatement("INSERT INTO PHIEU_NHAP "
            + "(ma_phieu_nhap,ma_nha_cung_cap,ma_nhan_vien,thoi_diem_lap,thoi_diem_hoan_tat,trang_"
            + "thai,tong_tien) VALUES (?, 'NCC001', 'NV001', NOW(), NOW(), 'HOAN_TAT', ?)");
        PreparedStatement detail = connection.prepareStatement(
            "INSERT INTO CHI_TIET_PHIEU_NHAP (ma_phieu_nhap,ma_san_pham,so_luong,don_gia_nhap) "
            + "VALUES (?, ?, ?, 1)");
        PreparedStatement lot = connection.prepareStatement("INSERT INTO LO_TON_KHO "
            + "(ma_phieu_nhap,ma_san_pham,so_luong_nhap,so_luong_con,"
            + "thoi_diem_nhap) VALUES (?, ?, ?, ?, NOW())")) {
      header.setString(1, purchase);
      header.setBigDecimal(2, BigDecimal.valueOf(quantity));
      header.executeUpdate();
      detail.setString(1, purchase);
      detail.setString(2, product);
      detail.setInt(3, quantity);
      detail.executeUpdate();
      lot.setString(1, purchase);
      lot.setString(2, product);
      lot.setInt(3, quantity);
      lot.setInt(4, quantity);
      lot.executeUpdate();
    }
  }
  private static int stock(String product) throws Exception {
    try (Connection connection = DBConnection.open()) {
      return new LoTonKhoDAO().ton(product, connection);
    }
  }
  private static void cleanup(String token) throws Exception {
    try (Connection connection = DBConnection.open()) {
      connection.setAutoCommit(false);
      try {
        execute(connection,
            "DELETE p FROM PHAN_BO_XUAT_LO p JOIN HOA_DON h ON h.ma_hoa_don=p.ma_hoa_don WHERE "
                + "h.ma_hoa_don LIKE ?",
            token);
        execute(connection, "DELETE FROM CHI_TIET_HOA_DON WHERE ma_hoa_don LIKE ?", token);
        execute(connection, "DELETE FROM HOA_DON WHERE ma_hoa_don LIKE ?", token);
        execute(connection, "DELETE FROM KHUYEN_MAI_SAN_PHAM WHERE ma_khuyen_mai LIKE ?", token);
        execute(connection, "DELETE FROM KHUYEN_MAI WHERE ma_khuyen_mai LIKE ?", token);
        execute(connection, "DELETE FROM LO_TON_KHO WHERE ma_san_pham LIKE ?", token);
        execute(connection,
            "DELETE FROM CHI_TIET_PHIEU_NHAP WHERE ma_san_pham LIKE ? OR ma_phieu_nhap LIKE ?",
            token, token);
        execute(connection, "DELETE FROM PHIEU_NHAP WHERE ma_phieu_nhap LIKE ?", token);
        execute(connection, "DELETE FROM SAN_PHAM WHERE ma_san_pham LIKE ?", token);
        connection.commit();
      } catch (Exception exception) {
        connection.rollback();
        throw exception;
      } finally {
        connection.setAutoCommit(true);
      }
    }
  }
  private static void execute(Connection connection, String sql, String... values)
      throws Exception {
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      for (int i = 0; i < values.length; i++) statement.setString(i + 1, "%" + values[i] + "%");
      statement.executeUpdate();
    }
  }
}
