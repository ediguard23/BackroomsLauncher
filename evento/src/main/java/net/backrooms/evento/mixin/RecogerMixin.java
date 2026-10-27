package net.backrooms.evento.mixin;

import net.backrooms.evento.estacion.BloquesEstacion;
import net.backrooms.evento.estacion.LimiteFusibles;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Al pisar un objeto que no se puede coger (la barra llena, o ya lleva el maximo de fusibles), se
 * queda en el suelo y se le avisa (como mucho cada 2 s): sin inventario, si no se dijera,
 * pareceria que el objeto se pierde.
 */
@Mixin(ItemEntity.class)
public abstract class RecogerMixin {
	@Shadow
	private int pickupDelay;

	@Shadow
	public abstract ItemStack getItem();

	@Inject(method = "playerTouch", at = @At("HEAD"))
	private void backrooms$avisoManosLlenas(Player j, CallbackInfo ci) {
		if (j.level().isClientSide() || this.pickupDelay != 0 || j.hasInfiniteMaterials() || j.isSpectator()) {
			return;
		}
		ItemStack s = this.getItem();
		Inventory inv = j.getInventory();
		Component aviso = null;
		if (s.is(BloquesEstacion.FUSIBLE) && LimiteFusibles.fusibles(inv) >= LimiteFusibles.MAX) {
			aviso = Component.literal("Ya llevas " + LimiteFusibles.MAX + " fusibles: no puedes coger más.").withStyle(ChatFormatting.GRAY);
		} else if (inv.getSlotWithRemainingSpace(s) == -1 && inv.getFreeSlot() == -1) {
			aviso = Component.literal("Tienes las manos llenas: suelta algo (Q) para coger esto.").withStyle(ChatFormatting.GRAY);
		}
		if (aviso != null && j.level().getGameTime() % 40 == 0) {
			j.displayClientMessage(aviso, true);
		}
	}
}
