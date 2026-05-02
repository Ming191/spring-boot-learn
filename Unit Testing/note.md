# Unit Testing

## Unit test

**Kiểm thử Đơn vị (Unit Test)** là cấp độ kiểm thử phần mềm ở mức nhỏ nhất, tập trung vào việc xác minh tính chính xác của một đơn vị  như một phương thức, một lớp hoặc một module. Các test case này do chính lập trình viên viết ngay trong giai đoạn phát triển, sử dụng các kỹ thuật giả lập (Test Double như mock, stub) để loại bỏ các phụ thuộc bên ngoài và cô lập hoàn toàn đoạn mã cần kiểm tra. 

Một Unit Test tiêu chuẩn phải có tính tự động hóa cao với kết quả Pass/Fail rõ ràng, đồng thời đáp ứng được ba tiêu chí: tốc độ chạy cực nhanh, độc lập với môi trường bên ngoài (DB, Network) và luôn cho kết quả lặp lại nhất quán. Dù độ phủ là một chỉ số tham khảo phổ biến, nhưng chất lượng của phần mềm vẫn phụ thuộc chủ yếu vào các kịch bản kiểm thử thay vì chỉ chạy theo những con số (ví dụ >95%).

Việc áp dụng Unit Test mang lại những lợi ích to lớn cho vòng đời phát triển phần mềm. Nó giúp phát hiện và sửa lỗi từ rất sớm, làm giảm thiểu đáng kể chi phí khắc phục so với các giai đoạn sau. Đồng thời, Unit Test đóng vai trò như một bộ tài liệu động và một lưới an toàn, giúp lập trình viên refactor mà không lo phá hỏng tính năng cũ. Hơn thế nữa, thói quen viết test còn để lập trình viên thiết kế hệ thống theo hướng low coupling và tăng tính module hóa.

## Test-Driven Development - TDD

**Test-Driven Development (TDD)** là một methodology, trong đó lập trình viên bị bắt buộc phải **viết kịch bản kiểm thử (test) trước** khi tiến hành viết mã nguồn cho tính năng. Mục tiêu của phương pháp này là chỉ viết lượng mã vừa đủ để vượt qua bài test đã định nghĩa.

Quy trình TDD vận hành dựa trên một vòng lặp liên tục gồm 3 bước cốt lõi:

1. **RED (Viết Test mới):** Định nghĩa yêu cầu tính năng thông qua một bài test. Khi chạy thử, bài test này chắc chắn sẽ thất bại (báo lỗi màu đỏ) vì logic thực thi tương ứng chưa tồn tại.
2. **GREEN (Viết mã tối thiểu):** Viết đoạn mã sản phẩm đơn giản nhất, vừa đủ để vượt qua bài test vừa viết. Mục tiêu ở bước này không phải là viết code hoàn hảo, mà là làm cho test chuyển sang trạng thái thành công (màu xanh lá) càng nhanh càng tốt.
3. **REFACTOR (Tái cấu trúc):** Sau khi bài test đã *pass*, tiến hành dọn dẹp và tối ưu hóa đoạn mã vừa viết (cải thiện cấu trúc, hiệu năng, áp dụng design pattern) mà không làm thay đổi hành vi của hệ thống (đảm bảo test vẫn *pass*).

Sau khi hoàn tất, quy trình lặp lại từ Bước 1 cho các tính năng hoặc yêu cầu tiếp theo.

**Ưu điểm:**

- Giúp mã nguồn rõ ràng, tập trung giải quyết đúng yêu cầu thực tế, tránh lãng phí công sức cho các tính năng thừa.
- Mỗi bài test đóng vai trò như một bộ "tài liệu động" mô tả chính xác cách hệ thống hoạt động ở mọi thời điểm.
- Tạo ra một hệ thống kiểm chứng tự động, giúp phát hiện bug sớm và mang lại sự tự tin tuyệt đối khi tái cấu trúc mã (refactor).

**Nhược điểm:**

- Đòi hỏi thời gian và nỗ lực lớn hơn ở giai đoạn đầu do phải liên tục viết kịch bản test trước khi code.
- Nếu lạm dụng việc kiểm thử chi tiết nội bộ (anti-pattern) hoặc viết test quá gắn chặt với cách triển khai, việc bảo trì test sẽ trở thành gánh nặng mỗi khi mã nguồn thay đổi.

## Tổng quan về JUnit 5

Ra mắt vào năm 2017, **JUnit 5** là một bước nhảy vọt so với phiên bản tiền nhiệm (JUnit 4 - 2006). Thay vì monolithic, JUnit 5 được thiết kế lại hoàn toàn với kiến trúc module hóa và tận dụng tối đa các tính năng của Java 8+ (như Lambdas và Streams).

Kiến trúc của JUnit 5 bao gồm 3 thành phần chính:

