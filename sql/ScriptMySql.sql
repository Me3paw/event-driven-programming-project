-- ============================================================
-- DATABASE: CUA_HANG_TIEN_LOI
-- DBMS: MICROSOFT SQL SERVER (T-SQL)
-- PURPOSE: TAO MOI CSDL VA BO DU LIEU MAU DAY DU
-- LUU Y: SCRIPT CHI XOA CAC BANG DU LIEU MAU CU; KHONG XOA DATABASE.
-- ============================================================

-- PHIEN BAN HOAN CHINH: KHONG DROP DATABASE, KHONG SINGLE_USER.
-- CHU Y: CHAY LAI SE XOA CAC BANG DU LIEU MAU CU TRONG DATABASE NAY.
-- NEN SAO LUU NEU CO DU LIEU CAN GIU.
USE master;
GO
IF DB_ID(N'cua_hang_tien_loi') IS NULL
BEGIN
EXEC(N'CREATE DATABASE cua_hang_tien_loi COLLATE Vietnamese_CI_AS');
END;
GO
USE cua_hang_tien_loi;
GO
-- Xoa bang cu theo thu tu khoa ngoai, khong ngat ket noi SQL Server.
IF OBJECT_ID(N'dbo.PHAN_BO_XUAT_LO', N'U') IS NOT NULL DROP TABLE dbo.PHAN_BO_XUAT_LO;
IF OBJECT_ID(N'dbo.CHI_TIET_HOA_DON', N'U') IS NOT NULL DROP TABLE dbo.CHI_TIET_HOA_DON;
IF OBJECT_ID(N'dbo.HOA_DON', N'U') IS NOT NULL DROP TABLE dbo.HOA_DON;
IF OBJECT_ID(N'dbo.CHI_TIET_PHIEU_DIEU_CHINH', N'U') IS NOT NULL DROP TABLE dbo.CHI_TIET_PHIEU_DIEU_CHINH;
IF OBJECT_ID(N'dbo.PHIEU_DIEU_CHINH_TON', N'U') IS NOT NULL DROP TABLE dbo.PHIEU_DIEU_CHINH_TON;
IF OBJECT_ID(N'dbo.LO_TON_KHO', N'U') IS NOT NULL DROP TABLE dbo.LO_TON_KHO;
IF OBJECT_ID(N'dbo.CHI_TIET_PHIEU_NHAP', N'U') IS NOT NULL DROP TABLE dbo.CHI_TIET_PHIEU_NHAP;
IF OBJECT_ID(N'dbo.PHIEU_NHAP', N'U') IS NOT NULL DROP TABLE dbo.PHIEU_NHAP;
IF OBJECT_ID(N'dbo.KHUYEN_MAI_SAN_PHAM', N'U') IS NOT NULL DROP TABLE dbo.KHUYEN_MAI_SAN_PHAM;
IF OBJECT_ID(N'dbo.KHUYEN_MAI', N'U') IS NOT NULL DROP TABLE dbo.KHUYEN_MAI;
IF OBJECT_ID(N'dbo.CAU_HINH_CUA_HANG', N'U') IS NOT NULL DROP TABLE dbo.CAU_HINH_CUA_HANG;
IF OBJECT_ID(N'dbo.NHA_CUNG_CAP', N'U') IS NOT NULL DROP TABLE dbo.NHA_CUNG_CAP;
IF OBJECT_ID(N'dbo.KHACH_HANG', N'U') IS NOT NULL DROP TABLE dbo.KHACH_HANG;
IF OBJECT_ID(N'dbo.SAN_PHAM', N'U') IS NOT NULL DROP TABLE dbo.SAN_PHAM;
IF OBJECT_ID(N'dbo.LOAI_SAN_PHAM', N'U') IS NOT NULL DROP TABLE dbo.LOAI_SAN_PHAM;
IF OBJECT_ID(N'dbo.TAI_KHOAN', N'U') IS NOT NULL DROP TABLE dbo.TAI_KHOAN;
IF OBJECT_ID(N'dbo.NHAN_VIEN', N'U') IS NOT NULL DROP TABLE dbo.NHAN_VIEN;
GO

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET NOCOUNT ON;
GO

-- ============================================================
-- 2. CREATE TABLES
-- ============================================================

-- 2.1 NHAN_VIEN
CREATE TABLE NHAN_VIEN (
                           ma_nhan_vien NVARCHAR(20) NOT NULL PRIMARY KEY,
                           ho_ten NVARCHAR(100) NOT NULL,
                           so_dien_thoai NVARCHAR(20) NULL,
                           chuc_vu NVARCHAR(30) NULL,
                           trang_thai NVARCHAR(20) NOT NULL,

                           CHECK (ma_nhan_vien LIKE N'NV[0-9][0-9][0-9]'),
                           CHECK (ho_ten NOT LIKE N'%[0-9]%'),
                           CHECK (
                               so_dien_thoai IS NULL OR
                               so_dien_thoai LIKE N'0[35789][0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9]'
                               ),
                           CHECK (trang_thai IN (N'DANG_LAM', N'NGHI_VIEC'))
);

-- 2.2 TAI_KHOAN
CREATE TABLE TAI_KHOAN (
                           ten_dang_nhap NVARCHAR(50) NOT NULL PRIMARY KEY,
                           mat_khau_bam NVARCHAR(255) NOT NULL,
                           vai_tro NVARCHAR(20) NOT NULL,
                           trang_thai NVARCHAR(20) NOT NULL,
                           ma_nhan_vien NVARCHAR(20) NOT NULL UNIQUE,

                           CHECK (vai_tro IN (N'THU_NGAN', N'QUAN_LY')),
                           CHECK (trang_thai IN (N'HOAT_DONG', N'KHOA')),

                           FOREIGN KEY (ma_nhan_vien)
                               REFERENCES NHAN_VIEN(ma_nhan_vien)
);

-- 2.3 LOAI_SAN_PHAM
CREATE TABLE LOAI_SAN_PHAM (
                               ma_loai NVARCHAR(20) NOT NULL PRIMARY KEY,
                               ten_loai NVARCHAR(100) NOT NULL UNIQUE,
                               mo_ta NVARCHAR(255) NULL,
                               trang_thai NVARCHAR(20) NOT NULL,

                               CHECK (ma_loai LIKE N'LSP[0-9]%'),
                               CHECK (
                                   trang_thai IN (
                                                  N'DANG_KINH_DOANH',
                                                  N'NGUNG_KINH_DOANH'
                                       )
                                   )
);

-- 2.4 SAN_PHAM
CREATE TABLE SAN_PHAM (
                          ma_san_pham NVARCHAR(20) NOT NULL PRIMARY KEY,
                          ma_loai NVARCHAR(20) NOT NULL,
                          ten_san_pham NVARCHAR(150) NOT NULL,
                          don_vi_tinh NVARCHAR(30) NOT NULL,
                          gia_ban DECIMAL(15,0) NOT NULL,
                          nguong_ton INT NOT NULL,
                          trang_thai NVARCHAR(20) NOT NULL,

                          CHECK (ma_san_pham LIKE N'893[0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9]'),
                          CHECK (gia_ban >= 0),
                          CHECK (nguong_ton >= 0),
                          CHECK (
                              trang_thai IN (
                                             N'DANG_KINH_DOANH',
                                             N'NGUNG_KINH_DOANH'
                                  )
                              ),

                          FOREIGN KEY (ma_loai)
                              REFERENCES LOAI_SAN_PHAM(ma_loai)
);

-- 2.5 KHACH_HANG
CREATE TABLE KHACH_HANG (
                            ma_khach_hang NVARCHAR(20) NOT NULL PRIMARY KEY,
                            ho_ten NVARCHAR(100) NOT NULL,
                            so_dien_thoai NVARCHAR(20) NULL,
                            diem_hien_co BIGINT NOT NULL DEFAULT 0,
                            diem_tich_luy BIGINT NOT NULL DEFAULT 0,
                            trang_thai NVARCHAR(20) NOT NULL,

                            CHECK (ma_khach_hang LIKE N'KH[0-9][0-9][0-9]'),
                            CHECK (ho_ten NOT LIKE N'%[0-9]%'),
                            CHECK (
                                so_dien_thoai IS NULL OR
                                so_dien_thoai LIKE N'0[35789][0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9]'
                                ),
                            CHECK (diem_hien_co >= 0),
                            CHECK (diem_tich_luy >= 0),
                            CHECK (
                                trang_thai IN (
                                               N'HOAT_DONG',
                                               N'NGUNG_HOAT_DONG'
                                    )
                                )
);

CREATE UNIQUE INDEX UX_KHACH_HANG_SDT
    ON KHACH_HANG(so_dien_thoai)
    WHERE so_dien_thoai IS NOT NULL;

