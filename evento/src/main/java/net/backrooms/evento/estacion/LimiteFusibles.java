package net.backrooms.evento.estacion;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Cuantos fusibles puede llevar cada jugador (LimiteFusiblesMixin) y cuantos lleva. */
public final class LimiteFusibles {
	public static final int MAX = 5;

	private LimiteFusibles() {
	}

	/** Cuantos fusibles lleva (en cualquier hueco, la mano izquierda incluida). */
	public static int fusibles(Inventory inv) {
		int n = 0;
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack s = inv.getItem(i);
			if (s.is(BloquesEstacion.FUSIBLE)) {
				n += s.getCount();
			}
		}
		return n;
	}
}
