package service;

public final class UnknownCheckoutStatusException extends java.sql.SQLException {
  private static final long serialVersionUID = 1L;
  public UnknownCheckoutStatusException(String maHoaDon, java.sql.SQLException cause) {
    super("Chưa xác định kết quả thanh toán " + maHoaDon
        + ". Giữ nguyên giỏ và kiểm tra lại cùng mã hóa đơn.", "08007", cause);
  }
}