-- 2.6 NHA_CUNG_CAP
CREATE TABLE NHA_CUNG_CAP (
                              ma_nha_cung_cap NVARCHAR(20) NOT NULL PRIMARY KEY,
                              ten_nha_cung_cap NVARCHAR(150) NOT NULL,
                              so_dien_thoai NVARCHAR(20) NULL,
                              dia_chi NVARCHAR(255) NULL,
                              trang_thai NVARCHAR(20) NOT NULL,

                              CHECK (ma_nha_cung_cap LIKE N'NCC[0-9]%'),
                              CHECK (
                                  trang_thai IN (
                                                 N'HOAT_DONG',
                                                 N'NGUNG_HOAT_DONG'
                                      )
                                  )
);

-- 2.7 CAU_HINH_CUA_HANG
CREATE TABLE CAU_HINH_CUA_HANG (
                                   id TINYINT NOT NULL PRIMARY KEY,
                                   ten_cua_hang NVARCHAR(150) NOT NULL,
                                   dia_chi NVARCHAR(255) NOT NULL,
                                   so_dien_thoai NVARCHAR(20) NOT NULL,
                                   ty_le_vat DECIMAL(5,2) NOT NULL,

                                   CHECK (id = 1),
                                   CHECK (ty_le_vat BETWEEN 0 AND 100)
);

-- 2.8 KHUYEN_MAI
CREATE TABLE KHUYEN_MAI (
                            ma_khuyen_mai NVARCHAR(20) NOT NULL PRIMARY KEY,
                            ten_khuyen_mai NVARCHAR(150) NOT NULL,
                            pham_vi NVARCHAR(20) NOT NULL,
                            ty_le_giam DECIMAL(5,2) NOT NULL,
                            don_toi_thieu DECIMAL(15,0) NULL,
                            bat_dau DATETIME2(0) NOT NULL,
                            ket_thuc DATETIME2(0) NOT NULL,
                            trang_thai NVARCHAR(20) NOT NULL,

                            CHECK (ma_khuyen_mai LIKE N'KM[0-9][0-9][0-9]'),
                            CHECK (ten_khuyen_mai LIKE N'Khuyến mãi%'),
                            CHECK (pham_vi IN (N'TOAN_DON', N'THEO_SAN_PHAM')),
                            CHECK (ty_le_giam > 0 AND ty_le_giam <= 100),
                            CHECK (don_toi_thieu IS NULL OR don_toi_thieu > 0),
                            CHECK (pham_vi <> N'TOAN_DON' OR don_toi_thieu IS NOT NULL),
                            CHECK (bat_dau < ket_thuc),
                            CHECK (
                                trang_thai IN (
                                               N'DANG_HOAT_DONG',
                                               N'NGUNG_HOAT_DONG'
                                    )
                                )
);

-- 2.9 KHUYEN_MAI_SAN_PHAM
CREATE TABLE KHUYEN_MAI_SAN_PHAM (
                                     ma_khuyen_mai NVARCHAR(20) NOT NULL,
                                     ma_san_pham NVARCHAR(20) NOT NULL,

                                     PRIMARY KEY (ma_khuyen_mai, ma_san_pham),

                                     FOREIGN KEY (ma_khuyen_mai)
                                         REFERENCES KHUYEN_MAI(ma_khuyen_mai),

                                     FOREIGN KEY (ma_san_pham)
                                         REFERENCES SAN_PHAM(ma_san_pham)
);

-- 2.10 PHIEU_NHAP
CREATE TABLE PHIEU_NHAP (
                            ma_phieu_nhap NVARCHAR(20) NOT NULL PRIMARY KEY,
                            ma_nha_cung_cap NVARCHAR(20) NOT NULL,
                            ma_nhan_vien NVARCHAR(20) NOT NULL,
                            thoi_diem_lap DATETIME2(0) NOT NULL,
                            thoi_diem_hoan_tat DATETIME2(0) NULL,
                            trang_thai NVARCHAR(20) NOT NULL,
                            tong_tien DECIMAL(15,0) NOT NULL,

                            CHECK (ma_phieu_nhap LIKE N'PN[0-9]%'),
                            CHECK (trang_thai IN (N'NHAP', N'HOAN_TAT')),
                            CHECK (tong_tien >= 0),

                            FOREIGN KEY (ma_nha_cung_cap)
                                REFERENCES NHA_CUNG_CAP(ma_nha_cung_cap),

                            FOREIGN KEY (ma_nhan_vien)
                                REFERENCES NHAN_VIEN(ma_nhan_vien)
);

-- 2.11 CHI_TIET_PHIEU_NHAP
CREATE TABLE CHI_TIET_PHIEU_NHAP (
                                     ma_phieu_nhap NVARCHAR(20) NOT NULL,
                                     ma_san_pham NVARCHAR(20) NOT NULL,
                                     so_luong INT NOT NULL,
                                     don_gia_nhap DECIMAL(15,0) NOT NULL,

                                     PRIMARY KEY (ma_phieu_nhap, ma_san_pham),

                                     CHECK (so_luong > 0),
                                     CHECK (don_gia_nhap >= 0),

                                     FOREIGN KEY (ma_phieu_nhap)
                                         REFERENCES PHIEU_NHAP(ma_phieu_nhap),

                                     FOREIGN KEY (ma_san_pham)
                                         REFERENCES SAN_PHAM(ma_san_pham)
);

-- 2.12 LO_TON_KHO
CREATE TABLE LO_TON_KHO (
                            ma_lo BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
                            ma_phieu_nhap NVARCHAR(20) NOT NULL,
                            ma_san_pham NVARCHAR(20) NOT NULL,
                            so_luong_nhap INT NOT NULL,
                            so_luong_con INT NOT NULL,
                            thoi_diem_nhap DATETIME2(0) NOT NULL,

                            UNIQUE (ma_phieu_nhap, ma_san_pham),

                            CHECK (so_luong_nhap > 0),
                            CHECK (so_luong_con BETWEEN 0 AND so_luong_nhap),

                            FOREIGN KEY (ma_phieu_nhap, ma_san_pham)
                                REFERENCES CHI_TIET_PHIEU_NHAP(ma_phieu_nhap, ma_san_pham)
);

-- 2.13 PHIEU_DIEU_CHINH_TON
CREATE TABLE PHIEU_DIEU_CHINH_TON (
                                      ma_dieu_chinh NVARCHAR(20) NOT NULL PRIMARY KEY,
                                      ma_nhan_vien NVARCHAR(20) NOT NULL,
                                      thoi_diem_lap DATETIME2(0) NOT NULL,
                                      ly_do NVARCHAR(255) NOT NULL,

                                      CHECK (ma_dieu_chinh LIKE N'DC[0-9]%'),

                                      FOREIGN KEY (ma_nhan_vien)
                                          REFERENCES NHAN_VIEN(ma_nhan_vien)
);

-- 2.14 CHI_TIET_PHIEU_DIEU_CHINH
CREATE TABLE CHI_TIET_PHIEU_DIEU_CHINH (
                                           ma_dieu_chinh NVARCHAR(20) NOT NULL,
                                           ma_lo BIGINT NOT NULL,
                                           so_luong_giam INT NOT NULL,

                                           PRIMARY KEY (ma_dieu_chinh, ma_lo),

                                           CHECK (so_luong_giam > 0),

                                           FOREIGN KEY (ma_dieu_chinh)
                                               REFERENCES PHIEU_DIEU_CHINH_TON(ma_dieu_chinh),

                                           FOREIGN KEY (ma_lo)
                                               REFERENCES LO_TON_KHO(ma_lo)
);

