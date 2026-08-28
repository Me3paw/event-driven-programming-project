CREATE TABLE NHAN_VIEN (
    ma_nhan_vien VARCHAR(20) PRIMARY KEY,
    ho_ten VARCHAR(100) NOT NULL,
    so_dien_thoai VARCHAR(20),
    chuc_vu VARCHAR(30),
    trang_thai VARCHAR(20) NOT NULL,
    CHECK (trang_thai IN ('DANG_LAM', 'NGHI_VIEC'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE TAI_KHOAN (
    ten_dang_nhap VARCHAR(50) PRIMARY KEY,
    mat_khau_bam VARCHAR(255) NOT NULL,
    vai_tro VARCHAR(20) NOT NULL,
    trang_thai VARCHAR(20) NOT NULL,
    ma_nhan_vien VARCHAR(20) NOT NULL UNIQUE,
    CHECK (vai_tro IN ('THU_NGAN', 'QUAN_LY')),
    CHECK (trang_thai IN ('HOAT_DONG', 'KHOA')),
    FOREIGN KEY (ma_nhan_vien) REFERENCES NHAN_VIEN(ma_nhan_vien)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE LOAI_SAN_PHAM (
    ma_loai VARCHAR(20) PRIMARY KEY,
    ten_loai VARCHAR(100) NOT NULL UNIQUE,
    mo_ta VARCHAR(255),
    trang_thai VARCHAR(20) NOT NULL,
    CHECK (trang_thai IN ('DANG_KINH_DOANH', 'NGUNG_KINH_DOANH'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE SAN_PHAM (
    ma_san_pham VARCHAR(20) PRIMARY KEY,
    ma_loai VARCHAR(20) NOT NULL,
    ten_san_pham VARCHAR(150) NOT NULL,
    don_vi_tinh VARCHAR(30) NOT NULL,
    gia_ban DECIMAL(15,0) NOT NULL,
    nguong_ton INT NOT NULL,
    trang_thai VARCHAR(20) NOT NULL,
    CHECK (gia_ban >= 0),
    CHECK (nguong_ton >= 0),
    CHECK (trang_thai IN ('DANG_KINH_DOANH', 'NGUNG_KINH_DOANH')),
    FOREIGN KEY (ma_loai) REFERENCES LOAI_SAN_PHAM(ma_loai)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE KHACH_HANG (
    ma_khach_hang VARCHAR(20) PRIMARY KEY,
    ho_ten VARCHAR(100) NOT NULL,
    so_dien_thoai VARCHAR(20) UNIQUE,
    diem_hien_co BIGINT NOT NULL DEFAULT 0,
    diem_tich_luy BIGINT NOT NULL DEFAULT 0,
    trang_thai VARCHAR(20) NOT NULL,
    CHECK (diem_hien_co >= 0),
    CHECK (diem_tich_luy >= 0),
    CHECK (trang_thai IN ('HOAT_DONG', 'NGUNG_HOAT_DONG'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE NHA_CUNG_CAP (
    ma_nha_cung_cap VARCHAR(20) PRIMARY KEY,
    ten_nha_cung_cap VARCHAR(150) NOT NULL,
    so_dien_thoai VARCHAR(20),
    dia_chi VARCHAR(255),
    trang_thai VARCHAR(20) NOT NULL,
    CHECK (trang_thai IN ('HOAT_DONG', 'NGUNG_HOAT_DONG'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE CAU_HINH_CUA_HANG (
    id TINYINT PRIMARY KEY,
    ten_cua_hang VARCHAR(150) NOT NULL,
    dia_chi VARCHAR(255) NOT NULL,
    so_dien_thoai VARCHAR(20) NOT NULL,
    ty_le_vat DECIMAL(5,2) NOT NULL,
    CHECK (id = 1),
    CHECK (ty_le_vat BETWEEN 0 AND 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE KHUYEN_MAI (
    ma_khuyen_mai VARCHAR(20) PRIMARY KEY,
    ten_khuyen_mai VARCHAR(150) NOT NULL,
    pham_vi VARCHAR(20) NOT NULL,
    ty_le_giam DECIMAL(5,2) NOT NULL,
    bat_dau DATETIME NOT NULL,
    ket_thuc DATETIME NOT NULL,
    trang_thai VARCHAR(20) NOT NULL,
    CHECK (pham_vi IN ('TOAN_DON', 'THEO_SAN_PHAM')),
    CHECK (ty_le_giam > 0 AND ty_le_giam <= 100),
    CHECK (bat_dau < ket_thuc),
    CHECK (trang_thai IN ('DANG_HOAT_DONG', 'NGUNG_HOAT_DONG'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE KHUYEN_MAI_SAN_PHAM (
    ma_khuyen_mai VARCHAR(20) NOT NULL,
    ma_san_pham VARCHAR(20) NOT NULL,
    PRIMARY KEY (ma_khuyen_mai, ma_san_pham),
    FOREIGN KEY (ma_khuyen_mai) REFERENCES KHUYEN_MAI(ma_khuyen_mai),
    FOREIGN KEY (ma_san_pham) REFERENCES SAN_PHAM(ma_san_pham)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE PHIEU_NHAP (
    ma_phieu_nhap VARCHAR(20) PRIMARY KEY,
    ma_nha_cung_cap VARCHAR(20) NOT NULL,
    ma_nhan_vien VARCHAR(20) NOT NULL,
    thoi_diem_lap DATETIME NOT NULL,
    thoi_diem_hoan_tat DATETIME,
    trang_thai VARCHAR(20) NOT NULL,
    tong_tien DECIMAL(15,0) NOT NULL,
    CHECK (trang_thai IN ('NHAP', 'HOAN_TAT')),
    CHECK (tong_tien >= 0),
    FOREIGN KEY (ma_nha_cung_cap) REFERENCES NHA_CUNG_CAP(ma_nha_cung_cap),
    FOREIGN KEY (ma_nhan_vien) REFERENCES NHAN_VIEN(ma_nhan_vien)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE CHI_TIET_PHIEU_NHAP (
    ma_phieu_nhap VARCHAR(20) NOT NULL,
    ma_san_pham VARCHAR(20) NOT NULL,
    so_luong INT NOT NULL,
    don_gia_nhap DECIMAL(15,0) NOT NULL,
    PRIMARY KEY (ma_phieu_nhap, ma_san_pham),
    CHECK (so_luong > 0),
    CHECK (don_gia_nhap >= 0),
    FOREIGN KEY (ma_phieu_nhap) REFERENCES PHIEU_NHAP(ma_phieu_nhap),
    FOREIGN KEY (ma_san_pham) REFERENCES SAN_PHAM(ma_san_pham)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE LO_TON_KHO (
    ma_lo BIGINT PRIMARY KEY AUTO_INCREMENT,
    ma_phieu_nhap VARCHAR(20) NOT NULL,
    ma_san_pham VARCHAR(20) NOT NULL,
    so_luong_nhap INT NOT NULL,
    so_luong_con INT NOT NULL,
    thoi_diem_nhap DATETIME NOT NULL,
    UNIQUE (ma_phieu_nhap, ma_san_pham),
    CHECK (so_luong_nhap > 0),
    CHECK (so_luong_con BETWEEN 0 AND so_luong_nhap),
    FOREIGN KEY (ma_phieu_nhap, ma_san_pham)
        REFERENCES CHI_TIET_PHIEU_NHAP(ma_phieu_nhap, ma_san_pham)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE DIEU_CHINH_TON (
    ma_dieu_chinh VARCHAR(20) PRIMARY KEY,
    ma_nhan_vien VARCHAR(20) NOT NULL,
    thoi_diem_lap DATETIME NOT NULL,
    ly_do VARCHAR(255) NOT NULL,
    FOREIGN KEY (ma_nhan_vien) REFERENCES NHAN_VIEN(ma_nhan_vien)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE CHI_TIET_DIEU_CHINH (
    ma_dieu_chinh VARCHAR(20) NOT NULL,
    ma_lo BIGINT NOT NULL,
    so_luong_giam INT NOT NULL,
    PRIMARY KEY (ma_dieu_chinh, ma_lo),
    CHECK (so_luong_giam > 0),
    FOREIGN KEY (ma_dieu_chinh) REFERENCES DIEU_CHINH_TON(ma_dieu_chinh),
    FOREIGN KEY (ma_lo) REFERENCES LO_TON_KHO(ma_lo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE HOA_DON (
    ma_hoa_don VARCHAR(20) PRIMARY KEY,
    ma_nhan_vien VARCHAR(20) NOT NULL,
    ma_khach_hang VARCHAR(20),
    ma_khuyen_mai VARCHAR(20),
    ma_nhan_vien_huy VARCHAR(20),
    thoi_diem_lap DATETIME NOT NULL,
    thoi_diem_huy DATETIME,
    phuong_thuc_tt VARCHAR(20) NOT NULL,
    ma_tham_chieu_tt VARCHAR(100),
    tong_gia_goc DECIMAL(15,0) NOT NULL,
    loai_giam_gia VARCHAR(20) NOT NULL,
    ty_le_giam DECIMAL(5,2) NOT NULL,
    tien_giam DECIMAL(15,0) NOT NULL,
    diem_su_dung BIGINT NOT NULL,
    tien_giam_diem DECIMAL(15,0) NOT NULL,
    diem_tich_luy_kiem BIGINT NOT NULL,
    diem_thuong_kiem BIGINT NOT NULL,
    ty_le_vat DECIMAL(5,2) NOT NULL,
    tien_vat DECIMAL(15,0) NOT NULL,
    tong_thanh_toan DECIMAL(15,0) NOT NULL,
    tien_khach_dua DECIMAL(15,0),
    tien_thua DECIMAL(15,0),
    trang_thai VARCHAR(20) NOT NULL,
    ly_do_huy VARCHAR(255),
    CHECK (phuong_thuc_tt IN ('TIEN_MAT', 'THE', 'QR')),
    CHECK (loai_giam_gia IN ('KHONG', 'HANG_THANH_VIEN', 'KHUYEN_MAI')),
    CHECK (ty_le_giam BETWEEN 0 AND 100),
    CHECK (ty_le_vat BETWEEN 0 AND 100),
    CHECK (tong_gia_goc >= 0 AND tien_giam >= 0 AND diem_su_dung >= 0),
    CHECK (tien_giam_diem >= 0 AND diem_tich_luy_kiem >= 0 AND diem_thuong_kiem >= 0),
    CHECK (tien_vat >= 0 AND tong_thanh_toan >= 0),
    CHECK (trang_thai IN ('DA_THANH_TOAN', 'DA_HUY')),
    CHECK (
        (phuong_thuc_tt = 'TIEN_MAT' AND ma_tham_chieu_tt IS NULL
            AND tien_khach_dua >= tong_thanh_toan
            AND tien_thua = tien_khach_dua - tong_thanh_toan)
        OR
        (phuong_thuc_tt IN ('THE', 'QR') AND ma_tham_chieu_tt IS NOT NULL
            AND tien_khach_dua IS NULL AND tien_thua IS NULL)
    ),
    CHECK (
        (trang_thai = 'DA_THANH_TOAN' AND thoi_diem_huy IS NULL
            AND ma_nhan_vien_huy IS NULL AND ly_do_huy IS NULL)
        OR
        (trang_thai = 'DA_HUY' AND thoi_diem_huy IS NOT NULL
            AND ma_nhan_vien_huy IS NOT NULL AND ly_do_huy IS NOT NULL)
    ),
    FOREIGN KEY (ma_nhan_vien) REFERENCES NHAN_VIEN(ma_nhan_vien),
    FOREIGN KEY (ma_khach_hang) REFERENCES KHACH_HANG(ma_khach_hang),
    FOREIGN KEY (ma_khuyen_mai) REFERENCES KHUYEN_MAI(ma_khuyen_mai),
    FOREIGN KEY (ma_nhan_vien_huy) REFERENCES NHAN_VIEN(ma_nhan_vien)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE CHI_TIET_HOA_DON (
    ma_hoa_don VARCHAR(20) NOT NULL,
    ma_san_pham VARCHAR(20) NOT NULL,
    so_luong INT NOT NULL,
    don_gia_ban DECIMAL(15,0) NOT NULL,
    tien_giam_dong DECIMAL(15,0) NOT NULL,
    thanh_tien DECIMAL(15,0) NOT NULL,
    PRIMARY KEY (ma_hoa_don, ma_san_pham),
    CHECK (so_luong > 0),
    CHECK (don_gia_ban >= 0 AND tien_giam_dong >= 0 AND thanh_tien >= 0),
    FOREIGN KEY (ma_hoa_don) REFERENCES HOA_DON(ma_hoa_don),
    FOREIGN KEY (ma_san_pham) REFERENCES SAN_PHAM(ma_san_pham)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE PHAN_BO_XUAT_LO (
    ma_hoa_don VARCHAR(20) NOT NULL,
    ma_san_pham VARCHAR(20) NOT NULL,
    ma_lo BIGINT NOT NULL,
    so_luong_xuat INT NOT NULL,
    don_gia_von DECIMAL(15,0) NOT NULL,
    PRIMARY KEY (ma_hoa_don, ma_san_pham, ma_lo),
    CHECK (so_luong_xuat > 0),
    CHECK (don_gia_von >= 0),
    FOREIGN KEY (ma_hoa_don, ma_san_pham)
        REFERENCES CHI_TIET_HOA_DON(ma_hoa_don, ma_san_pham),
    FOREIGN KEY (ma_lo) REFERENCES LO_TON_KHO(ma_lo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO NHAN_VIEN (ma_nhan_vien, ho_ten, so_dien_thoai, chuc_vu, trang_thai) VALUES
    ('NV001', 'Nguyen Van Quan', '0900000001', 'Quan ly', 'DANG_LAM'),
    ('NV002', 'Tran Thi Thu Ngan', '0900000002', 'Thu ngan', 'DANG_LAM');

INSERT INTO TAI_KHOAN (ten_dang_nhap, mat_khau_bam, vai_tro, trang_thai, ma_nhan_vien) VALUES
    ('quanly', 'pbkdf2$120000$AAECAwQFBgcICQoLDA0ODw==$9EcSy5q39lShSV+xiMTn1fXxIsEcdzeHoWHCQUxVtLM=', 'QUAN_LY', 'HOAT_DONG', 'NV001'),
    ('thungan', 'pbkdf2$120000$AAECAwQFBgcICQoLDA0ODw==$9EcSy5q39lShSV+xiMTn1fXxIsEcdzeHoWHCQUxVtLM=', 'THU_NGAN', 'HOAT_DONG', 'NV002');

INSERT INTO LOAI_SAN_PHAM (ma_loai, ten_loai, mo_ta, trang_thai) VALUES
    ('LSP001', 'Nuoc uong', 'San pham giai khat', 'DANG_KINH_DOANH');

INSERT INTO SAN_PHAM (ma_san_pham, ma_loai, ten_san_pham, don_vi_tinh, gia_ban, nguong_ton, trang_thai) VALUES
    ('SP001', 'LSP001', 'Nuoc suoi 500ml', 'Chai', 3000, 10, 'DANG_KINH_DOANH');

INSERT INTO KHACH_HANG (ma_khach_hang, ho_ten, so_dien_thoai, diem_hien_co, diem_tich_luy, trang_thai) VALUES
    ('KH001', 'Le Minh Khach', '0900000003', 600, 12000, 'HOAT_DONG');

INSERT INTO NHA_CUNG_CAP (ma_nha_cung_cap, ten_nha_cung_cap, so_dien_thoai, dia_chi, trang_thai) VALUES
    ('NCC001', 'Nha cung cap mau', '0900000004', 'TP. Ho Chi Minh', 'HOAT_DONG');

INSERT INTO CAU_HINH_CUA_HANG (id, ten_cua_hang, dia_chi, so_dien_thoai, ty_le_vat) VALUES
    (1, 'Cua hang tien loi', 'TP. Ho Chi Minh', '0900000000', 10.00);

INSERT INTO KHUYEN_MAI (ma_khuyen_mai, ten_khuyen_mai, pham_vi, ty_le_giam, bat_dau, ket_thuc, trang_thai) VALUES
    ('KM001', 'Giam gia nuoc suoi', 'THEO_SAN_PHAM', 10.00, '2026-01-01 00:00:00', '2026-12-31 23:59:59', 'DANG_HOAT_DONG');

INSERT INTO KHUYEN_MAI_SAN_PHAM (ma_khuyen_mai, ma_san_pham) VALUES
    ('KM001', 'SP001');

INSERT INTO PHIEU_NHAP (ma_phieu_nhap, ma_nha_cung_cap, ma_nhan_vien, thoi_diem_lap, thoi_diem_hoan_tat, trang_thai, tong_tien) VALUES
    ('PN001', 'NCC001', 'NV001', '2026-08-01 08:00:00', '2026-08-01 08:00:00', 'HOAN_TAT', 800000);

INSERT INTO CHI_TIET_PHIEU_NHAP (ma_phieu_nhap, ma_san_pham, so_luong, don_gia_nhap) VALUES
    ('PN001', 'SP001', 100, 8000);

INSERT INTO LO_TON_KHO (ma_lo, ma_phieu_nhap, ma_san_pham, so_luong_nhap, so_luong_con, thoi_diem_nhap) VALUES
    (1, 'PN001', 'SP001', 100, 95, '2026-08-01 08:00:00');

INSERT INTO DIEU_CHINH_TON (ma_dieu_chinh, ma_nhan_vien, thoi_diem_lap, ly_do) VALUES
    ('DC001', 'NV001', '2026-08-03 09:00:00', 'Hang hong');

INSERT INTO CHI_TIET_DIEU_CHINH (ma_dieu_chinh, ma_lo, so_luong_giam) VALUES
    ('DC001', 1, 1);

INSERT INTO HOA_DON (
    ma_hoa_don, ma_nhan_vien, ma_khach_hang, ma_khuyen_mai, ma_nhan_vien_huy,
    thoi_diem_lap, thoi_diem_huy, phuong_thuc_tt, ma_tham_chieu_tt,
    tong_gia_goc, loai_giam_gia, ty_le_giam, tien_giam, diem_su_dung,
    tien_giam_diem, diem_tich_luy_kiem, diem_thuong_kiem, ty_le_vat,
    tien_vat, tong_thanh_toan, tien_khach_dua, tien_thua, trang_thai, ly_do_huy
) VALUES (
    'HD001', 'NV002', 'KH001', NULL, NULL,
    '2026-08-02 10:00:00', NULL, 'TIEN_MAT', NULL,
    12000, 'KHONG', 0.00, 0, 0,
    0, 12000, 600, 10.00,
    1200, 13200, 15000, 1800, 'DA_THANH_TOAN', NULL
);

INSERT INTO CHI_TIET_HOA_DON (ma_hoa_don, ma_san_pham, so_luong, don_gia_ban, tien_giam_dong, thanh_tien) VALUES
    ('HD001', 'SP001', 4, 3000, 0, 12000);

INSERT INTO PHAN_BO_XUAT_LO (ma_hoa_don, ma_san_pham, ma_lo, so_luong_xuat, don_gia_von) VALUES
    ('HD001', 'SP001', 1, 4, 8000);
