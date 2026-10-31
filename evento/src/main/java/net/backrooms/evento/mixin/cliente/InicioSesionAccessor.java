package net.backrooms.evento.mixin.cliente;

import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.client.multiplayer.ServerData;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * El servidor al que se esta entrando, durante el inicio de sesion (Minecraft no lo apunta en
 * getCurrentServer hasta que ya se ha entrado): para volver a conectar tras canjear el codigo
 * (AccesoCliente, PantallaCodigo).
 */
@Mixin(ClientHandshakePacketListenerImpl.class)
public interface InicioSesionAccessor {
	@Accessor("serverData")
	@Nullable ServerData backrooms$servidor();
}
