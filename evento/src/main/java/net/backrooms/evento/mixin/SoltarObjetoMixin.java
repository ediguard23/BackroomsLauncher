package net.backrooms.evento.mixin;

import net.backrooms.evento.objetos.Objetos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * La linterna y la camara no se tiran con Q (el cliente ya no lo intenta, SoltarObjetoClienteMixin;
 * esto es por si llega el paquete igual): se queda en la mano y se le vuelve a mandar el hueco.
 */
@Mixin(ServerPlayer.class)
public abstract class SoltarObjetoMixin {
	@Inject(method = "drop(Z)V", at = @At("HEAD"), cancellable = true)
	private void backrooms$noSeTira(boolean todo, CallbackInfo ci) {
		ServerPlayer j = (ServerPlayer) (Object) this;
		Inventory inv = j.getInventory();
		if (Objetos.noSeTira(j, inv.getSelectedItem())) {
			j.containerMenu.findSlot(inv, inv.getSelectedSlot()).ifPresent(i -> j.containerMenu.setRemoteSlot(i, net.minecraft.world.item.ItemStack.EMPTY));
			j.containerMenu.broadcastChanges();
			ci.cancel();
		}
	}
}
