package org.csystem.app.chat.server

import com.karandev.util.net.TcpUtil
import kotlinx.coroutines.*
import org.csystem.app.chat.server.configuration.constant.*
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ExecutorService

@Component
class RegisterServer(private val threadPool: ExecutorService,
                     @Qualifier("app.chat.server.config.register.port.ServerSocket")
                     private val serverSocket: ServerSocket) {
    @Value("\${app.chat.server.config.register.timeout}")
    private var mTimeout: Int = 0;

    private fun clientOperationCallback(socket: Socket)
    {
        socket.soTimeout = mTimeout
        val name = TcpUtil.receiveLine(socket) ?: return
        val nickname = TcpUtil.receiveLine(socket) ?: return
        val password = TcpUtil.receiveLine(socket) ?: return
        val confirmPassword = TcpUtil.receiveLine(socket) ?: return

        if (name.isBlank()) {
            TcpUtil.sendLine(socket, ERR_NAME_BLANK)
            return
        }

        if (nickname.isBlank()) {
            TcpUtil.sendLine(socket, ERR_NICKNAME_BLANK)
            return
        }

        if (password.isBlank()) {
            TcpUtil.sendLine(socket, ERR_PASSWORD_BLANK)
            return
        }

        if (password != confirmPassword) {
            TcpUtil.sendLine(socket, ERR_CONFIRM_PASSWORD)
            return
        }

        //Save user information to database
        TcpUtil.sendLine(socket, SUC_REGISTER)
    }

    private fun handleClient(socket: Socket)
    {
        try {
            socket.use { clientOperationCallback(it) }
        }
        catch (ex: Throwable) {
            println("Error:${ex.message}")
        }
    }

    private fun serverCallback()
    {
        try {
            println("Register server waiting for a client")

            while (true) {
                val socket = serverSocket.accept()

                MainScope().launch { handleClient(socket) }
            }
        }
        catch (ex: Throwable) {
            println("Exception occurred:${ex.message}")
        }
    }

    fun run()
    {
        threadPool.execute(this::serverCallback)
    }
}