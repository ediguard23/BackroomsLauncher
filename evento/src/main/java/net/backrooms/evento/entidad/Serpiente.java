package net.backrooms.evento.entidad;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * La Serpiente, la entidad de las Piscinas: un cuerpo larguisimo de azulejos
 * blancos con la junta azul que se alza del agua, un craneo con los ojos cian
 * que brillan y dos pares de brazos de hueso (modelo y textura en
 * tools/texturas/entidades.js).
 *
 * De momento es solo el modelo: no piensa, no se mueve y no suena (sin IA,
 * animaciones ni sonidos; eso viene despues). No aparece sola en ningun sitio:
 * el staff la pone con /backrooms invocar serpiente. No se le puede hacer dano
 * (salvo /kill) ni empujar, y no desaparece al alejarse.
 */
public class Serpiente extends PathfinderMob {
	public Serpiente(EntityType<? extends Serpiente> tipo, Level nivel) {
		super(tipo, nivel);
		this.xpReward = 0;
		this.setPersistenceRequired();
		this.setNoAi(true);
		this.setSilent(true);
		this.setInvulnerable(true);
	}

	public static AttributeSupplier.Builder atributos() {
		return PathfinderMob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, 100.0)
			.add(Attributes.MOVEMENT_SPEED, 0.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean removeWhenFarAway(double distancia) {
		return false;
	}

	@Override
	protected @Nullable SoundEvent getHurtSound(DamageSource dano) {
		return null;
	}

	@Override
	protected @Nullable SoundEvent getDeathSound() {
		return null;
	}
}
