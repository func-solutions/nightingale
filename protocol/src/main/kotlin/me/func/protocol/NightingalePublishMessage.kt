package me.func.protocol

import ru.cristalix.core.network.CorePackage
import java.util.*

/**
 * Пакет для отправки сообщения на сервис,
 * затем, сервис направит его всем подписавшимся серверам
 */
data class NightingalePublishMessage(
    val uuid: UUID,
    val channel: String,
    val message: String,
    val metadata: String
) : CorePackage()