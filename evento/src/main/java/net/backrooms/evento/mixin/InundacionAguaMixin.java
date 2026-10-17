package net.backrooms.evento.mixin;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import java.util.Set;
import net.backrooms.evento.piscinas.Inundacion;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * La inundacion de las Piscinas (Inundacion) no pone bloques de agua: por debajo de su altura
 * el jugador cuenta como si estuviera en agua de Minecraft. Asi nada, se hunde, sube con el
 * salto, se ahoga con las burbujas de siempre y no se hace dano al caer, igual en el cliente
 * (que mueve al jugador) y en el servidor.
 */
@Mixin(Entity.class)
public abstract class InundacionAguaMixin {
	@Shadow
	protected boolean wasTouchingWater;
	@Shadow
	protected Object2DoubleMap<TagKey<Fluid>> fluidHeight;
	@Shadow
	@Final
	private Set<TagKey<Fluid>> fluidOnEyes;

	@Shadow
	public abstract double getY();

	@Shadow
	public abstract double getEyeY();

	@Shadow
	public abstract void resetFallDistance();

	@Inject(method = "updateInWaterStateAndDoWaterCurrentPushing", at = @At("TAIL"))
	private void backrooms$inundacion(CallbackInfo ci) {
		Entity yo = (Entity) (Object) this;
		if (!(yo instanceof Player)) {
			return;
		}
		double agua = Inundacion.nivelPara(yo.level());
		double h = agua - this.getY();
		if (h <= 0.0) {
			return;
		}
		if (h > this.fluidHeight.getDouble(FluidTags.WATER)) {
			this.fluidHeight.put(FluidTags.WATER, h);
		}
		this.resetFallDistance();
		this.wasTouchingWater = true;
	}

	@Inject(method = "updateFluidOnEyes", at = @At("TAIL"))
	private void backrooms$inundacionOjos(CallbackInfo ci) {
		Entity yo = (Entity) (Object) this;
		if (yo instanceof Player && this.getEyeY() < Inundacion.nivelPara(yo.level())) {
			this.fluidOnEyes.add(FluidTags.WATER);
		}
	}
}
