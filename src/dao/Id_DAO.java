package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/** Reserves numeric identifiers; gaps are intentional, including on cancelled forms. */
public final class Id_DAO {
  private static final Map<String, String[]> TYPES = Map.of(
      "NV", new String[] {"NHAN_VIEN", "ma_nhan_vien"},
      "KH", new String[] {"KHACH_HANG", "ma_khach_hang"},
      "KM", new String[] {"KHUYEN_MAI", "ma_khuyen_mai"},
      "LSP", new String[] {"LOAI_SAN_PHAM", "ma_loai"},
      "NCC", new String[] {"NHA_CUNG_CAP", "ma_nha_cung_cap"},
      "PN", new String[] {"PHIEU_NHAP", "ma_phieu_nhap"},
      "DC", new String[] {"PHIEU_DIEU_CHINH_TON", "ma_dieu_chinh"},
      "HD", new String[] {"HOA_DON", "ma_hoa_don"});

  private Id_DAO() {}

  public static String taoMaTiepTheo(String prefix, Connection connection) throws SQLException {
    String[] type = TYPES.get(prefix);
    if (type == null) throw new IllegalArgumentException("Loại mã không hợp lệ");
    for (;;) {
      long number;
      try (PreparedStatement statement = connection.prepareStatement(
          "SELECT NEXT VALUE FOR dbo.POS_SEQ_" + prefix);
          ResultSet result = statement.executeQuery()) {
        result.next();
        number = result.getLong(1);
      } catch (SQLException exception) {
        if (exception.getErrorCode() == 11728)
          throw new IllegalStateException("Đã hết mã " + prefix
              + "; cần thay đổi quy định độ dài mã trước khi tạo thêm.", exception);
        throw exception;
      }
      String id = prefix + String.format(java.util.Locale.ROOT,
          "HD".equals(prefix) ? "%013d" : "%03d", number);
      try (PreparedStatement statement = connection.prepareStatement(
          "SELECT 1 FROM dbo." + type[0] + " WHERE " + type[1] + "=?")) {
        statement.setString(1, id);
        try (ResultSet result = statement.executeQuery()) {
          if (!result.next()) return id;
        }
      }
    }
  }
}
