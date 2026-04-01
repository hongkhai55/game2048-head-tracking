# 🎓 2048 Head Control — Java Edition
> **Bài tập lớn cuối kỳ môn Lập trình Java**
> **Trường Đại học Sư phạm Đà Nẵng**
>
> Game 2048 điều khiển rảnh tay bằng Webcam, sử dụng công nghệ **JavaFX** và **OpenCV/JavaCV**.

## 👥 Thông tin nhóm (Lớp 25CNTT3)
| STT | Họ và Tên                    | Mã Sinh Viên | Vai trò / Nhiệm vụ | Link GitHub Cá Nhân |
|---|------------------------------|--------------|---|---|
| 1 | Trần Hồng Khải (Nhóm trưởng) | 3120225068   | Quản lý tiến độ dự án, Viết báo cáo, Hỗ trợ test lỗi (QA/QC) | [GitHub](link_cua_nhom_truong) |
| 2 | Đào Nhật Minh (Thành viên)   | [Mã SV]      | Code Controller, Thiết kế kiến trúc MVC, Giao diện JavaFX, Thuật toán 2048, Tích hợp AI OpenCV, File I/O, Exception | [GitHub](link_cua_khai) |

## 📝 Giới thiệu dự án (Description)
Dự án là phiên bản nâng cấp sáng tạo của tựa game giải đố huyền thoại 2048. Thay vì điều khiển bằng phím bấm truyền thống, ứng dụng tích hợp công nghệ Computer Vision (AI) để theo dõi chuyển động khuôn mặt theo thời gian thực. Người chơi hoàn toàn có thể chơi game mà không cần chạm tay vào thiết bị, chỉ cần gật, ngước, hoặc nghiêng mặt để vuốt các ô số.

## ✨ Các chức năng chính (Features)
- [x] **Logic Game:** Hỗ trợ tính điểm, thuật toán sinh số ngẫu nhiên (2, 4), và luật gộp khối ma trận 4x4 chuẩn xác.
- [x] **Trí tuệ Nhân tạo (OpenCV):** Sử dụng Haar Cascades để nhận diện và theo dõi chuyển động đầu (Head Tracking) qua Webcam với độ trễ cực thấp.
- [x] **Lưu trữ dữ liệu (File I/O):** Tự động lưu kỷ lục (Highscore) xuống file `highscore.txt` và tự động tải lại khi khởi động game.
- [x] **Giao diện hiện đại (GUI):** Thiết kế Dark Mode chuyên nghiệp bằng JavaFX, tối ưu UI/UX với các hiệu ứng Animation (Fade, Scale) mượt mà.
- [x] **Bảo vệ người chơi (Neutral Zone):** Tích hợp logic khóa 0.5s sau mỗi lần vuốt, bắt buộc người chơi đưa đầu về giữa để chống trượt phím liên tục ngoài ý muốn.
- [x] **Bắt lỗi chặt chẽ (Exceptions):** Tự định nghĩa `CameraNotFoundException` và bọc try-catch, hiển thị Popup cảnh báo an toàn mà không làm crash game.

## 💻 Công nghệ & Thư viện sử dụng
* **Ngôn ngữ:** Java (JDK 17+)
* **Giao diện:** JavaFX (Thay thế Swing để tối ưu Animation đồ họa)
* **Lưu trữ:** File Text (I/O Stream)
* **Thư viện AI:** JavaCV / OpenCV (Xử lý nhận diện khuôn mặt ngoại tuyến)
* **Công cụ khác:** Maven, Git, GitHub, IntelliJ IDEA

## 📂 Cấu trúc dự án (Mô hình MVC)
Dự án tuân thủ nghiêm ngặt mô hình **MVC (Model - View - Controller)** theo yêu cầu của đồ án:
```text
src/
├── model/
│   ├── GameBoard.java               # Logic 2048, thuật toán merge, xử lý File I/O score
│   ├── Direction.java               # Enum UP/DOWN/LEFT/RIGHT
│   ├── GazeState.java               # Dữ liệu trạng thái tracking
│   └── TileNode.java                # Đối tượng ô số 
├── view/
│   └── GameView.java                # Toàn bộ UI layout, Popup Alert
├── controller/
│   ├── GameApplication.java         # Khởi chạy Application JavaFX
│   ├── EyeTracker.java              # AI OpenCV Webcam + Head Tracking
│   └── CameraNotFoundException.java # Custom Exception bắt lỗi camera
├── Main.java                        # Entry point
├── style.css                        # Giao diện Dark theme
├── haarcascade_frontalface_default.xml # File Model AI nhận diện khuôn mặt
└── highscore.txt                    # File lưu trữ điểm số cao nhất
