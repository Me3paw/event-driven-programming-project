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
    luu(value);
  }
  private CauHinhCuaHang lay() throws SQLException {
    try (Connection c = DBConnection.open()) {
      return dao.get(c);
    }
  }
  private void luu(CauHinhCuaHang value) throws SQLException {
    BigDecimal rate = value.getTyLeVat();
    if (rate == null || rate.signum() < 0 || rate.compareTo(new BigDecimal("100")) > 0)
      throw new IllegalArgumentException("VAT không hợp lệ");
    try (Connection c = DBConnection.open()) {
      dao.save(value, c);
    }
  }
}
