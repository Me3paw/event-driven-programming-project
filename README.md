# Quản lý bán hàng tại cửa hàng tiện lợi

Bài tập lớn LTHSK Java năm học 2025–2026. Dự án dùng **Java 17, Java Swing, JDBC và Microsoft SQL Server**, theo mô hình `graphicUI → service → dao`. Entity chứa dữ liệu, service kiểm tra quyền và điều phối transaction, DAO truy cập JDBC.

Các chức năng gồm đăng nhập/phân quyền, danh mục, nhân sự/tài khoản, khách hàng, nhà cung cấp, nhập hàng, tồn theo lô FIFO, điều chỉnh giảm tồn, bán hàng, hủy/xem hóa đơn, khuyến mãi, tích điểm/VAT và báo cáo. Không có ứng dụng người mua hoặc cổng thanh toán thật.

## Chạy trên Linux

Cần Docker Compose và JDK 17. Bộ chạy ưu tiên `/usr/lib/jvm/java-17-openjdk`; nếu JDK nằm nơi khác, đặt `POS_JAVA_HOME` tới thư mục JDK 17. Không thay đổi Java mặc định của máy.

```bash
./dev setup
./dev smoke
./dev run
```

- `setup` khởi động SQL Server 2022 Developer bằng image chính thức của Microsoft, nạp schema/dữ liệu mẫu vào database mới và biên dịch Java 17.
- Lần đầu chạy, `dev` tạo `.env` riêng với mật khẩu ngẫu nhiên. File này bị Git bỏ qua; không chia sẻ thông tin đăng nhập thật.
- `smoke` kiểm tra biên dịch và kết nối JDBC bằng `SELECT 1`; không phải kiểm thử toàn bộ nghiệp vụ.
- `build` chỉ biên dịch. `db-up` khởi động/kiểm tra SQL Server; `db-down` dừng riêng SQL Server.
- `backend-test` chạy hai bộ regression Java 17 trên SQL Server thật, dùng database riêng mặc định `pos_test_audit_20261117`. Tên kiểm thử bắt buộc có tiền tố `pos_test_`; cả runner và Java kiểm tra trước khi ghi. Không chạy fixture vào database phát triển.
- `gui-test` chạy đăng nhập Swing, báo giá cũ/mới, thanh toán trùng, mất phản hồi commit, lỗi hóa đơn sau thanh toán và đăng nhập lại bằng Xvfb riêng. Cần `xvfb-run` và `timeout`; ảnh ở `.dev/audit-fixes/gui/`.
- Fixture dùng tài khoản app đọc/ghi; riêng kiểm thử migration/sequence dùng quản trị container. Mỗi bộ so sánh toàn bộ dữ liệu 17 bảng trước/sau cleanup, kể cả ca cố ý ném lỗi. Sequence/identity có thể tăng và để khoảng trống hợp lệ.
- `eclipse` mở Eclipse với workspace riêng và cấu hình JDK 17.

SQL Server chỉ mở trên `127.0.0.1:14330`, container dùng volume `pos-sqlserver-data`. Volume MariaDB cũ được giữ nguyên. Không tự xóa volume hay nạp lại script khi database đã tồn tại. `setup`, `db-up`, `run` và `smoke` áp dụng migration bổ sung cột/sequence tại `sql/migrations/20261117_audit.sql`; migration không xóa hay tính lại dữ liệu nghiệp vụ.

Script chính là `sql/ScriptMySql.sql`. **Tên file được giữ nguyên từ nhánh hiện tại nhưng nội dung là T-SQL cho Microsoft SQL Server**, không phải MySQL/MariaDB. Script tạo 17 bảng và dữ liệu mẫu. Chạy lại trực tiếp sẽ xóa các bảng cũ; bộ chạy chỉ nạp tự động khi chưa có database.

Ứng dụng đọc ba biến môi trường:

```text
POS_DB_URL=jdbc:sqlserver://127.0.0.1:14330;databaseName=cua_hang_tien_loi;encrypt=true;trustServerCertificate=true
POS_DB_USER=cua_hang_app
POS_DB_PASSWORD=<giá trị trong .env>
```

`trustServerCertificate=true` dành cho container phát triển cục bộ. Ứng dụng dùng login riêng với quyền đọc/ghi dữ liệu; không dùng tài khoản quản trị `sa`.

Tài khoản ứng dụng mẫu: `quanly`, `thungan01`, `thungan02`, `thungan03`, `thungan04`; mật khẩu mẫu là `MatKhau123`.

## Eclipse và Windows

1. Cài JDK 17 và Eclipse Java. Import bằng **Existing Projects into Workspace** tại thư mục chứa `.project` và `.classpath`.
2. Gán JDK 17 cho **JavaSE-17**, đặt mã hóa UTF-8. Driver đã có tại `lib/mssql-jdbc-13.2.0.jre11.jar`; hậu tố `jre11` tương thích runtime Java 17.
3. Khởi động SQL Server trên Windows hoặc dùng Docker, nạp `sql/ScriptMySql.sql` vào database mới, sau đó chạy `sql/migrations/20261117_audit.sql` trong database đó. Với database có dữ liệu, chỉ chạy migration. Tài khoản app cần quyền đọc/ghi và `GRANT UPDATE ON OBJECT::dbo.POS_SEQ_<tiền tố> TO [tài khoản app]` cho NV/KH/KM/LSP/NCC/PN/DC/HD.
4. Đặt `POS_DB_URL`, `POS_DB_USER`, `POS_DB_PASSWORD` trong Run Configuration theo máy đang dùng.
5. Chạy `graphicUI.PosApplication`. Đối số `--db-smoke` chỉ kiểm tra kết nối.

Không dùng MariaDB để thay thế SQL Server khi kiểm thử nhánh này: DAO sử dụng T-SQL và cơ chế khóa của SQL Server.

## Tài liệu và tình trạng kiểm thử

Đặc tả ở [docs/DAC_TA_HE_THONG.md](docs/DAC_TA_HE_THONG.md), sơ đồ ở `docs/diagrams/`. Báo cáo cần có yêu cầu, sơ đồ lớp, thiết kế CSDL và ảnh/mô tả giao diện thực tế. Một lần biên dịch/kết nối thành công không chứng minh các yêu cầu nghiệp vụ đã đạt.

Phím tắt được thiết kế: `F2` bán hàng, `Ctrl+F` tìm kiếm, `Ctrl+S` lưu dialog, `Esc` đóng dialog. Quyết định và kết quả sửa audit ở [docs/audit/implementation-2026-11-17.md](docs/audit/implementation-2026-11-17.md). DOCX được giữ nguyên để audit riêng sau.

Làm việc trên nhánh tính năng/sửa lỗi và tạo Pull Request trước khi merge vào `main`.

## Kiểm thử riêng

```bash
./dev backend-test
./dev backend-test  # chạy lại để kiểm tra cleanup và fixture độc lập
./dev gui-test
# Dùng một database kiểm thử mới khác nếu cần:
POS_TEST_DB_NAME=pos_test_fresh ./dev backend-test
```

Khuyến mãi toàn đơn giữ ngưỡng dương bắt buộc. Cho phép giá bán/nhập 0đ và đổi từng điểm thưởng. Bản ghi chưa có tham chiếu được xóa theo quyền hiện có; chứng từ và tham chiếu lịch sử được giữ. Khóa/nghỉ việc/đổi vai trò có hiệu lực ở thao tác được bảo vệ kế tiếp, theo thứ tự khóa transaction đã mô tả trong đặc tả.
