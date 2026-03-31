package controller;

import model.Direction;
import model.GazeState;
import org.bytedeco.javacv.*;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Head Tracker nâng cấp: Thêm khoảng dừng 0.5s sau khi vuốt để tạo cảm giác tự nhiên.
 */
public class EyeTracker {

    // ── Cấu hình độ nhạy ──────────────────────────────────────────
    public static final long   DWELL_MS      = 200L;
    private static final int   FRAME_SKIP    = 1;     // Xử lý mọi khung hình để mượt nhất
    private static final float H_THRESHOLD   = 0.03f;
    private static final float V_THRESHOLD   = 0.03f;

    // ── Trạng thái (State) ────────────────────────────────────────
    private volatile Direction currentGaze   = null;
    private volatile Direction dwellDir      = null;
    private volatile long      dwellStartMs  = 0;
    private volatile float     dwellProgress = 0f;
    private volatile boolean   running       = false;
    private volatile boolean   faceDetected  = false;
    private volatile boolean   uiPending     = false;

    private volatile boolean   waitForNeutral = false;
    private volatile long      lockoutUntil   = 0; // Thời điểm hết thời gian khóa 0.5s

    // ── Hiệu chỉnh (Calibration) ──────────────────────────────────
    private float baselineH = 0.5f;
    private float baselineV = 0.5f;
    private boolean calibrated = false;
    private int calibFrames = 0;
    private float calibSumH = 0, calibSumV = 0;
    private static final int CALIB_FRAMES = 30;

    // ── Callbacks ─────────────────────────────────────────────────
    private Consumer<Direction>          onMove;
    private Consumer<GazeState>          onGazeUpdate;
    private Consumer<javafx.scene.image.Image> onFrame;

    private OpenCVFrameGrabber grabber;
    private CascadeClassifier  faceCascade;
    private OpenCVFrameConverter.ToMat conv = new OpenCVFrameConverter.ToMat();
    private JavaFXFrameConverter        fxConv = new JavaFXFrameConverter();
    private ScheduledExecutorService executor;
    private int frameCount = 0;

    public void setOnMove(Consumer<Direction> cb)        { this.onMove = cb; }
    public void setOnGazeUpdate(Consumer<GazeState> cb)  { this.onGazeUpdate = cb; }
    public void setOnFrame(Consumer<javafx.scene.image.Image> cb) { this.onFrame = cb; }