-- 2.15 HOA_DON
CREATE TABLE HOA_DON (
                         ma_hoa_don NVARCHAR(20) NOT NULL PRIMARY KEY,
                         ma_nhan_vien NVARCHAR(20) NOT NULL,
                         ma_khach_hang NVARCHAR(20) NULL,
                         ma_khuyen_mai NVARCHAR(20) NULL,
                         ma_nhan_vien_huy NVARCHAR(20) NULL,

                         thoi_diem_lap DATETIME2(0) NOT NULL,
                         thoi_diem_huy DATETIME2(0) NULL,

                         phuong_thuc_tt NVARCHAR(20) NOT NULL,
                         ma_tham_chieu_tt NVARCHAR(100) NULL,

                         tong_gia_goc DECIMAL(15,0) NOT NULL,
                         loai_giam_gia NVARCHAR(20) NOT NULL,
                         ty_le_giam DECIMAL(5,2) NOT NULL,
                         tien_giam DECIMAL(15,0) NOT NULL,

                         diem_su_dung BIGINT NOT NULL,
                         tien_giam_diem DECIMAL(15,0) NOT NULL,

                         diem_tich_luy_kiem BIGINT NOT NULL,
                         diem_thuong_kiem BIGINT NOT NULL,

                         ty_le_vat DECIMAL(5,2) NOT NULL,
                         tien_vat DECIMAL(15,0) NOT NULL,

                         tong_thanh_toan DECIMAL(15,0) NOT NULL,

                         tien_khach_dua DECIMAL(15,0) NULL,
                         tien_thua DECIMAL(15,0) NULL,

                         trang_thai NVARCHAR(20) NOT NULL,
                         ly_do_huy NVARCHAR(255) NULL,

                         CHECK (ma_hoa_don LIKE N'HD[0-9]%'),

                         CHECK (
                             phuong_thuc_tt IN (
                                                N'TIEN_MAT',
                                                N'THE',
                                                N'QR'
                                 )
                             ),

                         CHECK (
                             loai_giam_gia IN (
                                               N'KHONG',
                                               N'HANG_THANH_VIEN',
                                               N'KHUYEN_MAI'
                                 )
                             ),

                         CHECK (ty_le_giam BETWEEN 0 AND 100),
                         CHECK (ty_le_vat BETWEEN 0 AND 100),

                         CHECK (
                             tong_gia_goc >= 0
                                 AND tien_giam >= 0
                                 AND diem_su_dung >= 0
                             ),

                         CHECK (
                             tien_giam_diem >= 0
                                 AND diem_tich_luy_kiem >= 0
                                 AND diem_thuong_kiem >= 0
                             ),

                         CHECK (
                             tien_vat >= 0
                                 AND tong_thanh_toan >= 0
                             ),

                         CHECK (trang_thai IN (N'DA_THANH_TOAN', N'DA_HUY')),

                         CHECK (
                             (
                                 phuong_thuc_tt = N'TIEN_MAT'
                                     AND ma_tham_chieu_tt IS NULL
                                     AND tien_khach_dua >= tong_thanh_toan
                                     AND tien_thua = tien_khach_dua - tong_thanh_toan
                                 )
                                 OR
                             (
                                 phuong_thuc_tt IN (N'THE', N'QR')
                                     AND ma_tham_chieu_tt IS NOT NULL
                                     AND tien_khach_dua IS NULL
                                     AND tien_thua IS NULL
                                 )
                             ),

                         CHECK (
                             (
                                 trang_thai = N'DA_THANH_TOAN'
                                     AND thoi_diem_huy IS NULL
                                     AND ma_nhan_vien_huy IS NULL
                                     AND ly_do_huy IS NULL
                                 )
                                 OR
                             (
                                 trang_thai = N'DA_HUY'
                                     AND thoi_diem_huy IS NOT NULL
                                     AND ma_nhan_vien_huy IS NOT NULL
                                     AND ly_do_huy IS NOT NULL
                                 )
                             ),

                         FOREIGN KEY (ma_nhan_vien)
                             REFERENCES NHAN_VIEN(ma_nhan_vien),

                         FOREIGN KEY (ma_khach_hang)
                             REFERENCES KHACH_HANG(ma_khach_hang),

                         FOREIGN KEY (ma_khuyen_mai)
                             REFERENCES KHUYEN_MAI(ma_khuyen_mai),

                         FOREIGN KEY (ma_nhan_vien_huy)
                             REFERENCES NHAN_VIEN(ma_nhan_vien)
);

-- 2.16 CHI_TIET_HOA_DON
CREATE TABLE CHI_TIET_HOA_DON (
                                  ma_hoa_don NVARCHAR(20) NOT NULL,
                                  ma_san_pham NVARCHAR(20) NOT NULL,
                                  so_luong INT NOT NULL,
                                  don_gia_ban DECIMAL(15,0) NOT NULL,
                                  tien_giam_dong DECIMAL(15,0) NOT NULL,
                                  thanh_tien DECIMAL(15,0) NOT NULL,

                                  PRIMARY KEY (ma_hoa_don, ma_san_pham),

                                  CHECK (so_luong > 0),
                                  CHECK (
                                      don_gia_ban >= 0
                                          AND tien_giam_dong >= 0
                                          AND thanh_tien >= 0
                                      ),

                                  FOREIGN KEY (ma_hoa_don)
                                      REFERENCES HOA_DON(ma_hoa_don),

                                  FOREIGN KEY (ma_san_pham)
                                      REFERENCES SAN_PHAM(ma_san_pham)
);

-- 2.17 PHAN_BO_XUAT_LO
CREATE TABLE PHAN_BO_XUAT_LO (
                                 ma_hoa_don NVARCHAR(20) NOT NULL,
                                 ma_san_pham NVARCHAR(20) NOT NULL,
                                 ma_lo BIGINT NOT NULL,
                                 so_luong_xuat INT NOT NULL,
                                 don_gia_von DECIMAL(15,0) NOT NULL,

                                 PRIMARY KEY (ma_hoa_don, ma_san_pham, ma_lo),

                                 CHECK (so_luong_xuat > 0),
                                 CHECK (don_gia_von >= 0),

                                 FOREIGN KEY (ma_hoa_don, ma_san_pham)
                                     REFERENCES CHI_TIET_HOA_DON(ma_hoa_don, ma_san_pham),

                                 FOREIGN KEY (ma_lo)
                                     REFERENCES LO_TON_KHO(ma_lo)
);
GO
-- ============================================================

-- ============================================================
-- 3. DU LIEU MAU - SO LUONG THEO YEU CAU
-- 5 nhan vien, 10 loai, 100 san pham, 40 khach hang,
-- 5 nha cung cap, 10 phieu nhap, 10 phieu dieu chinh,
-- 8 khuyen mai, 100 hoa don (30 hoa don trong thang hien tai).
-- ============================================================

SET XACT_ABORT ON;
DECLARE @ThoiDiemHienTai DATETIME2(0) = SYSDATETIME();
DECLARE @NgayHienTai DATE = CONVERT(DATE, @ThoiDiemHienTai);
DECLARE @DauThang DATE = DATEFROMPARTS(YEAR(@NgayHienTai), MONTH(@NgayHienTai), 1);

BEGIN TRY
BEGIN TRANSACTION;

    -- 3.1 NHAN_VIEN: chinh xac 5 nhan vien
INSERT INTO NHAN_VIEN (ma_nhan_vien, ho_ten, so_dien_thoai, chuc_vu, trang_thai)
VALUES
    (N'NV001', N'Nguyen Van Quan', N'0902000001', N'Quan ly', N'DANG_LAM'),
    (N'NV002', N'Tran Thi Thu Ngan', N'0902000002', N'Thu ngan', N'DANG_LAM'),
    (N'NV003', N'Pham Minh Anh', N'0902000003', N'Thu ngan', N'DANG_LAM'),
    (N'NV004', N'Vo Hoang Nam', N'0902000004', N'Thu ngan', N'DANG_LAM'),
    (N'NV005', N'Nguyen Bao Chau', N'0902000005', N'Thu ngan', N'DANG_LAM');

-- Mat khau bam mau duoc giu theo file goc.
INSERT INTO TAI_KHOAN (ten_dang_nhap, mat_khau_bam, vai_tro, trang_thai, ma_nhan_vien)
VALUES
    (N'quanly',    N'pbkdf2$120000$AAECAwQFBgcICQoLDA0ODw==$9EcSy5q39lShSV+xiMTn1fXxIsEcdzeHoWHCQUxVtLM=', N'QUAN_LY', N'HOAT_DONG', N'NV001'),
    (N'thungan01', N'pbkdf2$120000$AAECAwQFBgcICQoLDA0ODw==$9EcSy5q39lShSV+xiMTn1fXxIsEcdzeHoWHCQUxVtLM=', N'THU_NGAN', N'HOAT_DONG', N'NV002'),
    (N'thungan02', N'pbkdf2$120000$AAECAwQFBgcICQoLDA0ODw==$9EcSy5q39lShSV+xiMTn1fXxIsEcdzeHoWHCQUxVtLM=', N'THU_NGAN', N'HOAT_DONG', N'NV003'),
    (N'thungan03', N'pbkdf2$120000$AAECAwQFBgcICQoLDA0ODw==$9EcSy5q39lShSV+xiMTn1fXxIsEcdzeHoWHCQUxVtLM=', N'THU_NGAN', N'HOAT_DONG', N'NV004'),
    (N'thungan04', N'pbkdf2$120000$AAECAwQFBgcICQoLDA0ODw==$9EcSy5q39lShSV+xiMTn1fXxIsEcdzeHoWHCQUxVtLM=', N'THU_NGAN', N'HOAT_DONG', N'NV005');

