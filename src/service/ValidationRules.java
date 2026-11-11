package service;

final class ValidationRules {
  static final String PHONE = "\\+?[0-9][0-9 .()\\-]{5,19}";
  static final String STORE_PHONE = "[0-9+() .-]{8,20}";
  static final String VN_PHONE = "^0[35789][0-9]{8}$";
  static final String PERSON_NAME = "^[\\p{L}\\s]{1,100}$";
  static final String PROMOTION_NAME = "^Khuyến mãi[\\p{L}0-9\\s-]*$";
  static final String EMPLOYEE_ID = "NV[0-9]{3}";
  static final String CUSTOMER_ID = "KH[0-9]{3}";
  static final String PRODUCT_ID = "893[0-9]{9}";
  static final String CATEGORY_ID = "LSP[0-9]{3,}";
  static final String SUPPLIER_ID = "NCC[0-9]{3,}";
  static final String PROMOTION_ID = "KM[0-9]{3}";
  static final String INVOICE_ID = "HD[0-9]{3,}";
  static final String PURCHASE_ID = "PN[0-9]{3,}";
  static final String ADJUSTMENT_ID = "DC[0-9]{3,}";

  private ValidationRules() {
  }

  static boolean matches(String value, String regex) {
    return value != null && value.trim().matches(regex);
  }
}
