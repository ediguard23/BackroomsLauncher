package net.backrooms.evento.supervivencia;

import net.backrooms.evento.objetos.Objetos;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Agua de almendras, galletas o pizza tiradas en la moqueta. A diferencia de
 * los casetes, son de quien las coja primero. No se guardan con el mundo: las
 * crea Comida cuando alguien se acerca y su sitio queda apuntado al cogerlas.
 */
public class ComidaEntidad extends Entity {
	private static final EntityDataAccessor<Integer> TIPO = SynchedEntityData.defineId(ComidaEntidad.class, EntityDataSerializers.INT);

	public ComidaEntidad(EntityType<? extends ComidaEntidad> tipo, Level nivel) {
		super(tipo, nivel);
		this.noPhysics = true;
		this.setNoGravity(true);
	}

	public void preparar(int tipo, float giro) {
		this.entityData.set(TIPO, tipo);
		this.setYRot(giro);
	}

	public int tipo() {
		return this.entityData.get(TIPO);
	}

	public Item objeto() {
		return Objetos.comida(this.tipo());
	}

	@Override
	public boolean isPickable() {
		return true;
	}

	@Override
	public InteractionResult interact(Player jugador, InteractionHand mano) {
		if (jugador instanceof ServerPlayer sp) {
			Comida.get().coger(sp, this);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public boolean skipAttackInteraction(Entity atacante) {
		if (atacante instanceof ServerPlayer sp) {
			Comida.get().coger(sp, this);
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
		builder.define(TIPO, 1);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput entrada) {
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput salida) {
	}
}
