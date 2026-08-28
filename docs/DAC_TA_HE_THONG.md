# Đặc tả hệ thống quản lý bán hàng tại cửa hàng tiện lợi

Tài liệu này là Phần 1, 2 và 3 của bài tập lớn LTHSK Java năm học 2025–2026. Phần mềm phục vụ nhân viên của một cửa hàng tiện lợi; không có ứng dụng, tài khoản hay đơn đặt hàng cho người mua. Cơ sở dữ liệu là MariaDB theo chấp thuận của giảng viên, thay cho SQL Server nêu trong đề bài.

## 1. Thu thập và phân tích yêu cầu

### 1.1. Mục tiêu và phạm vi

Hệ thống desktop hỗ trợ cửa hàng bán tại quầy: quản lý danh mục và nhân sự, nhập hàng, quản lý tồn theo lô, bán hàng, in hóa đơn, khuyến mãi, khách hàng thân thiết và báo cáo cơ bản. Hệ thống chỉ có một cửa hàng, dùng VND và hoạt động nội bộ trên Java 8 Swing.

Ngoài phạm vi: bán trực tuyến, nhiều chi nhánh, trả hàng một phần, trả nhà cung cấp, tích hợp cổng thanh toán thật, chiến dịch CRM/gửi tin, hạn sử dụng theo lô và hủy phiếu nhập đã hoàn tất.

### 1.2. Tác nhân và quyền hạn

| Tác nhân | Quyền |
|---|---|
| Thu ngân | Đăng nhập; lập và in hóa đơn; quản lý khách hàng; xem sản phẩm, tồn kho và hóa đơn do mình lập. |
| Quản lý | Toàn bộ quyền của thu ngân; quản lý nhân viên/tài khoản, danh mục, nhà cung cấp, phiếu nhập, điều chỉnh tồn, khuyến mãi, cấu hình, hủy hóa đơn và báo cáo. |

Nhân viên không có quyền không nhìn thấy chức năng tương ứng; service vẫn phải kiểm tra quyền, không chỉ dựa vào nút giao diện.

### 1.3. Yêu cầu chức năng

#### Đăng nhập, nhân sự và danh mục

- Đăng nhập bằng tên đăng nhập và mật khẩu. Tài khoản hoặc nhân viên ngừng hoạt động không được đăng nhập.
- Quản lý nhân viên, tài khoản, loại sản phẩm, sản phẩm, khách hàng và nhà cung cấp: thêm, sửa, xóa, liệt kê, xem chi tiết, tìm kiếm đơn giản và nâng cao.
- Tìm kiếm đơn giản theo từ khóa; tìm kiếm nâng cao theo các điều kiện phù hợp, ví dụ trạng thái, loại, khoảng giá, khoảng tồn, hạng khách hàng hoặc khoảng ngày.
- Bản ghi chưa được tham chiếu có thể xóa. Bản ghi đã phát sinh chứng từ chỉ được chuyển ngừng hoạt động, không được xóa vật lý.
- Sản phẩm ngừng kinh doanh không được thêm vào hóa đơn mới. Nhân viên nghỉ việc/khóa tài khoản không được đăng nhập nhưng lịch sử chứng từ được giữ nguyên.

#### Nhập hàng và tồn kho

- Quản lý lập phiếu nhập từ một nhà cung cấp, thêm/sửa/xóa dòng hàng khi phiếu ở trạng thái `NHAP`.
- Hoàn tất phiếu nhập chuyển trạng thái thành `HOAN_TAT`, tạo các lô tồn kho và khóa nội dung phiếu. Không hỗ trợ hủy phiếu nhập đã hoàn tất.
- Tồn hiện tại của một sản phẩm là tổng `so_luong_con` của các lô tồn kho; không lưu trùng số lượng tồn trong `SAN_PHAM`.
- Quản lý có thể điều chỉnh giảm tồn; bắt buộc ghi lý do. Số lượng giảm được lấy theo FIFO, từ lô cũ nhất còn hàng. Không có điều chỉnh tăng trong phạm vi này.

#### Bán hàng, hóa đơn và in hóa đơn

