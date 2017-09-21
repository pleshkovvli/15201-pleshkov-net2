package ru.nsu.ccfit.pleshkov.net2.common

open class InitializationException(message: String) : Exception(message)
class InvalidPortException(message: String) : InitializationException(message)
