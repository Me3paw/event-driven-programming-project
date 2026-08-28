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
├── .github/
│   └── CODEOWNERS          Chủ sở hữu mã nguồn trên GitHub
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

## Quy trình làm việc với Git và GitHub

Không làm tính năng mới trực tiếp trên nhánh `main`. Mỗi tính năng hoặc thay đổi phải có một nhánh riêng, sau đó tạo Pull Request để được kiểm tra và hợp nhất vào `main`.

### Quy tắc đặt tên nhánh

Tên nhánh dùng chữ thường, không dấu, không khoảng trắng và nối các từ bằng dấu gạch ngang.

```text
feature/ten-tinh-nang
fix/ten-loi
docs/noi-dung-tai-lieu
```

Ví dụ:

```text
feature/quan-ly-san-pham
fix/loi-ket-noi-database
docs/cap-nhat-huong-dan-eclipse
```

Sử dụng `feature/` cho tính năng mới, `fix/` cho sửa lỗi và `docs/` cho thay đổi tài liệu.

### Tạo nhánh mới

Trước khi bắt đầu, chuyển về `main` và lấy phiên bản mới nhất:

```bash
git switch main
git pull origin main
```

Tạo nhánh mới từ `main`:

```bash
git switch -c feature/ten-tinh-nang
```

Thay `feature/ten-tinh-nang` bằng tên phù hợp với công việc đang thực hiện.

### Lưu thay đổi bằng commit

Kiểm tra các tập tin đã thay đổi:

```bash
git status
```

Thêm các tập tin cần lưu và tạo commit:

```bash
git add .
git commit -m "feat: them chuc nang quan ly san pham"
```

Nội dung commit cần ngắn gọn và mô tả đúng thay đổi. Có thể dùng tiền tố `feat:`, `fix:` hoặc `docs:` tương ứng với loại công việc.

### Đẩy nhánh lên GitHub

Lần đầu đẩy một nhánh mới:

```bash
git push -u origin feature/ten-tinh-nang
```

Những lần tiếp theo trên cùng nhánh chỉ cần:

```bash
git push
```

Không dùng `git push origin main` cho công việc phát triển tính năng.

### Tạo Pull Request

1. Mở repository trên GitHub sau khi đã push nhánh.
2. Chọn **Compare & pull request**. Nếu nút này không xuất hiện, mở thẻ **Pull requests** và chọn **New pull request**.
3. Chọn **base: main**.
4. Chọn **compare:** nhánh vừa push, ví dụ `feature/quan-ly-san-pham`.
5. Đặt tiêu đề ngắn gọn và ghi rõ nội dung đã thay đổi trong phần mô tả.
6. Kiểm tra lại danh sách tập tin trong thẻ **Files changed**.
7. Chọn **Create pull request**.
8. Chờ chủ sở hữu mã nguồn `@Me3paw` kiểm tra và chấp thuận.
9. Chỉ merge Pull Request vào `main` sau khi đã xử lý các yêu cầu sửa đổi.
10. Sau khi merge thành công, có thể xóa nhánh trên GitHub.

### Cập nhật máy cá nhân sau khi merge

```bash
git switch main
git pull origin main
git branch -d feature/ten-tinh-nang
```

Lệnh cuối chỉ xóa nhánh ở máy cá nhân sau khi nhánh đã được merge.

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
