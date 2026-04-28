# Spring Beans, Application Context và Annotations

## **Bean và ApplicationContext**

Trong Spring, Bean là các đối tượng (POJO) được IoC Container quản lý → Spring sẽ khởi tạo, thiết lập phụ thuộc và quản lý vòng đời của chúng. Mọi bean và quan hệ phụ thuộc của nó được đặt trong metadata cấu hình, được container đưa vào `BeanDefinition` . 

`ApplicationContext` làm một interface kế thừa từ `BeanFactory` , kèm thêm các functionality ở level enterprise. `BeanFactory` (legacy practice) hoạt động dựa trên nguyên lý Lazy Initialization (các Bean chỉ được khởi tạo và cấp phát bộ nhớ khi ứng dụng gọi `getBean()` ). Ngược lại `ApplicationContext` hoạt động theo nguyên lý Eager Inititalization (tất cả các Bean được định nghĩa là `singleton` đều được khởi tạo tại thời điểm contianer khởi động → làm chậm tốc độ load ban đầu nhưng mang lại lợi thế phát hiện lỗi sơn → giảm tình trạng các lỗi runtime khi hệ thống đang phục vụ người dùng).

## Spring Bean Lifecycle

Các bước chính:

1. Khởi tạo: Spring khởi tạo đối tượng bean (qua constructor noargs hoặc factory method)
2. Tiêm phụ thuộc: Container thiết lập các phụ thuộc cho bean, có thể qua constructor args, setter methods hoặc field (`@Autowired`). 
3. Aware interface: Nếu bean triển khai các interface Aware, container gọi các callback tương ứng (các interface này tiêm thông tin của container vào bean)
4. BeanPostProcessor (trước init): Trước khi gọi callback khởi tạo, BeanPostProcessor sẽ gọi `postProcessBeforeInitialization(bean, name)` , setup các logic nghiệp vụ
5. Init callbacks: Spring gọi các phương thức khởi tạo:
    - Các phương thức được đánh dấu `@PostConstruct`
    - Interface `InitializingBean.afterPropertiesSet()`
    - Init method
6. BeanPostProcessor (sau init): gọi `postProcessAfterInitialization(bean, name)` , có thể bọc raw obj vào một proxy aop, lưu vào container
7. Ready: Tất cả các bean Singleton thường đc khởi tạo sau bước này, Spring emit event `ContextRefreshedEvent` , ApplicationContextReady
8. Khi `ApplicationContext` đóng, Spring sẽ gọi callback huỷ các bean scope singleton, với bean prototype, Spring chỉ quản lý đến khâu tạo và init, sau đó bean trả về client, Spring không giữ ref, không gọi huỷ (việc dọn dẹp do client đảm nhận)

## **Stereotype annotations (Component Scanning)**

Khả năng quét + load components của Spring dựa trên một set meta-annotation gọi là Stereotypes. Bốn annotation bao gồm:  `@Component`, `@Controller`, `@Service`, và `@Repository`. `@Controller`, `@Service` và `@Repository` đều là các phiên bản specialized được xây dựng trực tiếp trên nền của `@Component`. Khi thực thi component scanning (thường bằng `@ComponentScan` hoặc nhờ cấu hình `@SpringBootApplication`), Spring sẽ quét các gói chỉ định, tìm các class gắn những annotation trên và tự động tạo bean definitions tương ứng. Tất cả bean này sẽ được quản lý như các bean bình thường trong container.

### `@Components`

là **stereotype generic** cho mọi Spring bean thông thường, được sử dụng cho các thực thể không thuộc tầng giao diện, dịch vụ hay CSDL. `@Component` chịu sự chi phối của `@Conditional`

`@Conditional` cho phép IoC Container đánh giá một bộ tiêu chí trước khi quyết định có khởi tạo và đưa bean vào context không:

- **`@ConditionalOnBean` / `@ConditionalOnMissingBean`**: Đánh giá dựa trên sự hiện diện hoặc vắng mặt của một kiểu bean khác trong registry, rất hữu ích để cung cấp các bean dự phòng (fallback beans).
- **`@ConditionalOnProperty`**: Quét các giá trị nạp từ `application.properties` (thông qua thuộc tính `name` và `havingValue`) để bật/tắt các module tại runtime.
- **`@ConditionalOnClass` / `@ConditionalOnMissingClass`**: Kiểm tra sự tồn tại của một class cụ thể trên classpath bằng cách sử dụng `ClassLoader` mà không làm crash ứng dụng nếu class đó không tồn tại.