-- 3.2 LOAI_SAN_PHAM: 10 loai
INSERT INTO LOAI_SAN_PHAM (ma_loai, ten_loai, mo_ta, trang_thai)
VALUES
    (N'LSP001', N'Do uong', N'Nuoc dong chai, nuoc giai khat va nuoc uong dong lon', N'DANG_KINH_DOANH'),
    (N'LSP002', N'Sua va san pham tu sua', N'Sua tuoi, sua hat, sua chua va phomai', N'DANG_KINH_DOANH'),
    (N'LSP003', N'Banh keo va snack', N'Banh, keo, socola va cac loai snack', N'DANG_KINH_DOANH'),
    (N'LSP004', N'Mi va thuc pham an lien', N'Mi, pho, bun, chao va mon an tien loi', N'DANG_KINH_DOANH'),
    (N'LSP005', N'Thuc pham dong goi va che bien', N'Thuc pham dong hop, dong goi va che bien', N'DANG_KINH_DOANH'),
    (N'LSP006', N'Gia vi va thuc pham kho', N'Gao, ngu coc, gia vi va do kho', N'DANG_KINH_DOANH'),
    (N'LSP007', N'Hoa my pham va ve sinh ca nhan', N'San pham cham soc ca nhan va ve sinh', N'DANG_KINH_DOANH'),
    (N'LSP008', N'Do dung gia dinh', N'Vat dung ve sinh va do dung gia dinh', N'DANG_KINH_DOANH'),
    (N'LSP009', N'Van phong pham va tien ich', N'Dung cu hoc tap va vat dung tien ich', N'DANG_KINH_DOANH'),
    (N'LSP010', N'Thuc pham tuoi va dong lanh', N'Thuc pham tuoi, dong lanh va mon an nhanh', N'DANG_KINH_DOANH');

-- 3.3 SAN_PHAM: 100 san pham, chia deu 10 san pham/loai
CREATE TABLE #ProductSeed (
                              product_no INT NOT NULL PRIMARY KEY,
                              ten_san_pham NVARCHAR(150) COLLATE DATABASE_DEFAULT NOT NULL,
                              don_vi_tinh NVARCHAR(30) COLLATE DATABASE_DEFAULT NOT NULL,
                              gia_ban DECIMAL(15,0) NOT NULL,
                              nguong_ton INT NOT NULL
);

INSERT INTO #ProductSeed (product_no, ten_san_pham, don_vi_tinh, gia_ban, nguong_ton)
VALUES
    (1, N'Nuoc suoi tinh khiet 500ml', N'Chai', 5000, 9),
    (2, N'Nuoc khoang co gas 500ml', N'Chai', 7000, 10),
    (3, N'Coca-Cola lon 330ml', N'Lon', 11000, 11),
    (4, N'Pepsi lon 330ml', N'Lon', 10000, 12),
    (5, N'7Up lon 330ml', N'Lon', 10000, 13),
    (6, N'Tra xanh khong do 455ml', N'Chai', 10000, 8),
    (7, N'Tra Oolong Tea Plus 455ml', N'Chai', 11000, 9),
    (8, N'Nuoc cam ep 330ml', N'Chai', 15000, 10),
    (9, N'Nuoc yen ngan nhuc 190ml', N'Lon', 12000, 11),
    (10, N'Nuoc tang luc 250ml', N'Lon', 10000, 12),
    (11, N'Sua tuoi co duong 180ml', N'Hop', 8000, 13),
    (12, N'Sua tuoi khong duong 180ml', N'Hop', 8500, 8),
    (13, N'Sua dau nanh hop 200ml', N'Hop', 7000, 9),
    (14, N'Sua chua co duong', N'Hop', 9000, 10),
    (15, N'Sua chua nha dam', N'Hop', 11000, 11),
    (16, N'Sua dac co duong 380g', N'Lon', 27000, 12),
    (17, N'Phomai que 5 cay', N'Goi', 28000, 13),
    (18, N'Phomai lat 10 mieng', N'Goi', 32000, 8),
    (19, N'Sua tuoi tiet trung 1 lit', N'Hop', 34000, 9),
    (20, N'Sua hat oc cho 180ml', N'Hop', 12000, 10),
    (21, N'Banh mi sandwich', N'Goi', 18000, 11),
    (22, N'Banh croissant bo', N'Cai', 15000, 12),
    (23, N'Banh quy bo hop 200g', N'Hop', 28000, 13),
    (24, N'Banh cracker man', N'Goi', 16000, 8),
    (25, N'Snack khoai tay vi tu nhien', N'Goi', 12000, 9),
    (26, N'Snack tom cay', N'Goi', 12000, 10),
    (27, N'Keo deo trai cay', N'Goi', 15000, 11),
    (28, N'Socola sua thanh 40g', N'Thanh', 25000, 12),
    (29, N'Hat dieu rang goi 100g', N'Goi', 42000, 13),
    (30, N'Banh gao lac', N'Goi', 14000, 8),
    (31, N'Mi Hao Hao tom chua cay', N'Goi', 5000, 9),
    (32, N'Mi Omachi bo ham', N'Goi', 9000, 10),
    (33, N'Mi ly thit bam', N'Ly', 12000, 11),
    (34, N'Pho bo an lien', N'Goi', 10000, 12),
    (35, N'Bun bo Hue an lien', N'Goi', 11000, 13),
    (36, N'Chao ga an lien', N'Ly', 12000, 8),
    (37, N'Mi tron tuong den', N'Goi', 10000, 9),
    (38, N'Mi spaghetti goi 200g', N'Goi', 18000, 10),
    (39, N'Sup nui an lien', N'Ly', 14000, 11),
    (40, N'Chao yen mach an lien', N'Ly', 15000, 12),
    (41, N'Xuc xich tuoi 200g', N'Goi', 30000, 13),
    (42, N'Cha lua goi 200g', N'Goi', 35000, 8),
    (43, N'Ca vien dong lanh 250g', N'Goi', 32000, 9),
    (44, N'Ca ngu ngam dau hop', N'Hop', 28000, 10),
    (45, N'Pate gan heo hop 150g', N'Hop', 22000, 11),
    (46, N'Bap ngot dong hop', N'Hop', 18000, 12),
    (47, N'Thit hop ham 150g', N'Hop', 33000, 13),
    (48, N'Thanh cua dong lanh', N'Goi', 30000, 8),
    (49, N'Trung ga hop 10 qua', N'Hop', 35000, 9),
    (50, N'Dau hu non', N'Hop', 12000, 10),
    (51, N'Gao thom tui 1kg', N'Tui', 28000, 11),
    (52, N'Yen mach can 500g', N'Goi', 45000, 12),
    (53, N'Duong trang goi 1kg', N'Goi', 26000, 13),
    (54, N'Muoi i-ot 500g', N'Goi', 8000, 8),
    (55, N'Nuoc mam 500ml', N'Chai', 30000, 9),
    (56, N'Nuoc tuong 500ml', N'Chai', 18000, 10),
    (57, N'Dau an thuc vat 1 lit', N'Chai', 48000, 11),
    (58, N'Tuong ot 250g', N'Chai', 15000, 12),
    (59, N'Ca phe hoa tan 3in1', N'Hop', 42000, 13),
    (60, N'Tra tui loc hop 25 goi', N'Hop', 30000, 8),
    (61, N'Ban chai danh rang mem', N'Cai', 18000, 9),
    (62, N'Kem danh rang  kem 120g', N'Tuyp', 26000, 10),
    (63, N'Dau goi dau goi nho 180ml', N'Chai', 32000, 11),
    (64, N'Dau goi dau tui  sachet', N'Goi', 5000, 12),
    (65, N'Sua tam 250ml', N'Chai', 39000, 13),
    (66, N'Nuoc rua tay 250ml', N'Chai', 28000, 8),
    (67, N'Xa phong tam 90g', N'Banh', 12000, 9),
    (68, N'Giay ve sinh cuon 10 cuon', N'Goi', 35000, 10),
    (69, N'Khan giay uot goi 20 mieng', N'Goi', 15000, 11),
    (70, N'Bang ve sinh goi 8 mieng', N'Goi', 25000, 12),
    (71, N'Nuoc rua chen 400ml', N'Chai', 22000, 13),
    (72, N'Bot giat tui 400g', N'Tui', 25000, 8),
    (73, N'Nuoc lau san 1 lit', N'Chai', 30000, 9),
    (74, N'Nuoc tay rua 500ml', N'Chai', 24000, 10),
    (75, N'Tui rac tu huy  cuon', N'Cuon', 18000, 11),
    (76, N'Mut rua chen', N'Cai', 7000, 12),
    (77, N'Khan giay hop 180 to', N'Hop', 22000, 13),
    (78, N'Giay an rut bich', N'Bich', 18000, 8),
    (79, N'Pin AA 2 vien', N'Vi', 26000, 9),
    (80, N'Bong den LED 9W', N'Cai', 35000, 10),
    (81, N'But bi xanh', N'Cay', 5000, 11),
    (82, N'But chi HB', N'Cay', 4000, 12),
    (83, N'Vo hoc sinh 200 trang', N'Quyen', 15000, 13),
    (84, N'Thuoc ke 20cm', N'Cai', 6000, 8),
    (85, N'Keo cat giay', N'Cai', 18000, 9),
    (86, N'Ho dan giay', N'Tuyp', 8000, 10),
    (87, N'Khau trang y te 10 cai', N'Hop', 25000, 11),
    (88, N'Ao mua dung mot lan', N'Cai', 12000, 12),
    (89, N'O cam gap mini', N'Cai', 45000, 13),
    (90, N'Den pin mini', N'Cai', 30000, 8),
    (91, N'Trung cut hop 20 qua', N'Hop', 18000, 9),
    (92, N'Chuoi cau 500g', N'Kg', 18000, 10),
    (93, N'Tao do nhap khau 1 qua', N'Qua', 12000, 11),
    (94, N'Cam tuoi 1kg', N'Kg', 35000, 12),
    (95, N'Banh bao nhan thit', N'Cai', 15000, 13),
    (96, N'Khoai tay dong lanh 500g', N'Goi', 32000, 8),
    (97, N'Ga vien chien 250g', N'Goi', 35000, 9),
    (98, N'Xiu mai dong lanh 250g', N'Goi', 30000, 10),
    (99, N'Kem oc que vani', N'Cai', 12000, 11),
    (100, N'Da vien tui 1kg', N'Tui', 10000, 12);

