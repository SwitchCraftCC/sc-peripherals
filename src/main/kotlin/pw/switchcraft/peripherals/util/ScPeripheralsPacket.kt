package pw.switchcraft.peripherals.util

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.createC2SPacket
import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.createS2CPacket
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayNetworkHandler
import net.minecraft.network.Packet
import net.minecraft.network.PacketByteBuf
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayNetworkHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier
import pw.switchcraft.peripherals.ScPeripherals.ModId

internal fun packetId(id: String) = ModId(id)

inline fun <reified T> event(noinline invokerFactory: (Array<T>) -> T): Event<T>
  = EventFactory.createArrayBacked(T::class.java, invokerFactory)

inline fun <reified T> clientPacketEvent() = event<(packet: T) -> Unit> { cb ->
  { packet -> cb.forEach { it(packet) } }
}

inline fun <reified T> serverPacketEvent() = event<(packet: T, player: ServerPlayerEntity,
                                                    handler: ServerPlayNetworkHandler,
                                                    responseSender: PacketSender) -> Unit> { cb ->
  { packet, player, handler, responseSender -> cb.forEach { it(packet, player, handler, responseSender) } }
}

fun <T: ScPeripheralsPacket> registerClientReceiver(id: Identifier, factory: (buf: PacketByteBuf) -> T) {
  ClientPlayNetworking.registerGlobalReceiver(id) { client, handler, buf, responseSender ->
    val packet = factory(buf)
    packet.onClientReceive(client, handler, responseSender)
  }
}

fun <T: ScPeripheralsPacket> registerServerReceiver(id: Identifier, factory: (buf: PacketByteBuf) -> T) {
  ServerPlayNetworking.registerGlobalReceiver(id) { server, player, handler, buf, responseSender ->
    val packet = factory(buf)
    packet.onServerReceive(server, player, handler, responseSender)
  }
}

abstract class ScPeripheralsPacket {
  abstract val id: Identifier

  abstract fun toBytes(buf: PacketByteBuf)
  open fun toBytes(): PacketByteBuf {
    val buf = PacketByteBufs.create()
    toBytes(buf)
    return buf
  }

  fun toC2SPacket(): Packet<*> = createC2SPacket(id, toBytes())
  fun toS2CPacket(): Packet<*> = createS2CPacket(id, toBytes())

  @Environment(EnvType.CLIENT)
  open fun onClientReceive(client: MinecraftClient, handler: ClientPlayNetworkHandler, responseSender: PacketSender) {}

  @Environment(EnvType.SERVER)
  open fun onServerReceive(server: MinecraftServer, player: ServerPlayerEntity,
                           handler: ServerPlayNetworkHandler, responseSender: PacketSender) {}
}
