package net.backrooms.evento.mixin.cliente;

import net.backrooms.evento.BackroomsEvento;
import net.backrooms.evento.objetos.Objetos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * En primera persona Minecraft dibuja el brazo con la skin del jugador, nunca
 * con la armadura: con el traje puesto se veia la mano desnuda. Mientras se
 * lleva la chaqueta del traje, el brazo se dibuja con traje_brazo.png (la
 * manga amarilla y el guante negro, generada por tools/texturas/traje.js).
 */
@Mixin(ItemInHandRenderer.class)
public abstract class BrazoTrajeMixin {
	private static final Identifier BRAZO_TRAJE = BackroomsEvento.id("textures/entity/traje_brazo.png");

	@ModifyVariable(method = "renderPlayerArm", at = @At("STORE"))
	private Identifier backrooms$brazoDelTraje(Identifier skin) {
		var jugador = Minecraft.getInstance().player;
		if (jugador != null && jugador.getItemBySlot(EquipmentSlot.CHEST).is(Objetos.TRAJE_CHAQUETA)) {
			return BRAZO_TRAJE;
		}
		return skin;
	}
}
