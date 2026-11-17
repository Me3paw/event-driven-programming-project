package testsupport;

import components.ReceiptPrintable;
import components.ReceiptPdfExporter;
import entity.*;
import service.*;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;
import static testsupport.TestFixtures.*;

public final class BackendSmoke {
  private BackendSmoke() {}
  public static void main(String[] args) throws Exception {
    TestDatabase.requireIsolated();
    try (TestFixtures fixtures = new TestFixtures()) {
      permissions(fixtures);
      invalidMasterSaves(fixtures.manager);
      invoiceScopeAndTiers(fixtures);
      pricesAndPromotion(fixtures);
      pointsAndRounding(fixtures);
      deletionAndHistory(fixtures);
      receipt(fixtures);
    }
    // Verify cleanup on a deliberately thrown exception as well as on a green run.
    try {
      try (TestFixtures fixtures = new TestFixtures()) {
        SanPham product = fixtures.product(1);
        fixtures.purchase(product, 2, 0, true);
        throw new IllegalStateException("injected fixture failure");
      }
    } catch (IllegalStateException expected) {
      check("injected fixture failure".equals(expected.getMessage()), "injected failure propagates after cleanup");
    }
    System.out.println("BackendSmoke checks: " + checks);
  }

  private static void invalidMasterSaves(final Actor manager) throws Exception {
    final MasterDataService master = new MasterDataService();
    invalid(new Action() {
      public void run() throws Exception {
        NhanVien value = new NhanVien();
        value.setTrangThai(TrangThaiNhanVien.DANG_LAM.name());
        master.luuNhanVien(manager, value);
      }
    });
    invalid(new Action() {
      public void run() throws Exception {
        TaiKhoan value = new TaiKhoan();
        value.setMaNhanVien("NV001");
        value.setVaiTro(VaiTro.THU_NGAN.name());
        value.setTrangThai(TrangThaiTaiKhoan.HOAT_DONG.name());
        master.luuTaiKhoan(manager, value, "MatKhau123".toCharArray());
      }
    });
    invalid(new Action() {
      public void run() throws Exception {
        LoaiSanPham value = new LoaiSanPham();
        value.setMaLoai("L-INVALID");
        value.setTrangThai(TrangThaiSanPham.DANG_KINH_DOANH.name());
        master.luuLoai(manager, value);
      }
    });
    invalid(new Action() {
      public void run() throws Exception {
        SanPham value = new SanPham();
        value.setMaSanPham("SP-INVALID");
        value.setMaLoai("LSP001");
        value.setTenSanPham("Sản phẩm kiểm thử");
        value.setDonViTinh("Cái");
        value.setGiaBan(BigDecimal.valueOf(-1));
        value.setTrangThai(TrangThaiSanPham.DANG_KINH_DOANH.name());
        master.luuSanPham(manager, value);
      }
    });
    invalid(new Action() {
      public void run() throws Exception {
        KhachHang value = new KhachHang();
        value.setMaKhachHang("KH-INVALID");
        value.setHoTen("Khách kiểm thử");
        value.setSoDienThoai("sai");
        value.setTrangThai(TrangThaiHoatDong.HOAT_DONG.name());
        master.luuKhachHang(manager, value);
      }
    });
    invalid(new Action() {
      public void run() throws Exception {
        NhaCungCap value = new NhaCungCap();
        value.setMaNhaCungCap("NCC-INVALID");
        value.setTrangThai(TrangThaiHoatDong.HOAT_DONG.name());
        master.luuNhaCungCap(manager, value);
      }
    });
  }
  private static void invalid(Action action) throws Exception {
    rejects(IllegalArgumentException.class, action, "invalid master data rejected");
  }

