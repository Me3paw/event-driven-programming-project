package dao;

import entity.CauHinhCuaHang;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CauHinhCuaHangDAO {
  public CauHinhCuaHang get(Connection c) throws SQLException {
    try (PreparedStatement s =
             c.prepareStatement("SELECT ten_cua_hang,dia_chi,so_dien_thoai,ty_le_vat FROM "
                 + "CAU_HINH_CUA_HANG WHERE id=1")) {
      try (ResultSet r = s.executeQuery()) {
        if (!r.next())
          throw new IllegalStateException("Thiếu cấu hình cửa hàng");
        CauHinhCuaHang v = new CauHinhCuaHang();
        v.setTenCuaHang(r.getString(1));
        v.setDiaChi(r.getString(2));
        v.setSoDienThoai(r.getString(3));
        v.setTyLeVat(r.getBigDecimal(4));
        return v;
      }
    }
  }
  public void save(CauHinhCuaHang v, Connection c) throws SQLException {
    try (PreparedStatement s = c.prepareStatement(
             "UPDATE CAU_HINH_CUA_HANG SET ten_cua_hang=?,dia_chi=?,so_dien_thoai=?,ty_le_vat=? "
             + "WHERE id=1")) {
      s.setString(1, v.getTenCuaHang());
      s.setString(2, v.getDiaChi());
      s.setString(3, v.getSoDienThoai());
      s.setBigDecimal(4, v.getTyLeVat());
      if (s.executeUpdate() != 1)
        throw new IllegalStateException("Thiếu cấu hình cửa hàng");
    }
  }
}