- Thu ngân tạo hóa đơn tại quầy, có thể chọn khách hàng hoặc bán cho khách lẻ.
- Tìm sản phẩm theo mã/tên, thêm vào giỏ, đổi số lượng hoặc bỏ dòng trước thanh toán. Hệ thống kiểm tra sản phẩm đang kinh doanh và đủ tồn.
- Phương thức thanh toán: tiền mặt, thẻ hoặc QR. Với tiền mặt, `tien_khach_dua` phải lớn hơn hoặc bằng `tong_thanh_toan` và tiền thừa bằng hiệu số. Với thẻ/QR, tiền khách đưa và tiền thừa để trống, phải lưu mã tham chiếu mô phỏng thành công. Thẻ/QR là mô phỏng thành công/thất bại tại máy, không gọi dịch vụ ngoài; mô phỏng thất bại không tạo hóa đơn, không trừ tồn, không đổi điểm.
- Hóa đơn thanh toán thành công là bất biến: không sửa/xóa trực tiếp hóa đơn hay chi tiết. Có thể xem danh sách, chi tiết và tìm theo mã, nhân viên, khách hàng, trạng thái, phương thức hoặc khoảng ngày.
- In/xem trước hóa đơn bằng API in có sẵn của Java; hóa đơn có thông tin cửa hàng, mã, thời gian, nhân viên, khách hàng nếu có, dòng hàng, giảm giá, VAT, tổng phải trả, phương thức thanh toán và tiền thừa nếu là tiền mặt.
- Chỉ quản lý được hủy hóa đơn. Hủy cần lý do và xác thực lại quản lý ngay trên máy; một hóa đơn không được hủy hai lần. Hủy hoàn lại chính xác các lô đã xuất, hoàn điểm thưởng đã dùng, nhưng giữ điểm tích lũy và điểm thưởng đã kiếm từ hóa đơn đó.

#### Khách hàng, khuyến mãi và cấu hình

- Hồ sơ khách hàng gồm thông tin liên hệ, lịch sử mua, điểm tích lũy và điểm thưởng khả dụng. Tổng chi tiêu và hạng là dữ liệu suy ra từ hóa đơn chưa hủy và điểm tích lũy, không lưu cột trùng trong `KHACH_HANG`. Không có chiến dịch chăm sóc hoặc gửi thông báo.
- Điểm tích lũy dùng để xếp hạng: dưới 50.000 là chưa xếp hạng; Đồng từ 50.000, Bạc từ 200.000, Vàng từ 1.000.000 điểm. Mỗi hóa đơn thành công kiếm đúng số điểm tích lũy bằng tổng giá gốc VND (`1 điểm tích lũy = 1đ`); điểm này chỉ dùng xếp hạng, không đổi thưởng.
- Với hóa đơn có khách hàng, hạng trước giao dịch được dùng cho giảm giá: Đồng 3%, Bạc 5%, Vàng 10%; chưa xếp hạng 0%.
- Điểm thưởng khả dụng là số điểm có thể đổi thưởng. Mỗi hóa đơn thành công kiếm `floor(5% × tổng giá gốc)` điểm thưởng; `1 điểm thưởng = 1đ` giảm giá. Hóa đơn không gắn khách hàng không kiếm/dùng hai loại điểm.
- Quản lý tạo khuyến mãi theo phần trăm, có thời gian hiệu lực và phạm vi toàn đơn hoặc danh sách sản phẩm. Tại một hóa đơn chỉ chọn một loại giảm giá: giảm theo hạng hoặc một sự kiện khuyến mãi; không cộng dồn hai loại. Điểm thưởng có thể dùng cùng lựa chọn đó.
- VAT mặc định 10%, nằm trong khoảng 0–100%. Quản lý có thể đổi VAT áp dụng cho giao dịch tương lai; mức VAT, tỷ lệ/mức giảm và các giá trị tính tại thời điểm bán phải được lưu trong hóa đơn.

#### Báo cáo

