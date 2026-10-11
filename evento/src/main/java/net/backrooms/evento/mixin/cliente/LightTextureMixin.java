package net.backrooms.evento.mixin.cliente;

import net.backrooms.evento.cliente.AmbienteCliente;
import net.backrooms.evento.piscinas.Piscinas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * En el apagon el mapa de luz se pone plano (como con vision nocturna al
 * maximo): todo sale con su color tal cual y es EfectosMundo quien decide que
 * se ve, segun las linternas. Si no, lo que alumbra una linterna bajo un tubo
 * se veria mas que lo que alumbra en una zona a oscuras.
 *
 * En las Piscinas la luz de los bloques es blanca y fria, y las sombras tiran a
 * azul: se manda el color de ambiente con una marca (azul + 2) que el mapa de luz
 * del mod (assets/minecraft/shaders/core/lightmap.fsh) reconoce. Fuera de las
 * Piscinas el color va tal cual y el mapa de luz es el de siempre.
 */
@Mixin(LightTexture.class)
public abstract class LightTextureMixin {
	/** Color de las sombras de las Piscinas (con la marca en el azul). */
	@Unique
	private static final Vector3fc AMBIENTE_PISCINAS = new Vector3f(0.36F, 0.62F, 0.72F + 2.0F);

	@ModifyArg(method = "updateLightTexture", at = @At(value = "INVOKE",
		target = "Lcom/mojang/blaze3d/buffers/Std140Builder;putFloat(F)Lcom/mojang/blaze3d/buffers/Std140Builder;", ordinal = 3))
	private float backrooms$luzPlanaEnApagon(float visionNocturna) {
		return AmbienteCliente.oscuridad() > 0.5F ? 1.0F : visionNocturna;
	}

	@ModifyArg(method = "updateLightTexture", at = @At(value = "INVOKE",
		target = "Lcom/mojang/blaze3d/buffers/Std140Builder;putVec3(Lorg/joml/Vector3fc;)Lcom/mojang/blaze3d/buffers/Std140Builder;", ordinal = 1))
	private Vector3fc backrooms$luzFriaEnPiscinas(Vector3fc ambiente) {
		Minecraft mc = Minecraft.getInstance();
		return mc.level != null && Piscinas.es(mc.level) ? AMBIENTE_PISCINAS : ambiente;
	}
}
