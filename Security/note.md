# A&A

## Khái niệm

Xác thực (Authentication), xét về mặt bản chất kỹ thuật, là quá trình hệ thống tiến hành xác minh tính xác thực của một thực thể. Quá trình này được thiết kế để giải quyết một câu hỏi duy nhất và cơ bản nhất: "Thực thể đang cố gắng kết nối vào hệ thống thực sự là ai?". Thực thể ở đây không chỉ giới hạn là end-users, mà còn bao gồm các IoT devices, các background jobs, hoặc các dịch vụ hệ thống khác. Để chứng minh danh tính của mình, thực thể yêu cầu truy cập phải cung cấp các credentials. 

Trong lý thuyết an toàn thông tin, các credentials này được phân loại thành ba nhóm yếu tố chính: 

- Yếu tố kiến thức (những gì người dùng ghi nhớ trong đầu, điển hình là tên đăng nhập, mật khẩu, hoặc mã PIN),
- Yếu tố sở hữu (những gì người dùng đang cầm nắm vật lý hoặc lưu trữ trên thiết bị cá nhân, ví dụ như thẻ thông minh, khóa bảo mật phần cứng YubiKey, mã OTP sinh ra từ ứng dụng Authenticator, hoặc một chuỗi JSON Web Token)
- Yếu tố sinh trắc học (những đặc điểm vật lý hoặc hành vi không thể sao chép của người dùng, chẳng hạn như dấu vân tay, cấu trúc mống mắt, hoặc mô hình nhận diện khuôn mặt)

Khi một hệ thống chỉ yêu cầu một trong các yếu tố trên, nó được gọi là Single-factor Authentication. Mặc dù dễ triển khai, phương pháp này tiềm ẩn rủi ro lộ lọt thông tin cực kỳ cao thông qua social engineering hoặc tấn công brute-force. Do đó, tiêu chuẩn ngành hiện tại bắt buộc áp dụng MFA, yêu cầu người dùng kết hợp ít nhất hai yếu tố thuộc hai nhóm khác nhau (ví dụ: mật khẩu kết hợp với mã OTP trên điện thoại) để tạo ra một lớp bảo mật nhiều lớp, vô hiệu hóa khả năng tấn công. 

Hơn nữa, xét về nguồn gốc quản lý thông tin, hệ thống có thể áp dụng mô hình Local Authentication, nơi toàn bộ dữ liệu người dùng được mã hóa và lưu trữ trực tiếp trên cơ sở dữ liệu nội bộ của hệ thống; hoặc mô hình External Authentication, hay còn gọi là Federated Authentication, nơi ứng dụng ủy thác hoàn toàn việc xác minh danh tính cho một nhà cung cấp định danh (Identity Provider - IdP) độc lập thông qua các giao thức ủy quyền phân tán như OAuth 2.0, SAML, hoặc OpenID Connect. Mô hình bên ngoài đặc biệt hữu ích trong việc triển khai SSO, giảm thiểu gánh nặng quản lý mật khẩu cho cả người dùng và quản trị viên.

Ngay sau khi danh tính của thực thể đã được xác lập thành công thông qua quá trình xác thực, hệ thống sẽ tự động chuyển sang bước thứ hai, phức tạp và mang tính nghiệp vụ cao hơn: Phân quyền (Authorization). Authorization là thuật toán quyết định xem một người dùng, sau khi đã được định danh, có được phép thực hiện một hành động cụ thể trên một vùng tài nguyên dữ liệu cụ thể hay không. Quá trình này trả lời cho câu hỏi: "Bạn được phép làm những gì?". Trong một kiến trúc phần mềm bảo mật, module phân quyền sẽ tiếp nhận các thông tin định danh thu thập được từ bước xác thực, đánh giá chúng dựa trên một bộ Access Control Policies, các Business Logic, hoặc các Access Control List - ACL tĩnh để đưa ra quyết định cuối cùng: Permit hoặc Deny.

Mô hình phân quyền hiện đại luôn tuân thủ nguyên tắc Principle of Least Privilege. Theo triết lý này, mỗi cá nhân hoặc dịch vụ chỉ được hệ thống cấp phát một lượng quyền hạn vừa đủ, không thừa và không thiếu, để thực thi trọn vẹn các tác vụ hợp lệ của họ trong một khoảng thời gian nhất định. Một hệ thống có thể xác thực người dùng thành công nhưng vẫn từ chối yêu cầu của họ ở bước phân quyền (mã lỗi HTTP 403 Forbidden), trong khi nếu quá trình xác thực thất bại ngay từ đầu, hệ thống sẽ trả về mã lỗi hoàn toàn khác (HTTP 401 Unauthorized). 

## Mô hình Stateful & Stateless

### Stateful

Mô hình Stateful hoạt động dựa trên nguyên tắc rằng máy chủ phải chủ động lưu giữ và cập nhật liên tục thông tin về ngữ cảnh hoặc trạng thái hiện tại của từng phiên làm việc của người dùng. Khi người dùng tương tác với hệ thống—ví dụ như đăng nhập, thêm sản phẩm vào giỏ hàng, hoặc tiến hành thanh toán qua nhiều màn hình—máy chủ sẽ tạo ra các bản ghi bộ nhớ để ghi nhớ từng bước di chuyển của họ. Mỗi HTTP request tiếp theo từ trình duyệt của người dùng đó không cần mang theo toàn bộ dữ liệu lịch sử; nó chỉ cần mang theo một id, và máy chủ sẽ tự động liên kết request đó với trạng thái đã lưu trữ trước đó trong bộ nhớ RAM hoặc trong cơ sở dữ liệu nội bộ.

