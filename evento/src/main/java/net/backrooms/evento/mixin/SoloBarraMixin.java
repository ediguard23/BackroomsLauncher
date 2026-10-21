package net.backrooms.evento.mixin;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * En el evento no hay inventario, solo la barra de abajo: lo que se coge va a la barra (o a la mano
 * izquierda) y, si esta llena, se queda en el suelo. Asi nada acaba en un hueco que no se puede ver
 * ni usar. En creativo, el inventario entero.
 */
@Mixin(Inventory.class)
public abstract class SoloBarraMixin {
	@Shadow
	@Final
	public net.minecraft.world.entity.player.Player player;

	@Inject(method = "getFreeSlot", at = @At("RETURN"), cancellable = true)
	private void backrooms$soloBarraLibre(CallbackInfoReturnable<Integer> cir) {
		int i = cir.getReturnValueI();
		if (i >= Inventory.getSelectionSize() && i < Inventory.INVENTORY_SIZE && !this.player.hasInfiniteMaterials()) {
			cir.setReturnValue(-1);
		}
	}

	@Inject(method = "getSlotWithRemainingSpace", at = @At("RETURN"), cancellable = true)
	private void backrooms$soloBarraHueco(ItemStack s, CallbackInfoReturnable<Integer> cir) {
		int i = cir.getReturnValueI();
		if (i >= Inventory.getSelectionSize() && i < Inventory.INVENTORY_SIZE && !this.player.hasInfiniteMaterials()) {
			cir.setReturnValue(-1);
		}
	}
}
