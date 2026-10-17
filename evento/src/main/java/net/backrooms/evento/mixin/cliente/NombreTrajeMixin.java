package net.backrooms.evento.mixin.cliente;

import net.backrooms.evento.objetos.Objetos;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Con la chaqueta del traje puesta no se ve el nombre encima de la cabeza: va en la
 * acreditacion del pecho (TarjetaPecho).
 */
@Mixin(AvatarRenderer.class)
public abstract class NombreTrajeMixin {
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("TAIL"))
	private void backrooms$sinNombre(Avatar avatar, AvatarRenderState estado, float parcial, CallbackInfo ci) {
		if (estado.chestEquipment.is(Objetos.TRAJE_CHAQUETA)) {
			estado.nameTag = null;
			estado.scoreText = null;
		}
	}
}
