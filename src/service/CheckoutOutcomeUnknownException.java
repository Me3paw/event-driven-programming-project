package service;

public final class CheckoutOutcomeUnknownException extends java.sql.SQLException {
  private static final long serialVersionUID = 1L;
  public CheckoutOutcomeUnknownException(String invoiceId, java.sql.SQLException cause) {
    super("Chưa xác định kết quả thanh toán " + invoiceId
        + ". Giữ nguyên giỏ và kiểm tra lại cùng mã hóa đơn.", "08007", cause);
  }
}
