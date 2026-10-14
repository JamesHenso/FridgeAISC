# FridgeWise — Đặc tả chức năng và luồng người dùng

## 1. Tổng quan

**FridgeWise** là ứng dụng di động giúp người dùng quản lý thực phẩm trong tủ lạnh, theo dõi hạn sử dụng, nhận gợi ý món ăn từ nguyên liệu sẵn có, khám phá video nấu ăn và chia sẻ việc quản lý thực phẩm với gia đình.

Tài liệu này mô tả các màn hình, chức năng và **luồng thao tác theo đúng trình tự sử dụng ứng dụng**. Đây là đặc tả chức năng ở mức sản phẩm, làm cơ sở cho thiết kế UI/UX, xây dựng database và phát triển ứng dụng.

---

## 2. Luồng tổng thể của ứng dụng

1. Người dùng mở FridgeWise.
2. Nếu chưa đăng nhập, người dùng đăng nhập bằng Google, Facebook hoặc Email/Password; có thể sử dụng chức năng quên mật khẩu.
3. Sau khi đăng nhập lần đầu, người dùng đặt tên cho tủ lạnh.
4. Ứng dụng chuyển đến màn hình **Inventory** — màn hình mặc định khi vào ứng dụng.
5. Từ thanh điều hướng dưới, người dùng có thể đi đến **Reels**, **Favorite**, **Scan**, **Family** hoặc **Inventory**.
6. Người dùng có thể thêm thực phẩm bằng cách quét hóa đơn, xác nhận dữ liệu nhận diện, phân loại thực phẩm vào ngăn đông/ngăn mát và hoàn tất để cập nhật Inventory.
7. Từ Inventory, người dùng có thể mở danh sách thực phẩm trong từng ngăn hoặc chuyển sang trang gợi ý món ăn.
8. Người dùng có thể thích/lưu món ăn, xem công thức, khám phá video nấu ăn và chia sẻ việc quản lý tủ lạnh với thành viên gia đình.

### 2.1. Bottom Navigation

Thanh điều hướng dưới có năm mục theo thứ tự **từ trái sang phải**:

1. **Reels** — khám phá video hướng dẫn nấu ăn.
2. **Favorite** — danh sách món ăn yêu thích.
3. **Scan** — quét hóa đơn để thêm thực phẩm; là nút trung tâm/chính.
4. **Family** — quản lý thành viên cùng sử dụng tủ lạnh.
5. **Inventory** — quản lý thực phẩm; là màn hình mặc định khi mở ứng dụng.

---

## 3. Đăng nhập và thiết lập tủ lạnh

### 3.1. Màn hình đăng nhập

Khi mở ứng dụng và chưa có phiên đăng nhập hợp lệ, hiển thị màn hình đăng nhập với các lựa chọn:

- **Continue with Google**.
- **Continue with Facebook**.
- Đăng nhập bằng **Email + Password**.
- **Forgot Password?** để khôi phục mật khẩu khi người dùng quên.

Sau khi đăng nhập thành công, ứng dụng kiểm tra xem người dùng đã thiết lập tủ lạnh hay chưa.

### 3.2. Đặt tên tủ lạnh

- Nếu đây là lần đầu sử dụng và chưa có tủ lạnh, yêu cầu người dùng nhập tên tủ lạnh.
- Lưu tên tủ lạnh để hiển thị trên TopBar của Inventory.
- Sau khi hoàn tất, chuyển người dùng đến **Inventory**.
- Với các lần mở ứng dụng sau, nếu đã đăng nhập và đã thiết lập tủ lạnh, chuyển thẳng đến Inventory.

---

## 4. Inventory — Màn hình quản lý thực phẩm

Inventory là màn hình chính và mặc định sau khi người dùng đăng nhập.

### 4.1. TopBar

TopBar nằm ở đầu màn hình, gồm:

