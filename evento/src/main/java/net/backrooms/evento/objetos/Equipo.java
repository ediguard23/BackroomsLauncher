package net.backrooms.evento.objetos;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * Todo explorador lleva el traje antirradiacion, la linterna y la camara: se
 * le ponen al entrar y al reaparecer. Va con maldicion de ligamiento (no se puede quitar), es
 * irrompible y sin el brillo de encantado ni la linea del encantamiento en la descripcion.
 * El staff en creativo puede quitarselo; en aventura nadie.
 */
public final class Equipo {
	private static final TooltipDisplay SIN_ENCANTAMIENTO = TooltipDisplay.DEFAULT.withHidden(DataComponents.ENCHANTMENTS, true);

	private Equipo() {
	}

	public static void registrar() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> vestir(handler.player));
		ServerPlayerEvents.AFTER_RESPAWN.register((viejo, nuevo, vivo) -> vestir(nuevo));
	}

	public static void vestir(ServerPlayer jugador) {
		if (jugador.isSpectator()) {
			return;
		}
		poner(jugador, EquipmentSlot.HEAD, Objetos.TRAJE_CASCO);
		poner(jugador, EquipmentSlot.CHEST, Objetos.TRAJE_CHAQUETA);
		poner(jugador, EquipmentSlot.LEGS, Objetos.TRAJE_PANTALON);
		poner(jugador, EquipmentSlot.FEET, Objetos.TRAJE_BOTAS);
		herramientas(jugador);
	}

	/** La linterna y la camara, si no las lleva: en los dos ultimos huecos de la barra si estan libres. */
	public static void herramientas(ServerPlayer jugador) {
		dar(jugador, Objetos.LINTERNA, 7);
		dar(jugador, Objetos.CAMARA, 8);
	}

	private static void dar(ServerPlayer jugador, Item objeto, int hueco) {
		if (HerramientasServidor.lleva(jugador, objeto)) {
			return;
		}
		ItemStack s = new ItemStack(objeto);
		if (jugador.getInventory().getItem(hueco).isEmpty()) {
			jugador.getInventory().setItem(hueco, s);
		} else {
			jugador.getInventory().add(s);
		}
	}

	private static void poner(ServerPlayer jugador, EquipmentSlot hueco, Item pieza) {
		ItemStack puesta = jugador.getItemBySlot(hueco);
		if (puesta.is(pieza)) {
			// los trajes de antes tambien pierden la linea del encantamiento
			puesta.set(DataComponents.TOOLTIP_DISPLAY, SIN_ENCANTAMIENTO);
			return;
		}
		ItemStack s = new ItemStack(pieza);
		Holder<Enchantment> ligamiento = jugador.level().registryAccess()
			.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.BINDING_CURSE);
		s.enchant(ligamiento, 1);
		s.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
		s.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, false);
		s.set(DataComponents.TOOLTIP_DISPLAY, SIN_ENCANTAMIENTO);
		jugador.setItemSlot(hueco, s);
	}
}