- **JUnit Platform:** Nền tảng cơ sở để khởi chạy các framework kiểm thử trên JVM.
- **JUnit Jupiter:** Thư viện API và mô hình extension mới, dùng để viết các test hiện đại.
- **JUnit Vintage:** Engine hỗ trợ tương thích ngược, cho phép chạy các đoạn mã test cũ viết bằng JUnit 3 và JUnit 4.

|  | JUnit 4 | JUnit 5 |
| --- | --- | --- |
| Kiến trúc | Đơn khối (monolithic), hỗ trợ Java ≤7 cơ bản | Module hóa (Platform, Jupiter, Vintage), hỗ trợ Java 8+ (lambdas) |
| Annotation | `@Before`, `@After`, `@BeforeClass`, `@AfterClass`, `@Test` | `@BeforeEach`, `@AfterEach`, `@BeforeAll`, `@AfterAll`, `@Test`, `@DisplayName` (mới) |
| Mở rộng (Extension) | Giới hạn (dùng Runner, Rule) | Mạnh mẽ (`@ExtendWith`, có thể định nghĩa resolver, listener, ...) |
| Test tham số | Cần `@RunWith(Parameterized.class)` (phức tạp) | Tích hợp sẵn `@ParameterizedTest`, `@ValueSource`, `@CsvSource`, ... |
| Kiểm thử có điều kiện | Rất hạn chế (không built-in) | Hỗ trợ `@EnabledIf`, `@DisabledOnOs/JavaVersion`, … |
| Test động | Không hỗ trợ (chỉ static) | Hỗ trợ `@TestFactory` tạo test động (runtime) |
| Assertion & Điểm dừng | Cơ bản (`assertEquals`, `assertTrue`, ... từ `org.junit.Assert`) | Mở rộng (`assertAll`, `assertThrows`, `assertTimeout`, ... từ `org.junit.jupiter.api.Assertions`) |
| Nhóm và lọc test | Chưa hoàn thiện (dùng Category, Group) | Hỗ trợ `@Tag` để nhóm và lọc test |
| Tương thích ngược | Hỗ trợ JUnit 4 qua Vintage engine | Không chạy mã JUnit 4 trực tiếp (phải migrate) |

### Framework Mockito

**Mockito** là một framework phổ biến trong hệ sinh thái Java, chuyên dùng để tạo và quản lý các đối tượng giả lập (**mock object**) trong Unit Test. Mục tiêu của Mockito là giúp cô lập lớp đang được kiểm thử bằng cách thay thế các phụ thuộc thực tế bằng các bản sao giả lập, cho phép lập trình viên kiểm soát hoàn toàn dữ liệu trả về mà không cần khởi tạo môi trường thực (như Database hay API).

**@Mock** (hay `Mockito.mock()`): tạo mock object *hoàn toàn giả lập*. Tất cả phương thức trả về kiểu dữ liệu trả về mặc định (null, 0, false, …) trừ khi đã stub. Ví dụ:

```java
@ExtendWith(MockitoExtension.class)
class MockExample {
    @Mock List<String> mockList;
    @Test
    void testMockBehavior() {
        mockList.add("hello");
        Mockito.verify(mockList).add("hello");// Xác nhận add("hello") được gọi
        assertEquals(0, mockList.size());// Vì chưa stub size(), kết quả mặc định là 0
        Mockito.when(mockList.get(0)).thenReturn("hi");
        assertEquals("hi", mockList.get(0));// Sau khi stub, trả về như thiết lập
    }
}
```

Như trên, `mockList.add("hello")` không thêm phần tử thực; `mockList.size()` trả 0 (mặc định).

**@Spy** (hay `Mockito.spy()`): tạo *spy object* bọc quanh một object thật. Các phương thức chưa stub sẽ thực thi bình thường trên object gốc, trong khi ta có thể stub được bất kỳ phương thức nào. Ví dụ:

```java
@ExtendWith(MockitoExtension.class)
class SpyExample {
    @Spy List<String> spyList = new ArrayList<>();
    @Test
    void testSpyBehavior() {
        spyList.add("test");
        assertEquals("test", spyList.get(0));    // Phương thức thật được gọi
        // Stubbing size() của spy:
        Mockito.doReturn(100).when(spyList).size();
        assertEquals(100, spyList.size());      // size() được giả lập
    }
}
```

Khi không stub, spy hoạt động như List thông thường (kết quả chính xác). Khi stub, nó trả theo kỳ vọng mà không gọi logic thật. Theo chia sẻ, điểm khác cơ bản: **Mock là object giả hoàn toàn**; **Spy là object thật được bọc thêm khả năng stub**

**When-Then**: Dùng `when(mock.method()).thenReturn(value)` hoặc `thenThrow(exception)` để định nghĩa trả về. Với spy hoặc phương thức void, dùng `Mockito.doReturn(x).when(spy).method()` hoặc `Mockito.doThrow(e).when(mock).methodVoid()` là an toàn hơn.

