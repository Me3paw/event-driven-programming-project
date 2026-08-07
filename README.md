# POS Nature CRUD

Đây là bài tập Java Swing cơ bản mô phỏng giao diện quản lý bán hàng. Dự án được tạo dưới dạng **Eclipse Java Project thông thường**, không sử dụng Maven hoặc Gradle. Thư viện MariaDB được quản lý thủ công trong thư mục `lib`.

## Yêu cầu môi trường

Mọi thành viên sử dụng cùng cấu hình sau:

- Eclipse IDE có hỗ trợ Java.
- JDK 8 được cấu hình trong Eclipse với tên môi trường `JavaSE-1.8`.
- MariaDB Java Client `3.5.7` đã có sẵn tại `lib/mariadb-java-client-3.5.7.jar`.
- Mã nguồn dùng UTF-8.

Không cần cài Maven hoặc Gradle để mở và chạy dự án.

## Cấu trúc thư mục

```text
.
├── .settings/             Cấu hình Java 8 và UTF-8 của Eclipse
├── lib/                   Thư viện JAR được thêm thủ công
├── sql/                   Tập tin SQL của dự án
├── src/
│   ├── components/        Thành phần giao diện dùng chung
│   ├── connectDB/         Kết nối và kiểm tra MariaDB
│   ├── dao/               Lớp truy cập dữ liệu
│   ├── entity/            Lớp biểu diễn dữ liệu
│   └── graphicUI/         Màn hình và lớp chạy chương trình
├── .classpath             Đường dẫn mã nguồn, JRE và thư viện
├── .project               Thông tin Eclipse Project
└── README.md
```

Hiện tại `dao`, `entity` và `sql/Script.sql` chưa có chức năng nghiệp vụ vì đề bài chính thức chưa quy định cấu trúc cơ sở dữ liệu.

## Nhập dự án vào Eclipse

1. Mở Eclipse.
2. Chọn **File > Import...**.
3. Trong cửa sổ Import, mở nhóm **General**.
4. Chọn **Existing Projects into Workspace**, sau đó bấm **Next**.
5. Chọn **Select root directory**.
6. Bấm **Browse...** và chọn đúng thư mục gốc của dự án, tức thư mục chứa `.project`, `.classpath`, `src` và `lib`.
7. Khi tên dự án `POS-Nature-CRUD` xuất hiện trong danh sách Projects, đánh dấu chọn dự án đó.
8. Không cần chọn **Copy projects into workspace** nếu đang mở trực tiếp thư mục đã tải hoặc đã clone.
9. Bấm **Finish**.
10. Chờ Eclipse hoàn tất quá trình build. Trong Project Explorer không được còn dấu X màu đỏ ở tên dự án.

## Kiểm tra Java 8

Sau khi import, trong Project Explorer phải thấy **JRE System Library [JavaSE-1.8]**.

Nếu Eclipse báo thiếu JRE:

1. Chọn **Window > Preferences > Java > Installed JREs**.
2. Bấm **Add... > Standard VM > Next**.
3. Tại **JRE home**, chọn thư mục cài đặt JDK 8.
4. Đặt tên dễ nhận biết, ví dụ `JDK 8`.
5. Bấm **Finish**, đánh dấu JDK 8 vừa thêm, sau đó bấm **Apply and Close**.
6. Nhấp phải dự án, chọn **Properties > Java Build Path > Libraries**.
7. Nếu JRE hiện tại không phải Java 8, xóa mục đó và chọn **Add Library... > JRE System Library > Execution environment > JavaSE-1.8**.
8. Bấm **Apply and Close**.

## Kiểm tra thư viện MariaDB

Trong Project Explorer phải thấy `lib/mariadb-java-client-3.5.7.jar` và thư viện này phải xuất hiện trong **Referenced Libraries**.

Nếu Eclipse báo thiếu thư viện:

1. Nhấp phải dự án và chọn **Properties**.
2. Chọn **Java Build Path > Libraries**.
3. Chọn **Classpath**, sau đó bấm **Add JARs...**.
4. Chọn `lib/mariadb-java-client-3.5.7.jar` trong dự án.
5. Bấm **Apply and Close**.
6. Chọn **Project > Clean...**, chọn dự án và bấm **Clean**.

## Chạy giao diện

1. Mở `src/graphicUI/PosApplication.java`.
2. Nhấp phải trong vùng mã nguồn.
3. Chọn **Run As > Java Application**.
4. Cửa sổ `POS Nature` sẽ xuất hiện.

Giao diện hiện có bốn mục:

- **Dashboard**: tổng quan dữ liệu mẫu.
- **Products**: danh sách và ô nhập sản phẩm mẫu.
- **Customers**: danh sách và ô nhập khách hàng mẫu.
- **Sales**: danh sách và ô nhập giao dịch mẫu.

Dữ liệu hiện tại chỉ nằm trong bộ nhớ. Các nút nhập liệu chỉ minh họa thao tác giao diện và không lưu xuống cơ sở dữ liệu.

## Cấu hình kiểm tra MariaDB

Giao diện có thể chạy mà không cần cơ sở dữ liệu. Chỉ thực hiện phần này khi cần kiểm tra kết nối MariaDB.

1. Chọn **Run > Run Configurations...**.
2. Mở **Java Application** và chọn cấu hình chạy `PosApplication`.
3. Tại thẻ **Arguments**, nhập `--db-smoke` vào ô **Program arguments**.
4. Mở thẻ **Environment** và thêm ba biến:

   - `POS_DB_URL`: ví dụ `jdbc:mariadb://127.0.0.1:3306/ten_co_so_du_lieu`
   - `POS_DB_USER`: tên tài khoản MariaDB
   - `POS_DB_PASSWORD`: mật khẩu MariaDB

5. Bấm **Run**.

Chương trình trả mã `0` khi câu lệnh kiểm tra `SELECT 1` thành công. Mã `2` cho biết thiếu biến môi trường, mã `3` là kết quả không hợp lệ và mã `4` là lỗi kết nối JDBC.

Sau khi kiểm tra, xóa `--db-smoke` khỏi Program arguments để chạy lại giao diện bình thường.

## Xử lý lỗi thường gặp

### Eclipse không nhận dự án

Kiểm tra lại thư mục được chọn có chứa `.project` và `.classpath`. Không chọn nhầm thư mục `src`.

### Có dấu X đỏ sau khi import

1. Chọn **Project > Clean...**.
2. Nhấp phải dự án và chọn **Refresh**.
3. Kiểm tra lại JRE phải là `JavaSE-1.8`.
4. Kiểm tra lại JAR MariaDB trong Java Build Path.

### Không tìm thấy lớp `graphicUI.PosApplication`

Kiểm tra `src` đã được Eclipse nhận là Source Folder. Nếu chưa, nhấp phải `src` và chọn **Build Path > Use as Source Folder**.

### Chữ tiếng Việt hiển thị sai

Nhấp phải dự án, chọn **Properties > Resource**, đặt **Text file encoding** thành `UTF-8`, sau đó bấm **Apply and Close**.

### Không kết nối được MariaDB

Kiểm tra MariaDB đang chạy, tên cơ sở dữ liệu đúng, cổng kết nối đúng và ba biến môi trường đã được nhập chính xác trong Run Configuration.
