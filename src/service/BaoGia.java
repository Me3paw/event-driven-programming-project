package service;

import entity.HoaDon;

public class BaoGia {
  private HoaDon hoaDon;
  public BaoGia(HoaDon hoaDon) {
    this.hoaDon = hoaDon;
  }
  public HoaDon getHoaDon() {
    return hoaDon;
  }
}
