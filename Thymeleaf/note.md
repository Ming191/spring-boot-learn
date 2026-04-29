# Thymeleaf
## Spring Boot SSR
Server-Side Rendering là một phương pháp render UI mà trong đó source code HTML được tạo ra tại máy chủ trước khi được gửi tới trình duyệt của client. SSR cho phép trình duyệt hiển thị nội dung ngay lập tức khi nhận được phản hồi đầu tiên từ server.

Trong mô hình SSR, server chịu trách nhiệm xử lý các logic, truy vấn cơ sở dữ liệu và đưa dữ liệu vào template -> giảm thiểu gánh nặng tính toán tại phía client.

Trong Spring MVC, `DispatcherServlet` là thành phần triển khai design pattern Front Controller. Thành phần này intercept toàn bộ các HTTP Request đến ứng dụng và điều phối tới các thành phần xử lý tương ứng trong pipeline:
1. Tiếp nhận request: DispatcherServlet tiếp nhận đối tượng HttpServletRequest từ Servlet Container (ví dụ: Tomcat)
2.  Handler Mapping: DispatcherServlet duyệt qua các bean HandlerMapping (như RequestMappingHandlerMapping) để xác định HandlerExecutionChain. Quá trình này ánh xạ URI, HTTP Method, và các tham số của request tới một method cụ thể trong Controller.
3.  Thực thi Controller: Lời gọi hàm được ủy quyền cho HandlerAdapter (như RequestMappingHandlerAdapter). Tại đây, Controller thực thi business logic, tương tác với các layer Service/Repository.
4.  Trả về Model và View: Kết thúc xử lý, method trong Controller trả về đối tượng ModelAndView  và một đối tượng Model mang dữ liệu payload.
5.  View Resolution: DispatcherServlet chuyển Logical View Name cho các bean ViewResolver để phân giải thành một đối tượng View thực tế.
6. Rendering: Đối tượng View thực thi phương thức render(). Giai đoạn này sử dụng dữ liệu từ Model để nội suy vào template, sinh ra  HTML đầu ra.
7. Phản hồi: Chuỗi byte HTML cuối cùng được ghi vào output stream của HttpServletResponse và trả về client.

## Thymeleaf syntax
### Biểu thức cơ bản
Biểu thức biến ${...}: Đây là loại biểu thức được sử dụng nhiều nhất, cho phép truy xuất các thuộc tính từ đối tượng Model của Spring MVC hoặc các biến trong request, session scope. Ví dụ: ${user.name} sẽ gọi phương thức getName() trên đối tượng user được truyền từ Controller.

Biểu thức dấu hoa thị \*{...}: gọi là Selection Expressions. Loại biểu thức này đánh giá dữ liệu dựa trên một đối tượng đã được chọn trước đó thông qua thuộc tính th:object. Nó giúp giảm bớt sự lặp lại của tên đối tượng chính, đặc biệt hữu ích trong các Form nhập liệu phức tạp.

Biểu thức thông điệp #{...}: Được sử dụng để thực hiện quốc tế hóa (I18n). Nó tìm kiếm các giá trị văn bản trong các tệp tài nguyên (.properties) dựa trên Locale của người dùng hiện tại.

Biểu thức liên kết @{...}: Dùng để xử lý các đường dẫn (URLs) trong ứng dụng. Hệ thống sẽ tự động xử lý các đường dẫn tương đối và thêm tiền tố Context Path của ứng dụng, đảm bảo các liên kết luôn hoạt động chính xác bất kể vị trí triển khai của ứng dụng trên máy chủ.

Việc hiển thị dữ liệu từ Model lên UI được thực hiện thông qua hai att chính là`th:text`và `th:utext`.

`th:text`: Thực hiện "HTML Escaping" tự động. Nếu biến chứa các ký tự đặc biệt như <, >, chúng sẽ được chuyển đổi thành thực thể HTML (&lt;, &gt;), ngăn chặn trình duyệt thực thi như mã lệnh -> tránh XSS.

`th:utext` (Unescaped Text): Hiển thị nội dung nguyên bản mà không xử lý ký tự.

### Vòng lặp th:each và biến trạng thái

Thuộc tính th:each được sử dụng để lặp qua một tập hợp dữ liệu (như List, Array, Map). Cú pháp cơ bản yêu cầu một biến đại diện cho phần tử hiện tại và một biến nguồn dữ liệu từ Model.

Nếu không được khai báo rõ ràng, Thymeleaf sẽ tự động tạo ra một biến trạng thái có tên trùng với tên biến phần tử kèm theo hậu tố Stat.

### Các cấu trúc rẽ nhánh: th:if, th:unless, và th:switch

Thymeleaf cung cấp các thuộc tính rẽ nhánh tương tự như ngôn ngữ lập trình Java :
- `th:if`: Phần tử chứa thuộc tính này sẽ chỉ được render vào mã HTML cuối cùng nếu điều kiện được đánh giá là true. Một điểm độc đáo của Thymeleaf là "Logic linh hoạt": các giá trị như null, 0, false, hoặc chuỗi "false", "off", "no" đều được coi là false.
- `th:unless`: Hoạt động ngược lại với th:if, nó sẽ hiển thị phần tử nếu điều kiện là false. Điều này tương đương với việc viết `th:if="!condition"` nhưng mang lại sự rõ ràng hơn về mặt ngôn ngữ.
- `th:switch` và `th:case`: Cho phép xử lý nhiều trường hợp điều kiện khác nhau một cách có cấu trúc. Thuộc tính `th:case="*"` đóng vai trò là trường hợp mặc định (default case) khi không có điều kiện nào khác khớp.

### `th:class` và `th:classappend`

**`th:class`**
Thuộc tính `th:class` hoạt động dựa trên nguyên tắc replacement. Khi biểu thức bên trong `th:class` được đánh giá, kết quả trả về sẽ xóa sạch và ghi đè lên toàn bộ thuộc tính class tĩnh gốc của thẻ HTML đó.

**`th:classappend`**
Để khắc phục điểm yếu của việc ghi đè, Thymeleaf giới thiệu `th:classappend`. Giống như tên gọi, nó hoạt động dựa trên nguyên tắc concatenation. Thuộc tính này sẽ giữ nguyên vẹn các class tĩnh gốc đã được khai báo, và chỉ nối thêm các class mới vào phía sau khoảng trắng.