Ưu điểm của Stateful là tính kiểm soát tuyệt đối và khả năng thực thi các luồng nghiệp vụ phức tạp một cách tự nhiên. Máy chủ có thể theo dõi chính xác thời gian người dùng trực tuyến, buộc họ đăng xuất lập tức, hoặc ngăn chặn các hành vi gian lận dựa trên lịch sử thao tác. Tuy nhiên, khi đối mặt với kỷ nguyên dữ liệu lớn và hàng triệu người dùng truy cập đồng thời, kiến trúc Stateful bộc lộ điểm yếu chí mạng về khả năng scaling theo chiều ngang. Trong một server cluster, việc định tuyến một người dùng luôn quay trở lại đúng máy chủ đang giữ trạng thái của họ đòi hỏi cơ chế "Sticky Sessions" ở layer Load Balancer. Nếu máy chủ đó gặp sự cố quá tải hoặc sập nguồn, toàn bộ dữ liệu phiên làm việc của hàng nghìn người dùng đang kết nối với nó sẽ bị bốc hơi hoàn toàn, dẫn đến sự gián đoạn dịch vụ nghiêm trọng. Để khắc phục, hệ thống phải áp dụng các cơ chế Session Replication phức tạp giữa các máy chủ, hoặc dựa dẫm vào một cụm bộ nhớ đệm phân tán trung tâm (như Redis Cluster), qua đó tạo ra độ trễ mạng đáng kể và các bottlenecks.

### Stateless

Trái ngược với điều đó, mô hình Stateless đại diện cho một bước nhảy vọt về tư duy thiết kế, đặc biệt phù hợp với triết lý của kiến trúc RESTful. Trong kiến trúc stateless, nguyên tắc vàng là máy chủ tuyệt đối không lưu giữ bất kỳ thông tin gì về trạng thái của các phiên giao dịch giữa các lần gọi mạng độc lập. Mỗi yêu cầu gửi từ máy khách đến máy chủ phải là một khối thông tin độc lập, khép kín và chứa đựng toàn bộ các dữ kiện, ngữ cảnh và bằng chứng xác thực cần thiết để máy chủ có thể phân tích, xác minh và xử lý yêu cầu đó ngay lập tức, mà không cần truy vấn lại lịch sử tương tác trước đó. Bản thân giao thức HTTP nguyên thủy chính là một giao thức stateless điển hình.

Sự loại bỏ hoàn toàn gánh nặng quản lý bộ nhớ phiên mang lại lợi thế vô song về khả năng mở rộng. Các hệ thống điều phối container như Kubernetes và các bộ cân bằng tải có thể tự do điều phối các yêu cầu HTTP từ cùng một người dùng đến bất kỳ node nào đang rảnh rỗi trong cụm mà không lo ngại về vấn đề mất mát trạng thái. Việc thêm mới máy chủ vào cụm hoặc loại bỏ các máy chủ bị lỗi diễn ra trơn tru mà không làm gián đoạn trải nghiệm người dùng. Đây là lý do kiến trúc stateless trở thành sự lựa chọn hiển nhiên và là tiêu chuẩn thiết kế bắt buộc cho các hệ thống vi dịch vụ (microservices) quy mô lớn, các hệ thống API công khai, và các ứng dụng vận hành trên môi trường Serverless.

## Các cơ chế xác thực

### Session & Cookie

Cơ chế Session/Cookie Authentication là phương pháp tiếp cận truyền thống và lâu đời nhất trong lịch sử phát triển ứng dụng web, trong đó máy chủ đóng vai trò trung tâm và duy trì toàn bộ quyền lực trong việc giám sát trạng thái định danh của khách hàng. Quá trình này được kích hoạt khi trình duyệt web của người dùng thực hiện một phương thức POST gửi các credentials (như tài khoản và mật khẩu) đến endpoint API của máy chủ.

Sau khi máy chủ nhận được yêu cầu, nó sẽ thực thi một chuỗi các hàm hashing để đối chiếu thông tin với bản ghi trong cơ sở dữ liệu. Nếu thông tin khớp, máy chủ sẽ tiến hành khởi tạo một đối tượng phiên làm việc mới. Đối tượng phiên này là một vùng nhớ ảo, nơi máy chủ có thể lưu trữ các siêu dữ liệu nhạy cảm của người dùng (ID người dùng, vai trò, thời điểm đăng nhập, địa chỉ IP). Để theo dõi vùng nhớ này, máy chủ tạo ra một chuỗi ký tự ngẫu nhiên, độ dài lớn (có entropy cao) làm mã định danh duy nhất, được gọi là Session ID. Tùy thuộc vào kiến trúc, máy chủ có thể lưu trữ Session ID và dữ liệu đi kèm trực tiếp trên RAM máy chủ (gây khó khăn cho việc mở rộng), lưu trên cơ sở dữ liệu quan hệ (gây chậm chạp do độ trễ truy xuất I/O), hoặc lưu trên hệ thống bộ nhớ đệm tốc độ cao như Redis hoặc Memcached.

Để duy trì trạng thái cho người dùng, máy chủ phải gửi Session ID này về cho trình duyệt. Thông qua cơ chế phản hồi HTTP, máy chủ sử dụng tiêu đề `Set-Cookie` để chỉ thị trình duyệt lưu trữ chuỗi Session ID này vào kho chứa cookie cục bộ. Kể từ thời điểm này, trình duyệt web đóng vai trò là một tác nhân tự động: đối với mọi yêu cầu HTTP tiếp theo gửi đến cùng một tên miền của ứng dụng, trình duyệt sẽ tự động nội suy và đính kèm cookie chứa Session ID vào header `Cookie` của request. Khi request đến máy chủ, hệ thống sẽ trích xuất Session ID từ cookie, thực hiện một thao tác tra cứu  để tìm kiếm đối tượng phiên tương ứng. Nếu tìm thấy và phiên chưa hết hạn, máy chủ xác nhận danh tính người dùng và tiến hành các bước kiểm tra phân quyền tiếp theo. Nếu người dùng nhấn đăng xuất, hoặc nếu một khoảng timeout trôi qua, máy chủ sẽ chủ động xóa bản ghi phiên làm việc trong kho lưu trữ, đồng thời yêu cầu trình duyệt hủy bỏ cookie, từ đó chấm dứt hoàn toàn khả năng truy cập.

