package ru.nsu.ccfit.pleshkov.net2.client

import ru.nsu.ccfit.pleshkov.net2.common.*
import java.io.*
import java.net.InetAddress
import java.net.Socket
import java.net.UnknownHostException

private const val BUFFER_SIZE: Int = 64 * 1024

fun main(args: Array<String>) {
    if (args.size < 3) {
        println("Usage: java -jar net2-client.jar PATH IP PORT")
        return
    }
    val ipString = args[1]
    val errorMessage = try {
        val file = File(args[0])
        validateFile(file)
        val ip = InetAddress.getByName(ipString)
        val port = getPort(args[2])
        uploadingRoutine(Socket(ip, port), file)
        null
    } catch (e: InitializationException) {
        e.message
    } catch (e: UnknownHostException) {
        "Couldn't find host at $ipString"
    } catch (e: IOException) {
        "Something went wrong: ${e.message}"
    }
    if(errorMessage != null) {
        println(errorMessage)
    }
}

private fun uploadingRoutine(socket: Socket, file: File) {
    DataOutputStream(socket.getOutputStream()).use { outputStream ->
        outputStream.writeInt(BEGIN)
        val nameBytes = file.name.toByteArray() //UTF-8
        outputStream.writeShort(nameBytes.size)
        outputStream.write(nameBytes)
        val size = file.length()
        outputStream.writeLong(size)
        FileInputStream(file).use { inputStream ->
            val buffer = ByteArray(BUFFER_SIZE)
            var writtenBytes = 0
            while (writtenBytes < size) {
                val rest = size - writtenBytes
                val bytesToRead: Int = if(rest < BUFFER_SIZE) rest.toInt() else BUFFER_SIZE
                val readBytes = inputStream.read(buffer, 0, bytesToRead)
                outputStream.write(buffer, 0, readBytes)
                writtenBytes += readBytes
            }
        }
        val message = DataInputStream(socket.getInputStream()).readInt()
        println(if (message == SUCCESS) "SUCCESS" else "FAILURE")
    }
}

private fun validateFile(file: File) {
    val name = file.name
    val message = when {
        name.isNullOrEmpty() -> "File doesn't have a name!"
        !file.exists() -> "Error: file $name doesn't exist"
        file.isDirectory -> "$name is a directory"
        !file.isFile -> "File $name is not a normal file"
        else -> null
    }
    if(message != null) {
        throw InvalidFileException(message)
    }
}

private class InvalidFileException(message: String) : InitializationException(message)