INSERT INTO SAN_PHAM (ma_san_pham, ma_loai, ten_san_pham, don_vi_tinh, gia_ban, nguong_ton, trang_thai)
SELECT
    N'893' + RIGHT(N'000000000' + CONVERT(NVARCHAR(9), p.product_no), 9),
    N'LSP' + RIGHT(N'000' + CONVERT(NVARCHAR(3), ((p.product_no - 1) / 10) + 1), 3),
    p.ten_san_pham,
    p.don_vi_tinh,
    p.gia_ban,
    p.nguong_ton,
    N'DANG_KINH_DOANH'
FROM #ProductSeed p;

-- 3.4 KHACH_HANG: chinh xac 40 khach hang, SDT duy nhat
INSERT INTO KHACH_HANG (ma_khach_hang, ho_ten, so_dien_thoai, diem_hien_co, diem_tich_luy, trang_thai)
VALUES
    (N'KH001', N'Le Minh Khach', N'0901000001', 120, 2500, N'HOAT_DONG'),
    (N'KH002', N'Pham Thi Mai', N'0901000002', 240, 5000, N'HOAT_DONG'),
    (N'KH003', N'Nguyen Hoang Long', N'0901000003', 360, 7500, N'HOAT_DONG'),
    (N'KH004', N'Tran Ngoc Han', N'0901000004', 480, 10000, N'HOAT_DONG'),
    (N'KH005', N'Le Thanh Tung', N'0901000005', 600, 12500, N'HOAT_DONG'),
    (N'KH006', N'Vo Minh Chau', N'0901000006', 720, 15000, N'HOAT_DONG'),
    (N'KH007', N'Do Gia Bao', N'0901000007', 840, 17500, N'HOAT_DONG'),
    (N'KH008', N'Bui Khanh Vy', N'0901000008', 960, 20000, N'HOAT_DONG'),
    (N'KH009', N'Hoang Duc Anh', N'0901000009', 1080, 22500, N'HOAT_DONG'),
    (N'KH010', N'Nguyen Thu Ha', N'0901000010', 1200, 25000, N'HOAT_DONG'),
    (N'KH011', N'Tran Minh Tuan', N'0901000011', 1320, 27500, N'HOAT_DONG'),
    (N'KH012', N'Pham Ngoc Anh', N'0901000012', 1440, 30000, N'HOAT_DONG'),
    (N'KH013', N'Le Hoang Phuc', N'0901000013', 1560, 32500, N'HOAT_DONG'),
    (N'KH014', N'Nguyen Bao Tram', N'0901000014', 1680, 35000, N'HOAT_DONG'),
    (N'KH015', N'Do Minh Quan', N'0901000015', 1800, 37500, N'HOAT_DONG'),
    (N'KH016', N'Bui Thi Thanh', N'0901000016', 1920, 40000, N'HOAT_DONG'),
    (N'KH017', N'Vo Quoc Bao', N'0901000017', 2040, 42500, N'HOAT_DONG'),
    (N'KH018', N'Ho Thi Kim Ngan', N'0901000018', 2160, 45000, N'HOAT_DONG'),
    (N'KH019', N'Dang Minh Khoa', N'0901000019', 2280, 47500, N'HOAT_DONG'),
    (N'KH020', N'Phan Thu Trang', N'0901000020', 2400, 50000, N'HOAT_DONG'),
    (N'KH021', N'Nguyen Duc Thinh', N'0901000021', 2520, 52500, N'HOAT_DONG'),
    (N'KH022', N'Tran Khanh Linh', N'0901000022', 2640, 55000, N'HOAT_DONG'),
    (N'KH023', N'Le Gia Huy', N'0901000023', 2760, 57500, N'HOAT_DONG'),
    (N'KH024', N'Pham Thanh Truc', N'0901000024', 2880, 60000, N'HOAT_DONG'),
    (N'KH025', N'Vu Minh Duc', N'0901000025', 3000, 62500, N'HOAT_DONG'),
    (N'KH026', N'Hoang Ngoc Mai', N'0901000026', 3120, 65000, N'HOAT_DONG'),
    (N'KH027', N'Dang Quoc Viet', N'0901000027', 3240, 67500, N'HOAT_DONG'),
    (N'KH028', N'Bui Minh Tam', N'0901000028', 3360, 70000, N'HOAT_DONG'),
    (N'KH029', N'Do Thanh Dat', N'0901000029', 3480, 72500, N'HOAT_DONG'),
    (N'KH030', N'Nguyen Phuong Thao', N'0901000030', 3600, 75000, N'HOAT_DONG'),
    (N'KH031', N'Tran Gia Han', N'0901000031', 3720, 77500, N'HOAT_DONG'),
    (N'KH032', N'Le Anh Kiet', N'0901000032', 3840, 80000, N'HOAT_DONG'),
    (N'KH033', N'Vo Thi My Duyen', N'0901000033', 3960, 82500, N'HOAT_DONG'),
    (N'KH034', N'Pham Quang Huy', N'0901000034', 4080, 85000, N'HOAT_DONG'),
    (N'KH035', N'Nguyen Hong Nhung', N'0901000035', 4200, 87500, N'HOAT_DONG'),
    (N'KH036', N'Hoang Tuan Kiet', N'0901000036', 4320, 90000, N'HOAT_DONG'),
    (N'KH037', N'Dang Thu Phuong', N'0901000037', 4440, 92500, N'HOAT_DONG'),
    (N'KH038', N'Bui Gia Minh', N'0901000038', 4560, 95000, N'HOAT_DONG'),
    (N'KH039', N'Do Ngoc Bich', N'0901000039', 4680, 97500, N'HOAT_DONG'),
    (N'KH040', N'Phan Minh Chau', N'0901000040', 4800, 100000, N'HOAT_DONG');

-- 3.5 NHA_CUNG_CAP: chinh xac 5 nha cung cap
INSERT INTO NHA_CUNG_CAP (ma_nha_cung_cap, ten_nha_cung_cap, so_dien_thoai, dia_chi, trang_thai)
VALUES
    (N'NCC001', N'Cong ty TNHH Phan phoi An Phat', N'0903000001', N'Quan 1, TP. Ho Chi Minh', N'HOAT_DONG'),
    (N'NCC002', N'Cong ty Co phan Hang tieu dung Minh Chau', N'0903000002', N'Thu Duc, TP. Ho Chi Minh', N'HOAT_DONG'),
    (N'NCC003', N'Nha phan phoi Thuc pham Viet', N'0903000003', N'Quan 5, TP. Ho Chi Minh', N'HOAT_DONG'),
    (N'NCC004', N'Cong ty TNHH Do uong Sai Gon', N'0903000004', N'Binh Thanh, TP. Ho Chi Minh', N'HOAT_DONG'),
    (N'NCC005', N'Nha cung cap Gia dung va Tien ich 365', N'0903000005', N'Go Vap, TP. Ho Chi Minh', N'HOAT_DONG');

-- 3.6 CAU_HINH_CUA_HANG
INSERT INTO CAU_HINH_CUA_HANG (id, ten_cua_hang, dia_chi, so_dien_thoai, ty_le_vat)
VALUES (1, N'Cua hang tien loi', N'Quan 1, Thanh pho Ho Chi Minh', N'02873000000', 10.00);