**Ưu điểm và Nhược điểm của Cơ chế Session/Cookie**

Mô hình Session-Based Authentication mang lại hàng loạt các ưu điểm vượt trội về mặt kiểm soát quản trị và an toàn lưu trữ ở phía client:

1. **Immediate Revocation:** Đây là ưu điểm lớn nhất của thiết kế Stateful. SysAdmin hoặc hệ thống phát hiện gian lận tự động có thể ngay lập tức hủy bỏ quyền truy cập của bất kỳ người dùng nào bằng cách xóa bản ghi phiên trong cơ sở dữ liệu. Mọi nỗ lực gửi yêu cầu với Session ID cũ sẽ lập tức bị từ chối do máy chủ không còn tìm thấy thông tin định danh tương ứng. Tính năng này vô cùng quan trọng đối với các hệ thống tài chính, ngân hàng, nơi việc khóa tài khoản khẩn cấp khi nghi ngờ rò rỉ mật khẩu là yêu cầu bắt buộc.
2. **Tối ưu hóa lưu trữ phía Client:** Bản thân cookie chỉ là một chuỗi định danh vô nghĩa đối với bất kỳ ai chặn bắt được nó, không chứa thông tin cá nhân dạng plaintext, và kích thước của nó cực kỳ nhỏ (chỉ khoảng vài chục bytes). Điều này giúp tiết kiệm đáng kể băng thông mạng trong các giao thức truyền tải.
3. **Hàng rào chống XSS:** Khi thiết lập cookie, máy chủ có thể gắn cờ `HttpOnly`. Cờ này là một chỉ thị cưỡng chế trình duyệt không cho phép bất kỳ script JS nào ở phía client (như `document.cookie`) đọc được nội dung của cookie. Cơ chế này gần như vô hiệu hóa hoàn toàn các nguy cơ rò rỉ thông tin xác thực nếu tin tặc lợi dụng được lỗ hổng inject mã XSS vào trang web.
4. **Tích hợp cơ chế Trình duyệt tự động:** Các FE devs không cần phải viết thêm bất kỳ logic JS nào để lưu trữ hay đính kèm token vào mỗi yêu cầu. Trình duyệt quản lý toàn bộ vòng đời và cơ chế truyền gửi của cookie một cách transparent.

Tuy nhiên, mô hình này không tránh khỏi những rào cản chí mạng, đặc biệt trong các mô hình hệ thống quy mô lớn hiện đại:

1. **Poor Scalability:** Việc buộc máy chủ phải lưu trữ trạng thái tạo ra sự phụ thuộc chặt chẽ giữa khách hàng và bộ nhớ hệ thống. Khi số lượng người dùng đồng thời vượt quá giới hạn RAM của một máy chủ, hệ thống phải mở rộng sang nhiều cụm máy chủ. Việc duy trì và đồng bộ hóa session giữa hàng trăm node máy chủ là một thách thức kỹ thuật to lớn, đòi hỏi hệ thống phải triển khai và duy trì các cụm cơ sở dữ liệu in-memory trung tâm đắt đỏ.
2. **Latency Overhead:** Mỗi một yêu cầu đơn lẻ chạm đến hệ thống đều bắt buộc máy chủ phải thực hiện ít nhất một thao tác I/O đọc cơ sở dữ liệu hoặc Redis để xác minh Session ID. Khi lưu lượng truy cập lên đến hàng triệu yêu cầu mỗi giây, các thao tác tra cứu này sẽ nhanh chóng gây thắt cổ chai hiệu suất.
3. **Lỗ hổng CSRF:** Chính sự tự động hóa tiện lợi của trình duyệt lại tạo ra điểm yếu chết người nhất cho cơ chế cookie. Vì trình duyệt tự động gửi cookie đến tên miền đích đối với mọi yêu cầu, tin tặc có thể thiết kế một trang web lừa đảo chứa mã độc HTML. Khi nạn nhân, người vừa đăng nhập vào ngân hàng ở một tab khác, vô tình truy cập trang web giả mạo này, đoạn mã ẩn sẽ kích hoạt một yêu cầu HTTP độc (ví dụ: chuyển khoản) hướng thẳng đến tên miền của ngân hàng. Trình duyệt, theo đúng thiết kế nguyên thủy, sẽ tự động gắn kèm cookie phiên hợp lệ vào yêu cầu đó. Ngân hàng nhận được yêu cầu, thấy cookie hợp lệ, liền tiến hành thực thi giao dịch mà không hề biết rằng nó bắt nguồn từ một trang web của bên thứ ba.

Để chống lại CSRF, hệ thống phải áp dụng các lớp phòng thủ bổ sung. Một phương pháp là máy chủ tạo ra một mã thông báo ngẫu nhiên (Anti-CSRF Token) gắn vào các form HTML và yêu cầu client phải gửi lại mã này. Một giải pháp hiện đại hơn ở cấp độ trình duyệt là sử dụng thuộc tính `SameSite` trên Cookie (với giá trị `Strict` hoặc `Lax`). Thuộc tính này ra lệnh cho trình duyệt chỉ được phép đính kèm cookie nếu yêu cầu thực sự được phát sinh từ cùng một tên miền gốc, chặt đứt hoàn toàn cơ chế tấn công CSRF từ các trang web độc hại.

