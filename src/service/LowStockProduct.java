package service;
public final class LowStockProduct {
  private final String productId, productName;
  private final int stock, threshold;
  public LowStockProduct(String productId, String productName, int stock, int threshold) {
    this.productId = productId;
    this.productName = productName;
    this.stock = stock;
    this.threshold = threshold;
  }
  public String getProductId() {
    return productId;
  }
  public String getProductName() {
    return productName;
  }
  public int getStock() {
    return stock;
  }
  public int getThreshold() {
    return threshold;
  }
}
