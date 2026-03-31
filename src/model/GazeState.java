package model;


/**
 * Snapshot of eye-tracker state published to the UI each frame.
 */
public record GazeState(
        Direction gazeDirection,   // current detected direction (null = center / unknown)
        float     dwellProgress,   // 0.0 – 1.0 fill for the dwell bar
        boolean   calibrated,      // true once baseline calibration is done
        float     calibProgress,   // 0.0 – 1.0 during calibration
        boolean   faceDetected     // true if a face is visible this frame
) {}
