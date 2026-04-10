# 🎓 2048 Head Control — Java Edition
> **Bài tập lớn cuối kỳ môn Lập trình Java**
> **Trường Đại học Sư phạm Đà Nẵng**
>
> Game 2048 điều khiển rảnh tay bằng Webcam, sử dụng công nghệ **JavaFX** và **OpenCV/JavaCV**.

## 👥 Thông tin nhóm (Lớp 25CNTT3)
| STT | Họ và Tên                    | Mã Sinh Viên | Vai trò / Nhiệm vụ | Link GitHub Cá Nhân |
|---|------------------------------|--------------|---|---|
| 1 | Trần Hồng Khải (Nhóm trưởng) | 3120225068   | Quản lý tiến độ dự án, Viết báo cáo, Hỗ trợ test lỗi (QA/QC) | [(https://github.com/hongkhai55)] |
| 2 | Đào Nhật Minh (Thành viên)   | [Mã SV]      | Code Controller, Thiết kế kiến trúc MVC, Giao diện JavaFX, Thuật toán 2048, Tích hợp AI OpenCV, File I/O, Exception | [GitHub](link_cua_minh) |

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
Gameplay:
b1: nhận diện khuôn mặt:
<img width="992" height="878" alt="Screenshot 2026-04-01 105248" src="https://github.com/user-attachments/assets/f4aaa48c-1f2a-4038-a103-f4d273351f62" />
b2: xoay theo các hướng:
<img width="1493" height="1313" alt="Screenshot 2026-04-01 105351" src="https://github.com/user-attachments/assets/d95bf818-78a6-4bab-8c38-157e258694ac" />
<img width="1492" height="1311" alt="Screenshot 2026-04-01 105402" src="https://github.com/user-attachments/assets/5da43585-2a90-4850-8a2a-8a598ff929e7" />
<img width="1495" height="1317" alt="Screenshot 2026-04-01 105423" src="https://github.com/user-attachments/assets/fcd09550-b21a-4822-a48f-1a6f48288d91" />
<img width="1496" height="1314" alt="Screenshot 2026-04-01 105416" src="https://github.com/user-attachments/assets/34ed53c9-ef1c-4671-83d8-5cba6618156e" />
<img width="1489" height="1307" alt="Screenshot 2026-04-01 105409" src="https://github.com/user-attachments/assets/22a2b48e-e74f-4107-89fd-b5766331292f" />
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

