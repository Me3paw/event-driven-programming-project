package service;
import entity.HoaDon;
import entity.KhachHang;
import java.math.BigDecimal;
import java.util.List;
public final class CustomerProfile {
  private final KhachHang customer;
  private final BigDecimal actualSpend;
  private final String tier;
  private final List<HoaDon> history;
  public CustomerProfile(
      KhachHang customer, BigDecimal actualSpend, String tier, List<HoaDon> history) {
    this.customer = customer;
    this.actualSpend = actualSpend;
    this.tier = tier;
    this.history = history;
  }
  public KhachHang getCustomer() {
    return customer;
  }
  public BigDecimal getActualSpend() {
    return actualSpend;
  }
  public String getTier() {
    return tier;
  }
  public List<HoaDon> getHistory() {
    return history;
  }
}
