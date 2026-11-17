package testsupport;

import connectDB.ConnectDB;
import dao.Id_DAO;
import entity.*;
import service.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.*;
import static testsupport.TestFixtures.*;

public final class BackendTransactionSmoke {
  private BackendTransactionSmoke() {}
  public static void main(String[] args) throws Exception {
    TestDatabase.requireIsolated();
    try (TestFixtures fixtures = new TestFixtures(); JdbcFaults faults = new JdbcFaults()) {
      check(faults.jdbcCompliant(), "real JDBC driver wrapped for failure injection");
      fifoAndRollback(fixtures);
      completionOrder(fixtures);
      idempotency(fixtures);
      concurrency(fixtures);
      sessionPolicy(fixtures);
      identifiers(fixtures);
      reconciliation(fixtures);
    }
    System.out.println("BackendTransactionSmoke checks: " + checks);
  }
  private static void fifoAndRollback(TestFixtures f) throws Exception {
    f.vat("10"); SanPham product = f.product(10000);
    PhieuNhap first = f.purchase(product, 2, 100, true), second = f.purchase(product, 3, 200, true);
    String customer = f.customer(5000, 0);
    YeuCauThanhToan request = f.request(product, 4); request.setMaKhachHang(customer); request.setDiemThuongSuDung(999);
    HoaDon invoice = f.sales.thanhToan(f.manager, request);
    check(invoice.getPhanBo().size() == 2 && invoice.getPhanBo().get(0).getSoLuongXuat() == 2
        && invoice.getPhanBo().get(1).getSoLuongXuat() == 2 && f.stock(product) == 1, "FIFO sale spans original lots");
    f.cancel(invoice);
    check(number("SELECT so_luong_con FROM LO_TON_KHO WHERE ma_phieu_nhap=?", first.getMaPhieuNhap()) == 2
        && number("SELECT so_luong_con FROM LO_TON_KHO WHERE ma_phieu_nhap=?", second.getMaPhieuNhap()) == 3,
        "cancellation restores exact original lots");
    check(number("SELECT diem_hien_co FROM KHACH_HANG WHERE ma_khach_hang=?", customer) == 7000
        && number("SELECT diem_tich_luy FROM KHACH_HANG WHERE ma_khach_hang=?", customer) == 40000,
        "cancellation restores spent points and retains earned points");
    rejects(IllegalStateException.class, () -> f.cancel(invoice), "double cancellation rejected");
    rejects(IllegalStateException.class, () -> f.purchases.luuNhap(f.manager, first), "completed purchase immutable");
    YeuCauThanhToan cash = f.request(product, 1); cash.setPhuongThucThanhToan("TIEN_MAT"); cash.setTienKhachDua(BigDecimal.ZERO);
    rejects(IllegalArgumentException.class, () -> f.sales.thanhToan(f.manager, cash), "insufficient cash rolls back");
    YeuCauThanhToan failed = f.request(product, 1); failed.setThanhToanDienTuThanhCong(false);
    rejects(IllegalArgumentException.class, () -> f.sales.thanhToan(f.manager, failed), "failed electronic payment rolls back");
    YeuCauThanhToan oversell = f.request(product, 6);
    rejects(IllegalStateException.class, () -> f.sales.thanhToan(f.manager, oversell), "oversell rolls back");
    check(f.stock(product) == 5 && number("SELECT COUNT(*) FROM HOA_DON WHERE ma_hoa_don IN (?,?,?)",
        cash.getMaHoaDon(), failed.getMaHoaDon(), oversell.getMaHoaDon()) == 0, "failed payments leave stock and invoices unchanged");
    YeuCauThanhToan overInvoice = f.request(product, 1); overInvoice.setMaKhachHang(customer);
    sql("UPDATE KHACH_HANG SET diem_hien_co=50000 WHERE ma_khach_hang=?", customer); overInvoice.setDiemThuongSuDung(10001);
    rejects(IllegalArgumentException.class, () -> f.sales.baoGia(f.manager, overInvoice), "over-invoice redemption rejected");
  }
  private static void completionOrder(TestFixtures f) throws Exception {
    SanPham product = f.product(1);
    PhieuNhap older = f.purchase(product, 1, 0, false), newer = f.purchase(product, 1, 0, false);
    older.setThoiDiemLap(java.time.LocalDateTime.now().minusDays(2));
    newer.setThoiDiemLap(java.time.LocalDateTime.now().minusDays(1));
    f.purchases.luuNhap(f.manager, older); f.purchases.luuNhap(f.manager, newer);
    f.purchases.hoanTat(f.manager, newer.getMaPhieuNhap()); f.purchases.hoanTat(f.manager, older.getMaPhieuNhap());
    List<LoTonKho> lots = new DichVuTonKho().tonKho(f.manager, product.getMaSanPham());
    check(lots.size() == 2 && newer.getMaPhieuNhap().equals(lots.get(0).getMaPhieuNhap()),
        "FIFO follows completion order rather than draft creation date");
  }

