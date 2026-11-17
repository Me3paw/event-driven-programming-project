package service;
import java.math.BigDecimal;
public final class TongHopBaoCao {
  private final BigDecimal doanhThu, purchaseSpend, cogs, grossProfit;
  private final long invoiceCount;
  public TongHopBaoCao(
      BigDecimal doanhThu, long invoiceCount, BigDecimal purchaseSpend, BigDecimal cogs) {
    this.doanhThu = doanhThu;
    this.invoiceCount = invoiceCount;
    this.purchaseSpend = purchaseSpend;
    this.cogs = cogs;
    this.grossProfit = doanhThu.subtract(cogs);
  }
  public BigDecimal getDoanhThu() {
    return doanhThu;
  }
  public long getSoHoaDon() {
    return invoiceCount;
  }
  public BigDecimal getTienNhapHang() {
    return purchaseSpend;
  }
  public BigDecimal getGiaVon() {
    return cogs;
  }
  public BigDecimal getLoiNhuanGop() {
    return grossProfit;
  }
}
