package net.backrooms.evento.mixin;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.ReadTimeoutHandler;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Minecraft cierra la conexion si en 30 s no llega nada (en el cliente y en el
 * servidor). En la beta #1 la red del hosting se corto ~20-30 s dos veces y echo a
 * todos a la vez: con 90 s un corte asi se queda en un tiron y la partida sigue.
 * Va en los dos lados (el mismo jar), junto con KeepAliveMixin.
 */
@Mixin(Connection.class)
public abstract class AguanteConexionMixin {
	private static final int SEGUNDOS = 90;

	@Inject(method = "channelActive", at = @At("TAIL"))
	private void backrooms$aguantar(ChannelHandlerContext ctx, CallbackInfo ci) {
		if (ctx.pipeline().get("timeout") instanceof ReadTimeoutHandler) {
			ctx.pipeline().replace("timeout", "timeout", new ReadTimeoutHandler(SEGUNDOS));
		}
	}
}