**verify**: Dùng `Mockito.verify(mock).method(args)` để kiểm tra xem một phương thức nào đó của mock có được gọi với tham số tương ứng hay không. Ví dụ:

```java
mockList.add("a");
verify(mockList).add("a");
```

**ArgumentCaptor**: Cho phép bắt giữ tham số mà mock nhận được để kiểm tra chi tiết. Dùng `@Captor ArgumentCaptor<T> captor;` rồi `verify(mock).add(captor.capture());` và so sánh `captor.getValue()` với giá trị mong đợi. Ví dụ:

```java
@Captor ArgumentCaptor<String> captor;
mockList.add("one");
verify(mockList).add(captor.capture());
assertEquals("one", captor.getValue());
```

**doReturn / doThrow**: Thường dùng với spy hoặc phương thức void. Ví dụ:

```java
Mockito.doThrow(new IllegalStateException("Error")).when(mockList).clear();
assertThrows(IllegalStateException.class, () -> mockList.clear());
```

**@InjectMocks**: Được dùng để tự động inject các mock vào đối tượng cần kiểm thử. Ví dụ, với class `MyDictionary` có `Map wordMap`, ta có:

```java
@ExtendWith(MockitoExtension.class)
class DictionaryTest {
    @Mock Map<String,String> mapMock;
    @InjectMocks MyDictionary dict;  // sẽ tạo MyDictionary và inject mapMock vào
    @Test
    void testGetMeaning() {
        when(mapMock.get("aWord")).thenReturn("aMeaning");
        assertEquals("aMeaning", dict.getMeaning("aWord"));
    }
}
```

Mockito sẽ tự thay thế dependency (được xác định bởi tên/kiểu biến) trong `dict` bằng các mock tương ứng

**InjectMocks với @Spy**: Lưu ý Mockito *không tự inject* các mock vào spy được đánh dấu `@InjectMocks`. Nếu muốn inject vào spy, cần làm thủ công (ví dụ, setter hoặc constructor). Khi dùng `@Spy MyDictionary spyDic` và `@InjectMocks`, test sẽ fail vì Mockito không hỗ trợ inject mock vào spy. Giải pháp là sửa `MyDictionary` có phương thức setter cho `wordMap`, rồi trong `@BeforeEach` gán thủ công:

```java
@BeforeEach void init() { spyDic.setWordMap(mapMock); }
```

## **Best Practices với Unit Test**

Một số **best practices**:

- **AAA Pattern:** Sử dụng rõ ràng các giai đoạn Arrange (thiết lập test data, stub), Act (gọi phương thức cần test), Assert (xác nhận kết quả) trong mỗi test.
- **Đặt tên rõ ràng:** Tên test nên diễn đạt hành vi cần kiểm thử (ví dụ `methodName_shouldReturnX_whenY`).
- **Một hành vi / một test:** Mỗi test chỉ kiểm tra một khía cạnh cụ thể, nên có 1–2 assert tập trung (hoặc dùng `assertAll` khi cần so sánh nhiều điều kiện liên quan).
- **Duy trì độc lập:** Các test không phụ thuộc lẫn nhau; không dựa vào thứ tự thực thi hoặc kết quả của test khác.
- **Setup/teardown sạch:** Sử dụng `@BeforeEach`/`@AfterEach` (JUnit5) để tạo/bỏ tài nguyên chung, tránh lặp code giữa các test.
- **Sử dụng Mock hợp lý:** Chỉ mock những thứ cần thiết, tránh mock (hoặc stub) logic phức tạp, giữ test đơn giản.
- **Không dùng Test như Code sản xuất:** Kiểm thử nhiều lần đòi hỏi code test phải rõ ràng và không chứa logics phức tạp.
- **Kiểm tra cả tình huống biên:** Đạt ít nhất kiểm tra các giá trị đầu vào biên hoặc sai (null, empty, âm, vượt giới hạn…) và xử lý ngoại lệ.
- **Bảo trì test:** Khi refactor mã, nếu thay đổi API thì cũng nên cập nhật test. Việc test tốt giúp tự tin refactor liên tục.
- **Tích hợp CI:** Đưa test vào pipeline CI/CD để mỗi commit đều chạy toàn bộ test suite, phát hiện sớm lỗi mới.

checklist khi viết unit test:

- Đảm bảo test **dễ đọc** và **dễ hiểu**.
- Test **chỉ một hành vi nhỏ**.
- Sử dụng Mock/Spy đúng **mục đích** (Mock để giả lập, Spy để ghi nhận hành vi).
- Không để **logic thực** trong test (trừ gọi hàm SUT).
- **Dọn dẹp** mock spy sau khi test (`@AfterEach` reset nếu cần).
- **Assert** không bị trống hay bỏ sót.