- Quản lý xem theo khoảng ngày: doanh thu, tiền nhập, giá vốn FIFO, lợi nhuận gộp, số hóa đơn, hàng bán chạy, sản phẩm tồn thấp và khách hàng theo hạng.
- Màn hình báo cáo dùng KPI, bảng và biểu đồ Java2D cơ bản; không thêm thư viện biểu đồ.

### 1.4. Luồng nghiệp vụ chính

#### UC01 — Đăng nhập

1. Người dùng nhập tên đăng nhập và mật khẩu.
2. Hệ thống xác thực tài khoản, trạng thái tài khoản và nhân viên.
3. Hệ thống mở giao diện theo vai trò, hoặc báo lỗi mà không tiết lộ mật khẩu sai ở đâu.

#### UC02 — Hoàn tất phiếu nhập

1. Quản lý tạo phiếu ở trạng thái `NHAP`, chọn nhà cung cấp và nhập các dòng sản phẩm, số lượng, đơn giá.
2. Hệ thống kiểm tra dữ liệu hợp lệ và cho phép sửa/xóa dòng khi phiếu chưa hoàn tất.
3. Quản lý xác nhận hoàn tất.
4. Trong một transaction, hệ thống khóa phiếu, tạo một lô tồn cho mỗi dòng và chuyển phiếu sang `HOAN_TAT`.
5. Sau khi hoàn tất, nội dung phiếu không còn sửa/xóa được.

#### UC03 — Thanh toán hóa đơn

1. Thu ngân tạo giỏ hàng, chọn khách hàng tùy chọn, thêm sản phẩm và số lượng.
2. Hệ thống tính tổng giá gốc, giảm giá, điểm thưởng, VAT và tổng phải trả theo thứ tự ở mục 1.5.
3. Thu ngân chọn phương thức thanh toán. Tiền mặt phải đủ tiền nhận; thẻ/QR phải có kết quả mô phỏng thành công và mã tham chiếu trước khi lưu.
4. Trong một transaction, nếu có khách hàng hệ thống khóa hàng `KHACH_HANG` bằng `SELECT ... FOR UPDATE`, kiểm tra lại số điểm thưởng khả dụng, rồi khóa các lô cần xuất bằng `SELECT ... FOR UPDATE`, kiểm tra tồn lại, lưu hóa đơn/chi tiết/phân bổ lô, trừ lô và cập nhật hai loại điểm.
5. Nếu bất kỳ bước nào lỗi, rollback toàn bộ; nếu thành công, in hoặc xem trước hóa đơn.

#### UC04 — Hủy hóa đơn

1. Quản lý chọn một hóa đơn `DA_THANH_TOAN`, nhập lý do và xác thực lại mật khẩu.
2. Trong một transaction, hệ thống khóa hóa đơn và các phân bổ lô, kiểm tra hóa đơn chưa hủy.
3. Hệ thống cộng lại số lượng vào đúng lô đã xuất, hoàn số điểm đã dùng, ghi thời điểm/lý do/người hủy và chuyển hóa đơn sang `DA_HUY`.
4. Điểm đã kiếm của hóa đơn không bị trừ; báo cáo chỉ tính hóa đơn chưa hủy.

### 1.5. Quy tắc nghiệp vụ và ràng buộc