  private static void invoiceScopeAndTiers(TestFixtures f) throws Exception {
    Actor cashier = new AuthService().dangNhapActor("thungan01", "MatKhau123".toCharArray());
    SanPham product = f.product(1); f.purchase(product, 2, 0, true);
    HoaDon invoice = f.sales.thanhToan(f.manager, f.request(product, 1));
    check(!f.sales.timHoaDon(f.manager, invoice.getMaHoaDon(), f.manager.getMaNhanVien(), null,
        "DA_THANH_TOAN", "QR", LocalDate.now(), LocalDate.now()).isEmpty(), "manager invoice filters use live fixture");
    check(f.sales.timHoaDon(cashier, invoice.getMaHoaDon(), f.manager.getMaNhanVien(), null,
        "DA_THANH_TOAN", "QR", LocalDate.now(), LocalDate.now()).isEmpty(), "cashier cannot override employee invoice scope");
    rejects(SecurityException.class, () -> f.sales.chiTietHoaDon(cashier, invoice.getMaHoaDon()), "cashier cannot inspect another employee invoice");
    rejects(SecurityException.class, () -> f.sales.huyHoaDon(cashier, invoice.getMaHoaDon(), "Kiểm thử", "quanly", "MatKhau123".toCharArray()), "cashier cannot cancel invoice");
    TaiKhoan account = f.account("THU_NGAN");
    f.masters.xoaTaiKhoan(f.manager, account.getTenDangNhap());
    account.setTenDangNhap(f.track("TAI_KHOAN", "test_blank_" + account.getMaNhanVien()));
    rejects(IllegalArgumentException.class, () -> f.masters.luuTaiKhoan(f.manager, account, new char[0]), "new account requires password");
    List<CustomerTierCount> counts = new ReportService().customerTierCounts(f.manager);
    check(counts.size() == HangThanhVien.values().length, "report includes every membership tier");
    long total = 0;
    for (int index = 0; index < counts.size(); index++) {
      check(counts.get(index).getTier().equals(HangThanhVien.values()[index].name()), "membership tier order " + index);
      total += counts.get(index).getCount();
    }
    check(total == number("SELECT COUNT(*) FROM KHACH_HANG WHERE trang_thai='HOAT_DONG'"), "membership counts reconcile without fixed seed totals");
  }

  private static void permissions(TestFixtures f) throws Exception {
    Actor cashier = new AuthService().dangNhapActor("thungan01", "MatKhau123".toCharArray());
    rejects(SecurityException.class, () -> f.masters.timNhanVien(cashier, null, null), "cashier cannot manage staff");
    rejects(SecurityException.class, () -> new ReportService().summary(cashier, LocalDate.now(), LocalDate.now()), "cashier cannot read manager reports");
    rejects(SecurityException.class, () -> f.sales.baoGia(null, new YeuCauThanhToan()), "anonymous quote denied");
    check(!new InventoryService().tonKho(cashier, null).isEmpty(), "cashier can inspect stock");
    rejects(IllegalStateException.class, () -> f.masters.xoaTaiKhoan(f.manager, f.manager.getTenDangNhap()), "cannot delete current manager");
    rejects(IllegalStateException.class, () -> f.masters.capNhatTrangThaiNhanVien(f.manager, f.manager.getMaNhanVien(), "NGHI_VIEC"), "cannot remove current employee");
  }

  private static void pricesAndPromotion(TestFixtures f) throws Exception {
    SanPham free = f.product(0);
    f.purchase(free, 5, 0, true);
    HoaDon invoice = f.sales.thanhToan(f.manager, f.request(free, 1));
    check(invoice.getTongThanhToan().signum() == 0 && f.stock(free) == 4, "zero-price purchase and sale update stock consistently");
    KhuyenMai freePromotion = f.promotion("10", null, free.getMaSanPham());
    YeuCauThanhToan freeRequest = f.request(free, 1);
    freeRequest.setLoaiGiamGia("KHUYEN_MAI"); freeRequest.setMaKhuyenMai(freePromotion.getMaKhuyenMai());
    check(f.sales.baoGia(f.manager, freeRequest).getHoaDon().getTongThanhToan().signum() == 0,
        "zero-price product remains eligible for product promotion");
    free.setGiaBan(BigDecimal.valueOf(-1));
    rejects(IllegalArgumentException.class, () -> f.masters.luuSanPham(f.manager, free), "negative sale price rejected");
    PhieuNhap purchase = f.purchase(free, 1, 0, false);
    purchase.getChiTiet().get(0).setDonGiaNhap(BigDecimal.valueOf(-1));
    rejects(IllegalArgumentException.class, () -> f.purchases.luuNhap(f.manager, purchase), "negative import price rejected");
    SanPham product = f.product(10); f.purchase(product, 100, 0, true);
    KhuyenMai promotion = f.promotion("10", 20L);
    check(promotion.getTenKhuyenMai().matches(ValidationRules.PROMOTION_NAME), "shared promotion validator accepts percent sign");
    YeuCauThanhToan request = f.request(product, 1);
    request.setLoaiGiamGia("KHUYEN_MAI"); request.setMaKhuyenMai(promotion.getMaKhuyenMai());
    rejects(IllegalArgumentException.class, () -> f.sales.baoGia(f.manager, request), "promotion below minimum rejected");
    request.getDongHang().get(0).setSoLuong(2);
    check(f.sales.baoGia(f.manager, request).getHoaDon().getTienGiam().intValueExact() == 2, "promotion exact minimum accepted");
    request.getDongHang().get(0).setSoLuong(3);
    check(f.sales.thanhToan(f.manager, request).getTienGiam().intValueExact() == 3, "promotion above minimum accepted at checkout");
    promotion.setDonToiThieu(null);
    rejects(IllegalArgumentException.class, () -> new PromotionService().luu(f.manager, promotion), "whole-order minimum remains mandatory");
    promotion.setDonToiThieu(BigDecimal.TEN); new PromotionService().luu(f.manager, promotion);
    check(true, "10 VND whole-order minimum is valid");
    promotion.setTenKhuyenMai("Giảm giá");
    rejects(IllegalArgumentException.class, () -> new PromotionService().luu(f.manager, promotion), "promotion prefix required");
    promotion.setTenKhuyenMai("Khuyến mãi " + "a".repeat(151));
    rejects(IllegalArgumentException.class, () -> new PromotionService().luu(f.manager, promotion), "promotion length limit enforced");
  }