### JWT

JWT mô tả một phương pháp chuẩn hóa, an toàn và nhỏ gọn để biểu diễn các credentials và claims giữa hai bên giao tiếp. Sự khác biệt mang tính cách mạng của JWT nằm ở chỗ nó là một thực thể self-contained và hoàn toàn stateless. Thay vì lưu trữ thông tin ở cơ sở dữ liệu trên máy chủ và chỉ gửi về máy khách một mã ID vô nghĩa, hệ thống JWT đóng gói toàn bộ các metadata cần thiết—chẳng hạn như định danh người dùng, danh sách các quyền hạn được cấp, và thời gian hết hạn—trực tiếp vào bên trong cấu trúc payload, mã hóa bằng signature, và giao phó toàn bộ khối dữ liệu này cho phía máy khách lưu trữ.

Về mặt cấu trúc vật lý, một JWT tiêu chuẩn được hiển thị dưới dạng một chuỗi văn bản rất dài, bao gồm ba thành phần riêng biệt được ngăn cách với nhau bằng dấu chấm (`.`): Header, Payload và Signature:

- Khối đầu tiên này là một đối tượng JSON nhỏ, chứa các metadata mô tả cấu trúc của bản thân token. Thông thường, nó bao gồm hai trường thông tin chính: trường `typ` (loại token, ở đây luôn mang giá trị "JWT") và trường `alg` (thuật toán chữ ký). Trường `alg` quy định thuật toán mật mã nào sẽ được máy chủ sử dụng để bảo vệ tính toàn vẹn của token. Các thuật toán phổ biến bao gồm thuật toán băm đối xứng HMAC sử dụng hàm SHA-256 (HS256) nơi cả quá trình ký và giải mã dùng chung một secret key, hoặc thuật toán mã hóa bất đối xứng (RS256) sử dụng public/private key pair. Khối JSON này sau đó được chuyển đổi qua định dạng mã hóa Base64URL để đảm bảo an toàn khi truyền tải qua các môi trường HTTP.
- **Payload :** Khối thứ hai chứa thông tin thực sự cần truyền tải, được gọi là các Claims. Các Claims là các cặp key-value định nghĩa ngữ cảnh của thực thể. Tiêu chuẩn RFC 7519 chia Claims thành ba nhóm:
    - *Registered Claims:* Các trường tiêu chuẩn do hệ thống định nghĩa như `sub` (subject - định danh duy nhất của người dùng), `iss` (issuer - nhà phát hành token), `exp` (expiration time - thời điểm token hết hạn), và `iat` (issued at - thời điểm phát hành).
    - *Public Claims:* Các trường tự định nghĩa dựa trên các public URI để tránh xung đột tên.
    - *Private Claims:* Các thông tin nghiệp vụ do nhà phát triển tùy ý đưa vào để phục vụ hệ thống, ví dụ như danh sách các quyền (roles) hoặc email người dùng.
    
    Tương tự như Header, toàn bộ đối tượng JSON Payload này cũng chỉ được mã hóa theo chuẩn Base64URL. Điều này đồng nghĩa với việc bất kỳ ai nắm giữ token cũng có thể dễ dàng giải mã và đọc được toàn bộ nội dung của Payload bằng các công cụ trực tuyến như jwt.io. Vì tính chất không được mã hóa bảo mật nội dung, một nguyên tắc vàng trong bảo mật JWT là **tuyệt đối không bao giờ được phép đưa các thông tin nhạy cảm** như mật khẩu, số thẻ tín dụng, hoặc khóa giải mã vào trong Payload.
    
- **Signature:** Chữ ký được máy chủ khởi tạo bằng cách ghép nối Header và Payload (đã mã hóa Base64URL) bằng dấu chấm, sau đó áp dụng thuật toán mật mã đã khai báo trong Header (ví dụ HMAC SHA-256) cùng với một Secret Key chỉ duy nhất máy chủ nắm giữ.

Khi người dùng đăng nhập bằng tài khoản và mật khẩu thành công, máy chủ xác thực sẽ khởi tạo một JWT, điền đầy các claims, ký nó bằng private secret key, và gửi trả lại cho client. Trình duyệt hoặc ứng dụng di động sẽ lưu trữ JWT này, và trong mọi request tiếp theo, máy khách phải đính kèm JWT vào hệ thống thông qua header HTTP chuẩn hóa là `Authorization` với định dạng tiền tố `Bearer <chuỗi_token>`.

Bất cứ khi nào máy chủ nhận được một yêu cầu có chứa JWT, nó không cần phải kết nối đến cơ sở dữ liệu hay Redis để kiểm tra trạng thái. Thay vào đó, security filter trên máy chủ chỉ cần trích xuất Header và Payload từ token nhận được, áp dụng secret key của mình để tính toán lại một signature mới. Nếu signature vừa tính toán khớp hoàn toàn 100% với chữ ký đính kèm trong token, máy chủ có thể đi đến một khẳng định chắc chắn tuyệt đối về mặt toán học rằng: Token này là do chính hệ thống của nó phát hành, và nội dung bên trong Payload chưa từng bị bất kỳ bên thứ ba nào sửa đổi trên đường truyền. Toàn bộ quy trình xác minh danh tính và quyền hạn diễn ra cục bộ tại cấp độ CPU của máy chủ thông qua thuật toán hashing, tiết kiệm hoàn toàn độ trễ khổng lồ từ các truy vấn I/O hệ thống mạng. Đặc tính này biến JWT trở thành hạt nhân lý tưởng cho các kiến trúc microservices. Bất kỳ một microservice nào, nếu được configure private key (đối với HMAC) hoặc sở hữu public key (đối với thuật toán RSA JWKS), đều có khả năng tự động giải mã và xác thực các token một cách độc lập và phân tán.