| Mã | Quy tắc |
|---|---|
| BR01 | Tiền dùng VND: Java dùng `BigDecimal`, CSDL dùng `DECIMAL(15,0)`; không dùng `double`. Tỷ lệ dùng `DECIMAL(5,2)`, điểm dùng `BIGINT`. |
| BR02 | Tổng giá gốc là tổng số lượng nhân đơn giá bán đã chốt trên từng dòng. |
| BR03 | Thứ tự tính: tổng giá gốc → một giảm giá theo hạng hoặc sự kiện → trừ điểm thưởng → tính VAT trên phần còn lại. Phép nhân tỷ lệ làm tròn `HALF_UP` đến VND; điểm thưởng cộng làm tròn xuống. |
| BR04 | Hạng áp dụng dựa trên điểm tích lũy trước giao dịch. Hóa đơn thành công kiếm điểm tích lũy bằng tổng giá gốc VND, chỉ dùng xếp hạng; đồng thời kiếm `floor(5% × tổng giá gốc)` điểm thưởng khả dụng. Một điểm thưởng đổi 1 VND. |
| BR05 | Chỉ dùng một trong hai loại giảm giá hạng/sự kiện. Khuyến mãi có tỷ lệ lớn hơn 0% và không quá 100%, chỉ hiệu lực trong khoảng thời gian đã cấu hình và đúng phạm vi; phạm vi sản phẩm chỉ giảm trên tổng giá gốc của các dòng sản phẩm thuộc sự kiện. |
| BR06 | Điểm thưởng dùng không vượt quá cả phần tiền sau giảm giá và số điểm thưởng khả dụng của khách hàng trước giao dịch; phần chịu VAT không âm. VAT và tỷ lệ giảm giá được lưu theo hóa đơn, VAT nằm trong 0–100%. |
| BR07 | Giá vốn và xuất tồn dùng FIFO: lô còn hàng có ngày tạo cũ nhất được xuất trước. Giá vốn được chốt theo từng phân bổ lô, không nằm trên chi tiết hóa đơn. |
| BR08 | Chứng từ `HOAN_TAT` hoặc `DA_THANH_TOAN` bất biến; chỉ nghiệp vụ hủy hóa đơn mới thay đổi trạng thái bán hàng và tồn. |
| BR09 | Hóa đơn hủy phục hồi đúng số lượng cho đúng lô đã xuất, hoàn điểm thưởng đã dùng, giữ hai loại điểm đã kiếm và không hủy hai lần. |
| BR10 | Tất cả thao tác tạo/hủy chứng từ và thay đổi tồn chạy trong transaction do service quản lý. |
| BR11 | Dữ liệu được tham chiếu lịch sử không xóa vật lý; dùng trạng thái hoạt động/ngừng hoạt động. |

### 1.6. Yêu cầu phi chức năng

- Java 8 Swing, JDBC và MariaDB; mô hình ba lớp `graphicUI → service → dao`.
- Mọi dữ liệu do người dùng nhập vào câu SQL phải dùng `PreparedStatement`.
- Giao diện tiếng Việt, thao tác rõ ràng, xác nhận trước xóa/hủy, hỗ trợ Tab, Enter, Esc, `F2` tạo hóa đơn, `Ctrl+S` lưu và `Ctrl+F` tìm.
- Dùng `CardLayout`; truy vấn có thể chờ dùng `SwingWorker` để không treo giao diện.
- Tuân thủ Java Coding Convention; mật khẩu không lưu dạng rõ và thông tin kết nối thật không đưa vào Git.

### 1.7. Tiêu chí nghiệm thu

- CRUD, danh sách, chi tiết, tìm đơn giản/nâng cao đúng quyền và xử lý bản ghi đã tham chiếu.
- Hoàn tất phiếu nhập tạo lô; điều chỉnh giảm có lý do và áp dụng FIFO.
- Các mốc hạng 49.999/50.000/200.000/1.000.000, giảm 0/3/5/10%, khuyến mãi toàn đơn/theo sản phẩm/hết hạn và quy tắc không cộng dồn đều cho kết quả đúng.
- Thanh toán mock thất bại không làm đổi CSDL; tiền mặt thiếu tiền bị chặn, thẻ/QR thành công có mã tham chiếu và không có tiền nhận/thừa; thứ tự giảm giá → điểm thưởng → VAT đúng, điểm thưởng dùng không vượt phần sau giảm hoặc số điểm trước giao dịch, VAT 0–100% và tiền làm tròn đúng.
- Bán qua nhiều lô xuất FIFO, không âm tồn; lỗi và hai giao dịch đồng thời không làm lệch tồn hoặc tạo hóa đơn dở dang.
- Hủy cần quản lý, không hủy hai lần, phục hồi đúng lô và điểm đã dùng nhưng giữ điểm đã kiếm.
- Các chỉ số báo cáo đối chiếu được với truy vấn SQL và chỉ loại hóa đơn chưa hủy.

