package net.backrooms.evento.vestibulo;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Entidad invisible sobre una butaca: el jugador "monta" en ella para
 * quedarse sentado. Se borra sola en cuanto se levanta (Mayus) o si se
 * rompe la butaca.
 */
public class Asiento extends Entity {
	public Asiento(EntityType<? extends Asiento> tipo, Level nivel) {
		super(tipo, nivel);
		this.noPhysics = true;
		this.setNoGravity(true);
		this.setInvisible(true);
	}

	@Override
	public void tick() {
		super.tick();
		if (!this.level().isClientSide() && this.tickCount > 2
			&& (this.getPassengers().isEmpty() || !(this.level().getBlockState(this.blockPosition()).getBlock() instanceof Butaca))) {
			this.ejectPassengers();
			this.discard();
		}
	}

	@Override
	protected Vec3 getPassengerAttachmentPoint(Entity pasajero, EntityDimensions dimensiones, float escala) {
		return new Vec3(0, 0.0, 0);
	}

	@Override
	public Vec3 getDismountLocationForPassenger(LivingEntity pasajero) {
		// se levanta delante de la butaca, no encima
		Vec3 delante = Vec3.directionFromRotation(0, this.getYRot()).scale(0.8);
		return this.position().add(delante.x, 0.1, delante.z);
	}

	@Override
	public boolean hurtServer(ServerLevel nivel, DamageSource fuente, float cantidad) {
		return false;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
	}

	@Override
	protected void readAdditionalSaveData(ValueInput entrada) {
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput salida) {
	}
}
