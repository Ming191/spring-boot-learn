# Tổng quan về Spring

## 1. Spring là gì?

Trước năm 2003, Dev phải sử dụng Java EE để xây dựng webapp. Trước khi viết logic nghiệp vụ, dev phải mất thời gian để configure hệ thống thủ công, mất nhiều thời gian, kém hiệu quá.
-> Năm 2003, Spring Framework ra đời để giải quyết vấn đề cấu hình rườm rà của Java EE

## 2. Các thành phần của Spring Framework
- Core container: Gồm 4 module:
    - **Core & Beans:** Cung cấp nền tảng cho IoC và DI -> Giúp quản lý và khởi tạo Beans tự động, giảm coupling giữa các component.
    - **Context:** Xây dựng dựa trên Core & Beans -> cung cấp khả năng truy cập các đối tượng theo framework. Với Spring Context, ta không cần sử dụng `new` để khởi tạo đối tượng mà Context sẽ tự động đọc cấu hình, tạo các đối tượng cần thiết và lưu trữ vào IoC Container. Khi cần lấy đối tượng thông qua DI, Spring Context sẽ tự động tìm  và trả về đối tượng đã cấu hình sẵn. Dev không cần quan tâm đối tượng đó được khởi tạo như thế nào.
    - **SpEL:** SpEL có thể coi như một công cụ tính toán và gán giá trị cho đối tượng tại runtime. Giả sử ta đang có đối tượng `ComponentA` có cấu hình địa chỉ IP. Khi khởi tạo `ComponentB`, ta có thể để SpEL vào `ComponentA` để lấy địa chỉ ip đấy. VD: `@Value("#{heThongA.ipAddress}")`, lấy thông tin hệ điều hành: `@Value("#{systemProperties['os.name']}")`, gọi method `@Value("#{chuoiNgauNhien.toUpperCase()}")`
- AOP:
    - **Module `spring-aop`:** cung cấp các tính năng AOP cơ bản -> Khi yêu cầu Spring apply một Aspect (vd ghi log trước khi chạy hàm `A()`, tại runtime, `spring-aop` tạo ra một wrapper bọc lấy `A()`, ứng dụng sẽ gọi vào wrapper thay vì `A()`, thực hiện logging rồi mới xử lý logic. Đặc điểm là nhẹ, dễ cấu hình, chỉ can thiệp vào các method exec, chỉ hoạt động với Spring Beans.
    - **Module `spring-aspects`:** cung cấp kết nối giữa Spring với framework AspectJ. AspectJ không tạo wrapper như `spring-aop` mà nhúng thẳng các code phụ trợ vào file bytecode -> có thể apply AOP lên bất kỳ đối tượng Java nào, không nhất thiết phải là Spring Bean
    - **Module `spring-instrument`:** Khi JVM nạp các file .class từ ổ cứng lên bộ nhớ để chuẩn bị chạy, nó cung cấp công cụ cần thiết để AspectJ strument cung cấp một lớp trung gian để chặn và biến đổi cấu trúc bytecode của class đó ngay trước khi nó hoạt động. Nó cung cấp công cụ cần thiết để AspectJ thực hiện weaving tại load-time, hỗ trợ các framework ORM theo dõi các thuộc tính của Entity
- Data Access:
    - **Module `JDBC`**: Tự động hóa các bước mở kết nối, viết query, lấy dữ liệu, catch exception, đóng kết nối
    - **Module `ORM`**: mapping dữ liệu trong `Table` -> Spring Bean
    - **Module `Transaction`**: Đảm bảo một giao dịch có tính toàn vẹn, hoặc là thành công, hoặc là thất bại khi thất bại thì sẽ thực hiện cơ chế roll back.
    - **Module `JMS, OXM`**: JMS giúp các dịch vụ khác nhau có thể gửi message an toàn (gửi email sau khi đăng ký thành công), OXM mapping từ đối tượng -> XML.
- Web:
    - **Module `Web`**: Cung cấp các tính năng tích hợp web cơ bản như khởi tạo IoC Container thông qua Servlet Listeners. Hỗ trợ xử lý file multipart, quản lý session, và đơn giản hóa việc thao tác với đối tượng HTTP.
    - **Module `Servlet`**: Sử dụng DispatcherServlet làm trung tâm để tiếp nhận, phân loại và điều phối các yêu cầu HTTP đến đúng các Controller xử lý.
    - **Module `WebSocket`**: Cho phép thiết lập kết nối full-duplex giữa client - server qua một kênh TCP duy nhất.
    - **Module `Portlet` (Legacy):** Tương tự Servlet nhưng được modify để tương thích với tiêu chuẩn Portlet API
- Testing

## Spring Boot

Với Spring Framework, lập trình viên phải tự tìm và thêm từng thư viện (database, web services, testing) một cách thủ công. Spring Boot gom nhóm các thư viện thường đi chung với nhau thành các gói "starter". Chúng đi kèm với các cấu hình mặc định (default configurations) để sử dụng ngay.
**Ví dụ:**
- `spring-boot-starter-web`: Tự động kéo về toàn bộ các thư viện cần thiết để xây dựng một ứng dụng Web RESTful.
- `spring-boot-starter-test`: Gom nhóm tất cả các thư viện cần thiết cho việc viết Unit Test và Integration Test.

Trong Spring Framework truyền thống, ta phải tự cài đặt một web server bên ngoài (như Tomcat), cấu hình nó, rồi mới deploy ứng dụng lên đó. Spring Boot nhúng sẵn web server vào bên trong chính ứng dụng.

Spring Boot cho phép tách biệt phần cấu hình ra khỏi mã nguồn -> có thể chạy cùng một ứng dụng trên nhiều môi trường khác nhau (Dev, Test, Production) chỉ với một mã nguồn duy nhất.

Auto Configuration là cơ chế Spring Boot tự động áp dụng các cấu hình phù hợp dựa trên dependency có trong classpath và các điều kiện runtime. Spring Boot nạp các auto-configuration classes được khai báo sẵn, rồi dùng các annotation điều kiện như:
- @ConditionalOnClass: chỉ áp dụng nếu class cần thiết có trên classpath
- @ConditionalOnMissingBean: chỉ tạo bean nếu chưa có bean cùng loại
- @ConditionalOnProperty: chỉ áp dụng khi một property thỏa điều kiện
## Dependency Injection

Dependency Injection (DI) là một pattern giúp loại bỏ sự phụ thuộc cứng nhắc giữa các thành phần trong mã nguồn. Thay vì một lớp tự khởi tạo các đối tượng mà nó cần, các đối tượng đó sẽ được inject từ bên ngoài vào -> Giúp source code đạt được loose-coupling.

Các dạng DI phổ biến: 
- Constructor Injection
- Setter Injection
- Fields/properties
- Interface Injection


## IoC & IoC Container

IoC tức là đảo ngược luồng điều khiển, để framework quản lý vòng đời + sự phụ thuộc của đối tượng thay vì tự khởi tạo bằng `new`. DI là một cách cụ thể để thực hiện IoC.

Khi ứng dụng sử dụng DI, một bài toán đặt ra là làm sao để biết lớp nào cần phụ thuộc vào lớp nào để tự động khởi tạo. Do đó, người ta tạo ra IoC Container. Nó có nhiệm vụ quản lý, cung cấp tài nguyên cho các thành phần dựa vào thông tin từ file cấu hình, giúp việc quản lý tập trung và đơn giản hơn rất nhiều so với để từng thành phần tự xử lý.