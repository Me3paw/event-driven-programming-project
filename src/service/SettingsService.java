package service;

import connectDB.DBConnection;
import dao.CauHinhCuaHangDAO;
import entity.CauHinhCuaHang;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;

public class SettingsService {
  private final CauHinhCuaHangDAO dao = new CauHinhCuaHangDAO();
  public CauHinhCuaHang lay(Actor actor) throws SQLException {
    Access.manager(actor);
    return lay();
  }
  public CauHinhCuaHang layChoHoaDon(Actor actor) throws SQLException {
    Access.actor(actor);
    return lay();
  }
  public void luu(Actor actor, CauHinhCuaHang value) throws SQLException {
    Access.manager(actor);
    luuInternal(actor, value);
  }
  private CauHinhCuaHang lay() throws SQLException {
    try (Connection c = DBConnection.open()) {
      return dao.get(c);
    }
  }
  private void luuInternal(Actor actor, CauHinhCuaHang value) throws SQLException {
    if (value == null)
      throw new IllegalArgumentException("Thiếu cấu hình cửa hàng");
    if (blank(value.getTenCuaHang()) || value.getTenCuaHang().trim().length() > 100)
      throw new IllegalArgumentException("Tên cửa hàng phải từ 1 đến 100 ký tự");
    if (blank(value.getDiaChi()) || value.getDiaChi().trim().length() > 255)
      throw new IllegalArgumentException("Địa chỉ phải từ 1 đến 255 ký tự");
    if (blank(value.getSoDienThoai())
        || !value.getSoDienThoai().trim().matches(ValidationRules.STORE_PHONE))
      throw new IllegalArgumentException("Số điện thoại không hợp lệ");
    BigDecimal rate = value.getTyLeVat();
    if (rate == null || rate.signum() < 0 || rate.compareTo(new BigDecimal("100")) > 0)
      throw new IllegalArgumentException("VAT không hợp lệ");
    try (SessionTransaction transaction = new SessionTransaction(actor, true, false)) {
      Connection c = transaction.connection();
      dao.save(value, c);
      transaction.commit();
    }
  }
  private boolean blank(String value) {
    return value == null || value.trim().isEmpty();
  }
}
