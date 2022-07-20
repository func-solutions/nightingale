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
import sun.audio.AudioPlayer.player
import java.util.*
import java.util.function.Consumer

object Nightingale {

    val activeChannels = arrayListOf<String>()

    init {
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

    fun subscribe(channels: List<String>) = apply {
        ISocketClient.get().write(NightingaleSubscribeChannels(channels))
        activeChannels.addAll(channels)
    }

    fun publish(packet: NightingalePublishMessage) = apply { ISocketClient.get().write(packet) }

    fun publish(channel: String, sender: UUID, message: String, metadata: String) =
        publish(NightingalePublishMessage(sender, channel, message, metadata))

    @JvmOverloads
    fun publish(channel: String, player: Player, message: String, metadata: String = "") =
        publish(channel, player.uniqueId, message, metadata)

    @JvmOverloads
    fun publishAll(sender: UUID, message: String, metadata: String = "") = apply {
        activeChannels.forEach {
            publish(it, sender, message, metadata)
        }
    }

    fun start() = startCustom({
        publishAll(it.player.uniqueId, it.message)
    }, {
        Bukkit.getOnlinePlayers().forEach { player -> player.sendMessage(it.message) }
    })

    fun startCustom(onSend: Consumer<AsyncPlayerChatEvent>, onReceive: Consumer<NightingalePublishMessage>) {
        ISocketClient.get().addListener(NightingalePublishMessage::class.java) { _, msg ->
            if (msg.channel in activeChannels)
                onReceive.accept(msg)
        }

        val eventExecutor = IServerPlatform.get().getPlatformEventExecutor<Any, Any, Any>()

        eventExecutor.registerListener(AsyncPlayerChatEvent::class.java, this, { event ->
            event.isCancelled = true
            onSend.accept(event)
        }, EventPriority.HIGH, true)
    }

}