package testsupport;

import connectDB.DBConnection;
import entity.HangThanhVien;
import entity.HoaDon;
import entity.KhachHang;
import entity.LoaiGiamGia;
import entity.LoaiSanPham;
import entity.NhaCungCap;
import entity.NhanVien;
import entity.SanPham;
import entity.TaiKhoan;
import entity.TrangThaiHoaDon;
import entity.TrangThaiHoatDong;
import entity.TrangThaiNhanVien;
import entity.TrangThaiSanPham;
import entity.TrangThaiTaiKhoan;
import entity.VaiTro;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.List;
import service.Actor;
import service.AuthService;
import service.CustomerTierCount;
import service.DongThanhToan;
import service.InventoryService;
import service.MasterDataService;
import service.PromotionService;
import service.PurchaseService;
import service.ReportService;
import service.SaleService;
import service.SettingsService;
import service.YeuCauThanhToan;

public final class BackendSmoke {
  private interface Action {
    void run() throws Exception;
  }

  private BackendSmoke() {}

  public static void main(String[] args) throws Exception {
    AuthService auth = new AuthService();
    Actor manager = auth.dangNhapActor("quanly", "MatKhau123".toCharArray());
    Actor cashier = auth.dangNhapActor("thungan", "MatKhau123".toCharArray());
    if (!manager.isQuanLy() || cashier.isQuanLy())
      throw new AssertionError("Đăng nhập vai trò thất bại");
    YeuCauThanhToan request = new YeuCauThanhToan();
    request.setMaHoaDon("T" + System.currentTimeMillis());
    request.setLoaiGiamGia(LoaiGiamGia.KHONG.name());
    request.setDiemThuongSuDung(0);
    request.setPhuongThucThanhToan("QR");
    request.setThanhToanDienTuThanhCong(true);
    request.setMaThamChieuThanhToan("SMOKE-" + System.currentTimeMillis());
    DongThanhToan line = new DongThanhToan();
    line.setMaSanPham("SP001");
    line.setSoLuong(1);
    request.getDongHang().add(line);
    SaleService sales = new SaleService();
    deny(new Action() {
      public void run() throws Exception {
        sales.baoGia(null, request);
      }
    });
    deny(new Action() {
      public void run() throws Exception {
        new MasterDataService().timNhanVien(cashier, null, null);
      }
    });
    deny(new Action() {
      public void run() throws Exception {
        new PurchaseService().tim(cashier, null, null);
      }
    });
    if (new InventoryService().tonKho(cashier, null).isEmpty())
      throw new AssertionError("Thu ngân phải xem được tồn kho");
    deny(new Action() {
      public void run() throws Exception {
        new InventoryService().giamTonFifo(cashier, "DC-BYPASS", "SP001", 1, "Kiem thu");
      }
    });
    deny(new Action() {
      public void run() throws Exception {
        new InventoryService().timDieuChinh(cashier, null);
      }
    });
    deny(new Action() {
      public void run() throws Exception {
        new InventoryService().chiTietDieuChinh(cashier, "DC001");
      }
    });
    deny(new Action() {
      public void run() throws Exception {
        new PromotionService().tim(cashier, null, null);
      }
    });
    deny(new Action() {
      public void run() throws Exception {
        new ReportService().summary(cashier, null, null);
      }
    });
    deny(new Action() {
      public void run() throws Exception {
        new SettingsService().lay(cashier);
      }
    });
    if (new MasterDataService().timLoai(cashier, "LSP001").isEmpty()
        || new MasterDataService().timNhanVien(manager, "NV001", null).isEmpty()
        || new MasterDataService().timNhaCungCap(manager, "NCC001").isEmpty()
        || new MasterDataService().timKhachHang(cashier, "KH001", null).isEmpty())
      throw new AssertionError("Tìm kiếm mã tự nhiên thất bại");
    invalidMasterSaves(manager);
    tiers(manager);
    TaiKhoan blankPassword = new TaiKhoan();
    blankPassword.setTenDangNhap("blank-password");
    blankPassword.setMaNhanVien("NV001");
    blankPassword.setVaiTro(VaiTro.THU_NGAN.name());
    blankPassword.setTrangThai(TrangThaiTaiKhoan.HOAT_DONG.name());
    invalid(new Action() {
      public void run() throws Exception {
        new MasterDataService().luuTaiKhoan(manager, blankPassword, new char[0]);
      }
    });
    HoaDon invoice = null;
    try {
      invoice = sales.thanhToan(manager, request);
      if (!TrangThaiHoaDon.DA_THANH_TOAN.name().equals(invoice.getTrangThai()))
        throw new AssertionError("Thanh toán thất bại");
      if (sales
              .timHoaDon(manager, invoice.getMaHoaDon(), "NV001", null,
                  TrangThaiHoaDon.DA_THANH_TOAN.name(), "QR", LocalDate.now(), LocalDate.now())
              .isEmpty()
          || !sales
              .timHoaDon(cashier, invoice.getMaHoaDon(), "NV001", null,
                  TrangThaiHoaDon.DA_THANH_TOAN.name(), "QR", LocalDate.now(), LocalDate.now())
              .isEmpty())
        throw new AssertionError("Lọc hóa đơn hoặc phạm vi thu ngân sai");
      final HoaDon saved = invoice;
      deny(new Action() {
        public void run() throws Exception {
          sales.chiTietHoaDon(cashier, saved.getMaHoaDon());
        }
      });
      deny(new Action() {
        public void run() throws Exception {
          sales.huyHoaDon(
              cashier, saved.getMaHoaDon(), "Kiem thu", "quanly", "MatKhau123".toCharArray());
        }
      });
      sales.huyHoaDon(
          manager, invoice.getMaHoaDon(), "Kiem thu", "quanly", "MatKhau123".toCharArray());
    } finally {
      if (invoice != null)
        cleanup(invoice.getMaHoaDon());
    }
  }

