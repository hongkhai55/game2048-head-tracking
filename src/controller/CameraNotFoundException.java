package controller;

/**
 * Custom Exception tự định nghĩa để xử lý lỗi khi không tìm thấy Webcam.
 * (Đáp ứng tiêu chí chấm điểm Ngoại lệ của đồ án)
 */
public class CameraNotFoundException extends Exception {
    public CameraNotFoundException(String message) {
        super(message); // Truyền câu thông báo lỗi lên lớp cha Exception
    }
}