-- 3.7 KHUYEN_MAI: chinh xac 8 chuong trinh
-- KM001-KM004: giam tren toan don; KM005-KM008: giam theo san pham.
INSERT INTO KHUYEN_MAI (ma_khuyen_mai, ten_khuyen_mai, pham_vi, ty_le_giam, don_toi_thieu, bat_dau, ket_thuc, trang_thai)
VALUES
    (N'KM001', N'Khuyến mãi giảm 5 phần trăm toàn đơn', N'TOAN_DON',       5.00, 200000, DATEADD(DAY, -180, CONVERT(DATETIME2(0), @DauThang)), DATEADD(SECOND, -1, DATEADD(DAY, 181, CONVERT(DATETIME2(0), @DauThang))), N'DANG_HOAT_DONG'),
    (N'KM002', N'Khuyến mãi giảm 7 phần trăm toàn đơn', N'TOAN_DON',       7.00, 500000, DATEADD(DAY, -180, CONVERT(DATETIME2(0), @DauThang)), DATEADD(SECOND, -1, DATEADD(DAY, 181, CONVERT(DATETIME2(0), @DauThang))), N'DANG_HOAT_DONG'),
    (N'KM003', N'Khuyến mãi giảm 10 phần trăm toàn đơn', N'TOAN_DON',     10.00, 1000000, DATEADD(DAY, -180, CONVERT(DATETIME2(0), @DauThang)), DATEADD(SECOND, -1, DATEADD(DAY, 181, CONVERT(DATETIME2(0), @DauThang))), N'DANG_HOAT_DONG'),
    (N'KM004', N'Khuyến mãi giảm 12 phần trăm toàn đơn', N'TOAN_DON',     12.00, 2000000, DATEADD(DAY, -180, CONVERT(DATETIME2(0), @DauThang)), DATEADD(SECOND, -1, DATEADD(DAY, 181, CONVERT(DATETIME2(0), @DauThang))), N'DANG_HOAT_DONG'),
    (N'KM005', N'Khuyến mãi nhóm đồ uống',               N'THEO_SAN_PHAM', 8.00, NULL, DATEADD(DAY, -180, CONVERT(DATETIME2(0), @DauThang)), DATEADD(SECOND, -1, DATEADD(DAY, 181, CONVERT(DATETIME2(0), @DauThang))), N'DANG_HOAT_DONG'),
    (N'KM006', N'Khuyến mãi nhóm sữa và sản phẩm sữa',  N'THEO_SAN_PHAM', 10.00, NULL, DATEADD(DAY, -180, CONVERT(DATETIME2(0), @DauThang)), DATEADD(SECOND, -1, DATEADD(DAY, 181, CONVERT(DATETIME2(0), @DauThang))), N'DANG_HOAT_DONG'),
    (N'KM007', N'Khuyến mãi nhóm bánh kẹo',             N'THEO_SAN_PHAM', 12.00, NULL, DATEADD(DAY, -180, CONVERT(DATETIME2(0), @DauThang)), DATEADD(SECOND, -1, DATEADD(DAY, 181, CONVERT(DATETIME2(0), @DauThang))), N'DANG_HOAT_DONG'),
    (N'KM008', N'Khuyến mãi nhóm đồ gia dụng',          N'THEO_SAN_PHAM', 15.00, NULL, DATEADD(DAY, -180, CONVERT(DATETIME2(0), @DauThang)), DATEADD(SECOND, -1, DATEADD(DAY, 181, CONVERT(DATETIME2(0), @DauThang))), N'DANG_HOAT_DONG');

INSERT INTO KHUYEN_MAI_SAN_PHAM (ma_khuyen_mai, ma_san_pham)
SELECT v.ma_khuyen_mai,
       N'893' + RIGHT(N'000000000' + CONVERT(NVARCHAR(9), v.product_no), 9)
FROM (VALUES
    (N'KM005', 1),(N'KM005', 2),(N'KM005', 3),(N'KM005', 4),(N'KM005', 5),
    (N'KM006', 11),(N'KM006', 12),(N'KM006', 13),(N'KM006', 14),(N'KM006', 15),
    (N'KM007', 21),(N'KM007', 22),(N'KM007', 23),(N'KM007', 24),(N'KM007', 25),
    (N'KM008', 71),(N'KM008', 72),(N'KM008', 73),(N'KM008', 74),(N'KM008', 75)
    ) AS v(ma_khuyen_mai, product_no);

-- 3.8 LAP KE HOACH 10 PHIEU NHAP
CREATE TABLE #ImportPlan (
                             import_no INT NOT NULL PRIMARY KEY,
                             thoi_diem_lap DATETIME2(0) NOT NULL
);

INSERT INTO #ImportPlan (import_no, thoi_diem_lap)
SELECT v.import_no,
       DATEADD(DAY, -240 + ((v.import_no - 1) * 2), CONVERT(DATETIME2(0), @NgayHienTai))
FROM (VALUES (1),(2),(3),(4),(5),(6),(7),(8),(9),(10)) AS v(import_no);

-- 3.9 PHIEU_NHAP: chinh xac 10 phieu, moi phieu nhap 10 mat hang
INSERT INTO PHIEU_NHAP (ma_phieu_nhap, ma_nha_cung_cap, ma_nhan_vien, thoi_diem_lap, thoi_diem_hoan_tat, trang_thai, tong_tien)
SELECT
    N'PN' + RIGHT(N'000' + CONVERT(NVARCHAR(3), p.import_no), 3),
    N'NCC' + RIGHT(N'000' + CONVERT(NVARCHAR(3), ((p.import_no - 1) % 5) + 1), 3),
    N'NV' + RIGHT(N'000' + CONVERT(NVARCHAR(3), ((p.import_no - 1) % 5) + 1), 3),
    p.thoi_diem_lap,
    p.thoi_diem_lap,
    N'HOAN_TAT',
    0
FROM #ImportPlan p;

INSERT INTO CHI_TIET_PHIEU_NHAP (ma_phieu_nhap, ma_san_pham, so_luong, don_gia_nhap)
SELECT
    N'PN' + RIGHT(N'000' + CONVERT(NVARCHAR(3), ((p.product_no - 1) / 10) + 1), 3),
    N'893' + RIGHT(N'000000000' + CONVERT(NVARCHAR(9), p.product_no), 9),
    80 + (p.product_no % 21),
    CONVERT(DECIMAL(15,0), ROUND(p.gia_ban * 0.65, 0))
FROM #ProductSeed p;

UPDATE pn
SET tong_tien = x.tong_tien
    FROM PHIEU_NHAP pn
    JOIN (
        SELECT ma_phieu_nhap, SUM(CONVERT(DECIMAL(15,0), so_luong * don_gia_nhap)) AS tong_tien
        FROM CHI_TIET_PHIEU_NHAP
        GROUP BY ma_phieu_nhap
    ) x ON x.ma_phieu_nhap = pn.ma_phieu_nhap;

-- 3.10 LO_TON_KHO: moi san pham co mot lo nhap tuong ung
INSERT INTO LO_TON_KHO (ma_phieu_nhap, ma_san_pham, so_luong_nhap, so_luong_con, thoi_diem_nhap)
SELECT ct.ma_phieu_nhap, ct.ma_san_pham, ct.so_luong, ct.so_luong, pn.thoi_diem_hoan_tat
FROM CHI_TIET_PHIEU_NHAP ct
         JOIN PHIEU_NHAP pn ON pn.ma_phieu_nhap = ct.ma_phieu_nhap;

-- 3.11 PHIEU_DIEU_CHINH_TON: chinh xac 10 phieu, moi phieu co mot dong dieu chinh
CREATE TABLE #AdjustmentPlan (
                                 adjustment_no INT NOT NULL PRIMARY KEY,
                                 product_no INT NOT NULL,
                                 so_luong_giam INT NOT NULL,
                                 ly_do NVARCHAR(255) COLLATE DATABASE_DEFAULT NOT NULL,
                                 thoi_diem_lap DATETIME2(0) NOT NULL
);

INSERT INTO #AdjustmentPlan (adjustment_no, product_no, so_luong_giam, ly_do, thoi_diem_lap)
SELECT v.adjustment_no,
       ((v.adjustment_no - 1) * 10) + 1,
       1 + (v.adjustment_no % 3),
       CASE v.adjustment_no % 3
    WHEN 0 THEN N'Hang hoa hu hong trong qua trinh bao quan'
               WHEN 1 THEN N'Dieu chinh do chenh lech kiem ke'
               ELSE N'Hang hoa het han su dung'
END,
           DATEADD(DAY, -200 + v.adjustment_no, CONVERT(DATETIME2(0), @NgayHienTai))
    FROM (VALUES (1),(2),(3),(4),(5),(6),(7),(8),(9),(10)) AS v(adjustment_no);

INSERT INTO PHIEU_DIEU_CHINH_TON (ma_dieu_chinh, ma_nhan_vien, thoi_diem_lap, ly_do)
SELECT N'DC' + RIGHT(N'000' + CONVERT(NVARCHAR(3), a.adjustment_no), 3),
    N'NV' + RIGHT(N'000' + CONVERT(NVARCHAR(3), ((a.adjustment_no - 1) % 5) + 1), 3),
    a.thoi_diem_lap,
    a.ly_do