Vì JWT không tự động được trình duyệt quản lý như cookie, các FE devs thường mắc phải một sai lầm phổ biến là lưu trữ token trong vùng nhớ `localStorage` hoặc `sessionStorage` của trình duyệt. Khác với cookie có thể được bảo vệ bằng cờ `HttpOnly`, bất kỳ JS script nào thực thi trên trang web đều có thể dễ dàng truy cập vào các vùng nhớ này. Nếu ứng dụng web tồn tại một lỗ hổng XSS, tin tặc chỉ cần tiêm một đoạn mã JavaScript đơn giản để trích xuất token trong `localStorage` và gửi nó về máy chủ của chúng. Do đó, best practice cho JWT trên webapp không phải là `localStorage`, mà là tiếp tục tận dụng cơ chế Cookie nhưng được thiết lập cấu hình nghiêm ngặt với các cờ `HttpOnly` và `Secure` để bảo vệ token.

Tiêu chuẩn RFC 7519 cho phép trường `alg` trong Header nhận giá trị `none`, biểu thị một token không cần chữ ký. Nếu thư viện giải mã JWT trên máy chủ không được thiết lập để từ chối các token này, kẻ tấn công có thể tạo ra một JWT với Payload giả mạo (ví dụ, thay đổi ID thành ID của quản trị viên), đặt `alg: none`, xóa phần chữ ký và gửi lên máy chủ. Máy chủ cấu hình kém sẽ chấp nhận nó như một token hợp lệ. Một biến thể tinh vi hơn là khi hệ thống đang sử dụng thuật toán RSA. Tin tặc tải khóa công khai của hệ thống xuống, dùng khóa công khai đó làm khóa bí mật để ký một token giả bằng thuật toán đối xứng HMAC (HS256), và sửa đổi Header thành `alg: HS256`. Nếu máy chủ không áp dụng cơ chế xác minh cứng loại thuật toán được phép, nó sẽ dùng khóa công khai hiện có của nó để đối chiếu HMAC, và do hai khóa trùng khớp, token giả mạo sẽ vượt qua bài kiểm tra chữ ký.

Nguy cơ thứ ba liên quan đến quản lý khóa mật mã. Khóa bí mật sử dụng trong thuật toán HMAC là chìa khóa của mọi bảo mật. Nếu khóa bí mật này quá ngắn, dễ đoán, hoặc bị vô tình harcoded trên các kho lưu trữ mã nguồn mở như GitHub, kẻ tấn công có thể thực hiện brute-force để tìm ra khóa bí mật. Khi đã có khóa, tin tặc có thể tự do sinh ra hàng nghìn token giả mạo với bất kỳ quyền hạn nào để phá hệ thống. Tiêu chuẩn công nghiệp khuyến nghị sử dụng các chuỗi ngẫu nhiên dài ít nhất 256 bits và quản lý chúng thông qua các hệ thống Vault chuyên dụng.

Khó khăn lớn nhất đối với kiến trúc JWT là giải quyết sự đánh đổi cốt lõi của tính phi trạng thái: Bài toán revocation.

Để chống lại hiện tượng này mà không phá vỡ hoàn toàn ưu điểm hiệu năng của thiết kế stateless, các kiến trúc sư đã phát triển method Atomic Invalidation, kết hợp nhiều lớp phòng thủ:

1. **Short-Lived Access Tokens:** Thay vì tạo ra JWT có hạn sử dụng nhiều ngày, hệ thống chỉ phát hành Access Token có vòng đời cực ngắn, lý tưởng nhất là từ 15 đến 30 phút. Nếu token bị đánh cắp, nó cũng sẽ nhanh chóng trở thành một chuỗi vô dụng. Để không làm phiền người dùng phải đăng nhập liên tục sau mỗi 15 phút, hệ thống sử dụng thêm một Refresh Token có thời hạn dài (ví dụ: 7 ngày) được lưu trữ an toàn trong các Cookie `HttpOnly`. Khi Access Token hết hạn, máy khách sẽ tự động gửi Refresh Token lên điểm cuối hệ thống để đổi lấy một Access Token mới.
2. **Refresh Token Rotation:** Hệ thống không chỉ cấp mới Access Token mà còn cấp mới luôn cả Refresh Token tại mỗi chu kỳ làm mới. Refresh Token cũ ngay lập tức bị hệ thống đánh dấu là đã sử dụng. Khi request làm mới token được gửi lên, máy chủ sẽ trả về đồng thời cả Access Token và Refresh Token mới, rồi lập tức hủy bỏ Refresh Token cũ. Nếu hệ thống ghi nhận một Refresh Token bị sử dụng từ hai lần trở lên — ví dụ do hacker đã xài trước đó và người dùng thật xài lại sau (hoặc ngược lại) — nó sẽ ngay lập tức nhận diện được hành vi đánh cắp và từ chối cấp quyền.
3. **Cơ chế The Nuclear Option thông qua Redis Blocklist:** Việc một token bị tái sử dụng là bằng chứng chắc chắn 100% cho thấy tài khoản đã bị xâm phạm. Khi phát hiện điều này, hệ thống sẽ không chỉ từ chối request hiện tại mà còn kích hoạt biện pháp bảo mật mạnh tay nhất: vô hiệu hóa lập tức toàn bộ phiên đăng nhập của người dùng đó. Để thực hiện, hệ thống tận dụng trường `jti` (JWT ID) – một claim tiêu chuẩn đóng vai trò là mã định danh duy nhất cho mỗi JWT. Hệ thống sẽ duy trì một Danh sách đen (Blocklist/Denylist) trên bộ nhớ đệm Redis để chứa các mã `jti` cần thu hồi. Khi cơ chế triệt để được kích hoạt, mã `jti` của kẻ gian (và thậm chí toàn bộ các token liên quan đến user đó) sẽ bị ném thẳng vào danh sách này. Điểm tinh tế của kiến trúc này nằm ở cách tối ưu bộ nhớ: Thời gian tồn tại (TTL - Time To Live) của mỗi bản ghi `jti` trong Redis được thiết lập bằng đúng thời gian hiệu lực còn lại của JWT đó. Nhờ vậy, ngay khi JWT tự nhiên hết hạn, bản ghi trong Redis cũng tự động bay màu, giúp bộ nhớ RAM không bao giờ bị phình to. Về quy trình xử lý, mọi request tiếp theo gọi lên API (ví dụ khi đi qua các bộ lọc bảo mật như `OncePerRequestFilter` trong ứng dụng Spring Boot) đều sẽ bị trích xuất chuỗi `jti` và đối chiếu nhanh với Redis. Với tốc độ truy xuất trên bộ nhớ đệm chỉ mất vài micro-giây, hệ thống chặn đứng mọi nỗ lực truy cập của kẻ tấn công mà gần như không gây ra độ trễ nào. Kỹ thuật Blocklist này về bản chất là việc mang một chút tính chất "stateful" (lưu trạng thái) gài ngược lại vào kiến trúc "stateless" (phi trạng thái) của JWT. Tuy nhiên, nó chỉ được áp dụng ở những chốt chặn an ninh mang tính sống còn, tạo ra một sự thỏa hiệp hoàn hảo giữa bảo mật tối đa và hiệu năng truy xuất."

