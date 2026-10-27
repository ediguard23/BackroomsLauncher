package net.backrooms.evento.mixin;

import net.backrooms.evento.estacion.BloquesEstacion;
import net.backrooms.evento.estacion.LimiteFusibles;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Nadie lleva mas de LimiteFusibles.MAX fusibles (con cien jugadores, uno solo podria acapararlos): venga de
 * donde venga (del suelo, de un buzon, de la caja de una reja), lo que pase de ese maximo no entra. En
 * creativo, sin limite.
 */
@Mixin(Inventory.class)
public abstract class LimiteFusiblesMixin {
	@Shadow
	@Final
	public net.minecraft.world.entity.player.Player player;

	@Inject(method = "add(ILnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
	private void backrooms$limiteFusibles(int hueco, ItemStack s, CallbackInfoReturnable<Boolean> cir) {
		if (s.isEmpty() || !s.is(BloquesEstacion.FUSIBLE) || this.player.hasInfiniteMaterials()) {
			return;
		}
		Inventory inv = (Inventory) (Object) this;
		int cabe = LimiteFusibles.MAX - LimiteFusibles.fusibles(inv);
		if (cabe <= 0) {
			cir.setReturnValue(false);
		} else if (s.getCount() > cabe) {
			// entra solo una parte: el resto se queda (en el suelo, o donde estuviera)
			ItemStack parte = s.copyWithCount(cabe);
			inv.add(hueco, parte);
			int puestos = cabe - parte.getCount();
			s.shrink(puestos);
			cir.setReturnValue(puestos > 0);
		}
	}
}