Không thể dùng các điều kiện kiểu `@ConditionalOnBean` ngay trên `@ComponentScan`, vì lúc Spring đang scan thì các bean vẫn chưa được tạo, nên chưa biết bean nào có hay không.

### `@Service`

`@Service` về mặt kỹ thuật không khác gì `@Component`: nếu thay thế qua lại, ứng dụng vẫn chạy bình thường và cơ chế dependency injection không bị ảnh hưởng vì Spring không có xử lý đặc biệt riêng cho annotation này. Tuy nhiên, giá trị chính của `@Service` nằm ở ý nghĩa kiến trúc. Nó được dùng để đánh dấu tầng nghiệp vụ (business layer), nơi chứa các logic xử lý chính của hệ thống như áp dụng quy tắc kinh doanh, phối hợp nhiều repository và thực hiện các tác vụ phức tạp. Nhờ việc phân định rõ ràng vai trò này, code trở nên dễ đọc và dễ hiểu hơn. Đồng thời, các lớp được gắn `@Service` thường trở thành mục tiêu lý tưởng cho AOP, vì đây là nơi cần áp dụng các chức năng cắt ngang như quản lý transaction (`@Transactional`), logging hoặc đo hiệu năng.

### `@Repository`

`@Repository` không chỉ là một annotation để đánh dấu như `@Service`, mà còn thay đổi hành vi runtime của bean. Nó dùng để chỉ các lớp làm việc với dữ liệu (DAO), như truy vấn, lưu trữ hoặc tìm kiếm trong cơ sở dữ liệu. Vấn đề là mỗi công nghệ database (JDBC, JPA, Hibernate…) lại ném ra các loại exception khác nhau, thường là checked exception như `SQLException`. Nếu để các exception này nổi lên tầng `@Service`, code nghiệp vụ sẽ bị phụ thuộc vào công nghệ cụ thể và phải xử lý try-catch rất rườm rà.

Spring giải quyết bằng cơ chế **dịch ngoại lệ tự động**. Khi một class được gắn `@Repository`, Spring sẽ tự động tạo proxy bao quanh nó. Proxy này có nhiệm vụ chặn các exception từ database, rồi chuyển chúng thành một hệ exception chung của Spring là `DataAccessException` (unchecked). Ví dụ, một lỗi vi phạm ràng buộc dữ liệu trong Hibernate sẽ được chuyển thành `DataIntegrityViolationException`. Nhờ vậy, tầng `@Service` chỉ cần xử lý một kiểu exception thống nhất, không cần biết bên dưới đang dùng JDBC, JPA hay Hibernate, giúp code sạch hơn và tách biệt công nghệ tốt hơn.

### `@Controller/@RestController`

`@Controller` là thành phần trung tâm của tầng giao diện trong Spring Web MVC, có nhiệm vụ nhận HTTP request, xử lý và trả về view (như HTML, JSP). Trong các hệ thống API hiện đại, `@RestController` là phiên bản mở rộng của `@Controller`, kết hợp thêm `@ResponseBody`, nghĩa là dữ liệu trả về sẽ không qua view mà được chuyển thẳng thành JSON/XML thông qua các bộ chuyển đổi như Jackson.

Việc kết nối giữa request từ client và method xử lý trong controller được Spring quản lý bởi `DispatcherServlet` và một cơ chế định tuyến gọi là `RequestMappingHandlerMapping`. Quá trình này có hai giai đoạn. Ở giai đoạn khởi động, Spring quét toàn bộ các bean, tìm những class có `@Controller` hoặc `@RestController`, sau đó dùng reflection để đọc các annotation như `@RequestMapping`, `@GetMapping`, `@PostMapping` trên từng method. Từ đó, nó tạo ra thông tin mapping (gồm URL, HTTP method, header, kiểu dữ liệu…) và lưu lại trong một cấu trúc nội bộ để tra cứu nhanh.

Đến lúc runtime, khi có HTTP request gửi đến, `DispatcherServlet` sẽ lấy thông tin request (URL, method…) rồi đối chiếu với dữ liệu đã lưu. Nhờ vậy, Spring nhanh chóng xác định đúng method cần gọi. Sau đó, việc bind dữ liệu đầu vào như `@RequestBody`, `@PathVariable` và thực thi method sẽ được xử lý tiếp bởi `RequestMappingHandlerAdapter`.

