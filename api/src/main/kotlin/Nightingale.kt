import Nightingale.activeChannels
import joptsimple.internal.Messages.message
import me.func.protocol.NightingalePublishMessage
import me.func.protocol.NightingaleSubscribeChannels
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.player.AsyncPlayerChatEvent
import ru.cristalix.core.IServerPlatform
import ru.cristalix.core.network.Capability
import ru.cristalix.core.network.ISocketClient
import java.util.*
import java.util.function.Consumer

object Nightingale {

    // Список каналов
    val activeChannels = arrayListOf<String>()
    // Использовать децентрализованную систему
    var p2p = false

    init {
        // Начинаем слушать эти пакеты
        ISocketClient.get().registerCapabilities(
            Capability.builder()
                .className(NightingalePublishMessage::class.java.name)
                .notification(true)
                .build(),
            Capability.builder()
                .className(NightingaleSubscribeChannels::class.java.name)
                .notification(true)
                .build(),
        )
    }

    fun subscribe(vararg channels: String) = subscribe(channels.toList())

    // Подписаться на канал
    fun subscribe(channels: List<String>) = apply {
        ISocketClient.get().write(NightingaleSubscribeChannels(channels))
        activeChannels.addAll(channels.filter { !activeChannels.contains(it) })
    }

    fun publish(packet: NightingalePublishMessage) = apply { ISocketClient.get().write(packet) }

    fun publish(channel: String, sender: UUID, message: String, metadata: String) =
        publish(NightingalePublishMessage(sender, channel, message, metadata))

    @JvmOverloads
    fun publish(channel: String, player: Player, message: String, metadata: String = "") =
        publish(channel, player.uniqueId, message, metadata)

    @JvmOverloads
    fun publishAll(sender: UUID, message: String, metadata: String = "") = apply {
        activeChannels.forEach { publish(it, sender, message, metadata) }
    }

    // Начать стандартный вариант работы
    fun start() = startCustom({
        publishAll(it.player.uniqueId, it.message)
    }, {
        Bukkit.getOnlinePlayers().forEach { player -> player.sendMessage(it.message) }
    })

    // Включить децентрализованный обмен пакетами
    fun useP2p() = apply { p2p = true }

    // Начать по своему
    fun startCustom(onSend: Consumer<AsyncPlayerChatEvent>, onReceive: Consumer<NightingalePublishMessage>) {
        // Слушаем сообщения и пишем
        ISocketClient.get().addListener(NightingalePublishMessage::class.java) { _, msg ->
            if (msg.channel in activeChannels || !p2p) onReceive.accept(msg)
        }

        val eventExecutor = IServerPlatform.get().getPlatformEventExecutor<Any, Any, Any>()

        // При отправке сообщения отправляем всем
        eventExecutor.registerListener(AsyncPlayerChatEvent::class.java, this, { event ->
            event.isCancelled = true
            onSend.accept(event)
        }, EventPriority.HIGH, true)
    }

}