### RBAC & ABAC

Kiến trúc **RBAC (Role-Based Access Control)** được xây dựng dựa trên sự tương tác của 6 thành phần chính:

- **Users:** Bất kỳ ai hoặc thứ gì yêu cầu truy cập (có thể là con người, thiết bị phần cứng, hoặc một service tự động).
- **Roles:** Đại diện logic cho chức danh hoặc bộ phận trong tổ chức (ví dụ: `Doctor`, `HR_Manager`, `Accountant`).
- **Resources (hoặc Objects):** Đích đến mà User muốn thao tác (ví dụ: API endpoint, file, hoặc database table).
- **Operations:** Các hành động cụ thể được phép thực thi lên Resources (phổ biến nhất là các thao tác CRUD: Create, Read, Update, Delete).
- **Permissions:** Ranh giới quyền hạn, được định nghĩa bằng cách ghép **Operations** và **Resources** lại với nhau (Ví dụ: quyền `Read` trên bảng `Salary`, hoặc quyền `Delete` bài viết).
- **Sessions:** Phiên làm việc hiện tại, nơi hệ thống xác nhận User đang mang Role nào.

**Mô hình RBAC được phân loại thành 4 cấp độ từ cơ bản đến hoàn thiện:**

- **Cấp 1: Flat RBAC (RBAC Phẳng)**
Là cấp độ nền tảng định hình cấu trúc phân quyền cốt lõi. Trong Flat RBAC, hệ thống quản lý Permissions thông qua một cấu trúc phẳng, không có sự phân nhánh hay kế thừa. Một User có thể được gán một hoặc nhiều Roles độc lập cùng lúc để đáp ứng yêu cầu nghiệp vụ.
- **Cấp 2: Hierarchical RBAC (RBAC Phân cấp)**
Mở rộng từ cấp độ 1 bằng cách tích hợp cơ chế kế thừa (inheritance). Hierarchical RBAC thiết lập mối quan hệ cấu trúc cây giữa các Roles, phản ánh sơ đồ tổ chức thực tế của doanh nghiệp. Theo đó, các Role ở phân cấp quản lý cao hơn sẽ tự động kế thừa toàn bộ Permissions của các Role ở phân cấp dưới. Ví dụ: Role `Manager` tự động bao hàm toàn bộ Permissions của Role `Employee` cộng thêm các quyền quản trị riêng, giúp loại bỏ yêu cầu gán kép nhiều Roles cho một User.
- **Cấp 3: Constrained RBAC (RBAC Ràng buộc)**
Bổ sung thiết chế quản trị rủi ro thông qua nguyên tắc **Separation of Duties (SoD - Phân tách trách nhiệm)**. Cấp độ này ngăn chặn các hành vi gian lận và trục lợi phát sinh từ việc một cá nhân tập trung quá nhiều đặc quyền.
    - **Static SoD (SoD Tĩnh):** Là rào cản ở mức độ cấu hình, cấm hệ thống gán đồng thời các Roles có tính xung đột nghiệp vụ cho cùng một User (ví dụ: không thể gán Role `Maker` và `Checker` cho cùng một định danh).
    - **Dynamic SoD (SoD Động):** Là rào cản ở mức độ thực thi. Hệ thống cho phép User sở hữu các Roles có tính xung đột, nhưng nghiêm cấm việc kích hoạt (activate) hoặc sử dụng các Roles này trong cùng một Session làm việc.
- **Cấp 4: Symmetric RBAC (RBAC Đối xứng)**
Là mức độ hoàn thiện cao nhất, tập trung vào khả năng kiểm toán (Auditing) tự động. Symmetric RBAC yêu cầu hệ thống cung cấp năng lực phân tích hai chiều liên tục: đánh giá tính hợp lệ của việc phân bổ Permissions cho Roles, và việc gán Roles cho Users. Cơ chế này hỗ trợ tổ chức rà soát thường xuyên, phát hiện và thu hồi các đặc quyền dư thừa, đảm bảo tuân thủ nghiêm ngặt nguyên tắc đặc quyền tối thiểu trong dài hạn.