  private static void idempotency(TestFixtures f) throws Exception {
    SanPham product = f.product(10000); f.purchase(product, 20, 0, true);
    String customer = f.customer(1000, 0);
    YeuCauThanhToan request = f.request(product, 1); request.setMaKhachHang(customer); request.setDiemThuongSuDung(1);
    request.setMaHoaDon(null);
    HoaDon first;
    try { first = f.sales.thanhToan(f.manager, request); }
    finally { if (request.getMaHoaDon() != null) f.track("HOA_DON", request.getMaHoaDon()); }
    HoaDon repeated = f.sales.thanhToan(f.manager, request);
    check(first.getMaHoaDon().equals(repeated.getMaHoaDon()) && f.stock(product) == 19
        && number("SELECT diem_hien_co FROM KHACH_HANG WHERE ma_khach_hang=?", customer) == 1499,
        "same logical request returns one invoice and updates stock and points once");
    YeuCauThanhToan changed = request.copy(); changed.getDongHang().get(0).setSoLuong(2);
    rejects(IllegalArgumentException.class, () -> f.sales.thanhToan(f.manager, changed), "same checkout ID rejects different quantity");
    changed.getDongHang().get(0).setSoLuong(1);
    changed.getDongHang().get(0).setMaSanPham(f.product(1).getMaSanPham());
    rejects(IllegalArgumentException.class, () -> f.sales.thanhToan(f.manager, changed), "same checkout ID rejects different product");
    changed.getDongHang().get(0).setMaSanPham(product.getMaSanPham()); changed.setMaThamChieuThanhToan("DIFFERENT");
    rejects(IllegalArgumentException.class, () -> f.sales.thanhToan(f.manager, changed), "same checkout ID rejects different payment");
    NguoiDung cashier = new DichVuDangNhap().dangNhapNguoiDung("thungan01", "MatKhau123".toCharArray());
    rejects(IllegalArgumentException.class, () -> f.sales.thanhToan(cashier, request), "same checkout ID rejects different actor");
    request.setMaHoaDon(" " + request.getMaHoaDon() + " ");
    check(f.sales.thanhToan(f.manager, request).getMaHoaDon().equals(first.getMaHoaDon()), "checkout normalizes supplied ID before retry locking");
    YeuCauThanhToan lost = f.request(product, 1);
    JdbcFaults.LOSE_COMMIT.set(Boolean.TRUE);
    HoaDon recovered = f.sales.thanhToan(f.manager, lost);
    check(recovered.getMaHoaDon().equals(lost.getMaHoaDon()) && f.stock(product) == 18
        && number("SELECT COUNT(*) FROM HOA_DON WHERE ma_hoa_don=?", lost.getMaHoaDon()) == 1,
        "lost commit acknowledgement resolves to original invoice");
    sql("DELETE FROM CAU_HINH_CUA_HANG WHERE id=1");
    rejects(IllegalStateException.class, () -> new DichVuCauHinh().layChoHoaDon(f.manager), "post-payment receipt settings failure injected");
    check(f.sales.thanhToan(f.manager, lost).getMaHoaDon().equals(recovered.getMaHoaDon()) && f.stock(product) == 18,
        "receipt failure cannot cause another deduction on retry");
    sql("INSERT INTO CAU_HINH_CUA_HANG (id,ten_cua_hang,dia_chi,so_dien_thoai,ty_le_vat) VALUES (1,?,?,?,10)",
        "Cửa hàng kiểm thử", "Địa chỉ kiểm thử", "0901234567");
  }
  private static void concurrency(TestFixtures f) throws Exception {
    SanPham product = f.product(10); f.purchase(product, 5, 0, true);
    YeuCauThanhToan same = f.request(product, 2);
    ExecutorService pool = Executors.newFixedThreadPool(2);
    try {
      CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
      Future<HoaDon> one = pool.submit(() -> { ready.countDown(); start.await(); return f.sales.thanhToan(f.manager, same.copy()); });
      Future<HoaDon> two = pool.submit(() -> { ready.countDown(); start.await(); return f.sales.thanhToan(f.manager, same.copy()); });
      if (!ready.await(5, TimeUnit.SECONDS)) throw new AssertionError("concurrent start timeout"); start.countDown();
      check(one.get(20, TimeUnit.SECONDS).getMaHoaDon().equals(two.get(20, TimeUnit.SECONDS).getMaHoaDon()) && f.stock(product) == 3,
          "concurrent identical checkout creates one invoice");
      YeuCauThanhToan a = f.request(product, 2), b = f.request(product, 2);
      Future<Boolean> first = pool.submit(() -> succeeds(f, a)); Future<Boolean> second = pool.submit(() -> succeeds(f, b));
      check(first.get(20, TimeUnit.SECONDS) != second.get(20, TimeUnit.SECONDS) && f.stock(product) == 1,
          "competing sales cannot oversell stock");
    } finally { pool.shutdownNow(); }
  }
  private static boolean succeeds(TestFixtures f, YeuCauThanhToan request) throws Exception {
    try { f.sales.thanhToan(f.manager, request); return true; }
    catch (IllegalStateException expected) { return false; }
  }
  private static void sessionPolicy(TestFixtures f) throws Exception {
    SanPham product = f.product(10); f.purchase(product, 10, 0, true);
    TaiKhoan account = f.account("THU_NGAN"); NguoiDung cashier = f.login(account);
    f.masters.capNhatTrangThaiTaiKhoan(f.manager, account.getTenDangNhap(), "KHOA");
    YeuCauThanhToan request = f.request(product, 1);
    rejects(SessionExpiredException.class, () -> f.sales.thanhToan(cashier, request), "locked cashier cannot reuse active session");
    f.masters.capNhatTrangThaiTaiKhoan(f.manager, account.getTenDangNhap(), "HOAT_DONG");
    rejects(SessionExpiredException.class, () -> f.sales.thanhToan(cashier, request), "unlock does not revive old session");
    NguoiDung fresh = f.login(account); check(f.sales.thanhToan(fresh, request) != null, "fresh login after unlock can sell");
    rejects(IllegalStateException.class, () -> f.masters.xoaTaiKhoan(f.manager, account.getTenDangNhap()), "account referenced by checkout history cannot be deleted");
    f.masters.capNhatTrangThaiNhanVien(f.manager, account.getMaNhanVien(), "NGHI_VIEC");
    rejects(SessionExpiredException.class, () -> f.sales.tonKho(fresh, product.getMaSanPham()), "departed employee session rejected");
    f.masters.capNhatTrangThaiNhanVien(f.manager, account.getMaNhanVien(), "DANG_LAM");
    TaiKhoan managerAccount = f.account("QUAN_LY"); NguoiDung oldManager = f.login(managerAccount);
    managerAccount.setVaiTro("THU_NGAN"); f.masters.luuTaiKhoan(f.manager, managerAccount, null);
    rejects(SessionExpiredException.class, () -> f.masters.timNhanVien(oldManager, null, null), "demoted manager session rejected");
    // Pause checkout after its shared authorization lock, then request revocation.
    NguoiDung active = f.login(account); YeuCauThanhToan blocked = f.request(product, 1);
    CountDownLatch stockReached = new CountDownLatch(1), release = new CountDownLatch(1), revoking = new CountDownLatch(1);
    ExecutorService pool = Executors.newFixedThreadPool(2);
    try {
      Future<HoaDon> payment = pool.submit(() -> {
        JdbcFaults.BEFORE_STOCK.set(() -> {
          stockReached.countDown();
          try { if (!release.await(10, TimeUnit.SECONDS)) throw new AssertionError("stock release timeout"); }
          catch (InterruptedException exception) { Thread.currentThread().interrupt(); throw new IllegalStateException(exception); }
        });
        return f.sales.thanhToan(active, blocked);
      });
      if (!stockReached.await(10, TimeUnit.SECONDS)) throw new AssertionError("checkout did not reach protected transaction");
      Future<?> revocation = pool.submit(() -> {
        revoking.countDown();
        try { f.masters.capNhatTrangThaiTaiKhoan(f.manager, account.getTenDangNhap(), "KHOA"); }
        catch (Exception exception) { throw new IllegalStateException(exception); }
      });
      if (!revoking.await(5, TimeUnit.SECONDS)) throw new AssertionError("revocation not started");
      Thread.sleep(150);
      check(!revocation.isDone(), "revocation waits behind a checkout holding authorization lock");
      release.countDown(); payment.get(20, TimeUnit.SECONDS); revocation.get(20, TimeUnit.SECONDS);
      rejects(SessionExpiredException.class, () -> f.sales.tonKho(active, product.getMaSanPham()), "revocation applies to the next operation after ordered checkout");
    } finally { release.countDown(); pool.shutdownNow(); }
  }
  private static Connection admin() throws Exception {
    String password = System.getenv("POS_TEST_ADMIN_PASSWORD");
    if (password == null) throw new IllegalStateException("admin credential required for migration-only tests");
    return DriverManager.getConnection(System.getenv("POS_DB_URL"), "sa", password);
  }
  private static void identifiers(TestFixtures f) throws Exception {
    for (String id : new String[] {"LSP999", "LSP1000"}) {
      if (number("SELECT COUNT(*) FROM LOAI_SAN_PHAM WHERE ma_loai=?", id) == 0) {
        f.track("LOAI_SAN_PHAM", id); sql("INSERT INTO LOAI_SAN_PHAM(ma_loai,ten_loai,trang_thai) VALUES (?,?,'DANG_KINH_DOANH')", id, "Kiểm thử thứ tự mã " + id);
      }
    }
    java.util.Map<String, java.util.List<String>> before = snapshot();
    try (Connection connection = admin(); Statement statement = connection.createStatement()) {
      statement.execute(Files.readString(Path.of("sql/migrations/20261117_audit.sql")));
    }
    check(before.equals(snapshot()), "migration on populated database preserves every business row");
    String next = f.masters.maLoaiTiepTheo(f.manager);
    check(Long.parseLong(next.substring(3)) > 1000, "numeric sequence advances beyond LSP999 and LSP1000");
    ExecutorService pool = Executors.newFixedThreadPool(2);
    try {
      Future<String> first = pool.submit(() -> f.masters.maLoaiTiepTheo(f.manager));
      Future<String> second = pool.submit(() -> f.masters.maLoaiTiepTheo(f.manager));
      check(!first.get(10, TimeUnit.SECONDS).equals(second.get(10, TimeUnit.SECONDS)), "parallel ID allocations differ");
    } finally { pool.shutdownNow(); }
    for (String prefix : new String[] {"NV", "KH", "KM", "HD"}) {
      long current = number("SELECT CONVERT(bigint,current_value) FROM sys.sequences WHERE name=?", "POS_SEQ_" + prefix);
      long maximum = prefix.equals("HD") ? 9999999999999L : 999;
      try {
        try (Connection connection = admin(); Statement statement = connection.createStatement()) {
          statement.execute("ALTER SEQUENCE dbo.POS_SEQ_" + prefix + " RESTART WITH " + maximum);
        }
        try (Connection connection = ConnectDB.open()) {
          Id_DAO.taoMaTiepTheo(prefix, connection); // Reserve the final suffix; the following call must fail.
          rejects(IllegalStateException.class, () -> Id_DAO.taoMaTiepTheo(prefix, connection), "explicit sequence exhaustion " + prefix);
        }
      } finally {
        try (Connection connection = admin(); Statement statement = connection.createStatement()) {
          statement.execute("ALTER SEQUENCE dbo.POS_SEQ_" + prefix + " RESTART WITH " + (current + 1));
        }
      }
    }
  }
  private static void reconciliation(TestFixtures f) throws Exception {
    LocalDate today = LocalDate.now();
    TongHopBaoCao summary = new DichVuBaoCao().tongHop(f.manager, today, today);
    BigDecimal direct = decimal("SELECT COALESCE(SUM(tong_thanh_toan),0) FROM HOA_DON WHERE trang_thai='DA_THANH_TOAN' AND CAST(thoi_diem_lap AS date)=?", Date.valueOf(today));
    check(summary.getDoanhThu().compareTo(direct) == 0, "report revenue reconciles with direct SQL on fixture date");
  }
}
