package net.backrooms.evento.piscinas;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Una tarjeta de la salida de emergencia tirada en algun sitio: en su escondite o donde se
 * le cayo a alguien. Flota un palmo y gira, con luz propia (TarjetaRenderer). Se coge con
 * clic (derecho o izquierdo). No se guarda con el mundo: la pone y la quita Tarjetas, que es
 * quien sabe donde esta cada una.
 */
public class TarjetaEntidad extends Entity {
	private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(TarjetaEntidad.class, EntityDataSerializers.INT);

	public TarjetaEntidad(EntityType<? extends TarjetaEntidad> tipo, Level nivel) {
		super(tipo, nivel);
		this.noPhysics = true;
		this.setNoGravity(true);
	}

	public void color(int color) {
		this.entityData.set(COLOR, color);
	}

	public int color() {
		return this.entityData.get(COLOR);
	}

	@Override
	public boolean isPickable() {
		return true;
	}

	@Override
	public InteractionResult interact(Player jugador, InteractionHand mano) {
		if (jugador instanceof ServerPlayer sp) {
			Tarjetas.get().coger(sp, this);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public boolean skipAttackInteraction(Entity atacante) {
		if (atacante instanceof ServerPlayer sp) {
			Tarjetas.get().coger(sp, this);
		}
		return true;
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
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(COLOR, 0);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput entrada) {
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput salida) {
	}
}
