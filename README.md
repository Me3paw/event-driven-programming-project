# Quản lý bán hàng tại cửa hàng tiện lợi

Bài tập lớn LTHSK Java năm học 2025–2026. Chương trình được xây dựng cho nhân viên cửa hàng tiện lợi.

Dự án dùng Java 8, Java Swing và mô hình ba lớp. Cơ sở dữ liệu chính thức của dự án là MariaDB theo chấp thuận của giảng viên, thay cho SQL Server trong đề bài gốc.

## Chức năng của đề tài

- Đăng nhập và phân quyền quản lý, nhân viên bán hàng.
- Quản lý nhân viên, loại sản phẩm, sản phẩm và khách hàng.
- Thêm, sửa, xóa, xem danh sách và xem chi tiết dữ liệu.
- Tìm kiếm đơn giản theo từ khóa và tìm kiếm nâng cao theo nhiều điều kiện.
- Lập hóa đơn, kiểm tra tồn kho, nhận tiền và tính tiền thừa.
- Xem, tìm kiếm, hủy và in hóa đơn.
- Thống kê doanh thu, số hóa đơn, sản phẩm bán chạy và sản phẩm sắp hết.
- Phím tắt: `F2` mở POS, `Ctrl+F` đặt focus ô tìm kiếm, `Ctrl+S` lưu dialog chỉnh sửa đang mở, `Esc` đóng dialog; hỗ trợ chuyển ô nhập liệu bằng phím Tab.

Khi lưu hóa đơn, chương trình phải lưu chi tiết hóa đơn và trừ tồn kho cùng lúc. Nếu có lỗi thì toàn bộ thao tác phải được hủy để dữ liệu không bị sai.

Đề tài có chức năng nhập hàng, nhà cung cấp, khuyến mãi, tích điểm, thanh toán điện tử mô phỏng, thống kê hàng hóa/thu chi, CRM nội bộ và tính thuế trên đơn hàng. Chương trình không có bán hàng trực tuyến hoặc ứng dụng cho người mua.

## Công nghệ sử dụng

- Eclipse Java Project thông thường, không dùng Maven hoặc Gradle.
- Java 8 và Java Swing.
- MariaDB.
- MariaDB Connector/J `3.5.7` tại `lib/mariadb-java-client-3.5.7.jar`.
- Mã nguồn và dữ liệu tiếng Việt dùng UTF-8.

## Cấu trúc mã nguồn hiện tại

```text
src/
├── components/
├── connectDB/
│   └── DBConnection.java
├── dao/
├── entity/
├── service/
├── testsupport/
└── graphicUI/
    ├── PosApplication.java
    └── PosFrame.java
```

- `graphicUI` chứa đăng nhập, điều hướng và các màn hình nghiệp vụ Swing.
- `service` kiểm tra quyền theo `Actor`, điều phối transaction và gọi DAO.
- `dao` chỉ truy cập JDBC; `entity` chứa dữ liệu; `connectDB` đọc cấu hình kết nối.
- `testsupport` chứa smoke backend và kiểm thử transaction/concurrency tự dọn fixture.

## Trạng thái hiện tại

Dự án hiện có schema MariaDB 17 bảng, đăng nhập/phân quyền, danh mục/khách hàng/nhà cung cấp/nhân sự, nhập hàng-lô FIFO, điều chỉnh tồn, bán hàng-hủy/in hóa đơn, khuyến mãi/điểm/VAT, CRM và báo cáo typed. Service kiểm tra quyền ngoài giao diện; checkout có transaction và retry hữu hạn khi MariaDB báo lỗi tranh chấp tạm thời.

Các màn hình Swing hiện là phần hiện thực đang kiểm thử; ảnh và mô tả Phần 4 vẫn cần chuẩn bị khi nộp.

## Chuẩn bị trên Linux

Máy cần có Docker. Bộ công cụ cục bộ không thay đổi Java đang dùng của hệ thống.

Tại thư mục gốc của dự án, chạy:

```bash
./dev setup
./dev smoke
./dev backend-test
./dev eclipse
```

