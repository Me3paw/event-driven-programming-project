package service;
import java.math.BigDecimal;
public final class ReportSummary {
  private final BigDecimal revenue, purchaseSpend, cogs, grossProfit;
  private final long invoiceCount;
  public ReportSummary(
      BigDecimal revenue, long invoiceCount, BigDecimal purchaseSpend, BigDecimal cogs) {
    this.revenue = revenue;
    this.invoiceCount = invoiceCount;
    this.purchaseSpend = purchaseSpend;
    this.cogs = cogs;
    this.grossProfit = revenue.subtract(cogs);
  }
  public BigDecimal getRevenue() {
    return revenue;
  }
  public long getInvoiceCount() {
    return invoiceCount;
  }
  public BigDecimal getPurchaseSpend() {
    return purchaseSpend;
  }
  public BigDecimal getCogs() {
    return cogs;
  }
  public BigDecimal getGrossProfit() {
    return grossProfit;
  }
}
