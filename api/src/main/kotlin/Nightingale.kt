import me.func.protocol.NightingalePublishMessage
import me.func.protocol.NightingaleServicePublishMessage
import me.func.protocol.NightingaleSubscribeChannels
import net.md_5.bungee.chat.ComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import ru.cristalix.core.IServerPlatform
import ru.cristalix.core.event.AsyncPlayerChatEvent
import ru.cristalix.core.network.Capability
import ru.cristalix.core.network.CorePackage
import ru.cristalix.core.network.ISocketClient
import java.util.*
import java.util.function.Consumer

class Nightingale : Listener {
    companion object {
        // Список каналов
        val activeChannels = arrayListOf<String>()

        // Использовать децентрализованную систему
        var p2p = false

        fun subscribe(vararg channels: String) = subscribe(channels.toList())

        // Подписаться на канал
        fun subscribe(channels: List<String>) = apply {
            ISocketClient.get().write(NightingaleSubscribeChannels(channels))
            activeChannels.addAll(channels.filter { !activeChannels.contains(it) })
        }

        fun publish(packet: NightingalePublishMessage) = apply { ISocketClient.get().write(packet) }

        // Если включено P2P, создается пакет для других серверов, иначе сервисный
        fun publish(channel: String, sender: UUID, message: String, metadata: String) = publish(
            if (p2p) NightingalePublishMessage(sender, channel, message, metadata) else
                NightingaleServicePublishMessage(sender, channel, message, metadata)
        )

        @JvmOverloads
        fun publish(channel: String, player: Player, message: String, metadata: String = "") =
            publish(channel, player.uniqueId, message, metadata)

        @JvmOverloads
        fun publishAll(sender: UUID, message: String, metadata: String = "") = apply {
            activeChannels.forEach { publish(it, sender, message, metadata) }
        }

        // Начать стандартный вариант работы
        fun start() = startCustom({ event ->
            // Превращаем сообщения в текст компоненты JSON
            event.message?.thenAccept {
                val data = ComponentSerializer.toString(*it)
                publishAll(event.player.uniqueId, data)
            }
        }, {
            val data = ComponentSerializer.parse(it.message)
            Bukkit.getOnlinePlayers().forEach { player -> player.sendMessage(*data) }
        })

        // Включить децентрализованный обмен пакетами
        fun useP2p() = apply { p2p = true }

        // Отправить всем сообщение
        fun broadcast(channel: String, message: String) =
            publish(channel, UUID.randomUUID(), message, "")

        private inline fun <reified T> startRead() where T : CorePackage = ISocketClient.get().registerCapabilities(
            Capability.builder().className(T::class.java.name).notification(true).build()
        )

        // Начать по своему
        fun startCustom(onSend: Consumer<AsyncPlayerChatEvent>, onReceive: Consumer<NightingalePublishMessage>) {
            val socket = ISocketClient.get()

            if (p2p) {
                // Начинаем слушать пакет на P2P сообщения
                startRead<NightingalePublishMessage>()
                // Слушаем сообщения от других серверов и пишем
                socket.addListener(NightingalePublishMessage::class.java) { _, msg ->
                    if (msg.channel in activeChannels) onReceive.accept(msg)
                }
            } else {
                // Начинаем слушать пакет на сервисные сообщения
                startRead<NightingaleServicePublishMessage>()
                // Слушаем сообщения от сервиса и пишем
                socket.addListener(NightingaleServicePublishMessage::class.java) { _, msg -> onReceive.accept(msg) }
            }

            val eventExecutor = IServerPlatform.get().getPlatformEventExecutor<Any, Any, Any>()

            // При отправке сообщения отправляем всем
            eventExecutor.registerListener(AsyncPlayerChatEvent::class.java, this, { event ->
                onSend.accept(event)
            }, EventPriority.HIGH, false)
        }
    }
}