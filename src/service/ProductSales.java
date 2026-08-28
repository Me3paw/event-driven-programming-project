package service;
import java.math.BigDecimal;
public final class ProductSales {
  private final String productId, productName;
  private final long quantity;
  private final BigDecimal revenue;
  public ProductSales(String productId, String productName, long quantity, BigDecimal revenue) {
    this.productId = productId;
    this.productName = productName;
    this.quantity = quantity;
    this.revenue = revenue;
  }
  public String getProductId() {
    return productId;
  }
  public String getProductName() {
    return productName;
  }
  public long getQuantity() {
    return quantity;
  }
  public BigDecimal getRevenue() {
    return revenue;
  }
}
