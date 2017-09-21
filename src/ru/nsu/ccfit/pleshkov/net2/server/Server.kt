package ru.nsu.ccfit.pleshkov.net2.server

import ru.nsu.ccfit.pleshkov.net2.common.*
import java.io.*
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket

private const val BUFFER_SIZE: Int = 64 * 1024
private const val UPLOADS_DIR_NAME: String = "uploads"

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Usage: java -jar net2-server.jar PORT")
        return
    }
    try {
        val port = getPort(args[0])
        initUploads()
        startServer(port)
    } catch (e: InitializationException) {
        println(e.message)
    } catch (e: IOException) {
        println("Something went wrong: ${e.message}")
    }
}

private fun startServer(port: Int) {
    ServerSocket(port).use { serverSocket ->
        println("PORT: $port")
        while (!serverSocket.isClosed) {
            val socket: Socket = serverSocket.accept()
            Thread({
                try {
                    downloadingRoutine(socket)
                } catch (e: IOException) {
                    println("Something went wrong: ${e.message}")
                }
            }).start()
        }
    }
}

private fun downloadingRoutine(socket: Socket) {
    socket.keepAlive = true
    val clientIP = socket.inetAddress
    DataInputStream(socket.getInputStream()).use { inputStream ->
        val code = inputStream.readInt()
        when (code) {
            BEGIN -> {
                val message: Int = try {
                    onBegin(inputStream)
                    SUCCESS
                } catch (e: Exception) {    //Kotlin does not have multi-catch
                    when (e) {
                        is DownloadException, is IOException -> {
                            println("Failed to write file from $clientIP: ${e.message}")
                            FAILURE
                        }
                        else -> throw e
                    }
                }
                DataOutputStream(socket.getOutputStream()).writeInt(message)
                handleConnectionClosing(socket, inputStream, clientIP)
            }
            else -> {
                println("Unknown code: $code")
            }
        }
    }
}

private fun handleConnectionClosing(socket: Socket, inputStream: DataInputStream, clientIP: InetAddress?){
    socket.shutdownOutput()
    socket.soTimeout = 3000
    try {
        inputStream.readInt()   //Waiting for client to close connection
    } catch (e: EOFException) {
        println("Connection on $clientIP closed")
    }
}

private fun onBegin(inputStream: DataInputStream) {
    val fileName = getFileName(inputStream)
    val size = inputStream.readLong()
    if (size <= 0) {
        throw DownloadException("Bad size of the file = $size")
    }
    val file = initFile(fileName)
    val speedCounter = SpeedCounter()
    FileOutputStream(file).use { outputStream ->
        val buffer = ByteArray(BUFFER_SIZE)
        var writtenBytes = 0L
        while (writtenBytes < size) {
            val rest = size - writtenBytes
            val bytesToRead: Int = if(rest < BUFFER_SIZE) rest.toInt() else BUFFER_SIZE
            val readBytes = inputStream.read(buffer, 0, bytesToRead)
            if (readBytes > 0) {
                outputStream.write(buffer, 0, readBytes)
                writtenBytes += readBytes
                speedCounter.updateBytes(readBytes)
            } else if (readBytes == -1) {
                throw DownloadException("Stream was corrupted")
            }
            if (speedCounter.isReadyToTest()) {
                println("$fileName: ${speedCounter.getSpeed(writtenBytes)}")
            }
        }
    }
    println("$fileName: success; ${speedCounter.getSpeed(size)}")
}

private fun getFileName(inputStream: DataInputStream): String {
    val sizeOfName = inputStream.readShort()
    if (sizeOfName <= 0) {
        throw DownloadException("Bad size of the name = $sizeOfName")
    }
    val nameBytes = ByteArray(sizeOfName.toInt())
    inputStream.readFully(nameBytes)
    val givenName = String(nameBytes)   //UTF-8
    val fileName = File(givenName).name
    if (fileName.isNullOrEmpty()) {
        throw DownloadException("Failed to extract name of the file from $givenName")
    }
    return fileName
}

private fun initFile(fileName: String): File {
    val file = File("$UPLOADS_DIR_NAME/$fileName")
    if (file.exists()) {
        file.delete()
    }
    file.createNewFile()
    return file
}

private fun initUploads() {
    val dir = File(UPLOADS_DIR_NAME)
    if (!dir.exists()) {
        dir.mkdir()
    } else if (!dir.isDirectory) {
        throw UploadsDirectoryException("File '$UPLOADS_DIR_NAME' exist and is not a directory")
    }
}

private class UploadsDirectoryException(message: String) : InitializationException(message)
private class DownloadException(message: String) : Exception(message)


