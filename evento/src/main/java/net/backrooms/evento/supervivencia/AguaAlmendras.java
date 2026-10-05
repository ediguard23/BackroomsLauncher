package net.backrooms.evento.supervivencia;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Agua de almendras: lo unico que sube la cordura en los Backrooms (+30 %).
 * Tambien quita un poco el hambre. Se puede beber aunque no tengas hambre.
 */
public class AguaAlmendras extends Item {
	public static final float CORDURA = 30.0F;

	public AguaAlmendras(Item.Properties propiedades) {
		super(propiedades);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack objeto, Level nivel, LivingEntity quien) {
		if (quien instanceof ServerPlayer j) {
			Supervivencia.get().beber(j, CORDURA);
		}
		return super.finishUsingItem(objeto, nivel, quien);
	}
}
