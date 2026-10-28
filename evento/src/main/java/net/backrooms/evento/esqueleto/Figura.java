package net.backrooms.evento.esqueleto;

import java.util.function.BiFunction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Una figura humana con esqueleto articulado (Hueso, Pose), para las cinematicas: solo existe en el
 * cliente que la crea (como el Anfitrion de la salida de la Fiesta), nunca se guarda ni la ve nadie
 * mas. Quien la usa le pone la piel (formato de piel de jugador), la pose de cada momento (`animador`:
 * la figura y el tick parcial -> la pose) y, si hace falta, una textura encima (la sangre) o huesos que
 * no se dibujan (las manos solas, vistas desde los ojos). Ver ModeloArticulado y FiguraRenderer.
 */
public class Figura extends Entity {
	public @Nullable Identifier piel;
	/** Una segunda piel encima (transparente: manchas). */
	public @Nullable Identifier encima;
	/** La pose de cada fotograma (la figura y el tick parcial). Sin ella, de pie quieta. */
	public @Nullable BiFunction<Figura, Float, Pose> animador;
	/** Huesos que no se dibujan (sus hijos si), por bits de Hueso.ordinal(). */
	public int ocultos;

	public Figura(EntityType<? extends Figura> tipo, Level nivel) {
		super(tipo, nivel);
		this.noPhysics = true;
		this.setNoGravity(true);
	}

	/** Oculta esos huesos (solo su trozo; lo que cuelga de ellos se sigue viendo). */
	public Figura ocultar(Hueso... huesos) {
		for (Hueso h : huesos) {
			this.ocultos |= 1 << h.ordinal();
		}
		return this;
	}

	public Pose pose(float parcial) {
		return this.animador != null ? this.animador.apply(this, parcial) : Ciclos.quieto((this.tickCount + parcial) / 20.0F);
	}

	@Override
	public boolean hurtServer(ServerLevel nivel, DamageSource fuente, float cantidad) {
		return false;
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
	}

	@Override
	protected void readAdditionalSaveData(ValueInput entrada) {
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput salida) {
	}
}
