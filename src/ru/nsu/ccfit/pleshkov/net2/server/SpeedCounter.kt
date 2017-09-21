package ru.nsu.ccfit.pleshkov.net2.server

private const val TIME_TO_TEST_SPEED = 3000
private const val MILLIS_IN_SECOND = 1000

class SpeedCounter {
    private val startTime = System.currentTimeMillis()
    private var currentWrittenBytes = 0L
    private var lastSpeedTestTime = System.currentTimeMillis()

    fun updateBytes(bytes: Int) {
        currentWrittenBytes += bytes
    }

    fun isReadyToTest() : Boolean {
        val passedTime = System.currentTimeMillis() - lastSpeedTestTime
        return (passedTime > TIME_TO_TEST_SPEED)
    }

    fun getSpeed(writtenBytes: Long) : LoadingSpeed {
        val currentTime = System.currentTimeMillis()
        val average = writtenBytes * MILLIS_IN_SECOND / (currentTime - startTime)
        val current = currentWrittenBytes * MILLIS_IN_SECOND / (currentTime - lastSpeedTestTime)
        lastSpeedTestTime = currentTime
        currentWrittenBytes = 0
        return LoadingSpeed(average, current)
    }
}