- **Search icon:** mở chức năng tìm kiếm thực phẩm.
- **Tên tủ lạnh:** hiển thị tên tủ lạnh hiện tại.
- **Settings icon:** mở trang cài đặt.

#### 4.1.1. Các mục trong Settings

**Account**
- Xem và quản lý thông tin tài khoản.

**Fridge**
- Xem hoặc thay đổi tên tủ lạnh.

**Preferences**
- **Language:** thay đổi ngôn ngữ ứng dụng.
- **Family:** truy cập phần quản lý gia đình.
- **Notification Settings:** cấu hình thông báo, chẳng hạn thông báo thực phẩm sắp hết hạn.

**Help & Legal**
- **Send App Feedback:** gửi phản hồi về ứng dụng.
- **Data Sources:** xem thông tin về nguồn dữ liệu được ứng dụng sử dụng.

### 4.2. Bốn thẻ thống kê

Ngay dưới TopBar, hiển thị bốn thẻ tóm tắt trạng thái thực phẩm:

- **In Stock:** tổng quan thực phẩm hiện có trong tủ lạnh.
- **Use Today:** thực phẩm nên được sử dụng trong hôm nay.
- **Use Soon:** thực phẩm sắp đến hạn sử dụng.
- **Clean Out:** nhóm thực phẩm cần kiểm tra và dọn khỏi danh sách lưu trữ, theo quy tắc trạng thái của ứng dụng.

Các thẻ giúp người dùng nhanh chóng nắm tình trạng thực phẩm. Khi người dùng chọn một thẻ, có thể mở danh sách thực phẩm thuộc trạng thái tương ứng.

### 4.3. Khu vực ngăn đông và ngăn mát

Bên dưới các thẻ thống kê, màn hình hiển thị hai khung lưu trữ:

1. **Freezer — Ngăn đông:** chứa các thực phẩm đã được phân loại vào ngăn đông.
2. **Fridge — Ngăn mát:** chứa các thực phẩm đã được phân loại vào ngăn mát.

Mỗi khung hiển thị thông tin tóm tắt về các thực phẩm đang được lưu trong ngăn đó. Khi chọn một khung, ứng dụng mở màn hình danh sách chi tiết của ngăn tương ứng.

### 4.4. Nút Nấu ăn

Ở cuối màn hình Inventory có nút **Nấu ăn**.

- Khi nhấn nút, ứng dụng chuyển sang trang gợi ý món ăn.
- Danh sách món ăn được đề xuất dựa trên các nguyên liệu đang có trong tủ lạnh.
- Người dùng có thể chọn món để xem công thức chi tiết.

### 4.5. Xem danh sách thực phẩm trong từng ngăn

Khi người dùng nhấn vào khung **Freezer** hoặc **Fridge**, ứng dụng hiển thị danh sách thực phẩm trong ngăn đó.

Mỗi mục thực phẩm gồm:
- Icon/hình minh họa nguyên liệu.
- Tên thực phẩm.
- Trạng thái hạn sử dụng thể hiện bằng màu.
- Thông tin hạn sử dụng khi cần xem chi tiết.

Quy ước màu trạng thái:

- **Đỏ — Use Today:** cần ưu tiên sử dụng hôm nay.
- **Vàng — Use Soon:** sắp đến hạn, ví dụ còn 1, 2 hoặc 3 ngày.
- **Xám:** hiển thị thông tin ngày hết hạn/ngày còn lại theo giao diện thiết kế.

Người dùng có thể xem từng thực phẩm để kiểm tra thông tin và quyết định sử dụng trước những nguyên liệu sắp hết hạn.

---

## 5. Recipe — Gợi ý món ăn và công thức

### 5.1. Mở trang gợi ý món ăn

Người dùng mở trang này bằng cách nhấn nút **Nấu ăn** trong Inventory.