  private static void deny(Action action) throws Exception {
    try {
      action.run();
    } catch (SecurityException expected) {
      return;
    }
    throw new AssertionError("Bypass quyền không bị chặn");
  }
  private static void invalid(Action action) throws Exception {
    try {
      action.run();
    } catch (IllegalArgumentException expected) {
      return;
    }
    throw new AssertionError("Dữ liệu không hợp lệ không bị chặn");
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
  private static void tiers(Actor manager) throws Exception {
    List<CustomerTierCount> values = new ReportService().customerTierCounts(manager);
    HangThanhVien[] expected = HangThanhVien.values();
    if (values.size() != expected.length)
      throw new AssertionError("Thiếu hạng khách hàng trong báo cáo");
    for (int i = 0; i < expected.length; i++) {
      if (!expected[i].name().equals(values.get(i).getTier()))
        throw new AssertionError("Thứ tự hạng khách hàng không ổn định");
      if (expected[i] != HangThanhVien.CHUA_XEP_HANG && values.get(i).getCount() != 0)
        throw new AssertionError("Hạng không có khách phải trả về 0");
    }
  }
  private static void cleanup(String id) throws Exception {
    try (Connection c = DBConnection.open()) {
      c.setAutoCommit(false);
      try {
        try (PreparedStatement s =
                 c.prepareStatement("DELETE FROM PHAN_BO_XUAT_LO WHERE ma_hoa_don=?")) {
          s.setString(1, id);
          s.executeUpdate();
        }
        try (PreparedStatement s =
                 c.prepareStatement("DELETE FROM CHI_TIET_HOA_DON WHERE ma_hoa_don=?")) {
          s.setString(1, id);
          s.executeUpdate();
        }
        try (PreparedStatement s = c.prepareStatement("DELETE FROM HOA_DON WHERE ma_hoa_don=?")) {
          s.setString(1, id);
          s.executeUpdate();
        }
        c.commit();
      } catch (Exception e) {
        c.rollback();
        throw e;
      } finally {
        c.setAutoCommit(true);
      }
    }
  }
}