FROM #AdjustmentPlan a;

INSERT INTO CHI_TIET_PHIEU_DIEU_CHINH (ma_dieu_chinh, ma_lo, so_luong_giam)
SELECT N'DC' + RIGHT(N'000' + CONVERT(NVARCHAR(3), a.adjustment_no), 3),
    l.ma_lo,
    a.so_luong_giam
FROM #AdjustmentPlan a
    JOIN LO_TON_KHO l
ON l.ma_san_pham = N'893' + RIGHT(N'000000000' + CONVERT(NVARCHAR(9), a.product_no), 9);

-- 3.12 TAO 100 HOA DON: 70 hoa don truoc thang hien tai, 30 hoa don trong thang hien tai
CREATE TABLE #InvoiceBase (
                              invoice_no INT NOT NULL PRIMARY KEY,
                              ma_nhan_vien NVARCHAR(20) COLLATE DATABASE_DEFAULT NOT NULL,
                              ma_khach_hang NVARCHAR(20) COLLATE DATABASE_DEFAULT NOT NULL,
                              ma_khuyen_mai NVARCHAR(20) COLLATE DATABASE_DEFAULT NULL,
                              thoi_diem_lap DATETIME2(0) NOT NULL,
                              phuong_thuc_tt NVARCHAR(20) COLLATE DATABASE_DEFAULT NOT NULL
);

INSERT INTO #InvoiceBase (invoice_no, ma_nhan_vien, ma_khach_hang, ma_khuyen_mai, thoi_diem_lap, phuong_thuc_tt)
SELECT
    n.n,
    N'NV' + RIGHT(N'000' + CONVERT(NVARCHAR(3), ((n.n - 1) % 4) + 2), 3),
    N'KH' + RIGHT(N'000' + CONVERT(NVARCHAR(3), ((n.n - 1) % 40) + 1), 3),
    CASE WHEN n.n % 4 = 0 THEN N'KM00' + CONVERT(NVARCHAR(1), (((n.n / 4) - 1) % 4) + 1) ELSE NULL END,
        CASE
            WHEN n.n <= 70 THEN
                DATEADD(MINUTE, (n.n * 19) % 600,
                    DATEADD(DAY, -((n.n * 2) + 1), CONVERT(DATETIME2(0), @DauThang)))
            ELSE
                DATEADD(SECOND, -((n.n - 71) * (DATEDIFF(SECOND, CONVERT(DATETIME2(0), @DauThang), @ThoiDiemHienTai) / 29)), @ThoiDiemHienTai)
END,
        CASE n.n % 3 WHEN 1 THEN N'TIEN_MAT' WHEN 2 THEN N'THE' ELSE N'QR' END
    FROM (SELECT TOP (100) ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS n FROM sys.all_objects) AS n;

    -- Moi hoa don co 3 san pham khac nhau; tong so dong chi tiet = 300.
CREATE TABLE #InvoiceLines (
                               invoice_no INT NOT NULL,
                               line_no INT NOT NULL,
                               product_no INT NOT NULL,
                               ma_san_pham NVARCHAR(20) COLLATE DATABASE_DEFAULT NOT NULL,
                               so_luong INT NOT NULL,
                               don_gia_ban DECIMAL(15,0) NOT NULL,
                               tien_giam_dong DECIMAL(15,0) NOT NULL,
                               thanh_tien DECIMAL(15,0) NOT NULL,
                               PRIMARY KEY (invoice_no, product_no)
);

INSERT INTO #InvoiceLines (invoice_no, line_no, product_no, ma_san_pham, so_luong, don_gia_ban, tien_giam_dong, thanh_tien)
SELECT
    b.invoice_no,
    v.line_no,
    p.product_no,
    N'893' + RIGHT(N'000000000' + CONVERT(NVARCHAR(9), p.product_no), 9),
    1 + ((b.invoice_no + v.line_no) % 4),
    p.gia_ban,
    0,
    CONVERT(DECIMAL(15,0), (1 + ((b.invoice_no + v.line_no) % 4)) * p.gia_ban)
FROM #InvoiceBase b
    CROSS JOIN (VALUES (1),(2),(3)) AS v(line_no)
    JOIN #ProductSeed p
ON p.product_no = ((b.invoice_no * 7 + v.line_no * 13 - 1) % 100) + 1;

-- Tinh tong tien tu chi tiet truoc khi tao HOA_DON de bao dam so lieu khop nhau.
CREATE TABLE #InvoiceTotals (
                                invoice_no INT NOT NULL PRIMARY KEY,
                                ma_nhan_vien NVARCHAR(20) COLLATE DATABASE_DEFAULT NOT NULL,
                                ma_khach_hang NVARCHAR(20) COLLATE DATABASE_DEFAULT NOT NULL,
                                ma_khuyen_mai NVARCHAR(20) COLLATE DATABASE_DEFAULT NULL,
                                thoi_diem_lap DATETIME2(0) NOT NULL,
                                phuong_thuc_tt NVARCHAR(20) COLLATE DATABASE_DEFAULT NOT NULL,
                                tong_gia_goc DECIMAL(15,0) NOT NULL,
                                ty_le_giam DECIMAL(5,2) NOT NULL,
                                tien_giam DECIMAL(15,0) NOT NULL
);

INSERT INTO #InvoiceTotals (invoice_no, ma_nhan_vien, ma_khach_hang, ma_khuyen_mai, thoi_diem_lap, phuong_thuc_tt, tong_gia_goc, ty_le_giam, tien_giam)
SELECT
    b.invoice_no,
    b.ma_nhan_vien,
    b.ma_khach_hang,
    b.ma_khuyen_mai,
    b.thoi_diem_lap,
    b.phuong_thuc_tt,
    CONVERT(DECIMAL(15,0), SUM(l.thanh_tien)),
    CONVERT(DECIMAL(5,2), COALESCE(MAX(km.ty_le_giam), 0)),
    CONVERT(DECIMAL(15,0), ROUND(SUM(l.thanh_tien) * COALESCE(MAX(km.ty_le_giam), 0) / 100.0, 0))
FROM #InvoiceBase b
         JOIN #InvoiceLines l ON l.invoice_no = b.invoice_no
         LEFT JOIN KHUYEN_MAI km ON km.ma_khuyen_mai = b.ma_khuyen_mai COLLATE DATABASE_DEFAULT
GROUP BY b.invoice_no, b.ma_nhan_vien, b.ma_khach_hang, b.ma_khuyen_mai, b.thoi_diem_lap, b.phuong_thuc_tt;

INSERT INTO HOA_DON (
    ma_hoa_don, ma_nhan_vien, ma_khach_hang, ma_khuyen_mai, ma_nhan_vien_huy,
    thoi_diem_lap, thoi_diem_huy, phuong_thuc_tt, ma_tham_chieu_tt,
    tong_gia_goc, loai_giam_gia, ty_le_giam, tien_giam,
    diem_su_dung, tien_giam_diem, diem_tich_luy_kiem, diem_thuong_kiem,
    ty_le_vat, tien_vat, tong_thanh_toan, tien_khach_dua, tien_thua,
    trang_thai, ly_do_huy
)
SELECT
    N'HD' + RIGHT(N'000' + CONVERT(NVARCHAR(3), t.invoice_no), 3),
    t.ma_nhan_vien,
    t.ma_khach_hang,
    t.ma_khuyen_mai,
    NULL,
    t.thoi_diem_lap,
    NULL,
    t.phuong_thuc_tt,
    CASE WHEN t.phuong_thuc_tt = N'TIEN_MAT' THEN NULL
    ELSE N'PAY-' + CONVERT(NVARCHAR(4), YEAR(@NgayHienTai)) + N'-' + RIGHT(N'000000' + CONVERT(NVARCHAR(6), t.invoice_no), 6)
END,
        t.tong_gia_goc,
        CASE WHEN t.ma_khuyen_mai IS NULL THEN N'KHONG' ELSE N'KHUYEN_MAI' END,
        t.ty_le_giam,
        t.tien_giam,
        0,
        0,
        CONVERT(BIGINT, FLOOR((t.tong_gia_goc - t.tien_giam) / 1000.0)),
        0,
        CONVERT(DECIMAL(5,2), 10.00),
        v.tien_vat,
        tt.tong_thanh_toan,
        CASE WHEN t.phuong_thuc_tt = N'TIEN_MAT'
             THEN CONVERT(DECIMAL(15,0), CEILING(tt.tong_thanh_toan / 10000.0) * 10000)
             ELSE NULL
END,
        CASE WHEN t.phuong_thuc_tt = N'TIEN_MAT'
             THEN CONVERT(DECIMAL(15,0), CEILING(tt.tong_thanh_toan / 10000.0) * 10000) - tt.tong_thanh_toan
             ELSE NULL