    public void start() throws Exception {
        loadCascades();
        grabber = new OpenCVFrameGrabber(0); // Số 0 là camera mặc định
        grabber.setImageWidth(320);
        grabber.setImageHeight(240);

        try {
            grabber.start(); // Cố gắng bật camera

            // Test thử xem camera có lấy được khung hình nào không
            org.bytedeco.javacv.Frame testFrame = grabber.grab();
            if (testFrame == null) {
                grabber.stop();
                throw new CameraNotFoundException("Webcam đã bật nhưng không thu được hình ảnh!");
            }
        } catch (Exception e) {
            // NẾU BẬT THẤT BẠI -> NÉM RA CUSTOM EXCEPTION
            throw new CameraNotFoundException("Không tìm thấy Webcam hoặc Camera đang bị ứng dụng khác (Zalo, Chrome...) sử dụng!");
        }

        running = true;
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "HeadTracker");
            t.setDaemon(true);
            return t;
        });
        executor.scheduleAtFixedRate(this::processFrame, 0, 33, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        running = false;
        if (executor != null) executor.shutdownNow();
        try { if (grabber != null) grabber.stop(); } catch (Exception ignored) {}
    }

    public void resetCalibration() {
        calibrated = false;
        calibFrames = 0;
        calibSumH = 0;
        calibSumV = 0;
        waitForNeutral = false;
        lockoutUntil = 0;
    }

    private void processFrame() {
        if (!running) return;
        try {
            Frame frame = grabber.grab();
            if (frame == null || frame.image == null) return;
            frameCount++;
            Mat mat = conv.convert(frame);
            if (mat == null || mat.empty()) return;

            Mat mirrored = new Mat();
            opencv_core.flip(mat, mirrored, 1);

            if (frameCount % FRAME_SKIP != 0) { mirrored.release(); mat.release(); return; }

            if (onFrame != null && !uiPending) {
                uiPending = true;
                javafx.scene.image.Image img = fxConv.convert(conv.convert(mirrored));
                if (img != null) {
                    onFrame.accept(img);
                    javafx.application.Platform.runLater(() -> uiPending = false);
                } else { uiPending = false; }
            }

            Mat gray = new Mat();
            opencv_imgproc.cvtColor(mirrored, gray, opencv_imgproc.COLOR_BGR2GRAY);
            opencv_imgproc.equalizeHist(gray, gray);

            RectVector faces = new RectVector();
            faceCascade.detectMultiScale(gray, faces, 1.1, 2, 0, new Size(80, 80), new Size(gray.cols(), gray.rows()));

            if (faces.size() == 0) {
                faceDetected = false;
                setGaze(null);
                gray.release(); mirrored.release(); mat.release();
                return;
            }
            faceDetected = true;
            Rect face = largestRect(faces);
            float hRatio = (face.x() + face.width() / 2.0f) / gray.cols();
            float vRatio = (face.y() + face.height() / 2.0f) / gray.rows();

            if (!calibrated) {
                calibSumH += hRatio; calibSumV += vRatio; calibFrames++;
                if (calibFrames >= CALIB_FRAMES) {
                    baselineH = calibSumH / CALIB_FRAMES;
                    baselineV = calibSumV / CALIB_FRAMES;
                    calibrated = true;
                }
                publishState(null, (float) calibFrames / CALIB_FRAMES);
            } else {
                setGaze(classify(hRatio - baselineH, vRatio - baselineV));
            }
            gray.release(); mirrored.release(); mat.release();
        } catch (Exception e) { System.err.println("[HeadTracker] Lỗi: " + e.getMessage()); }
    }

    private Direction classify(float dH, float dV) {
        boolean strongH = Math.abs(dH) > Math.abs(dV) * 1.1f;
        if (strongH) {
            if (dH > H_THRESHOLD)  return Direction.RIGHT;
            if (dH < -H_THRESHOLD) return Direction.LEFT;
        } else {
            if (dV > V_THRESHOLD)  return Direction.DOWN;
            if (dV < -V_THRESHOLD) return Direction.UP;
        }
        return null;
    }

    private void setGaze(Direction newDir) {
        long now = System.currentTimeMillis();
        currentGaze = newDir;

        // 1. KIỂM TRA THỜI GIAN KHÓA (LOCKOUT)
        // Nếu vừa mới vuốt xong và chưa hết 0.5s, dừng xử lý tại đây.
        if (now < lockoutUntil) {
            publishState(newDir, 1f); // Giữ thanh dwell đầy để báo hiệu đang nghỉ
            return;
        }

        // 2. KIỂM TRA YÊU CẦU VỀ GIỮA (NEUTRAL RESET)
        if (waitForNeutral) {
            if (newDir == null) {
                waitForNeutral = false; // Đã về giữa thành công, cho phép nhận lệnh mới
                dwellDir = null;
            } else {
                publishState(newDir, 1f);
                return;
            }
        }

        // 3. LOGIC TÍNH THỜI GIAN GIỮ (DWELL)
        if (newDir == null || newDir != dwellDir) {
            dwellDir = newDir;
            dwellStartMs = now;
            dwellProgress = 0f;
            publishState(newDir, 0f);
            return;
        }

        long elapsed = now - dwellStartMs;
        dwellProgress = Math.min((float) elapsed / DWELL_MS, 1f);
        publishState(newDir, dwellProgress);

        // 4. KÍCH HOẠT VÀ ĐẶT THỜI GIAN KHÓA
        if (elapsed >= DWELL_MS) {
            Direction triggered = dwellDir;

            // ĐẶT KHOẢNG NGHỈ 500ms
            lockoutUntil = now + 500;
            waitForNeutral = true;

            if (onMove != null) javafx.application.Platform.runLater(() -> onMove.accept(triggered));
        }
    }

    private void publishState(Direction dir, float progress) {
        if (onGazeUpdate == null) return;
        GazeState state = new GazeState(dir, progress, calibrated,
                calibrated ? 1f : (float) calibFrames / CALIB_FRAMES, faceDetected);
        javafx.application.Platform.runLater(() -> onGazeUpdate.accept(state));
    }

    private void loadCascades() throws Exception {
        faceCascade = loadCascade("haarcascade_frontalface_default.xml");
    }

    private CascadeClassifier loadCascade(String name) throws Exception {
        java.io.File cascadeFile = new java.io.File(name);
        if (!cascadeFile.exists()) throw new Exception("Không tìm thấy file: " + cascadeFile.getAbsolutePath());
        CascadeClassifier cc = new CascadeClassifier(cascadeFile.getAbsolutePath());
        if (cc.empty()) throw new Exception("File lỗi: " + name);
        return cc;
    }

    private Rect largestRect(RectVector rects) {
        Rect best = rects.get(0);
        for (int i = 1; i < rects.size(); i++) {
            Rect r = rects.get(i);
            if (r.width() * r.height() > best.width() * best.height()) best = r;
        }
        return best;
    }
}