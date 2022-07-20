package me.func.protocol

import ru.cristalix.core.network.CorePackage
import java.util.*

/**
 * Пакет для отправки сообщения на все сервера
 */
open class NightingalePublishMessage(
    val uuid: UUID,
    val channel: String,
    val message: String,
    val metadata: String
) : CorePackage()