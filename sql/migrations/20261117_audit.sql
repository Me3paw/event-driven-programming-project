-- Non-destructive SQL Server migration. Run in the intended existing database.
SET NOCOUNT ON;
SET XACT_ABORT ON;
BEGIN TRANSACTION;
DECLARE @lockResult int;
EXEC @lockResult = sys.sp_getapplock @Resource=N'POS_AUTHORIZATION',
    @LockMode='Exclusive', @LockOwner='Transaction', @LockTimeout=15000;
IF @lockResult < 0 THROW 50001, 'Cannot lock authorization for migration', 1;
IF COL_LENGTH('dbo.TAI_KHOAN','session_version') IS NULL
    ALTER TABLE dbo.TAI_KHOAN ADD session_version bigint NOT NULL CONSTRAINT DF_TK_SESSION DEFAULT 0;
IF COL_LENGTH('dbo.NHAN_VIEN','session_version') IS NULL
    ALTER TABLE dbo.NHAN_VIEN ADD session_version bigint NOT NULL CONSTRAINT DF_NV_SESSION DEFAULT 0;
IF COL_LENGTH('dbo.HOA_DON','checkout_actor') IS NULL
    ALTER TABLE dbo.HOA_DON ADD checkout_actor nvarchar(50) NULL;
IF COL_LENGTH('dbo.HOA_DON','checkout_fingerprint') IS NULL
    ALTER TABLE dbo.HOA_DON ADD checkout_fingerprint varchar(64) NULL;
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_HD_CHECKOUT_ACCOUNT')
    EXEC(N'ALTER TABLE dbo.HOA_DON ADD CONSTRAINT FK_HD_CHECKOUT_ACCOUNT
        FOREIGN KEY(checkout_actor) REFERENCES dbo.TAI_KHOAN(ten_dang_nhap)');
DECLARE @largest bigint, @next bigint, @current bigint, @command nvarchar(max);

-- NV: numeric suffix, preserve every existing identifier.
SELECT @largest=COALESCE(MAX(TRY_CONVERT(bigint,SUBSTRING(ma_nhan_vien,3,20))),0)
FROM dbo.NHAN_VIEN WHERE ma_nhan_vien LIKE N'NV%';
SET @next=CASE WHEN @largest >= 999 THEN 999 ELSE @largest+1 END;
IF OBJECT_ID(N'dbo.POS_SEQ_NV',N'SO') IS NULL
BEGIN
    SET @command=N'CREATE SEQUENCE dbo.POS_SEQ_NV AS bigint START WITH '+CONVERT(nvarchar(20),@next)
        +N' INCREMENT BY 1 MINVALUE 1 MAXVALUE 999 NO CYCLE NO CACHE';
    EXEC sys.sp_executesql @command;
END
ELSE
BEGIN
    SELECT @current=CONVERT(bigint,current_value) FROM sys.sequences WHERE object_id=OBJECT_ID(N'dbo.POS_SEQ_NV');
    IF @current < @largest AND @largest < 999
    BEGIN
        SET @command=N'ALTER SEQUENCE dbo.POS_SEQ_NV RESTART WITH '+CONVERT(nvarchar(20),@next);
        EXEC sys.sp_executesql @command;
    END;
END;

-- KH: numeric suffix, preserve every existing identifier.
SELECT @largest=COALESCE(MAX(TRY_CONVERT(bigint,SUBSTRING(ma_khach_hang,3,20))),0)
FROM dbo.KHACH_HANG WHERE ma_khach_hang LIKE N'KH%';
SET @next=CASE WHEN @largest >= 999 THEN 999 ELSE @largest+1 END;
IF OBJECT_ID(N'dbo.POS_SEQ_KH',N'SO') IS NULL
BEGIN
    SET @command=N'CREATE SEQUENCE dbo.POS_SEQ_KH AS bigint START WITH '+CONVERT(nvarchar(20),@next)
        +N' INCREMENT BY 1 MINVALUE 1 MAXVALUE 999 NO CYCLE NO CACHE';
    EXEC sys.sp_executesql @command;
END
ELSE
BEGIN
    SELECT @current=CONVERT(bigint,current_value) FROM sys.sequences WHERE object_id=OBJECT_ID(N'dbo.POS_SEQ_KH');
    IF @current < @largest AND @largest < 999
    BEGIN
        SET @command=N'ALTER SEQUENCE dbo.POS_SEQ_KH RESTART WITH '+CONVERT(nvarchar(20),@next);
        EXEC sys.sp_executesql @command;
    END;
END;

-- KM: numeric suffix, preserve every existing identifier.
SELECT @largest=COALESCE(MAX(TRY_CONVERT(bigint,SUBSTRING(ma_khuyen_mai,3,20))),0)
FROM dbo.KHUYEN_MAI WHERE ma_khuyen_mai LIKE N'KM%';
SET @next=CASE WHEN @largest >= 999 THEN 999 ELSE @largest+1 END;
IF OBJECT_ID(N'dbo.POS_SEQ_KM',N'SO') IS NULL
BEGIN
    SET @command=N'CREATE SEQUENCE dbo.POS_SEQ_KM AS bigint START WITH '+CONVERT(nvarchar(20),@next)
        +N' INCREMENT BY 1 MINVALUE 1 MAXVALUE 999 NO CYCLE NO CACHE';
    EXEC sys.sp_executesql @command;
END
ELSE
BEGIN
    SELECT @current=CONVERT(bigint,current_value) FROM sys.sequences WHERE object_id=OBJECT_ID(N'dbo.POS_SEQ_KM');
    IF @current < @largest AND @largest < 999
    BEGIN
        SET @command=N'ALTER SEQUENCE dbo.POS_SEQ_KM RESTART WITH '+CONVERT(nvarchar(20),@next);
        EXEC sys.sp_executesql @command;
    END;
END;

-- LSP: numeric suffix, preserve every existing identifier.
SELECT @largest=COALESCE(MAX(TRY_CONVERT(bigint,SUBSTRING(ma_loai,4,20))),0)
FROM dbo.LOAI_SAN_PHAM WHERE ma_loai LIKE N'LSP%';
SET @next=CASE WHEN @largest >= 99999999999999999 THEN 99999999999999999 ELSE @largest+1 END;
IF OBJECT_ID(N'dbo.POS_SEQ_LSP',N'SO') IS NULL
BEGIN
    SET @command=N'CREATE SEQUENCE dbo.POS_SEQ_LSP AS bigint START WITH '+CONVERT(nvarchar(20),@next)
        +N' INCREMENT BY 1 MINVALUE 1 MAXVALUE 99999999999999999 NO CYCLE NO CACHE';
    EXEC sys.sp_executesql @command;
END
ELSE
BEGIN
    SELECT @current=CONVERT(bigint,current_value) FROM sys.sequences WHERE object_id=OBJECT_ID(N'dbo.POS_SEQ_LSP');
    IF @current < @largest AND @largest < 99999999999999999
    BEGIN
        SET @command=N'ALTER SEQUENCE dbo.POS_SEQ_LSP RESTART WITH '+CONVERT(nvarchar(20),@next);
        EXEC sys.sp_executesql @command;
    END;
END;

-- NCC: numeric suffix, preserve every existing identifier.
SELECT @largest=COALESCE(MAX(TRY_CONVERT(bigint,SUBSTRING(ma_nha_cung_cap,4,20))),0)
FROM dbo.NHA_CUNG_CAP WHERE ma_nha_cung_cap LIKE N'NCC%';
SET @next=CASE WHEN @largest >= 99999999999999999 THEN 99999999999999999 ELSE @largest+1 END;
IF OBJECT_ID(N'dbo.POS_SEQ_NCC',N'SO') IS NULL
BEGIN
    SET @command=N'CREATE SEQUENCE dbo.POS_SEQ_NCC AS bigint START WITH '+CONVERT(nvarchar(20),@next)
        +N' INCREMENT BY 1 MINVALUE 1 MAXVALUE 99999999999999999 NO CYCLE NO CACHE';
    EXEC sys.sp_executesql @command;
END
ELSE
BEGIN
    SELECT @current=CONVERT(bigint,current_value) FROM sys.sequences WHERE object_id=OBJECT_ID(N'dbo.POS_SEQ_NCC');
    IF @current < @largest AND @largest < 99999999999999999
    BEGIN
        SET @command=N'ALTER SEQUENCE dbo.POS_SEQ_NCC RESTART WITH '+CONVERT(nvarchar(20),@next);
        EXEC sys.sp_executesql @command;
    END;
END;

-- PN: numeric suffix, preserve every existing identifier.
SELECT @largest=COALESCE(MAX(TRY_CONVERT(bigint,SUBSTRING(ma_phieu_nhap,3,20))),0)
FROM dbo.PHIEU_NHAP WHERE ma_phieu_nhap LIKE N'PN%';
SET @next=CASE WHEN @largest >= 999999999999999999 THEN 999999999999999999 ELSE @largest+1 END;
IF OBJECT_ID(N'dbo.POS_SEQ_PN',N'SO') IS NULL
BEGIN
    SET @command=N'CREATE SEQUENCE dbo.POS_SEQ_PN AS bigint START WITH '+CONVERT(nvarchar(20),@next)
        +N' INCREMENT BY 1 MINVALUE 1 MAXVALUE 999999999999999999 NO CYCLE NO CACHE';
    EXEC sys.sp_executesql @command;
END
ELSE
BEGIN
    SELECT @current=CONVERT(bigint,current_value) FROM sys.sequences WHERE object_id=OBJECT_ID(N'dbo.POS_SEQ_PN');
    IF @current < @largest AND @largest < 999999999999999999
    BEGIN
        SET @command=N'ALTER SEQUENCE dbo.POS_SEQ_PN RESTART WITH '+CONVERT(nvarchar(20),@next);
        EXEC sys.sp_executesql @command;
    END;
END;

-- DC: numeric suffix, preserve every existing identifier.
SELECT @largest=COALESCE(MAX(TRY_CONVERT(bigint,SUBSTRING(ma_dieu_chinh,3,20))),0)
FROM dbo.PHIEU_DIEU_CHINH_TON WHERE ma_dieu_chinh LIKE N'DC%';
SET @next=CASE WHEN @largest >= 999999999999999999 THEN 999999999999999999 ELSE @largest+1 END;
IF OBJECT_ID(N'dbo.POS_SEQ_DC',N'SO') IS NULL
BEGIN
    SET @command=N'CREATE SEQUENCE dbo.POS_SEQ_DC AS bigint START WITH '+CONVERT(nvarchar(20),@next)
        +N' INCREMENT BY 1 MINVALUE 1 MAXVALUE 999999999999999999 NO CYCLE NO CACHE';
    EXEC sys.sp_executesql @command;
END
ELSE
BEGIN
    SELECT @current=CONVERT(bigint,current_value) FROM sys.sequences WHERE object_id=OBJECT_ID(N'dbo.POS_SEQ_DC');
    IF @current < @largest AND @largest < 999999999999999999
    BEGIN
        SET @command=N'ALTER SEQUENCE dbo.POS_SEQ_DC RESTART WITH '+CONVERT(nvarchar(20),@next);
        EXEC sys.sp_executesql @command;
    END;
END;

-- HD: numeric suffix, preserve every existing identifier.
SELECT @largest=COALESCE(MAX(TRY_CONVERT(bigint,SUBSTRING(ma_hoa_don,3,20))),0)
FROM dbo.HOA_DON WHERE ma_hoa_don LIKE N'HD%';
SET @next=CASE WHEN @largest >= 9999999999999 THEN 9999999999999 ELSE @largest+1 END;
IF OBJECT_ID(N'dbo.POS_SEQ_HD',N'SO') IS NULL
BEGIN
    SET @command=N'CREATE SEQUENCE dbo.POS_SEQ_HD AS bigint START WITH '+CONVERT(nvarchar(20),@next)
        +N' INCREMENT BY 1 MINVALUE 1 MAXVALUE 9999999999999 NO CYCLE NO CACHE';
    EXEC sys.sp_executesql @command;
END
ELSE
BEGIN
    SELECT @current=CONVERT(bigint,current_value) FROM sys.sequences WHERE object_id=OBJECT_ID(N'dbo.POS_SEQ_HD');
    IF @current < @largest AND @largest < 9999999999999
    BEGIN
        SET @command=N'ALTER SEQUENCE dbo.POS_SEQ_HD RESTART WITH '+CONVERT(nvarchar(20),@next);
        EXEC sys.sp_executesql @command;
    END;
END;
COMMIT;