## 2. Thiết kế sơ đồ lớp và ràng buộc

Sơ đồ lớp PlantUML được lưu cùng tài liệu trong `docs/`; sơ đồ thể hiện UI chỉ gọi service, service điều phối transaction và DAO chỉ truy cập JDBC.

### 2.1. Các nhóm lớp

| Nhóm | Lớp/chức năng chính | Ràng buộc |
|---|---|---|
| `entity` | `NhanVien`, `TaiKhoan`, `LoaiSanPham`, `SanPham`, `KhachHang`, `NhaCungCap`, `PhieuNhap`, `ChiTietPhieuNhap`, `LoTonKho`, `DieuChinhTon`, `ChiTietDieuChinh`, `HoaDon`, `ChiTietHoaDon`, `PhanBoXuatLo`, `KhuyenMai`, `KhuyenMaiSanPham`, `CauHinhCuaHang` | Entity biểu diễn dữ liệu; không chứa JDBC hay điều khiển Swing. |
| `dao` | DAO tương ứng với entity và truy vấn báo cáo | Chỉ thực hiện SQL/ánh xạ kết quả; nhận `Connection` do service truyền khi nằm trong transaction. |
| `service` | `AuthService`, `MasterDataService`, `PurchaseService`, `InventoryService`, `PromotionService`, `SaleService`, `ReportService`, `SettingsService` | Kiểm tra quyền, dữ liệu, điều phối DAO và sở hữu transaction. |
| `graphicUI` | Đăng nhập, dashboard, danh mục, nhập hàng, điều chỉnh tồn, bán hàng, hóa đơn, khuyến mãi, báo cáo, cấu hình | Không gọi DAO trực tiếp; chỉ lấy dữ liệu từ service. |

Không tạo interface chỉ có một implementation, factory hoặc lớp `BaseDAO`. Các enum tối thiểu: `VaiTro`, `TrangThaiNhanVien`, `TrangThaiSanPham`, `TrangThaiPhieuNhap`, `TrangThaiHoaDon`, `PhuongThucThanhToan`, `LoaiGiamGia`, `PhamViKhuyenMai`, `HangThanhVien`.

### 2.2. Quan hệ lớp quan trọng

| Quan hệ | Bội số | Ràng buộc |
|---|---|---|
| `NhanVien` — `TaiKhoan` | 1 — 0..1 | Một nhân viên có tối đa một tài khoản; tài khoản gắn đúng một nhân viên. |
| `LoaiSanPham` — `SanPham` | 1 — 0..n | Sản phẩm phải thuộc một loại. |
| `NhaCungCap` — `PhieuNhap` | 1 — 0..n | Phiếu nhập phải có một nhà cung cấp. |
| `PhieuNhap` — `ChiTietPhieuNhap` | 1 — 1..n | Mỗi phiếu có ít nhất một dòng khi hoàn tất. |
| `ChiTietPhieuNhap` — `LoTonKho` | 1 — 1 | Hoàn tất phiếu tạo một lô cho mỗi dòng nhập. |
| `HoaDon` — `ChiTietHoaDon` | 1 — 1..n | Hóa đơn thanh toán có ít nhất một dòng. |
| `ChiTietHoaDon` — `PhanBoXuatLo` | 1 — 1..n | Mỗi dòng bán được phân bổ vào một hoặc nhiều lô FIFO. |
| `KhuyenMai` — `KhuyenMaiSanPham` | 1 — 0..n | Phạm vi sản phẩm cần ít nhất một dòng; phạm vi toàn đơn không cần dòng. |

### 2.3. Giao dịch và luồng phụ thuộc

`SaleService` mở transaction; nếu hóa đơn có khách hàng, khóa hàng khách bằng `SELECT ... FOR UPDATE` trước khi kiểm tra/trừ điểm thưởng khả dụng; sau đó khóa lô, kiểm tra lại số lượng, ghi hóa đơn/chi tiết/phân bổ, trừ lô và cập nhật hai loại điểm rồi mới commit. `PurchaseService` và `InventoryService` xử lý hoàn tất nhập/điều chỉnh theo cùng nguyên tắc. Khi có lỗi, service rollback và giao diện chỉ nhận thông báo lỗi nghiệp vụ.