  private static void pointsAndRounding(TestFixtures f) throws Exception {
    SanPham product = f.product(15); f.purchase(product, 100, 0, true);
    KhuyenMai promotion = f.promotion("3.30", 1L);
    YeuCauThanhToan request = f.request(product, 1);
    request.setLoaiGiamGia("KHUYEN_MAI"); request.setMaKhuyenMai(promotion.getMaKhuyenMai());
    check(f.sales.baoGia(f.manager, request).getHoaDon().getTienGiam().signum() == 0, "15 x 3.30 percent rounds once to zero");
    f.vat("3.30"); request.setLoaiGiamGia("KHONG"); request.setMaKhuyenMai(null);
    check(f.sales.baoGia(f.manager, request).getHoaDon().getTienVat().signum() == 0, "VAT percentage rounds once");
    for (String rate : new String[] {"3.33", "3.34", "3.40"}) {
      f.vat(rate);
      check(f.sales.baoGia(f.manager, request).getHoaDon().getTienVat().intValueExact()
          == (rate.equals("3.33") ? 0 : 1), "VAT HALF_UP boundary " + rate);
    }
    SanPham hundred = f.product(100); f.purchase(hundred, 10, 0, true);
    YeuCauThanhToan half = f.request(hundred, 1);
    for (String rate : new String[] {"0.49", "0.50", "0.51"}) {
      f.vat(rate);
      check(f.sales.baoGia(f.manager, half).getHoaDon().getTienVat().intValueExact()
          == (rate.equals("0.49") ? 0 : 1), "exact half VND boundary " + rate);
    }
    f.vat("10");
    SanPham fiveA = f.product(5), fiveB = f.product(5);
    f.purchase(fiveA, 2, 0, true); f.purchase(fiveB, 2, 0, true);
    KhuyenMai allocationPromotion = f.promotion("10", 1L);
    YeuCauThanhToan allocated = f.request(fiveA, 1);
    DongThanhToan extra = new DongThanhToan(); extra.setMaSanPham(fiveB.getMaSanPham()); extra.setSoLuong(1);
    allocated.getDongHang().add(extra); allocated.setLoaiGiamGia("KHUYEN_MAI");
    allocated.setMaKhuyenMai(allocationPromotion.getMaKhuyenMai());
    HoaDon allocatedInvoice = f.sales.thanhToan(f.manager, allocated);
    BigDecimal lineSum = BigDecimal.ZERO;
    for (ChiTietHoaDon detail : allocatedInvoice.getChiTiet()) lineSum = lineSum.add(detail.getTienGiamDong());
    check(lineSum.compareTo(allocatedInvoice.getTienGiam()) == 0 && lineSum.intValueExact() == 1,
        "line discount allocation sums exactly to header discount");
    BigDecimal historicalTotal = allocatedInvoice.getTongThanhToan();
    f.vat("99"); fiveA.setGiaBan(BigDecimal.valueOf(999)); f.masters.luuSanPham(f.manager, fiveA);
    check(f.sales.chiTietHoaDon(f.manager, allocatedInvoice.getMaHoaDon()).getTongThanhToan().compareTo(historicalTotal) == 0,
        "saved invoice unchanged after price and VAT updates");
    f.vat("10");
    SanPham expensive = f.product(10000); f.purchase(expensive, 100, 0, true);
    String customer = f.customer(5000, 0);
    request = f.request(expensive, 1); request.setMaKhachHang(customer);
    final YeuCauThanhToan points = request;
    for (long used : new long[] {1, 999, 1000, 5000}) {
      points.setDiemThuongSuDung(used);
      HoaDon quote = f.sales.baoGia(f.manager, points).getHoaDon();
      check(quote.getDiemSuDung() == used && quote.getTienVat().longValueExact()
          == BigDecimal.valueOf(10000 - used).multiply(new BigDecimal("0.10")).setScale(0, java.math.RoundingMode.HALF_UP).longValueExact(),
          "whole-point redemption and post-points VAT " + used);
    }
    points.setDiemThuongSuDung(-1);
    rejects(IllegalArgumentException.class, () -> f.sales.baoGia(f.manager, points), "negative points rejected");
    points.setDiemThuongSuDung(5001);
    rejects(IllegalArgumentException.class, () -> f.sales.baoGia(f.manager, points), "over-balance points rejected");
    points.setDiemThuongSuDung(1); points.setMaKhachHang(null);
    rejects(IllegalArgumentException.class, () -> f.sales.baoGia(f.manager, points), "guest cannot redeem points");
    for (long loyalty : new long[] {49999, 50000, 200000, 1000000}) {
      sql("UPDATE KHACH_HANG SET diem_tich_luy=? WHERE ma_khach_hang=?", loyalty, customer);
      points.setMaKhachHang(customer); points.setDiemThuongSuDung(0); points.setLoaiGiamGia("HANG_THANH_VIEN");
      int expected = loyalty == 49999 ? 0 : loyalty == 50000 ? 3 : loyalty == 200000 ? 5 : 10;
      check(f.sales.baoGia(f.manager, points).getHoaDon().getTyLeGiam().intValueExact() == expected, "membership boundary " + loyalty);
    }
    points.setMaHoaDon("HD901");
    rejects(IllegalArgumentException.class, () -> f.sales.baoGia(f.manager, points), "malformed invoice ID rejected at quote");
    points.setMaHoaDon(null);
    long before = number("SELECT CONVERT(bigint,current_value) FROM sys.sequences WHERE name='POS_SEQ_HD'");
    for (int index = 0; index < 3; index++) f.sales.baoGia(f.manager, points);
    check(points.getMaHoaDon() == null && before == number("SELECT CONVERT(bigint,current_value) FROM sys.sequences WHERE name='POS_SEQ_HD'"), "quotation does not consume invoice IDs");
  }

