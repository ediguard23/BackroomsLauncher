package net.backrooms.evento.mixin;

import net.backrooms.evento.acceso.ColaEntrada;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Quien espera en la cola de entrada no se va por tardar en entrar (Minecraft echa a los 30 s). */
@Mixin(ServerLoginPacketListenerImpl.class)
public abstract class ColaLoginMixin {
	@Shadow
	private int tick;

	@Inject(method = "tick", at = @At("HEAD"))
	private void backrooms$enCola(CallbackInfo ci) {
		if (ColaEntrada.esperando((ServerLoginPacketListenerImpl) (Object) this)) {
			this.tick = 0;
		}
	}
}
