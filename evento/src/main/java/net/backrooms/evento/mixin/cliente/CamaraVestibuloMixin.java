package net.backrooms.evento.mixin.cliente;

import net.backrooms.evento.cliente.cinematica.CinematicaCliente;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Al empezar la cinematica del /start, mientras las puertas estan abiertas, la camara del
 * juego se pone en la cabina del ascensor mirando al vestibulo (ver
 * CinematicaCliente#vistaVestibulo): por la abertura de la cinta se ve el de verdad. El
 * FOV va a 70 en GameRendererEfectosMixin, como el de la cinta.
 */
@Mixin(Camera.class)
public abstract class CamaraVestibuloMixin {
	/** Un poco hacia abajo, como la camara del shader (pitch -0,08 rad). */
	private static final float BACKROOMS$INCLINACION = 4.6F;

	@Shadow
	protected abstract void setPosition(Vec3 posicion);

	@Shadow
	protected abstract void setRotation(float giro, float inclinacion);

	@Inject(method = "setup", at = @At("TAIL"))
	private void backrooms$vistaVestibulo(Level nivel, Entity entidad, boolean detras, boolean delante, float parcial, CallbackInfo ci) {
		Vec3 vista = CinematicaCliente.vistaVestibulo();
		if (vista != null) {
			this.setRotation(180.0F, BACKROOMS$INCLINACION);
			this.setPosition(vista);
		}
	}
}
