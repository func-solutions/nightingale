package me.func.nightingale

import me.func.protocol.NightingalePublishMessage
import me.func.protocol.NightingaleSubscribeChannels
import me.func.serviceapi.runListener
import ru.cristalix.core.CoreApi
import ru.cristalix.core.microservice.MicroServicePlatform
import ru.cristalix.core.microservice.MicroserviceBootstrap
import ru.cristalix.core.network.Capability
import ru.cristalix.core.network.ISocketClient
import ru.cristalix.core.permissions.IPermissionService
import ru.cristalix.core.permissions.PermissionService
import ru.cristalix.core.realm.RealmId

fun main() {
    // Запускаем микро-сервис
    MicroserviceBootstrap.bootstrap(MicroServicePlatform(2))

    // Настраиваем Core-клиент
    ISocketClient.get().apply {
        // Подключаем сервис прав, чтобы формировать префиксы
        CoreApi.get().registerService(IPermissionService::class.java, PermissionService(this))

        // Подписываемся на получение пакетов
        registerCapabilities(
            Capability.builder()
                .className(NightingalePublishMessage::class.java.name)
                .notification(true)
                .build(),
            Capability.builder()
                .className(NightingaleSubscribeChannels::class.java.name)
                .notification(true)
                .build(),
        )

        // Словарь слушателей каналов
        val subscribers = hashMapOf<String, ArrayList<RealmId>>() // channel to realm list

        // Обрабатываем запрос на подписку к каналу
        runListener<NightingaleSubscribeChannels> { realm, pckg ->
            // Перебираем все каналы, на которые хочет подписаться реалм
            pckg.channels.forEach { channel ->
                // Если к данному реалму еще никто не подписался, создать пустой список
                val realmList = subscribers[channel] ?: arrayListOf()

                // Добавляем в список на реалм
                if (!realmList.contains(realm)) realmList.add(realm)

                // Записываем список реалмов
                subscribers[channel] = realmList
            }
            println("Subscribe from ${realm.realmName} to channels: ${pckg.channels.joinToString() }")
        }

        // Обрабатываем запрос на рассылку сообщения
        runListener<NightingalePublishMessage> { realm, pckg ->
            // Берем все реалм по данному каналу, пересылаем этот пакет всем подписавшимся
            subscribers[pckg.channel]?.forEach { targetRealm -> forward(targetRealm, pckg) }
            println("Publish from ${realm.realmName} to channel: ${pckg.channel}")
        }
    }
}