Hạn chế nội tại của mô hình RBAC bộc lộ rõ khi hệ thống phải xử lý các điều kiện truy cập động. Ví dụ, với chính sách bảo mật: *"Kế toán viên chỉ được phép truy cập hồ sơ lương trong khung giờ hành chính và thông qua địa chỉ IP mạng nội bộ"*.

Do RBAC định nghĩa quyền hạn dựa trên cấu trúc tĩnh, nó thiếu khả năng xử lý các tham số không gian và thời gian. Để đáp ứng yêu cầu trên, quản trị viên buộc phải sinh ra các Roles kết hợp phức tạp (ví dụ: `Accountant_OfficeHours_InternalIP`). Khi các biến số ngữ cảnh tăng lên, số lượng Vai trò sẽ tăng trưởng theo cấp số nhân. Hiện tượng này được gọi là **Role Explosion** (Sự bùng nổ Vai trò), gây quá tải nghiêm trọng cho quá trình quản trị và duy trì hệ thống.

Để khắc phục điểm nghẽn của RBAC, mô hình **ABAC** được đề xuất. Thay vì phụ thuộc vào việc gán Role cứng nhắc, ABAC thực thi phân quyền thông qua việc đánh giá các hàm logic Boolean dựa trên sự kết hợp của 4 nhóm **Attributes** (Thuộc tính):

- **User Attributes:** Các thuộc tính định danh (chức danh, phòng ban, mức độ giải mật).
- **Resource Attributes:** Các thuộc tính của dữ liệu (nhãn phân loại, mức độ nhạy cảm).
- **Environment Attributes:** Các biến số ngữ cảnh động (thời gian, địa chỉ IP, tọa độ vị trí, loại thiết bị truy cập).
- **Action:** Hành động được yêu cầu thực thi (Ví dụ: Read, Update, Delete).

Nhờ kiến trúc này, ABAC cung cấp khả năng kiểm soát truy cập ở mức độ chi tiết cao (**Fine-grained Access Control**), đáp ứng hoàn hảo các quyết định bảo mật phụ thuộc vào ngữ cảnh.

Mặc dù ưu việt về tính linh hoạt, ABAC đòi hỏi chi phí tính toán lớn cho việc phân tích Attributes theo thời gian thực. Sự phức tạp trong việc thiết lập hệ thống rule engine cũng khiến mô hình này khó triển khai độc lập và cực kỳ khó thực hiện **Auditing** thủ công.

## Spring Security

### DelegatingFilterProxy và FilterChainProxy

Về mặt hạ tầng, hệ thống bảo mật hoạt động như một lớp bảo vệ bên ngoài cùng. Mọi **HTTP Request** gửi đến máy chủ Servlet (như Apache Tomcat) đều phải đi qua một chuỗi các bộ lọc trước khi tiếp cận logic nghiệp vụ tại các **Controllers**.

Tuy nhiên, các Servlet Container nguyên thủy không có sự liên kết trực tiếp với **ApplicationContext** của Spring. Để giải quyết vấn đề này, Spring Security sử dụng **DelegatingFilterProxy** làm cầu nối. Thành phần này được đăng ký trong vòng đời của Servlet nhưng không trực tiếp xử lý logic; nhiệm vụ của nó là intercept yêu cầu và ủy quyền (**delegate**) việc kiểm soát cho **FilterChainProxy** — một Bean được quản lý bởi Spring.

### FilterChainProxy

**FilterChainProxy** đóng vai trò là trung tâm điều phối, chứa một danh sách các **SecurityFilterChain**. Mỗi **SecurityFilterChain** đại diện cho một cụm cấu hình bảo mật riêng biệt, được gắn với một **RequestMatcher** cụ thể. Khi có yêu cầu gửi đến, **FilterChainProxy** sẽ phân tích URL để xác định chuỗi bộ lọc phù hợp nhất (ví dụ: một chuỗi xử lý JWT cho `/api/`, chuỗi khác xử lý Form Login cho `/admin/`).

### **Thứ tự thực thi trong Security Filter Chain**

Một **SecurityFilterChain** là tập hợp các **Filters** được sắp xếp theo một thứ tự nghiêm ngặt. Thứ tự này quyết định tính toàn vẹn của logic bảo mật:

**Lớp bảo vệ hạ tầng:** Khởi đầu chuỗi thường là các bộ lọc chống tấn công cơ bản như `CorsFilter` (kiểm soát tài nguyên chéo gốc) và `CsrfFilter` (bảo vệ chống tấn công CSRF).

**Quản lý ngữ cảnh:** `SecurityContextHolderFilter` chịu trách nhiệm tải `SecurityContext` hiện có vào `SecurityContextHolder` khi bắt đầu yêu cầu và dọn dẹp bộ nhớ này sau khi yêu cầu kết thúc.

**Giai đoạn Authentication:** Các bộ lọc như `UsernamePasswordAuthenticationFilter`, `BasicAuthenticationFilter` hoặc các `Custom JWT Filter` sẽ trích xuất thông tin **credentials** và đóng gói thành đối tượng **Authentication**.

**Cơ chế xử lý Authentication:** Đối tượng này được chuyển cho `AuthenticationManager`, nơi điều phối các `AuthenticationProvider`. Các **Providers** sẽ phối hợp với `UserDetailsService` để tra cứu dữ liệu người dùng và dùng `PasswordEncoder` để đối chiếu mật khẩu. Nếu thành công, thông tin người dùng và quyền hạn sẽ được lưu vào SecurityContext.

### Authorization và Exception Handling

Sau khi định danh hoàn tất, yêu cầu sẽ đi qua `AuthorizationFilter`. Tại đây, hệ thống đối chiếu **Authorities** của người dùng với các **Access Rules** đã định nghĩa để thực thi mô hình **RBAC**.

