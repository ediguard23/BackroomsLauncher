package net.backrooms.evento.entidad;

import java.util.UUID;
import net.backrooms.evento.Sonidos;
import net.backrooms.evento.ambiente.Ambiente;
import net.backrooms.evento.objetos.HerramientasServidor;
import net.backrooms.evento.red.Susto;
import net.backrooms.evento.supervivencia.Supervivencia;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Smiler: una sonrisa y dos ojos que brillan en la oscuridad. Solo sale en
 * los apagones, cerca de un jugador (casi siempre a su espalda), y se va
 * cuando vuelve la luz.
 *
 *  - Si alguien lo mira directamente a simple vista: flashbang (pantalla en
 *    blanco, ceguera, pitido en los oidos y un buen golpe a la cordura) y
 *    desaparece.
 *  - Por el visor de la camara se le puede mirar sin peligro: mientras le
 *    graban se queda quieto (y cuenta para la mision de grabarlo).
 *  - Si nadie lo mira, se acerca despacio a su objetivo; si lo alcanza, se
 *    le echa encima (dano, susto y cordura) y desaparece.
 */
public class Smiler extends PathfinderMob {
	/** Coseno del angulo dentro del cual cuenta como "mirarlo a la cara". */
	private static final double MIRADA = Math.cos(Math.toRadians(11));
	private static final double ALCANCE_MIRADA = 22;

	private @Nullable UUID objetivo;
	private int quieto;

	public Smiler(EntityType<? extends Smiler> tipo, Level nivel) {
		super(tipo, nivel);
		this.xpReward = 0;
		this.setPersistenceRequired();
	}

	public static AttributeSupplier.Builder atributos() {
		return PathfinderMob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, 40.0)
			.add(Attributes.MOVEMENT_SPEED, 0.2)
			.add(Attributes.FOLLOW_RANGE, 48.0)
			.add(Attributes.STEP_HEIGHT, 1.0);
	}

	public void perseguir(ServerPlayer j) {
		this.objetivo = j.getUUID();
	}

	public @Nullable ServerPlayer objetivo() {
		if (this.objetivo == null || !(this.level() instanceof ServerLevel sl)) {
			return null;
		}
		return sl.getServer().getPlayerList().getPlayer(this.objetivo) instanceof ServerPlayer j && j.level() == this.level() ? j : null;
	}

	/** Donde estan la cara y los ojos (para las miradas y la camara). */
	public Vec3 cara() {
		return new Vec3(this.getX(), this.getY() + 1.95, this.getZ());
	}

	/** true si `j` lo esta mirando a la cara (con o sin camara) y sin paredes en medio. */
	public boolean mirado(ServerPlayer j) {
		if (j.isSpectator() || j.level() != this.level()) {
			return false;
		}
		Vec3 ojo = j.getEyePosition();
		Vec3 hacia = this.cara().subtract(ojo);
		double d = hacia.length();
		if (d > ALCANCE_MIRADA || d < 0.01) {
			return false;
		}
		return j.getViewVector(1.0F).dot(hacia.scale(1.0 / d)) > MIRADA && j.hasLineOfSight(this);
	}

	@Override
	public void tick() {
		super.tick();
		if (!(this.level() instanceof ServerLevel nivel)) {
			return;
		}
		if (!Ambiente.get().apagon(nivel) || this.tickCount > 20 * 150) {
			this.discard(); // vuelve la luz: ya no esta
			return;
		}
		ServerPlayer obj = this.objetivo();
		if (obj == null || obj.distanceTo(this) > 56 || obj.isSpectator()
			|| net.backrooms.evento.fase.Fases.get().viajando(obj)) {
			this.discard();
			return;
		}
		this.getLookControl().setLookAt(obj, 30.0F, 30.0F);
		boolean grabado = false;
		for (ServerPlayer j : nivel.players()) {
			if (!this.mirado(j)) {
				continue;
			}
			if (HerramientasServidor.get().camara(j)) {
				grabado = true; // por el visor no pasa nada
			} else if (!j.isCreative()) {
				this.flashbang(j);
				this.discard();
				return;
			}
		}
		if (grabado) {
			this.getNavigation().stop();
			this.quieto++;
			return;
		}
		this.quieto = 0;
		if (this.tickCount % 10 == 0) {
			this.getNavigation().moveTo(obj, 1.0);
		}
		if (this.distanceTo(obj) < 1.4 && !obj.isCreative()) {
			this.alcanzar(obj);
		}
	}

	private void flashbang(ServerPlayer j) {
		ServerPlayNetworking.send(j, new Susto(Susto.FLASH));
		j.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20 * 3, 0, false, false, false));
		j.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20 * 3, 1, false, false, false));
		this.level().playSound(null, this.getX(), this.getEyeY(), this.getZ(), Sonidos.SMILER_FLASH, SoundSource.HOSTILE, 2.0F, 1.0F);
		Supervivencia.get().asustar(j, 12.0F);
	}

	private void alcanzar(ServerPlayer j) {
		ServerPlayNetworking.send(j, new Susto(Susto.CARA));
		this.level().playSound(null, this.getX(), this.getEyeY(), this.getZ(), Sonidos.SMILER_GRITO, SoundSource.HOSTILE, 2.5F, 1.0F);
		j.hurtServer((ServerLevel) this.level(), this.damageSources().mobAttack(this), 7.0F);
		Supervivencia.get().asustar(j, 20.0F);
		this.discard();
	}

	@Override
	public boolean hurtServer(ServerLevel nivel, DamageSource fuente, float cantidad) {
		return fuente.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && super.hurtServer(nivel, fuente, cantidad);
	}

	@Override
	public boolean removeWhenFarAway(double distancia) {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void doPush(net.minecraft.world.entity.Entity otra) {
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}
}