## 3. Thiết kế cơ sở dữ liệu MariaDB và ràng buộc

ERD PlantUML được lưu cùng tài liệu trong `docs/`. Lược đồ có đúng 17 bảng, không tạo bảng dự phòng.

### 3.1. Danh sách bảng

| Nhóm | Bảng | Mục đích |
|---|---|---|
| Danh mục/cấu hình | `NHAN_VIEN`, `TAI_KHOAN`, `LOAI_SAN_PHAM`, `SAN_PHAM`, `KHACH_HANG`, `NHA_CUNG_CAP`, `CAU_HINH_CUA_HANG` | Nhân sự, tài khoản, hàng hóa, khách và cấu hình VAT/thông tin cửa hàng. |
| Nhập/tồn | `PHIEU_NHAP`, `CHI_TIET_PHIEU_NHAP`, `LO_TON_KHO`, `DIEU_CHINH_TON`, `CHI_TIET_DIEU_CHINH` | Chứng từ nhập, lô FIFO và điều chỉnh giảm. |
| Bán hàng | `HOA_DON`, `CHI_TIET_HOA_DON`, `PHAN_BO_XUAT_LO` | Hóa đơn, dòng hàng và nguồn lô thực tế. |
| Khuyến mãi | `KHUYEN_MAI`, `KHUYEN_MAI_SAN_PHAM` | Sự kiện giảm giá và phạm vi sản phẩm. |

### 3.2. Khóa, trường nghiệp vụ và quan hệ

| Bảng | Khóa/chỉ mục chính | Ràng buộc quan trọng |
|---|---|---|
| `NHAN_VIEN` | `ma_nhan_vien` PK | Họ tên bắt buộc; trạng thái hoạt động/nghỉ việc. |
| `TAI_KHOAN` | `ten_dang_nhap` PK, `ma_nhan_vien` UNIQUE/FK | Mật khẩu băm; vai trò và trạng thái bắt buộc. |
| `LOAI_SAN_PHAM` | `ma_loai` PK | Tên loại không rỗng và duy nhất theo quy ước nghiệp vụ. |
| `SAN_PHAM` | `ma_san_pham` PK, `ma_loai` FK | Tên, đơn vị, giá bán không âm, ngưỡng tồn không âm, trạng thái; không có cột tồn hiện tại. |
| `KHACH_HANG` | `ma_khach_hang` PK | Điện thoại duy nhất khi có; điểm tích lũy và điểm thưởng khả dụng `BIGINT` không âm. Tổng chi tiêu và hạng là giá trị suy ra, không lưu cột. |
| `NHA_CUNG_CAP` | `ma_nha_cung_cap` PK | Tên và trạng thái bắt buộc. |
| `CAU_HINH_CUA_HANG` | `id` PK cố định bằng 1 | `CHECK (id = 1)` cùng PK bảo đảm chỉ một hàng; tên cửa hàng, địa chỉ, VAT mặc định `DECIMAL(5,2)` trong 0–100. |
| `PHIEU_NHAP` | `ma_phieu_nhap` PK; FK nhà cung cấp, nhân viên | Trạng thái `NHAP`/`HOAN_TAT`; chỉ hoàn tất mới tạo lô. |
| `CHI_TIET_PHIEU_NHAP` | PK ghép phiếu/sản phẩm; FK phiếu, sản phẩm | Số lượng lớn hơn 0, đơn giá nhập không âm. |
| `LO_TON_KHO` | `ma_lo` PK; FK chi tiết nhập, sản phẩm | Số lượng ban đầu lớn hơn 0; `so_luong_con` từ 0 đến số lượng ban đầu; ngày tạo xác định FIFO. |
| `DIEU_CHINH_TON` | `ma_dieu_chinh` PK; FK nhân viên | Lý do, thời điểm và người quản lý thực hiện bắt buộc. |
| `CHI_TIET_DIEU_CHINH` | PK ghép điều chỉnh/lô; FK điều chỉnh, lô | Chỉ số lượng giảm, lớn hơn 0. |
| `HOA_DON` | `ma_hoa_don` PK; FK nhân viên, khách hàng tùy chọn | Thời điểm, trạng thái, phương thức; lưu tổng giá gốc, loại/mức giảm, điểm thưởng dùng, VAT 0–100%, tổng thanh toán, tiền nhận/thừa hoặc mã tham chiếu thanh toán, điểm tích lũy kiếm, điểm thưởng kiếm và dữ liệu hủy. |
| `CHI_TIET_HOA_DON` | PK ghép hóa đơn/sản phẩm; FK hóa đơn, sản phẩm | Số lượng lớn hơn 0; lưu ảnh chụp đơn giá bán, giảm giá dòng và thành tiền, không lưu giá vốn. |
| `PHAN_BO_XUAT_LO` | PK ghép chi tiết hóa đơn/lô; FK chi tiết hóa đơn, lô | Số lượng xuất lớn hơn 0; lưu `don_gia_von` là ảnh chụp giá vốn lô để tính giá vốn và phục hồi đúng lô khi hủy. |
| `KHUYEN_MAI` | `ma_khuyen_mai` PK | Tỷ lệ giảm `DECIMAL(5,2)` lớn hơn 0 và không quá 100; thời gian bắt đầu trước/kết thúc; phạm vi toàn đơn/theo sản phẩm, trạng thái. |
| `KHUYEN_MAI_SAN_PHAM` | PK ghép khuyến mãi/sản phẩm; FK khuyến mãi, sản phẩm | Chỉ tồn tại với khuyến mãi phạm vi sản phẩm. |

