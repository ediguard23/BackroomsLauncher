package net.backrooms.evento.mixin;

import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * El servidor manda un keepalive cada 15 s y echa ("Timed out") a quien no contesta
 * el anterior: un corte de red de 15 s ya te saca. Con 45 s aguanta los tirones del
 * hosting (ver AguanteConexionMixin). El ping del TAB se actualiza cada 45 s en vez de
 * cada 15: en el evento da igual.
 */
@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class KeepAliveMixin {
	@ModifyConstant(method = "keepConnectionAlive", constant = @Constant(longValue = 15000L))
	private long backrooms$aguantar(long original) {
		return 45000L;
	}
}