END,
        N'DA_THANH_TOAN',
        NULL
    FROM #InvoiceTotals t
    CROSS APPLY (
        SELECT CONVERT(DECIMAL(15,0), ROUND((t.tong_gia_goc - t.tien_giam) * 10.00 / 100.0, 0)) AS tien_vat
    ) v
    CROSS APPLY (
        SELECT CONVERT(DECIMAL(15,0), (t.tong_gia_goc - t.tien_giam) + v.tien_vat) AS tong_thanh_toan
    ) tt;

    -- 3.13 CHI_TIET_HOA_DON
INSERT INTO CHI_TIET_HOA_DON (ma_hoa_don, ma_san_pham, so_luong, don_gia_ban, tien_giam_dong, thanh_tien)
SELECT
    N'HD' + RIGHT(N'000' + CONVERT(NVARCHAR(3), l.invoice_no), 3),
    l.ma_san_pham,
    l.so_luong,
    l.don_gia_ban,
    l.tien_giam_dong,
    l.thanh_tien
FROM #InvoiceLines l;

-- 3.14 PHAN_BO_XUAT_LO: moi dong ban duoc gan vao lo nhap cua chinh san pham do
INSERT INTO PHAN_BO_XUAT_LO (ma_hoa_don, ma_san_pham, ma_lo, so_luong_xuat, don_gia_von)
SELECT
    ct.ma_hoa_don,
    ct.ma_san_pham,
    l.ma_lo,
    ct.so_luong,
    ctpn.don_gia_nhap
FROM CHI_TIET_HOA_DON ct
         JOIN LO_TON_KHO l ON l.ma_san_pham = ct.ma_san_pham
         JOIN CHI_TIET_PHIEU_NHAP ctpn
              ON ctpn.ma_phieu_nhap = l.ma_phieu_nhap
                  AND ctpn.ma_san_pham = l.ma_san_pham;

-- Cap nhat ton con theo cong thuc: nhap - xuat ban - so luong dieu chinh giam.
UPDATE l
SET so_luong_con = l.so_luong_nhap
    - COALESCE(x.so_luong_xuat, 0)
    - COALESCE(d.so_luong_giam, 0)
    FROM LO_TON_KHO l
    OUTER APPLY (
        SELECT SUM(pbl.so_luong_xuat) AS so_luong_xuat
        FROM PHAN_BO_XUAT_LO pbl
        WHERE pbl.ma_lo = l.ma_lo
    ) x
    OUTER APPLY (
        SELECT SUM(ctdc.so_luong_giam) AS so_luong_giam
        FROM CHI_TIET_PHIEU_DIEU_CHINH ctdc
        WHERE ctdc.ma_lo = l.ma_lo
    ) d;

-- Doi chieu ton kho; neu am ton hoac vuot so luong nhap thi script se rollback.
IF EXISTS (SELECT 1 FROM LO_TON_KHO WHERE so_luong_con < 0 OR so_luong_con > so_luong_nhap)
        THROW 51001, N'Du lieu ton kho khong hop le.', 1;

    -- Doi chieu tong tien hoa don voi tong chi tiet.
    IF EXISTS (
        SELECT 1
        FROM HOA_DON hd
        JOIN CHI_TIET_HOA_DON ct ON ct.ma_hoa_don = hd.ma_hoa_don
        GROUP BY hd.ma_hoa_don, hd.tong_gia_goc
        HAVING hd.tong_gia_goc <> SUM(ct.so_luong * ct.don_gia_ban - ct.tien_giam_dong)
    )
        THROW 51002, N'Tong gia goc hoa don khong khop chi tiet.', 1;

COMMIT TRANSACTION;
END TRY
BEGIN CATCH
IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;

-- ============================================================
-- 4. KIEM TRA KET QUA SAU KHI TAO DU LIEU
-- ============================================================
PRINT N'============================================================';
PRINT N'TAO DATABASE CUA_HANG_TIEN_LOI VA DU LIEU MAU THANH CONG';
PRINT N'============================================================';

SELECT N'NHAN_VIEN' AS TEN_BANG, COUNT(*) AS SO_LUONG FROM NHAN_VIEN
UNION ALL SELECT N'TAI_KHOAN', COUNT(*) FROM TAI_KHOAN
UNION ALL SELECT N'LOAI_SAN_PHAM', COUNT(*) FROM LOAI_SAN_PHAM
UNION ALL SELECT N'SAN_PHAM', COUNT(*) FROM SAN_PHAM
UNION ALL SELECT N'KHACH_HANG', COUNT(*) FROM KHACH_HANG
UNION ALL SELECT N'NHA_CUNG_CAP', COUNT(*) FROM NHA_CUNG_CAP
UNION ALL SELECT N'KHUYEN_MAI', COUNT(*) FROM KHUYEN_MAI
UNION ALL SELECT N'KHUYEN_MAI_SAN_PHAM', COUNT(*) FROM KHUYEN_MAI_SAN_PHAM
UNION ALL SELECT N'PHIEU_NHAP', COUNT(*) FROM PHIEU_NHAP
UNION ALL SELECT N'CHI_TIET_PHIEU_NHAP', COUNT(*) FROM CHI_TIET_PHIEU_NHAP
UNION ALL SELECT N'LO_TON_KHO', COUNT(*) FROM LO_TON_KHO
UNION ALL SELECT N'PHIEU_DIEU_CHINH_TON', COUNT(*) FROM PHIEU_DIEU_CHINH_TON
UNION ALL SELECT N'CHI_TIET_PHIEU_DIEU_CHINH', COUNT(*) FROM CHI_TIET_PHIEU_DIEU_CHINH
UNION ALL SELECT N'HOA_DON', COUNT(*) FROM HOA_DON
UNION ALL SELECT N'CHI_TIET_HOA_DON', COUNT(*) FROM CHI_TIET_HOA_DON
UNION ALL SELECT N'PHAN_BO_XUAT_LO', COUNT(*) FROM PHAN_BO_XUAT_LO
ORDER BY TEN_BANG;

-- So hoa don trong thang hien tai phai bang 30/100.
SELECT
    COUNT(*) AS TONG_HOA_DON,
    SUM(CASE WHEN thoi_diem_lap >= DATEFROMPARTS(YEAR(SYSDATETIME()), MONTH(SYSDATETIME()), 1)
        AND thoi_diem_lap < DATEADD(MONTH, 1, DATEFROMPARTS(YEAR(SYSDATETIME()), MONTH(SYSDATETIME()), 1))
                 THEN 1 ELSE 0 END) AS HOA_DON_TRONG_THANG_NAY
FROM HOA_DON;

-- Danh sach san pham va ton kho hien tai.
SELECT
    sp.ma_san_pham,
    sp.ten_san_pham,
    l.so_luong_nhap AS TONG_NHAP,
    l.so_luong_con AS TON_KHO_HIEN_TAI,
    sp.nguong_ton,
    CASE WHEN l.so_luong_con <= sp.nguong_ton THEN N'SAP HET HANG' ELSE N'DU HANG' END AS TRANG_THAI_TON
FROM SAN_PHAM sp
         LEFT JOIN LO_TON_KHO l ON l.ma_san_pham = sp.ma_san_pham
ORDER BY sp.ma_san_pham;

-- Kiem tra tong tien hoa don: hai cot CHI_TIET va TONG_GIA_GOC phai bang nhau.
SELECT
    hd.ma_hoa_don,
    hd.tong_gia_goc,
    SUM(ct.so_luong * ct.don_gia_ban - ct.tien_giam_dong) AS TONG_CHI_TIET,
    hd.tien_giam,
    hd.tien_vat,
    hd.tong_thanh_toan
FROM HOA_DON hd
         JOIN CHI_TIET_HOA_DON ct ON ct.ma_hoa_don = hd.ma_hoa_don
GROUP BY hd.ma_hoa_don, hd.tong_gia_goc, hd.tien_giam, hd.tien_vat, hd.tong_thanh_toan
ORDER BY hd.ma_hoa_don;

-- Truy van nay phai khong tra ve dong nao neu ton kho duoc tinh dung.
SELECT l.ma_lo, l.ma_san_pham, l.so_luong_nhap, l.so_luong_con
FROM LO_TON_KHO l
    OUTER APPLY (SELECT SUM(p.so_luong_xuat) AS da_xuat FROM PHAN_BO_XUAT_LO p WHERE p.ma_lo = l.ma_lo) x
OUTER APPLY (SELECT SUM(c.so_luong_giam) AS da_dieu_chinh FROM CHI_TIET_PHIEU_DIEU_CHINH c WHERE c.ma_lo = l.ma_lo) d
WHERE l.so_luong_con <> l.so_luong_nhap - COALESCE(x.da_xuat, 0) - COALESCE(d.da_dieu_chinh, 0);
GO
