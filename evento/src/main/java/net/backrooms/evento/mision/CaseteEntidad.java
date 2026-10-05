package net.backrooms.evento.mision;

import java.util.UUID;
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
 * Un casete tirado en el suelo. Es de un jugador concreto: solo a el se le
 * envia (broadcastToPlayer), asi que cada uno ve y recoge los suyos y nadie
 * puede quitarselos.
 *
 * No se guarda con el mundo: Misiones lo crea cuando su dueno se acerca y lo
 * quita cuando se aleja. La fuente de verdad es el estado de las misiones.
 */
public class CaseteEntidad extends Entity {
	private UUID dueno;
	private int indice;

	public CaseteEntidad(EntityType<? extends CaseteEntidad> tipo, Level nivel) {
		super(tipo, nivel);
		this.noPhysics = true;
		this.setNoGravity(true);
	}

	public void preparar(UUID dueno, int indice, float giro) {
		this.dueno = dueno;
		this.indice = indice;
		this.setYRot(giro);
	}

	public UUID dueno() {
		return this.dueno;
	}

	public int indice() {
		return this.indice;
	}

	@Override
	public boolean broadcastToPlayer(ServerPlayer jugador) {
		return this.dueno != null && this.dueno.equals(jugador.getUUID());
	}

	@Override
	public boolean isPickable() {
		return true;
	}

	@Override
	public InteractionResult interact(Player jugador, InteractionHand mano) {
		if (jugador instanceof ServerPlayer sp && this.dueno != null && this.dueno.equals(sp.getUUID())) {
			Misiones.get().recoger(sp, this);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public boolean skipAttackInteraction(Entity atacante) {
		// pegarle tambien lo recoge: hay quien hace clic izquierdo
		if (atacante instanceof ServerPlayer sp && this.dueno != null && this.dueno.equals(sp.getUUID())) {
			Misiones.get().recoger(sp, this);
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
	}

	@Override
	protected void readAdditionalSaveData(ValueInput entrada) {
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput salida) {
	}
}
