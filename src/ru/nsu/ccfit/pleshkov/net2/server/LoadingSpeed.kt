package ru.nsu.ccfit.pleshkov.net2.server

import ru.nsu.ccfit.pleshkov.net2.common.asLoadingSpeed

data class LoadingSpeed(private val average: Long, private val current: Long) {
    override fun toString(): String {
        return "average is ${average.asLoadingSpeed()}, current is ${current.asLoadingSpeed()}"
    }
}