## **`@Configuration, @Bean, @Autowired, @Value`**

Trong Spring Framework, cấu hình bằng Java xoay quanh hai annotation chính là `@Configuration` và `@Bean`. Một class được đánh dấu `@Configuration` đóng vai trò như nơi định nghĩa cấu hình, trong đó các phương thức gắn `@Bean` sẽ khai báo cách tạo và quản lý các đối tượng (bean) trong IoC container. Khi ứng dụng khởi động, Spring đọc các phương thức này để tạo bean và đưa vào context. Điểm quan trọng là khi `@Bean` nằm trong `@Configuration`, Spring sẽ tạo proxy cho class đó để đảm bảo mỗi bean chỉ được tạo một lần đúng theo lifecycle (ví dụ singleton), kể cả khi các method gọi lẫn nhau.

Bên cạnh việc tạo bean, Spring cung cấp cơ chế tiêm phụ thuộc thông qua `@Autowired`. Annotation này cho phép container tự động inject các bean cần thiết vào constructor, setter hoặc field dựa trên kiểu dữ liệu. Theo docs, nếu một class chỉ có một constructor thì không cần ghi `@Autowired`, Spring vẫn tự hiểu và inject dependency tương ứng. Ngoài ra, `@Autowired` có thể đánh dấu dependency là optional bằng `required=false`, giúp hệ thống linh hoạt hơn khi không tìm thấy bean phù hợp .

Một annotation khác thường đi kèm là `@Value`, dùng để inject giá trị từ file cấu hình (như `application.properties`) hoặc environment vào bean. Điều này cho phép tách cấu hình ra khỏi code, ví dụ inject URL, API key hoặc feature flag trực tiếp vào biến trong class mà không cần hard-code.

## Spring Web

`@RequestMapping` là annotation tổng quát trong Spring MVC dùng để ánh xạ (map) HTTP request vào các method xử lý trong controller. Nó có thể dùng cho mọi loại HTTP method như GET, POST, PUT, DELETE thông qua thuộc tính `method`, và có thể đặt ở cả cấp class (để định nghĩa base URL) lẫn cấp method. 

Trong khi đó, `@PostMapping` là phiên bản rút gọn chuyên dùng cho HTTP POST, giúp code ngắn gọn và dễ đọc hơn. Về bản chất, `@PostMapping("/users")` hoàn toàn tương đương với `@RequestMapping(value = "/users", method = RequestMethod.POST)`. 

Trong thực tế, các annotation chuyên biệt như `@GetMapping`, `@PostMapping` thường được ưu tiên sử dụng vì rõ ràng và ít boilerplate hơn, còn `@RequestMapping` sẽ hữu ích khi cần cấu hình linh hoạt hơn, ví dụ kết hợp nhiều điều kiện như header, consumes/produces hoặc nhiều HTTP method trong cùng một mapping.

`@ResponseBody` dùng để chỉ định rằng giá trị trả về của một method sẽ không được xử lý qua view (như JSP/HTML) mà được ghi trực tiếp vào HTTP response, thường dưới dạng JSON hoặc XML thông qua các `HttpMessageConverter`. Annotation này chính là nền tảng của `@RestController` (vì `@RestController` = `@Controller` + `@ResponseBody`)

`@RequestBody` dùng để ánh xạ (bind) phần body của HTTP request (thường là JSON) vào một object Java. Khi client gửi dữ liệu lên (ví dụ POST JSON), Spring sẽ dùng các converter như Jackson để deserialize JSON đó thành object tương ứng. 

`@PathVariable` dùng để lấy giá trị trực tiếp từ URL. Ví dụ với endpoint `/users/{id}`, thì `{id}` sẽ được bind vào parameter trong method thông qua `@PathVariable`, giúp xử lý các resource theo định danh.

`@Profile` quản lý cấu hình theo môi trường. Annotation này cho phép chỉ kích hoạt một bean hoặc một class cấu hình khi ứng dụng chạy dưới một profile cụ thể (ví dụ `dev`, `test`, `prod`). Nhờ đó, tacó thể định nghĩa các bean khác nhau cho từng môi trường, như dùng database local khi dev và database thật khi production, mà không cần thay đổi code.