Ứng dụng hiển thị khoảng **10 món ăn được đề xuất** dựa trên nguyên liệu đang có trong tủ lạnh. Mục tiêu là ưu tiên các món có thể nấu từ nguyên liệu sẵn có và giúp người dùng nhận biết nguyên liệu còn thiếu.

### 5.2. Tìm kiếm và lọc món ăn

Ở đầu trang có icon tìm kiếm. Người dùng có thể tìm hoặc lọc món ăn dựa trên:

- **Category:** danh mục món ăn.
- **Cooking Method:** phương pháp chế biến.

Kết quả hiển thị theo tiêu chí tìm kiếm/lọc đã chọn.

### 5.3. Xem chi tiết Recipe

Khi chọn một món ăn, ứng dụng mở trang chi tiết công thức. Trang này hiển thị đầy đủ thông tin món ăn, bao gồm tên món, khẩu phần, thời gian, độ khó, danh sách nguyên liệu và hướng dẫn chế biến (cùng các thông tin công thức khác nếu có trong dữ liệu).

Ở khu vực phía trên có hai nút:

- **Have:** xem các nguyên liệu người dùng đang có.
- **Need:** xem các nguyên liệu còn thiếu để nấu món đó.

### 5.4. Hành vi của Have và Need

- Khi nhấn **Have**, một **Bottom Sheet** trượt từ dưới màn hình lên, hiển thị danh sách nguyên liệu đã có.
- Khi nhấn **Need**, một **Bottom Sheet** trượt từ dưới màn hình lên, hiển thị danh sách nguyên liệu còn thiếu.
- Người dùng có thể đóng Bottom Sheet để quay lại nội dung công thức.

Hai nút giúp người dùng nhanh chóng kiểm tra khả năng nấu món ăn mà không cần rời khỏi trang Recipe.

---

## 6. Reels — Khám phá video nấu ăn

Reels là khu vực hiển thị các video hướng dẫn nấu ăn ngẫu nhiên để người dùng khám phá món mới.

### 6.1. Nội dung mỗi Reel

Mỗi video hiển thị:
- Video hướng dẫn nấu ăn.
- Tên món ăn.
- Khẩu phần (**Servings**).
- Thời gian chế biến (**Time**).
- Độ khó (**Difficulty**).
- Lượng lượt thích (**Like count**).

### 6.2. Các thao tác trên Reel

Người dùng có thể:

- **Like:** thích món ăn/video; trạng thái thích được cập nhật và món có thể xuất hiện trong Favorite.
- **Save to Favorite:** lưu món ăn vào danh sách yêu thích.
- **Nấu ngay:** mở trang Recipe tương ứng để xem nguyên liệu và hướng dẫn nấu.

Reels tập trung vào việc khám phá nhanh; khi muốn nấu một món cụ thể, người dùng chuyển từ video sang trang công thức.

---

## 7. Favorite — Món ăn yêu thích

Favorite lưu các món ăn mà người dùng đã thích hoặc lưu từ Reels.

Màn hình hiển thị danh sách món ăn yêu thích. Khi chọn một món, ứng dụng mở trang Recipe tương ứng để người dùng xem lại thông tin, nguyên liệu và cách chế biến.

Nếu người dùng bỏ thích hoặc bỏ lưu một món, trạng thái món đó trong danh sách Favorite được cập nhật tương ứng.

---

## 8. Scan — Quét hóa đơn và thêm thực phẩm

Scan là nút trung tâm trên Bottom Navigation. Chức năng này giúp người dùng nhận diện thực phẩm từ ảnh hóa đơn bằng OCR, kiểm tra dữ liệu nhận diện, sau đó phân loại từng thực phẩm vào ngăn đông hoặc ngăn mát.

### 8.1. Bước 1 — Quét hóa đơn

