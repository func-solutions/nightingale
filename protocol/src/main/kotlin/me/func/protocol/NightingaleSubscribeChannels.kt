package me.func.protocol

import ru.cristalix.core.network.CorePackage

/**
 * Пакет для подписки на каналы сообщений
 */
class NightingaleSubscribeChannels(
    val channels: List<String>,
) : CorePackage()