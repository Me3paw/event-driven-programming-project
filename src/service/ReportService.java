package service;

import connectDB.DBConnection;
import dao.BaoCaoDAO;
import dao.KhachHangDAO;
import entity.HangThanhVien;
import entity.KhachHang;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class ReportService {
  private final BaoCaoDAO dao = new BaoCaoDAO();
  private final KhachHangDAO customerDao = new KhachHangDAO();
  public ReportSummary summary(Actor actor, LocalDate tuNgay, LocalDate denNgay)
      throws SQLException {
    Access.manager(actor);
    return summary(tuNgay, denNgay);
  }
  public List<DailyRevenue> dailyRevenue(Actor actor, LocalDate tuNgay, LocalDate denNgay)
      throws SQLException {
    Access.manager(actor);
    return dailyRevenue(tuNgay, denNgay);
  }
  public List<ProductSales> topProducts(Actor actor, LocalDate tuNgay, LocalDate denNgay)
      throws SQLException {
    Access.manager(actor);
    return topProducts(tuNgay, denNgay);
  }
  public List<LowStockProduct> lowStock(Actor actor) throws SQLException {
    Access.manager(actor);
    try (Connection c = DBConnection.open()) {
      return dao.lowStock(c);
    }
  }
  public List<CustomerTierCount> customerTierCounts(Actor actor) throws SQLException {
    Access.manager(actor);
    try (Connection c = DBConnection.open()) {
      return dao.customerTierCounts(c);
    }
  }
  public List<KhachHang> customersByTier(Actor actor, String tier) throws SQLException {
    Access.manager(actor);
    if (!Access.is(tier, HangThanhVien.class))
      throw new IllegalArgumentException("Hạng khách hàng không hợp lệ");
    try (Connection c = DBConnection.open()) {
      return customerDao.byTier(tier, c);
    }
  }
  private ReportSummary summary(LocalDate tuNgay, LocalDate denNgay) throws SQLException {
    range(tuNgay, denNgay);
    try (Connection c = DBConnection.open()) {
      return dao.summary(tuNgay, denNgay, c);
    }
  }
  private List<DailyRevenue> dailyRevenue(LocalDate tuNgay, LocalDate denNgay) throws SQLException {
    range(tuNgay, denNgay);
    try (Connection c = DBConnection.open()) {
      return dao.dailyRevenue(tuNgay, denNgay, c);
    }
  }
  private List<ProductSales> topProducts(LocalDate tuNgay, LocalDate denNgay) throws SQLException {
    range(tuNgay, denNgay);
    try (Connection c = DBConnection.open()) {
      return dao.topProducts(tuNgay, denNgay, c);
    }
  }
  private void range(LocalDate from, LocalDate to) {
    if (from == null || to == null || from.isAfter(to))
      throw new IllegalArgumentException("Khoảng ngày không hợp lệ");
  }
}
