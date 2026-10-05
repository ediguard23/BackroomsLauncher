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
 * Hay Bacillus. Vaga por los pasillos; si te ve (o te oye correr cerca) ruge
 * y va a por ti, mas rapida que alguien andando y casi tan rapida como un
 * jugador corriendo: mientras te persigue se la oye jadear y pisar fuerte.
 * Para escapar hay que correr, romper la linea de vista y aguantar: a los 8 s
 * sin verte, te pierde.
 *
 * Con la alarma encendida esta mas furiosa: ve y oye mas lejos y corre mas
 * que tu (hay que esconderse, no basta con correr). En cada fase es mas
 * rapida (Fase.velocidad). No se le puede hacer dano.
 */
public class Bacteria extends Monster {
	/**
	 * Bloques por segundo cazando en la fase 1. Un jugador anda a 4,3 y corre
	 * a 5,6: si corres en linea recta te le escapas por muy poco, y en cuanto
	 * se te acaba la estamina te alcanza.
	 */
	private static final double CAZA_BPS = 5.3;
	/** Paseando por los pasillos sin presa. */
	private static final double PASEO_BPS = 1.6;
	/** Con la alarma corre esto mas. */
	private static final double FURIA = 1.15;
	/** Ticks sin verte para dejar de perseguirte. */
	private static final int OLVIDA = 160;
	/** Cada cuanto jadea mientras te persigue. */
	private static final int JADEO = 40;

	private static final EntityDataAccessor<Boolean> CAZANDO = SynchedEntityData.defineId(Bacteria.class, EntityDataSerializers.BOOLEAN);

	private int sinVer;
	private int ultimoGrito = -1000;
	private int siguienteJadeo;
	/** En el cliente: 0..1 suavizado de si esta cazando (para la animacion). */
	private float caza;
	private float cazaAntes;

	public Bacteria(EntityType<? extends Bacteria> tipo, Level nivel) {
		super(tipo, nivel);
		this.xpReward = 0;
		this.setPersistenceRequired();
	}

	/**
	 * El atributo de velocidad de un mob no va en bloques por segundo, ni
	 * siquiera es lineal: en llano (moqueta, rozamiento 0,6) un mob avanza
	 * 20 · 0,98·v² / (1 − 0,546) ≈ 43,2·v² bloques por segundo (Mob#setSpeed
	 * usa v como empuje y como multiplicador a la vez). Esto lo invierte.
	 */
	static double atributo(double bps) {
		return Math.sqrt(bps / 43.17);
	}

	public static AttributeSupplier.Builder atributos() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 200.0)
			.add(Attributes.MOVEMENT_SPEED, atributo(CAZA_BPS))
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
		// el modificador multiplica el atributo, y la velocidad va con su cuadrado
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, Math.sqrt(PASEO_BPS / CAZA_BPS)));
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
			this.level().playSound(null, this.getX(), this.getEyeY(), this.getZ(), Sonidos.BACTERIA_GRITO, SoundSource.HOSTILE, 3.0F, 0.88F + this.random.nextFloat() * 0.14F);
			this.siguienteJadeo = this.tickCount + 70;
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
		double v = atributo(CAZA_BPS * (f == null ? 1.0 : f.velocidad()) * (this.furiosa() ? FURIA : 1.0));
		AttributeInstance a = this.getAttribute(Attributes.MOVEMENT_SPEED);
		if (a != null && Math.abs(a.getBaseValue() - v) > 1e-4) {
			a.setBaseValue(v);
		}
		LivingEntity t = this.getTarget();
		this.entityData.set(CAZANDO, t != null);
		if (t != null && this.tickCount >= this.siguienteJadeo) {
			// jadea mientras te persigue: se la oye venir aunque no la veas
			this.siguienteJadeo = this.tickCount + JADEO + this.random.nextInt(12);
			this.level().playSound(null, this.getX(), this.getEyeY(), this.getZ(), Sonidos.BACTERIA_CAZA, SoundSource.HOSTILE, 2.0F, 0.9F + this.random.nextFloat() * 0.15F);
		}
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
			nivel.playSound(null, j.getX(), j.getEyeY(), j.getZ(), Sonidos.BACTERIA_GOLPE, SoundSource.HOSTILE, 1.2F, 0.9F + this.random.nextFloat() * 0.2F);
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
	public void playAmbientSound() {
		// cazando ya suenan los jadeos
		if (!this.entityData.get(CAZANDO)) {
			super.playAmbientSound();
		}
	}

	@Override
	protected float getSoundVolume() {
		return 1.6F;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState estado) {
		// cazando pisa fuerte: se la oye correr detras de ti
		boolean caza = this.entityData.get(CAZANDO);
		this.playSound(Sonidos.BACTERIA_PASOS, caza ? 1.5F : 0.7F, (caza ? 0.75F : 0.8F) + this.random.nextFloat() * 0.3F);
	}

	@Override
	public boolean canBeLeashed() {
		return false;
	}
}