- `setup` tải Java 8, Eclipse và MariaDB cần cho dự án.
- `smoke` chỉ biên dịch và kiểm tra kết nối database bằng `SELECT 1`.
- `backend-test` chạy smoke quyền, kiểm thử transaction/FIFO/làm tròn/concurrency và tự dọn fixture tạm.
- `eclipse` mở Eclipse với workspace riêng của dự án.

Khi không còn làm dự án, có thể xóa môi trường cục bộ bằng:

```bash
./dev wipe
```

Lệnh này xóa Java 8, Eclipse, file biên dịch và dữ liệu MariaDB của riêng dự án. Image MariaDB dùng chung của Docker vẫn được giữ lại.

## Chuẩn bị Eclipse trên Windows

Tập tin `dev` chỉ dùng cho Linux. Trên Windows, cài và cấu hình thủ công như sau:

1. Cài JDK 8. Có thể dùng Eclipse Temurin 8 từ trang [Adoptium](https://adoptium.net/temurin/releases/?version=8).
2. Tải bản **Eclipse IDE for Java Developers** từ trang [Eclipse Packages](https://www.eclipse.org/downloads/packages/).
3. Mở Eclipse, chọn **Window > Preferences > Java > Installed JREs**.
4. Chọn **Add... > Standard VM**, rồi chọn thư mục JDK 8 vừa cài.
5. Đặt tên JDK là `Temurin 8` hoặc `JDK 8`, sau đó đánh dấu JDK này làm mặc định.
6. Trong **Java > Installed JREs > Execution Environments**, gán JDK 8 cho `JavaSE-1.8`.
7. Đặt mã hóa workspace tại **General > Workspace > Text file encoding > UTF-8**.
8. Import dự án theo hướng dẫn ở phần tiếp theo.

Trên Windows, có thể chạy MariaDB bằng Docker Desktop hoặc cài MariaDB trực tiếp. Các thành viên phải dùng cùng cấu trúc database trong `sql/Script.sql`.

## Import dự án vào Eclipse

1. Chọn **File > Import...**.
2. Mở nhóm **General** và chọn **Existing Projects into Workspace**.
3. Chọn **Select root directory**.
4. Chọn thư mục chứa `.project`, `.classpath`, `src` và `lib`.
5. Đánh dấu dự án `POS-Cua-Hang-Tien-Loi`.
6. Không chọn **Copy projects into workspace** nếu đang mở trực tiếp thư mục đã clone.
7. Chọn **Finish** và chờ Eclipse build xong.

Trong Project Explorer cần thấy:

- `JRE System Library [JavaSE-1.8]`.
- `mariadb-java-client-3.5.7.jar` trong **Referenced Libraries**.

Nếu thiếu JAR MariaDB, nhấp phải dự án, chọn **Properties > Java Build Path > Libraries > Classpath > Add JARs...**, rồi chọn file JAR trong thư mục `lib`.

## Chạy chương trình

1. Mở `src/graphicUI/PosApplication.java`.
2. Nhấp phải trong file và chọn **Run As > Java Application**.
3. Cửa sổ `POS Cửa Hàng Tiện Lợi` sẽ xuất hiện.

Trên Linux cũng có thể chạy từ terminal:

```bash
./dev run
```

## MariaDB dùng cho dự án

Trên Linux, khởi động database bằng:

```bash
./dev db-up
```

Cấu hình cục bộ mặc định:

```text
Địa chỉ: 127.0.0.1
Cổng: 3307
Database: cua_hang_tien_loi
Tài khoản: cua_hang_app
Mật khẩu: cua_hang_dev
```

Tài khoản dữ liệu mẫu chỉ dành cho môi trường phát triển:

```text
Quản lý: quanly / MatKhau123
Thu ngân: thungan / MatKhau123
```

`compose.yaml` tạo MariaDB với database `cua_hang_tien_loi`; ứng dụng chỉ nhận đúng ba biến `POS_DB_URL`, `POS_DB_USER` và `POS_DB_PASSWORD`. `./dev wipe` xóa volume của dự án, nên lần `./dev db-up` hoặc `./dev smoke` tiếp theo sẽ nạp lại `sql/Script.sql`.

Để kiểm tra kết nối trong Eclipse, mở **Run > Run Configurations...**, chọn cấu hình `PosApplication` và thêm:

- Program argument: `--db-smoke`
- `POS_DB_URL`: `jdbc:mariadb://127.0.0.1:3307/cua_hang_tien_loi`
- `POS_DB_USER`: `cua_hang_app`
- `POS_DB_PASSWORD`: `cua_hang_dev`

Các thông tin trên chỉ dùng để phát triển cục bộ. Không đưa mật khẩu hoặc thông tin kết nối thật vào Git.

## Nếu muốn dùng SQL Server

Phần này chỉ dành cho người muốn tự chạy thử dự án với SQL Server. Dự án chính và `sql/Script.sql` vẫn dùng MariaDB.

Không thể chỉ đổi địa chỉ kết nối vì MariaDB và SQL Server có khác biệt về driver, kiểu dữ liệu và câu lệnh tạo bảng. Muốn dùng SQL Server cần:

1. Tải [Microsoft JDBC Driver for SQL Server](https://learn.microsoft.com/sql/connect/jdbc/download-microsoft-jdbc-driver-for-sql-server) và lấy file JAR dành cho Java 8, có tên kết thúc bằng `.jre8.jar`.
2. Đặt file JAR trong `lib` và thêm nó vào **Java Build Path** của Eclipse.
3. Dùng URL kết nối SQL Server, ví dụ:

   ```text
   jdbc:sqlserver://localhost:1433;databaseName=cua_hang_tien_loi;encrypt=true;trustServerCertificate=true
   ```

4. Tạo một bản script riêng theo cú pháp SQL Server. Không ghi đè `sql/Script.sql` của MariaDB.
5. Giữ nguyên ba biến `POS_DB_URL`, `POS_DB_USER` và `POS_DB_PASSWORD` khi tạo Run Configuration.

`trustServerCertificate=true` chỉ phù hợp khi thử trên máy cá nhân với chứng chỉ tự ký. Không dùng tùy chọn này cho máy chủ thật.

`DBConnection.java` hiện đọc URL, tài khoản và mật khẩu từ biến môi trường, nên không cần sửa. Tuy nhiên, các lớp `dao` có câu lệnh riêng của MariaDB vẫn có thể cần điều chỉnh khi chạy với SQL Server.

## Tài liệu cần nộp

1. Thu thập và phân tích yêu cầu.
2. Sơ đồ lớp và mô tả các ràng buộc.
3. Sơ đồ cơ sở dữ liệu và mô tả các ràng buộc.
4. Ảnh và mô tả các màn hình chính của chương trình.

Tài liệu phải ghi rõ MariaDB được dùng thay SQL Server theo chấp thuận của giảng viên.

Đặc tả chi tiết Phần 1–3 nằm tại [docs/DAC_TA_HE_THONG.md](docs/DAC_TA_HE_THONG.md).

## Làm việc với GitHub

- Không làm tính năng trực tiếp trên nhánh `main`.
- Dùng nhánh `feature/`, `fix/` hoặc `docs/` phù hợp với thay đổi.
- Commit ngắn gọn và đúng nội dung.
- Push nhánh lên GitHub, tạo Pull Request và chờ kiểm tra trước khi merge.

Ví dụ:

```bash
git switch main
git pull origin main
git switch -c feature/quan-ly-san-pham
```

## Lỗi thường gặp

### Eclipse không nhận dự án

Kiểm tra thư mục được chọn có chứa `.project` và `.classpath`. Không chọn thư mục `src` làm thư mục gốc.

### Có dấu X đỏ sau khi import

Chọn **Project > Clean...**, sau đó kiểm tra lại Java 8 và file JAR trong **Java Build Path**.

### Không tìm thấy `graphicUI.PosApplication`

Nhấp phải thư mục `src` và chọn **Build Path > Use as Source Folder**.

### Chữ tiếng Việt bị lỗi

Nhấp phải dự án, chọn **Properties > Resource**, rồi đặt **Text file encoding** thành `UTF-8`.

### Không kết nối được database

Kiểm tra database đang chạy, cổng kết nối, tên database, tài khoản, mật khẩu và driver JDBC trong **Java Build Path**.
