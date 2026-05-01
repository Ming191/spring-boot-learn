## Nguyên lý ORM

### Impedance Mismatch

**ORM** là kỹ thuật giúp ánh xạ giữa mô hình hướng đối tượng và mô hình dữ liệu quan hệ. Sự khác biệt về mặt kiến trúc giữa hai mô hình này tạo ra rào cản gọi là **Object/Relational Impedance Mismatch**.

- **Mô hình quan hệ (RDBMS):** Quản lý dữ liệu qua bảng, cột, và dùng khóa chính (Primary Key), khóa ngoại (Foreign Key) để định danh và liên kết.
- **Mô hình đối tượng (Java):** Quản lý qua đồ thị đối tượng (Object Graph), hỗ trợ tính kế thừa (Inheritance) và liên kết bằng tham chiếu (Composition/Association).

**Cách ORM giải quyết:**

- **Định danh:** Sử dụng `@Id` và `@GeneratedValue` cho khóa chính.
- **Ánh xạ quan hệ:** Sử dụng các annotation như `@OneToMany`, `@ManyToOne`, `@ManyToMany`.
- **Ánh xạ kế thừa:** Sử dụng `@Inheritance`, `@MappedSuperclass`, `@DiscriminatorColumn`.

### Fetching Strategies

Các quan hệ giữa các Entity mặc định được quản lý theo hai chiến lược tải dữ liệu: **LAZY** (Tải lười) và **EAGER** (Tải nóng).

| **Loại Quan Hệ** | **Mặc định trong JPA** | **Hành vi** |
| --- | --- | --- |
| `@ManyToOne`, `@OneToOne` | **EAGER** | Dữ liệu liên quan được truy vấn và nạp ngay lập tức cùng với Entity gốc. |
| `@OneToMany`, `@ManyToMany` | **LAZY** | Dữ liệu liên quan chỉ được truy vấn khi có lời gọi hàm `getter` tương ứng. |

**Vấn đề và Best Practice:**

- **Vấn đề N+1 Query:** EAGER fetching thường dẫn đến việc thực thi quá nhiều câu lệnh SQL không cần thiết (N+1 query) làm giảm hiệu năng hệ thống.
- **Best Practice:** Nên đặt tất cả các quan hệ thành `FetchType.LAZY`. Khi thực sự cần nạp dữ liệu liên quan, hãy chủ động sử dụng **`JOIN FETCH`** trong JPQL hoặc cấu hình **Entity Graph** tùy chỉnh

### Vòng đời Entity và Persistence Context

Một **EntityManager** (JPA) hoặc **Session** (Hibernate) đại diện cho một **Persistence Context (PC)** quản lý vòng đời của các Entity.

Các trạng thái của Entity:

- **Managed:** Entity đang được kiểm soát bởi Persistence Context (đã gọi `persist()` hoặc được lấy lên từ DB).
- **Detached:** Entity không còn bị PC theo dõi (khi EntityManager/Session bị đóng, hoặc gọi lệnh `clear()`). Thay đổi trên Entity lúc này sẽ không được tự động lưu.
- **Removed:** Entity được đánh dấu để xóa (sau khi gọi `remove()`), sẽ bị xóa khỏi DB ở lần flush tiếp theo.

Cơ chế Dirty Checking và Flush/Commit

- **Dirty Checking:** Khi một Entity ở trạng thái *Managed*, mọi thay đổi trên các thuộc tính của nó sẽ được Hibernate tự động theo dõi.
- **Flush Mechanism:** Trước khi transaction commit hoặc trước khi chạy một query ảnh hưởng đến dữ liệu (với `FlushMode.AUTO`), Hibernate sẽ tự động flush các thay đổi này xuống CSDL bằng các lệnh `INSERT/UPDATE/DELETE`.
- **Không cần gọi lại hàm `save()`** sau khi thay đổi dữ liệu của một Managed Entity. Hibernate sẽ tự tạo và thực thi lệnh `UPDATE` khi transaction kết thúc. Để tối ưu hóa việc so sánh dữ liệu, Hibernate có hỗ trợ tính năng *Enhanced Dirty Checking* (so sánh bytecode).

#### **Transactions**

JPA yêu cầu mọi thao tác thay đổi dữ liệu (Insert, Update, Delete) đều phải nằm trong một Transaction.

Tích hợp Spring Data JPA (`@Transactional`)