1. Người dùng nhấn **Scan**.
2. Ứng dụng mở luồng quét hóa đơn để người dùng chụp ảnh hoặc chọn ảnh hóa đơn.
3. Hệ thống sử dụng **OCR** để đọc nội dung trên hóa đơn và nhận diện các mặt hàng/thực phẩm.
4. Sau khi xử lý, ứng dụng chuyển sang màn hình xác nhận dữ liệu trước khi thêm vào Inventory.

Kết quả OCR cần được người dùng kiểm tra vì tên mặt hàng, số lượng hoặc thông tin trên hóa đơn có thể được nhận diện chưa chính xác.

### 8.2. Bước 2 — Màn hình xác nhận thực phẩm

Màn hình này hiển thị danh sách thực phẩm được nhận diện. Mỗi mục gồm:

- Icon/hình minh họa nguyên liệu.
- Tên nguyên liệu.
- Số lượng.
- Ngày mua.
- Ngày hết hạn.
- Icon **bút chì** để chỉnh sửa thông tin.

Người dùng có thể chỉnh sửa các trường dữ liệu nếu kết quả OCR không chính xác hoặc cần bổ sung thông tin.

Ở cuối màn hình có hai nút:

- **Thêm thực phẩm:** thêm thủ công một mục thực phẩm vào danh sách đang xác nhận.
- **Xác nhận:** xác nhận danh sách thực phẩm sau khi đã kiểm tra và chỉnh sửa.

Khi nhấn **Xác nhận**, ứng dụng chuyển sang bước phân loại thực phẩm vào ngăn đông/ngăn mát. Chỉ sau khi hoàn tất luồng phân loại, danh sách mới được đưa vào Inventory theo vị trí lưu trữ đã chọn.

### 8.3. Bước 3 — Phân loại vào ngăn đông/ngăn mát

Màn hình phân loại gồm hai khu vực:

- **Freezer — Ngăn đông.**
- **Fridge — Ngăn mát.**

Các thực phẩm đã xác nhận ở bước trước lần lượt xuất hiện dưới dạng mục đang chờ phân loại. Người dùng kéo và thả (**drag and drop**) từng mục vào ngăn phù hợp.

Luồng thao tác:
1. Hiển thị một hoặc nhiều thực phẩm đang chờ phân loại.
2. Người dùng kéo một thực phẩm vào Freezer hoặc Fridge.
3. Thực phẩm được đặt trong khu vực tương ứng.
4. Lặp lại cho đến khi đã phân loại xong danh sách.

Ở cuối màn hình có nút **Hoàn tất**. Khi người dùng nhấn nút này, ứng dụng lưu các thực phẩm cùng thông tin và vị trí lưu trữ đã chọn, sau đó quay về **Inventory** để hiển thị dữ liệu mới.

---

## 9. Family — Quản lý tủ lạnh gia đình

Family cho phép người dùng thêm các thành viên trong gia đình để cùng quản lý thực phẩm trong một tủ lạnh.

Các thao tác chính:
- Mở mục **Family** từ Bottom Navigation hoặc từ Settings.
- Thêm/mời thành viên gia đình.
- Quản lý danh sách thành viên đã tham gia.

Các thành viên trong cùng gia đình có thể cùng theo dõi và cập nhật dữ liệu thực phẩm dùng chung, tùy theo quyền truy cập được ứng dụng hỗ trợ.

---

## 10. Luồng chính: từ mua thực phẩm đến nấu ăn

Ví dụ về luồng sử dụng xuyên suốt:

1. Người dùng mở FridgeWise và đăng nhập.
2. Người dùng đặt tên tủ lạnh nếu đây là lần đầu sử dụng.
3. Ứng dụng mở Inventory.
4. Sau khi mua thực phẩm, người dùng nhấn Scan và quét hóa đơn.
5. OCR nhận diện các mặt hàng; người dùng kiểm tra, chỉnh sửa, bổ sung và xác nhận danh sách.
6. Người dùng kéo thả từng thực phẩm vào ngăn đông hoặc ngăn mát.
7. Người dùng nhấn Hoàn tất; ứng dụng cập nhật Inventory.
8. Inventory hiển thị trạng thái thực phẩm và các nguyên liệu sắp hết hạn.
9. Người dùng nhấn Nấu ăn để xem khoảng 10 món được đề xuất từ nguyên liệu hiện có.
10. Người dùng chọn món, xem Recipe và kiểm tra Have/Need.
11. Người dùng cũng có thể khám phá video ở Reels, thích/lưu món vào Favorite và mở Recipe bằng nút Nấu ngay.
12. Nếu cần quản lý chung, người dùng thêm thành viên tại Family.

