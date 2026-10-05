package net.backrooms.evento.mixin.cliente;

import net.backrooms.evento.cliente.AmbienteCliente;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * En el apagon el mapa de luz se pone plano (como con vision nocturna al
 * maximo): todo sale con su color tal cual y es EfectosMundo quien decide que
 * se ve, segun las linternas. Si no, lo que alumbra una linterna bajo un tubo
 * se veria mas que lo que alumbra en una zona a oscuras.
 */
@Mixin(LightTexture.class)
public abstract class LightTextureMixin {
	@ModifyArg(method = "updateLightTexture", at = @At(value = "INVOKE",
		target = "Lcom/mojang/blaze3d/buffers/Std140Builder;putFloat(F)Lcom/mojang/blaze3d/buffers/Std140Builder;", ordinal = 3))
	private float backrooms$luzPlanaEnApagon(float visionNocturna) {
		return AmbienteCliente.oscuridad() > 0.5F ? 1.0F : visionNocturna;
	}
}
