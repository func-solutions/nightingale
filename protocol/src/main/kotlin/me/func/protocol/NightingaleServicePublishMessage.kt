package me.func.protocol

import java.util.*

/**
 * Пакет для отправки сообщения на сервис,
 * затем, сервис направит его всем подписавшимся серверам
 */
class NightingaleServicePublishMessage(uuid: UUID, channel: String, message: String, metadata: String) :
    NightingalePublishMessage(uuid, channel, message, metadata)