Đứng sau cùng để giám sát toàn bộ quá trình là `ExceptionTranslationFilter`. Bộ lọc này có nhiệm vụ bắt các ngoại lệ xác thực (`AuthenticationException`) hoặc phân quyền (`AccessDeniedException`) phát sinh trong chuỗi, từ đó chuyển đổi chúng thành các phản hồi HTTP tương ứng (như `401 Unauthorized` hoặc `403 Forbidden`) để trả về cho người dùng.

### RBAC trong Spring Security

Trong Spring Security, đối tượng `GrantedAuthority` đại diện cho một đặc quyền hoặc quyền lợi ở cấp độ cơ sở nhất (fine-grained), mô tả cho một hành động đặc thù (ví dụ: `READ_USERS`, `CREATE_INVOICE`, `OP_DELETE_ACCOUNT`). Giao diện của nó có chứa hàm `getAuthority()`, trả về một chuỗi văn bản mô tả quyền này. Khối logic **AuthorizationManager** sẽ trích xuất chuỗi này để kiểm tra xem nó có khớp với yêu cầu bảo mật tài nguyên hay không. Trong đại đa số hệ thống, lớp `SimpleGrantedAuthority` là đủ để bao bọc các quyền dưới định dạng chuỗi đơn giản.

Ở khía cạnh đối lập, một `Role` có chức năng gom nhóm các `GrantedAuthority` cơ sở lại với nhau theo ngữ nghĩa phân quyền vĩ mô (coarse-grained). Điểm cực kỳ quan trọng làm nên sự khác biệt về mặt cấu trúc trong mã nguồn là Spring Security mặc định nhận diện một Role thông qua quy ước đặt tên: mọi tên của Role đều phải bắt đầu bằng một chuỗi tiền tố `ROLE_`. Khi một nhà phát triển ra lệnh cho hệ thống chỉ cho phép role "ADMIN" truy cập thông qua câu lệnh cấu hình `hasRole("ADMIN")`, cơ chế nội bộ của Spring Security sẽ ngầm nối thêm tiền tố để tìm kiếm trong danh sách của người dùng một `GrantedAuthority` có chuỗi ký tự chính xác là `ROLE_ADMIN`. Cơ chế này giúp phân tách rạch ròi giữa việc kiểm tra quyền thao tác (Permission) và kiểm tra chức danh quản lý (Role)

Một kiến trúc **RBAC** thực tiễn đòi hỏi sơ đồ cơ sở dữ liệu phải phản ánh đúng mối quan hệ đa tầng.

Hệ thống thường bao gồm 3 bảng thực thể chính: `users`, `roles`, và `permissions`. Các bảng này liên kết với nhau qua các mối quan hệ **Many-to-Many**:

- `users_roles`: Liên kết người dùng với các vai trò.
- `roles_permissions`: Định nghĩa tập hợp các quyền hạn cho từng vai trò.

Khi truy xuất dữ liệu, lớp `CustomUserDetailsService` thực hiện hợp nhất các quyền hạn:

1. Duyệt danh sách các **Permissions** trực tiếp của người dùng và chuyển đổi thành `SimpleGrantedAuthority` (ví dụ: `READ_DOCUMENT`).
2. Duyệt danh sách các **Roles**, tự động thêm tiền tố `ROLE_` (ví dụ: `ROLE_MANAGER`) và chuyển đổi thành `SimpleGrantedAuthority`.
3. Tất cả được tập hợp vào đối tượng `UserDetails`, cho phép hệ thống kiểm soát truy cập linh hoạt theo cả hai hướng: hành vi chức năng và vai trò quản lý.

Để giải quyết vấn đề **Role Explosion** Spring Security cung cấp giải pháp **RoleHierarchy**.

Thông qua việc khởi tạo một `@Bean` kiểu `RoleHierarchy`, nhà quản trị có thể thiết lập các quy tắc kế thừa logic, ví dụ:

$ROLE\_SUPERUSER > ROLE\_ADMIN > ROLE\_USER$

Với cấu trúc này, nếu một API yêu cầu quyền `ROLE_USER`, những người dùng sở hữu vai trò `ROLE_ADMIN` hoặc `ROLE_SUPERUSER` sẽ mặc nhiên vượt qua vòng kiểm tra bảo mật mà không cần khai báo trực tiếp quyền `ROLE_USER` trong cơ sở dữ liệu.

Spring Security 6 giới thiệu annotation `@EnableMethodSecurity`, thay thế cho `@EnableGlobalMethodSecurity` vốn đã bị **deprecated**.

- Các tính năng cốt lõi được mặc định kích hoạt (không cần tham số `prePostEnabled = true`).
- Chuyển dịch cơ chế xử lý từ các mô hình **Voter** cũ sang đối tượng `AuthorizationManager`, tận dụng sức mạnh của **Native AOP**.

Annotation `@PreAuthorize` cho phép thiết lập các chốt dựa trên **Spring Expression Language (SpEL)** ngay trên các hàm nghiệp vụ. Điều này giúp bảo vệ hệ thống khỏi lỗ hổng **IDOR (Insecure Direct Object Reference)**.

**Ví dụ thực tế:**

```java
@PreAuthorize("hasRole('ADMIN') or #invoice.ownerId == authentication.principal.id")
public void deleteInvoice(Invoice invoice) {
    // Logic xóa hóa đơn
}
```

Biểu thức trên đảm bảo rằng hành động xóa hóa đơn chỉ được thực thi nếu:

1. Người dùng có vai trò `ADMIN`.
2. **Hoặc** người dùng hiện tại chính là chủ sở hữu của hóa đơn đó.