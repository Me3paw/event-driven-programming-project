package entity;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class DieuChinhTon {
  private String maDieuChinh;
  private String maNhanVien;
  private LocalDateTime thoiDiemLap;
  private String lyDo;
  private Map<Long, Integer> soLuongGiamTheoLo = new LinkedHashMap<Long, Integer>();

  public String getMaDieuChinh() {
    return maDieuChinh;
  }
  public void setMaDieuChinh(String maDieuChinh) {
    this.maDieuChinh = maDieuChinh;
  }
  public String getMaNhanVien() {
    return maNhanVien;
  }
  public void setMaNhanVien(String maNhanVien) {
    this.maNhanVien = maNhanVien;
  }
  public LocalDateTime getThoiDiemLap() {
    return thoiDiemLap;
  }
  public void setThoiDiemLap(LocalDateTime thoiDiemLap) {
    this.thoiDiemLap = thoiDiemLap;
  }
  public String getLyDo() {
    return lyDo;
  }
  public void setLyDo(String lyDo) {
    this.lyDo = lyDo;
  }
  public Map<Long, Integer> getSoLuongGiamTheoLo() {
    return soLuongGiamTheoLo;
  }
  public void setSoLuongGiamTheoLo(Map<Long, Integer> soLuongGiamTheoLo) {
    this.soLuongGiamTheoLo = soLuongGiamTheoLo;
  }
}
