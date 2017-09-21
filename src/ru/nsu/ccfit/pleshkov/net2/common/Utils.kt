package ru.nsu.ccfit.pleshkov.net2.common

private const val NEXT_NB: Long = 1024
private const val KB: Long = NEXT_NB * 1
private const val MB: Long = NEXT_NB * KB
private const val GB: Long = NEXT_NB * MB

private fun Double.format(n: Int) = java.lang.String.format("%.${n}f", this)

fun Long.asLoadingSpeed() : String {
    return when(this) {
        in 0..KB -> "${this}B/s"
        in (KB + 1)..MB -> {
            val kbSpeed = this.toDouble() / KB
            "${kbSpeed.format(2)}KB/s"
        }
        in (MB + 1)..GB -> {
            val mbSpeed = this.toDouble() / MB
            "${mbSpeed.format(2)}MB/s"
        }
        else -> {
            val gbSpeed = this.toDouble() / GB
            "${gbSpeed.format(2)}GB/s"
        }
    }
}

fun getPort(portString: String) : Int {
    val port = portString.toIntOrNull()
    if(port == null || port !in 0..65535) {
        throw InvalidPortException("$portString is not a port number")
    }
    return port
}
