# Data binding

**1. Data Binding (Chuyển đổi HTML Form sang Java Model)**

Quá trình Data Binding giúp tự động liên kết và chuyển đổi dữ liệu người dùng nhập từ giao diện HTML thành một đối tượng Java để xử lý tại Server. Các thành phần chính tham gia vào quá trình này bao gồm:  

- **Định nghĩa đối tượng Form (th:object)**: Trong thẻ `<form>` của Thymeleaf, thuộc tính `th:object` được sử dụng để xác định đối tượng sẽ ràng buộc với các trường của form. Đối tượng này có nhiệm vụ chứa dữ liệu để hiển thị lên màn hình hoặc thu thập dữ liệu người dùng gửi về Controller.
- **Liên kết trường dữ liệu (th:field)**: Thuộc tính `th:field="*{tên_thuộc_tính}"` là thành phần đóng vai trò binding giá trị nhập vào thẻ HTML (như input, select, textarea) với thuộc tính tương ứng của form-backing bean.
- **Nhận dữ liệu tại Controller**: Khi người dùng submit form, dữ liệu sẽ được đẩy lên máy chủ. Tại phương thức xử lý (thường dùng HTTP POST), Controller sẽ nhận bộ dữ liệu này thông qua tham số được đánh dấu bằng annotation `@ModelAttribute` để tiếp tục xử lý logic.

**2. Cấu hình Validate (Hibernate Validator / JSR-303 Annotations)**

Các annotation để quy định tính hợp lệ cho các thuộc tính trong class Form.  

- `@NotNull`: Báo lỗi khi giá trị là null.
- `@NotEmpty`: Báo lỗi khi giá trị null, chuỗi rỗng hoặc collection rỗng.
- `@NotBlank`: Báo lỗi khi giá trị null hoặc chuỗi có độ dài bằng 0 sau khi đã xóa khoảng trắng (trim).
- `@Min`, `@Max`: Kiểm tra giới hạn giá trị tối thiểu/tối đa.
- `@Pattern`: Kiểm tra dữ liệu nhập vào có khớp với biểu thức chính quy (regex) hay không.
- `@Length`, `@Size`: Kiểm tra độ dài của chuỗi hoặc số lượng phần tử của Collection.
- **Kích hoạt Validation tại Controller**:
    - Sử dụng annotation `@Valid` đặt ngay trước đối tượng tham số cần được xác thực.
    - Khai báo đối tượng `BindingResult` **ngay phía sau** đối tượng được validate để hứng các thông báo lỗi.
    - Sử dụng phương thức `bindingResult.hasErrors()` để kiểm tra. Nếu có lỗi, Controller thường sẽ trả người dùng về lại View chứa form để nhập lại.
    - **Hiển thị lỗi trên View**:
        - Dùng phương thức `th:if="${#fields.hasErrors('tên_trường')}"` để kiểm tra xem một trường cụ thể có bị vi phạm luật validate không.
        - Dùng thuộc tính `th:errors="*{tên_trường}"` để lấy và in ra giá trị message lỗi tương ứng từ `BindingResult`.