  private static void deletionAndHistory(TestFixtures f) throws Exception {
    String customer = f.customer(0, 0); f.masters.xoaKhachHang(f.manager, customer);
    check(number("SELECT COUNT(*) FROM KHACH_HANG WHERE ma_khach_hang=?", customer) == 0, "unused customer can be deleted");
    TaiKhoan account = f.account("THU_NGAN");
    rejects(IllegalStateException.class, () -> f.masters.xoaNhanVien(f.manager, account.getMaNhanVien()), "employee with account reference cannot be deleted");
    f.masters.xoaTaiKhoan(f.manager, account.getTenDangNhap()); f.masters.xoaNhanVien(f.manager, account.getMaNhanVien());
    check(number("SELECT COUNT(*) FROM NHAN_VIEN WHERE ma_nhan_vien=?", account.getMaNhanVien()) == 0, "unused account then employee can be deleted");
    NhaCungCap supplier = new NhaCungCap(); supplier.setMaNhaCungCap(f.track("NHA_CUNG_CAP", f.masters.maNhaCungCapTiepTheo(f.manager)));
    supplier.setTenNhaCungCap("Nhà cung cấp chưa dùng"); supplier.setTrangThai("HOAT_DONG");
    f.masters.luuNhaCungCap(f.manager, supplier); f.masters.xoaNhaCungCap(f.manager, supplier.getMaNhaCungCap());
    check(number("SELECT COUNT(*) FROM NHA_CUNG_CAP WHERE ma_nha_cung_cap=?", supplier.getMaNhaCungCap()) == 0, "unused supplier can be deleted");
    SanPham product = f.product(10000); f.purchase(product, 5, 0, true);
    String customerWithHistory = f.customer(0, 0);
    YeuCauThanhToan request = f.request(product, 1); request.setMaKhachHang(customerWithHistory);
    HoaDon invoice = f.sales.thanhToan(f.manager, request);
    rejects(IllegalStateException.class, () -> f.masters.xoaSanPham(f.manager, product.getMaSanPham()), "referenced product preserved");
    rejects(IllegalStateException.class, () -> f.masters.xoaKhachHang(f.manager, customerWithHistory), "customer invoice history preserved");
    rejects(IllegalStateException.class, () -> f.masters.xoaNhaCungCap(f.manager, f.supplier()), "supplier purchase history preserved");
    product.setTrangThai("NGUNG_KINH_DOANH"); f.masters.luuSanPham(f.manager, product);
    List<ProductSales> top = new ReportService().topProducts(f.manager, LocalDate.now(), LocalDate.now());
    check(top.stream().anyMatch(row -> product.getMaSanPham().equals(row.getProductId()) && row.getQuantity() == 1), "discontinued product stays in historical best sellers");
    f.cancel(invoice);
    check(new ReportService().topProducts(f.manager, LocalDate.now(), LocalDate.now()).stream()
        .noneMatch(row -> product.getMaSanPham().equals(row.getProductId())), "cancelled invoice removed from best sellers");
  }