### 3.3. Ràng buộc toàn vẹn và xóa/cập nhật

- Khóa ngoại chứng từ phải chặn xóa bản ghi cha đã tham chiếu. Với danh mục có lịch sử, service đổi trạng thái thay vì xóa.
- Chi tiết phiếu nhập, chi tiết hóa đơn, phân bổ lô và chi tiết điều chỉnh không được sửa/xóa trực tiếp sau khi chứng từ hoàn tất/thanh toán.
- `LO_TON_KHO.so_luong_con` chỉ được thay đổi trong transaction hoàn tất nhập, bán, hủy hóa đơn hoặc điều chỉnh giảm; luôn không âm.
- Tổng giá vốn của hóa đơn lấy từ tổng `so_luong_xuat × don_gia_von` tại `PHAN_BO_XUAT_LO` theo FIFO. Báo cáo doanh thu/lợi nhuận chỉ tính `HOA_DON` trạng thái `DA_THANH_TOAN`.
- Mã nghiệp vụ, tên đăng nhập và điện thoại khách hàng (nếu có) là duy nhất; mọi số lượng bán/nhập/điều chỉnh phải lớn hơn 0.

### 3.4. Truy vết yêu cầu

| Yêu cầu đề bài | Màn hình/service | Bảng |
|---|---|---|
| CRUD, danh sách, chi tiết, tìm kiếm | Danh mục, `MasterDataService` | Nhân viên, tài khoản, loại, sản phẩm, khách, nhà cung cấp, khuyến mãi. |
| Lập và xuất hóa đơn | Bán hàng, `SaleService` | Hóa đơn, chi tiết hóa đơn, phân bổ xuất lô, lô tồn. |
| Cập nhật/xóa bảng quan hệ | Danh mục/chứng từ, các service | Toàn bộ FK; trạng thái thay cho xóa khi có lịch sử. |
| Thống kê cơ bản | Báo cáo, `ReportService` | Hóa đơn, chi tiết, lô, phiếu nhập, sản phẩm, khách hàng. |
| 2 hoặc 3 lớp | Tất cả màn hình/service/DAO | Không áp dụng trực tiếp. |

## Phần 4 cần bổ sung khi hiện thực

Chụp và mô tả các màn hình đăng nhập, trang chính, danh mục, nhập hàng, điều chỉnh tồn, bán hàng, hóa đơn, khuyến mãi và báo cáo sau khi chức năng hoàn thành.
