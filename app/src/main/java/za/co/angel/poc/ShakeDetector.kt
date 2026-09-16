package za.co.angel.poc

import kotlin.math.sqrt

/**
 * Detects two strong acceleration peaks that reverse direction within a short window.
 * Time values are supplied by the sensor event so this class is deterministic and testable.
 */
class ShakeDetector(
    private val peakThreshold: Float = 10.5f,
    private val minimumPeakGapMs: Long = 120,
    private val gestureWindowMs: Long = 1_200,
    private val cooldownMs: Long = 3_000,
    private val reversalDotProduct: Float = -0.25f,
    private val onShake: () -> Unit
) {
    private var firstPeak: Peak? = null
    private var lastAcceptedPeakMs = Long.MIN_VALUE
    private var lastGestureMs = Long.MIN_VALUE

    fun addSample(x: Float, y: Float, z: Float, timestampMs: Long) {
        val magnitude = sqrt(x * x + y * y + z * z)
        if (magnitude < peakThreshold) return
        if (elapsed(timestampMs, lastGestureMs) < cooldownMs) return
        if (elapsed(timestampMs, lastAcceptedPeakMs) < minimumPeakGapMs) return

        val current = Peak(x / magnitude, y / magnitude, z / magnitude, timestampMs)
        lastAcceptedPeakMs = timestampMs

        val previous = firstPeak
        if (previous == null || elapsed(timestampMs, previous.timestampMs) > gestureWindowMs) {
            firstPeak = current
            return
        }

        val directionSimilarity =
            previous.x * current.x + previous.y * current.y + previous.z * current.z

        if (directionSimilarity <= reversalDotProduct) {
            lastGestureMs = timestampMs
            firstPeak = null
            onShake()
        } else {
            firstPeak = current
        }
    }

    fun reset() {
        firstPeak = null
        lastAcceptedPeakMs = Long.MIN_VALUE
        lastGestureMs = Long.MIN_VALUE
    }

    private fun elapsed(now: Long, then: Long): Long =
        if (then == Long.MIN_VALUE) Long.MAX_VALUE else now - then

    private data class Peak(
        val x: Float,
        val y: Float,
        val z: Float,
        val timestampMs: Long
    )
}

