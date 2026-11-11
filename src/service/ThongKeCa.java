package service;

import java.math.BigDecimal;

public final class ThongKeCa {
    private long soHoaDon;
    private BigDecimal doanhThu = BigDecimal.ZERO;
    private BigDecimal tienMat = BigDecimal.ZERO;
    private BigDecimal tienThe = BigDecimal.ZERO;
    private long soKhachMua;

    public long getSoHoaDon() {
        return soHoaDon;
    }

    public void setSoHoaDon(long soHoaDon) {
        this.soHoaDon = soHoaDon;
    }

    public BigDecimal getDoanhThu() {
        return doanhThu;
    }

    public void setDoanhThu(BigDecimal doanhThu) {
        this.doanhThu = doanhThu;
    }

    public BigDecimal getTienMat() {
        return tienMat;
    }

    public void setTienMat(BigDecimal tienMat) {
        this.tienMat = tienMat;
    }

    public BigDecimal getTienThe() {
        return tienThe;
    }

    public void setTienThe(BigDecimal tienThe) {
        this.tienThe = tienThe;
    }

    public long getSoKhachMua() {
        return soKhachMua;
    }

    public void setSoKhachMua(long soKhachMua) {
        this.soKhachMua = soKhachMua;
    }
}