Spring framework hỗ trợ quản lý giao dịch thông qua annotation `@Transactional`. Nó sẽ tự động `commit()` nếu hàm chạy thành công, hoặc `rollback()` nếu có Exception xảy ra.

Cấu hình mặc định trong Spring Data JPA:

- **Các phương thức đọc** (như `findById`, `findAll`): Được cấu hình mặc định là `@Transactional(readOnly = true)` để tối ưu hiệu năng.
- **Các phương thức ghi** (như `save`, `delete`): Được áp dụng `@Transactional` thông thường.
- **Các Custom Query:** Các phương thức tùy chỉnh bằng tên hàm hoặc `@Query` (DML như `UPDATE`, `DELETE`) không mặc định có transaction. Bắt buộc phải thêm `@Transactional` và `@Modifying` trên đầu phương thức để đảm bảo tính nhất quán của giao dịch.

## Kiến trúc Spring Data JPA

Spring Data JPA cung cấp một abstraction layer là **Repository** giúp đơn giản hóa tối đa việc tương tác với JPA/Hibernate. Thay vì phải tự tiêm và quản lý `EntityManager` thủ công, nhà phát triển chỉ cần khai báo các Interface, Spring sẽ tự động sinh implementation tại runtime.

#### Repository Interfaces

| **Interface** | **Kế thừa từ** | **Chức năng chính & Đặc điểm** |
| --- | --- | --- |
| **`CrudRepository<T, ID>`** | (Root) | Cung cấp các thao tác CRUD cơ bản: `save()`, `findById()`, `findAll()`, `count()`, `delete()`, `existsById()`. |
| **`PagingAndSortingRepository<T, ID>`** | `CrudRepository` | Bổ sung khả năng phân trang và sắp xếp: `findAll(Pageable pageable)`, `findAll(Sort sort)`. Trả về đối tượng `Page<T>`. |
| **`JpaRepository<T, ID>`** | `PagingAndSortingRepository` | Tích hợp toàn bộ tính năng của 2 interface trên, bổ sung thêm các hàm đặc thù của JPA: `flush()`, `saveAllAndFlush()`, `deleteAllInBatch()`, `getReferenceById()` (trả về proxy/reference). |

#### Querying

Spring Data JPA hỗ trợ nhiều cách để linh hoạt truy xuất dữ liệu từ Database:

1. Query Derivation
- Spring Data có khả năng phân tích cú pháp tên của phương thức trong Interface để tự động dịch thành câu lệnh JPQL.
- Tách các từ khóa (And, Or, Between, OrderBy...) và đối chiếu với các thuộc tính của Entity. Hệ thống hỗ trợ duyệt cả các thuộc tính lồng nhau (nested properties).

```java
// SELECT u FROM User u WHERE u.email = ?1 AND u.lastname = ?2
List<User> findByEmailAndLastname(String email, String lastname);
```

2. Custom Query

Đối với các truy vấn phức tạp mà việc đặt tên hàm quá dài hoặc không thể biểu diễn được, ta sử dụng annotation `@Query`. Việc này giúp tách biệt logic truy vấn khỏi Entity, giữ cho Domain class gọn gàng.

```java
// 1. JPQL (Truy vấn trên Entity)
@Query("SELECT u FROM User u WHERE u.status = :status")
List<User> findByStatus(@Param("status") String status);
```

```java
// 2. Native SQL (Truy vấn thẳng xuống Database)
@Query(value = "SELECT * FROM users WHERE status = ?1", nativeQuery = true)
List<User> findByStatusNative(String status);
```

3. Pagination & Sorting

```java
// Định nghĩa trong Repository
Page<User> findByLastname(String lastname, Pageable pageable);
```

```java
// Gọi hàm (Lấy trang 0, mỗi trang 10 phần tử, sắp xếp theo tên)
Pageable pageable = PageRequest.of(0, 10, Sort.by("firstname").ascending());
Page<User> users = userRepository.findByLastname("Nguyen", pageable);
```

4. Custom Repository Implementation

Nếu cần viết các logic Repository quá phức tạp, có nhiều dynamic query không thể dùng `@Query`, có thể tự viết code implement.
Tạo một interface riêng (vd: `UserRepositoryCustom`), sau đó viết class implement có hậu tố `Impl` (vd: `UserRepositoryImpl`). Spring Data sẽ tự động nhận diện và gắn class implement này vào `UserRepository` chính.

