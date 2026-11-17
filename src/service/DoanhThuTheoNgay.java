package service;
import java.math.BigDecimal;
import java.time.LocalDate;
public final class DoanhThuTheoNgay {
  private final LocalDate date;
  private final BigDecimal doanhThu;
  private final long invoiceCount;
  public DoanhThuTheoNgay(LocalDate date, BigDecimal doanhThu, long invoiceCount) {
    this.date = date;
    this.doanhThu = doanhThu;
    this.invoiceCount = invoiceCount;
  }
  public LocalDate getNgay() {
    return date;
  }
  public BigDecimal getDoanhThu() {
    return doanhThu;
  }
  public long getSoHoaDon() {
    return invoiceCount;
  }
}
