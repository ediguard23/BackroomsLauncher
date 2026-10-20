package net.backrooms.evento.supervivencia;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Comida que ademas del hambre sube un poco la cordura al terminar de comerla: las galletas
 * (+5 %) y la pizza (+3 %). El agua de almendras sigue siendo la que mas sube (AguaAlmendras).
 */
public class ComidaCordura extends Item {
	private final float cordura;

	public ComidaCordura(Item.Properties propiedades, float cordura) {
		super(propiedades);
		this.cordura = cordura;
	}

	@Override
	public ItemStack finishUsingItem(ItemStack objeto, Level nivel, LivingEntity quien) {
		if (quien instanceof ServerPlayer j) {
			Supervivencia.get().beber(j, this.cordura);
		}
		return super.finishUsingItem(objeto, nivel, quien);
	}
}