### Entity và các Annotation trong JPA

Trong JPA, một **Entity** thực chất là một lớp Java cơ bản (POJO) được đánh dấu bằng annotation `@Entity`, đóng vai trò đại diện cho một bảng trong cơ sở dữ liệu.

#### Annotation cơ bản

**`@Entity` & `@Table`**: Gắn trên đầu class.

- `@Entity`: Bắt buộc phải có để JPA nhận diện đây là một thực thể. Mặc định tên bảng sẽ trùng với tên class.
- `@Table(name="...", schema="...")`: Dùng khi muốn chỉ định rõ tên bảng hoặc schema khác với tên class.

**`@Column`**: Gắn trên thuộc tính. Dùng để tùy chỉnh chi tiết cột như `name` (tên cột), `length` (độ dài), `nullable` (cho phép null không), `unique` (ràng buộc duy nhất). Nếu không có, mặc định tên cột là tên thuộc tính.

**`@Id` & `@GeneratedValue`**: Định nghĩa khóa chính. JPA bắt buộc mọi Entity phải có một `@Id`. `@GeneratedValue` dùng để xác định chiến lược sinh ID tự động.

| **Chiến lược (GenerationType)** | **Đặc điểm & Cơ chế hoạt động** |
| --- | --- |
| **`AUTO`** | (Mặc định). JPA/Hibernate tự động chọn chiến lược phù hợp nhất với loại Database đang dùng (VD: SEQUENCE cho Oracle/Postgres, IDENTITY cho MySQL). |
| **`IDENTITY`** | Dành cho các DB hỗ trợ cột tự tăng (như `AUTO_INCREMENT` của MySQL hay `IDENTITY` của SQL Server). Quá trình `INSERT` diễn ra ngay lập tức khi gọi `persist()`. |
| **`SEQUENCE`** | Dùng đối tượng `SEQUENCE` của DB (Oracle, PostgreSQL). Cho hiệu năng rất tốt do hỗ trợ sinh sẵn batch ID. |
| **`TABLE`** | Dùng một bảng giả lập trong DB để lưu giá trị sequence. Tương thích với mọi DB nhưng hiệu năng chậm nhất. |

#### Relationships

JPA hỗ trợ đầy đủ 4 loại quan hệ trong CSDL, đi kèm với các quy tắc nạp dữ liệu:

1. **`@ManyToOne` & `@OneToOne` (Mặc định là `EAGER`)**
    - Thường được đặt ở class chứa khóa ngoại.
    - Ví dụ: `Order` có `@ManyToOne User user;`. Khi query `Order`, `User` sẽ được `JOIN` và lấy lên ngay lập tức.
    - *Lưu ý:* Nên chủ động chuyển thành `fetch = FetchType.LAZY` để tránh giảm hiệu năng.
2. **`@OneToMany` & `@ManyToMany` (Mặc định là `LAZY`)**
    - Thường trả về một Collection (`List`, `Set`).
    - Dữ liệu chỉ được truy vấn thêm khi thực sự gọi đến hàm getter của collection đó.

**Các thuộc tính quan trọng đi kèm quan hệ:**

- **`mappedBy`**: Đặt ở phía "không chứa khóa ngoại" (phía Inverse) trong quan hệ 2 chiều, báo cho JPA biết hãy dựa vào cấu hình của thuộc tính phía kia để map.
- **`@JoinColumn(name="...")`**: Chỉ định đích danh tên cột khóa ngoại.
- **`@JoinTable`**: Bắt buộc dùng trong `@ManyToMany` để định nghĩa bảng trung gian (join table) và các cột khóa ngoại tương ứng.

Khi thiết kế quan hệ 1-N (Cha - Con), ta thường cấu hình đẩy thao tác từ bảng cha xuống bảng con:

- **`cascade = CascadeType.ALL`**: Các thao tác (Lưu mới, Cập nhật, Xóa) gọi trên Entity Cha sẽ tự động áp dụng xuống các Entity Con.
- **`orphanRemoval = true`**: Nếu một Entity Con bị loại bỏ khỏi danh sách (List/Set) của Entity Cha (tức là không còn được tham chiếu), nó sẽ bị **xóa hẳn khỏi Database**. (Khác với `CascadeType.REMOVE` chỉ xóa bản ghi con khi bản ghi cha bị xóa).

#### Inheritance & Embedding

1. Nhúng đối tượng

