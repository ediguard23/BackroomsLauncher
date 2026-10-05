package net.backrooms.evento.entidad;

import net.backrooms.evento.Sonidos;
import net.backrooms.evento.ambiente.Ambiente;
import net.backrooms.evento.fase.Fase;
import net.backrooms.evento.supervivencia.Supervivencia;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * La Bacteria: la entidad del Nivel 0. Alta, flaca, negra, hecha de hilos de
 * Hay Bacillus. Vaga por los pasillos; si te ve (o te oye correr cerca) grita
 * y va a por ti, casi tan rapida como un jugador corriendo. Para escapar hay
 * que romper la linea de vista y aguantar: a los 8 s sin verte, te pierde.
 *
 * Con la alarma encendida esta mas furiosa: ve y oye mas lejos y corre mas
 * que tu (hay que esconderse, no basta con correr). En cada fase es mas
 * rapida (Fase.velocidad). No se le puede hacer dano.
 */
public class Bacteria extends Monster {
	private static final double VELOCIDAD = 0.29;
	/** Ticks sin verte para dejar de perseguirte. */
	private static final int OLVIDA = 160;

	private static final EntityDataAccessor<Boolean> CAZANDO = SynchedEntityData.defineId(Bacteria.class, EntityDataSerializers.BOOLEAN);

	private int sinVer;
	private int ultimoGrito = -1000;
	/** En el cliente: 0..1 suavizado de si esta cazando (para la animacion). */
	private float caza;
	private float cazaAntes;

	public Bacteria(EntityType<? extends Bacteria> tipo, Level nivel) {
		super(tipo, nivel);
		this.xpReward = 0;
		this.setPersistenceRequired();
	}

	public static AttributeSupplier.Builder atributos() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 200.0)
			.add(Attributes.MOVEMENT_SPEED, VELOCIDAD)
			.add(Attributes.ATTACK_DAMAGE, 9.0)
			.add(Attributes.ATTACK_KNOCKBACK, 0.6)
			.add(Attributes.FOLLOW_RANGE, 48.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
			.add(Attributes.STEP_HEIGHT, 1.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(CAZANDO, false);
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide()) {
			this.cazaAntes = this.caza;
			this.caza += ((this.entityData.get(CAZANDO) ? 1.0F : 0.0F) - this.caza) * 0.15F;
		}
	}

	/** 0..1 interpolado entre ticks: cuanto esta en modo caza. */
	public float caza(float parcial) {
		return this.cazaAntes + (this.caza - this.cazaAntes) * parcial;
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.45));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 5, false, false, (p, nivel) -> this.detecta(p)));
	}

	private boolean furiosa() {
		return Ambiente.get().alarma(this.level());
	}

	/** Te ve si estas a tiro y sin paredes en medio; te oye si corres cerca. */
	private boolean detecta(LivingEntity p) {
		if (!(p instanceof ServerPlayer j) || j.isCreative() || j.isSpectator()) {
			return false;
		}
		double d = this.distanceTo(j);
		boolean furiosa = this.furiosa();
		if (d <= (furiosa ? 34 : 22) && this.hasLineOfSight(j)) {
			return true;
		}
		return j.isSprinting() && d <= (furiosa ? 20 : 12);
	}

	@Override
	public void setTarget(@Nullable LivingEntity objetivo) {
		LivingEntity antes = this.getTarget();
		super.setTarget(objetivo);
		if (objetivo != null && antes == null && this.tickCount - this.ultimoGrito > 100) {
			// te ha visto
			this.ultimoGrito = this.tickCount;
			this.level().playSound(null, this.getX(), this.getEyeY(), this.getZ(), Sonidos.BACTERIA_GRITO, SoundSource.HOSTILE, 3.0F, 0.9F + this.random.nextFloat() * 0.2F);
			if (objetivo instanceof ServerPlayer j) {
				Supervivencia.get().asustar(j, 4.0F);
			}
		}
	}

	@Override
	protected void customServerAiStep(ServerLevel nivel) {
		super.customServerAiStep(nivel);
		// mas rapida en las fases altas y con la alarma
		Fase f = Fase.de(nivel);
		double v = VELOCIDAD * (f == null ? 1.0 : f.velocidad()) * (this.furiosa() ? 1.28 : 1.0);
		AttributeInstance a = this.getAttribute(Attributes.MOVEMENT_SPEED);
		if (a != null && Math.abs(a.getBaseValue() - v) > 1e-4) {
			a.setBaseValue(v);
		}
		LivingEntity t = this.getTarget();
		this.entityData.set(CAZANDO, t != null);
		if (t != null) {
			if (!t.isAlive() || (t instanceof ServerPlayer j && (j.isCreative() || j.isSpectator())) || t.level() != nivel) {
				this.setTarget(null);
			} else if (this.hasLineOfSight(t)) {
				this.sinVer = 0;
			} else if (++this.sinVer > OLVIDA) {
				this.sinVer = 0;
				this.setTarget(null);
			}
		}
	}

	@Override
	public boolean doHurtTarget(ServerLevel nivel, Entity objetivo) {
		boolean dio = super.doHurtTarget(nivel, objetivo);
		if (dio && objetivo instanceof ServerPlayer j) {
			Supervivencia.get().asustar(j, 10.0F);
		}
		return dio;
	}

	@Override
	public boolean hurtServer(ServerLevel nivel, DamageSource fuente, float cantidad) {
		// solo /kill y el vacio
		return fuente.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && super.hurtServer(nivel, fuente, cantidad);
	}

	@Override
	public boolean removeWhenFarAway(double distancia) {
		return false;
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return Sonidos.BACTERIA_ACECHO;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 140;
	}

	@Override
	protected float getSoundVolume() {
		return 1.6F;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState estado) {
		this.playSound(Sonidos.BACTERIA_PASOS, 0.7F, 0.8F + this.random.nextFloat() * 0.4F);
	}

	@Override
	public boolean canBeLeashed() {
		return false;
	}
}
