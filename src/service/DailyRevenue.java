package service;
import java.math.BigDecimal;
import java.time.LocalDate;
public final class DailyRevenue {
  private final LocalDate date;
  private final BigDecimal revenue;
  private final long invoiceCount;
  public DailyRevenue(LocalDate date, BigDecimal revenue, long invoiceCount) {
    this.date = date;
    this.revenue = revenue;
    this.invoiceCount = invoiceCount;
  }
  public LocalDate getDate() {
    return date;
  }
  public BigDecimal getRevenue() {
    return revenue;
  }
  public long getInvoiceCount() {
    return invoiceCount;
  }
}