Dùng để gộp các trường hay đi liền với nhau thành một class riêng cho gọn code Java, nhưng dưới CSDL vẫn nằm chung 1 bảng.

- **`@Embeddable`**: Gắn trên class con (VD: `Address`).
- **`@Embedded`**: Gắn trên thuộc tính nằm trong class cha (VD: `Customer`). Dưới DB, bảng `Customer` sẽ có thêm các cột `address_street`, `address_city`.
2. Kế thừa (`@Inheritance`)

Gắn trên class Cha để quyết định cấu trúc bảng. Cần kết hợp `@DiscriminatorColumn` để phân biệt các dòng dữ liệu.

- **`SINGLE_TABLE` (Mặc định):** Cha và các Con nằm chung 1 bảng to. Cực nhanh vì không cần JOIN, nhưng DB sẽ có nhiều cột bị bỏ trống (null).
- **`JOINED`:** Mỗi class (cả Cha và Con) là 1 bảng riêng. Bảng con chỉ lưu các cột khác biệt và trỏ khóa ngoại về bảng cha. Chuẩn chuẩn hóa DB, nhưng query chậm vì phải JOIN.
- **`TABLE_PER_CLASS`:** Mỗi class Con là một bảng độc lập chứa cả thuộc tính của Cha. Ít dùng vì truy vấn đa hình rất nặng (phải dùng `UNION`).

3. **`@MappedSuperclass`**
Nếu Class Cha chỉ đóng vai trò "cung cấp các trường dùng chung" (như `createdAt`, `updatedBy`) mà không cần tạo bảng riêng cho nó, hãy dùng `@MappedSuperclass`.

### Annotation Hỗ trợ & Tiện ích

- **`@Version`**: Áp dụng Optimistic Locking. JPA tự động tăng số version mỗi lần update, giúp ngăn ngừa lỗi "ghi đè dữ liệu" khi nhiều transaction cùng sửa 1 bản ghi.
- **`@Transient`**: Báo cho JPA bỏ qua thuộc tính này, KHÔNG map thành cột trong Database (thường dùng cho các trường tính toán tạm thời).
- **`@Enumerated(EnumType.STRING)`**: Cấu hình lưu Enum vào DB dưới dạng Text. (Mặc định JPA lưu `ORDINAL` - số nguyên thứ tự, rất nguy hiểm nếu sau này đổi thứ tự Enum).
- **`@Lob`**: Dành cho các chuỗi rất dài (CLOB) hoặc mảng byte hình ảnh/file (BLOB).
- **`@AttributeConverter`**: Dùng để custom logic chuyển đổi kiểu dữ liệu giữa Java và DB (VD: mã hóa chuỗi trước khi lưu, giải mã khi đọc lên).

| **Annotation** | **Vị trí đặt** | **Chức năng chính** |
| --- | --- | --- |
| **`@Entity`** / **`@Table`** | Class | Đánh dấu thực thể / Tùy chỉnh tên bảng. |
| **`@Id`** / **`@GeneratedValue`** | Field | Khai báo Khóa chính / Chiến lược sinh ID tự động. |
| **`@Column`** | Field | Tùy chỉnh chi tiết cột (tên, độ dài, null, unique). |
| **`@Embedded`** / **`@Embeddable`** | Field / Class | Nhúng class con vào bảng của thực thể cha. |
| **`@OneToOne`** / **`@ManyToOne`** | Field | Quan hệ tham chiếu (Mặc định nạp dữ liệu **EAGER**). |
| **`@OneToMany`** / **`@ManyToMany`** | Field (Collection) | Quan hệ tập hợp (Mặc định nạp dữ liệu **LAZY**). |
| **`@JoinColumn`** / **`@JoinTable`** | Trên Quan hệ | Định nghĩa tên cột Khóa ngoại / Bảng trung gian. |
| **`@Inheritance`** | Class (Cha) | Chọn chiến lược map kế thừa (`SINGLE_TABLE`, `JOINED`...). |
| **`@MappedSuperclass`** | Class (Cha) | Lớp cha trừu tượng (chia sẻ trường, không tạo bảng). |
| **`@Version`** | Field | Quản lý khóa lạc quan (Optimistic Locking). |
| **`@Enumerated`** | Field | Cách lưu Enum (Nên dùng `EnumType.STRING`). |
| **`@Transient`** | Field | Bỏ qua không lưu vào Database. |