---

## 11. Tóm tắt màn hình

| Màn hình | Mục đích | Thành phần/chức năng chính |
|---|---|---|
| Login | Xác thực người dùng | Google, Facebook, Email/Password, Forgot Password |
| Set Up Fridge | Thiết lập ban đầu | Nhập tên tủ lạnh |
| Inventory | Tổng quan thực phẩm | TopBar, Settings, 4 thẻ trạng thái, Freezer, Fridge, Nấu ăn |
| Storage List | Xem thực phẩm trong một ngăn | Icon, tên, màu trạng thái, hạn sử dụng |
| Recipe List | Gợi ý món ăn | Khoảng 10 món, tìm kiếm Category và Cooking Method |
| Recipe Detail | Xem công thức | Thông tin món, nguyên liệu, hướng dẫn, Have, Need |
| Have Bottom Sheet | Xem nguyên liệu sẵn có | Danh sách nguyên liệu đã có |
| Need Bottom Sheet | Xem nguyên liệu còn thiếu | Danh sách nguyên liệu cần bổ sung |
| Reels | Khám phá video | Video, tên món, khẩu phần, thời gian, độ khó, lượt thích, Like, Favorite, Nấu ngay |
| Favorite | Xem món đã lưu | Danh sách món yêu thích và mở Recipe |
| Scan / OCR | Nhận diện hóa đơn | Chụp/chọn ảnh, OCR |
| Confirm Ingredients | Kiểm tra dữ liệu OCR | Icon, tên, số lượng, ngày mua, hạn sử dụng, chỉnh sửa, Thêm thực phẩm, Xác nhận |
| Assign Storage | Phân loại thực phẩm | Drag and drop vào Freezer/Fridge, Hoàn tất |
| Family | Quản lý gia đình | Thêm/mời thành viên, danh sách thành viên |
| Settings | Cài đặt ứng dụng | Account, Fridge, Language, Family, Notification Settings, Feedback, Data Sources |

---

## 12. Các quy tắc luồng cần giữ nhất quán

- **Inventory là màn hình mặc định** sau khi đăng nhập và thiết lập tủ lạnh thành công.
- Bottom Navigation luôn theo thứ tự: **Reels → Favorite → Scan → Family → Inventory**.
- **Scan không kết thúc ngay sau OCR**: người dùng phải xác nhận danh sách, phân loại vào ngăn đông/ngăn mát, rồi nhấn Hoàn tất mới quay về Inventory.
- Dữ liệu thực phẩm trong Inventory cần gắn với ngăn lưu trữ tương ứng để hiển thị đúng trong Freezer hoặc Fridge.
- Màu trạng thái hạn sử dụng phải nhất quán: đỏ = Use Today; vàng = Use Soon (1–3 ngày); xám = hiển thị thông tin hạn sử dụng.
- **Nấu ăn** từ Inventory mở trang gợi ý món ăn; chọn món mở Recipe Detail.
- **Nấu ngay** từ Reels mở Recipe Detail của món đang xem.
- **Have** và **Need** mở Bottom Sheet từ dưới lên, không điều hướng người dùng khỏi trang Recipe.
- Like/lưu món từ Reels phải được phản ánh trong Favorite theo trạng thái đã lưu.
- Family là nơi quản lý thành viên cùng chia sẻ việc quản lý tủ lạnh.
