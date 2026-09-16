package za.co.angel.poc

import org.junit.Assert.assertEquals
import org.junit.Test

class ShakeDetectorTest {
    @Test
    fun oppositePeaksWithinWindowTriggerGesture() {
        var triggers = 0
        val detector = ShakeDetector(onShake = { triggers++ })

        detector.addSample(12f, 0f, 0f, 1_000)
        detector.addSample(-12f, 0f, 0f, 1_250)

        assertEquals(1, triggers)
    }

    @Test
    fun sameDirectionPeaksDoNotTriggerGesture() {
        var triggers = 0
        val detector = ShakeDetector(onShake = { triggers++ })

        detector.addSample(12f, 0f, 0f, 1_000)
        detector.addSample(13f, 1f, 0f, 1_250)

        assertEquals(0, triggers)
    }

    @Test
    fun weakMovementDoesNotTriggerGesture() {
        var triggers = 0
        val detector = ShakeDetector(onShake = { triggers++ })

        detector.addSample(3f, 2f, 1f, 1_000)
        detector.addSample(-3f, -2f, -1f, 1_250)

        assertEquals(0, triggers)
    }

    @Test
    fun peaksOutsideGestureWindowDoNotTriggerGesture() {
        var triggers = 0
        val detector = ShakeDetector(onShake = { triggers++ })

        detector.addSample(12f, 0f, 0f, 1_000)
        detector.addSample(-12f, 0f, 0f, 2_500)

        assertEquals(0, triggers)
    }
}
