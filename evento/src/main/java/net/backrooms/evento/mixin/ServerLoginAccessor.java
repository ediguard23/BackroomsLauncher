package net.backrooms.evento.mixin;

import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** El nick con el que se conecta alguien, para comprobar su entrada (Acceso). */
@Mixin(ServerLoginPacketListenerImpl.class)
public interface ServerLoginAccessor {
	@Accessor("requestedUsername")
	String backrooms$nick();
}