  private static void receipt(TestFixtures f) throws Exception {
    HoaDon invoice = new HoaDon(); invoice.setMaHoaDon("HD0000000000001"); invoice.setMaKhachHang("KH001");
    invoice.setDiemTichLuyKiem(10000); invoice.setDiemThuongKiem(500);
    ReceiptPrintable receipt = new ReceiptPrintable(new SettingsService().lay(f.manager), invoice);
    check(receipt.lines().contains("Điểm tích lũy lần này: 10000") && receipt.lines().contains("Điểm thưởng nhận được: 500"), "receipt distinguishes loyalty and reward points");
    invoice.setMaKhachHang(null);
    check(new ReceiptPrintable(new SettingsService().lay(f.manager), invoice).lines().stream().noneMatch(line -> line.startsWith("Điểm")), "guest receipt does not imply points award");
    for (int index = 0; index < 100; index++) {
      ChiTietHoaDon line = new ChiTietHoaDon(); line.setMaSanPham("893000000001");
      line.setTenSanPham("Sản phẩm tiếng Việt rất dài " + index + " " + "X".repeat(160));
      line.setSoLuong(1); invoice.getChiTiet().add(line);
    }
    receipt = new ReceiptPrintable(new SettingsService().lay(f.manager), invoice);
    Paper paper = new Paper(); paper.setSize(300, 400); paper.setImageableArea(20, 30, 260, 330);
    PageFormat format = new PageFormat(); format.setPaper(paper);
    int pages = 0;
    for (; pages < 200; pages++) {
      BufferedImage image = new BufferedImage(300, 400, BufferedImage.TYPE_INT_RGB);
      Graphics2D graphics = image.createGraphics(); graphics.setColor(Color.WHITE); graphics.fillRect(0, 0, 300, 400);
      int result = receipt.print(graphics, format, pages); graphics.dispose();
      if (result == Printable.NO_SUCH_PAGE) break;
      boolean ink = false;
      for (int y = 0; y < 400; y++) for (int x = 0; x < 300; x++) if (image.getRGB(x, y) != Color.WHITE.getRGB()) {
        if (x < 20 || x >= 280 || y < 30 || y >= 360) throw new AssertionError("Receipt exceeds imageable area");
        ink = true;
      }
      if (!ink) throw new AssertionError("Blank printed page " + pages);
    }
    check(pages > 1 && pages < 200, "long receipt paginates with visible content inside imageable bounds");
    java.nio.file.Path pdf = Files.createTempFile("pos-receipt-", ".pdf");
    try { ReceiptPdfExporter.export(receipt, pdf.toFile()); check(Files.size(pdf) > 1000, "receipt PDF export remains functional"); }
    finally { Files.deleteIfExists(pdf); }
  }
}
