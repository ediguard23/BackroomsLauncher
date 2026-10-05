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
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * Todo explorador lleva el traje antirradiacion: se le pone al entrar y al
 * reaparecer. Va con maldicion de ligamiento (no se puede quitar), es
 * irrompible y sin el brillo de encantado. El staff en creativo puede
 * quitarselo; en aventura nadie.
 */
public final class Equipo {
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
	}

	private static void poner(ServerPlayer jugador, EquipmentSlot hueco, Item pieza) {
		if (jugador.getItemBySlot(hueco).is(pieza)) {
			return;
		}
		ItemStack s = new ItemStack(pieza);
		Holder<Enchantment> ligamiento = jugador.level().registryAccess()
			.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.BINDING_CURSE);
		s.enchant(ligamiento, 1);
		s.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
		s.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, false);
		jugador.setItemSlot(hueco, s);
	}
}
