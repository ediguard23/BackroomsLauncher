package net.backrooms.evento.entidad;

import net.backrooms.evento.BackroomsEvento;
import net.backrooms.evento.Sonidos;
import net.backrooms.evento.fase.Fases;
import net.backrooms.evento.mision.Misiones;
import net.backrooms.evento.red.Mordisco;
import net.backrooms.evento.supervivencia.Supervivencia;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * La Serpiente, la entidad de las Piscinas: un cuerpo larguisimo de azulejos con un craneo
 * de ojos cian y brazos de hueso (modelo en tools/texturas/entidades.js, animaciones en
 * ModeloSerpiente). Las pone Habitantes: una por cada 10 jugadores en las Piscinas.
 *
 * Ronda despacio por salas y piscinas. Detecta jugadores a 25 bloques como mucho (si los
 * ve; pegados a ella, a 8, aunque no) y va a por el mas cercano; si otro se queda claramente
 * mas cerca (3 bloques) cambia de presa, pero no mas de una vez cada 2 s: dos a la misma
 * distancia no la vuelven loca. Si te pierde de vista 5 s o te alejas mas de 28, te deja.
 *
 * Velocidad: la base es la de la Bacteria cazando en la fase 1 (5,5 bloques/s); por el
 * azulejo, un 15 % menos (4,7: corriendo se le escapa uno) y en el agua un 25 % mas (6,9:
 * en el agua no hay quien se le escape). Las cuentas, en ATRIBUTO y AGUA_MOD.
 *
 * Se mueve como la Bacteria: gira de golpe hacia donde va (Movimiento), sin las curvas
 * abiertas de antes, y en el agua frena enseguida (ARRASTRE), sin patinar.
 *
 * Si te alcanza te mata con un ataque propio (MORDISCO_*): se echa atras, se lanza con la
 * mandibula abierta, muerde y sacude, y mueres devorado; quien muere lo ve todo (Mordisco,
 * MordiscoCliente). Luego sigue a lo suyo. No se le puede hacer dano (solo /kill).
 */
public class Serpiente extends Monster {
	/** Bloques por segundo de base (cazando). */
	private static final double BASE_BPS = 5.5;
	private static final double TIERRA = 0.85;
	private static final double AGUA = 1.25;
	/** En el agua conserva esto de la velocidad cada tick (el ajolote, 0,9: patinaba). */
	private static final double ARRASTRE = 0.75;
	/** Grados por tick que gira nadando (en seco, 90 como cualquier mob). */
	private static final float GIRO_AGUA = 35.0F;
	/** Modificador de velocidad paseando: ~1,8 bloques/s de base (la velocidad va con su cuadrado). */
	private static final double PASEO = Math.sqrt(1.8 / BASE_BPS);
	/** Distancias de deteccion: si te ve; pegado a ella, aunque no te vea; y hasta donde te sigue. */
	private static final double DETECCION = 25.0;
	private static final double OIDO = 8.0;
	private static final double MANTENER = 28.0;
	/** Para cambiar de presa, la nueva tiene que estar esto mas cerca, y no antes de CAMBIO_TICKS. */
	private static final double CAMBIO = 3.0;
	private static final int CAMBIO_TICKS = 40;
	/** Ticks sin verte para dejarte. */
	private static final int OLVIDA = 100;
	/** A esta distancia (horizontal, desde su caja: la cabeza esta encima) te alcanza. */
	private static final double ALCANCE = 2.8;
	/** Despues de matar, este rato sin cazar. */
	private static final int DESCANSO = 20 * 6;

	/* el ataque, en ticks desde que empieza */
	public static final int AT_ATRAS = 7;
	public static final int AT_MUERDE = 13;
	public static final int AT_MATA = 24;
	public static final int AT_FIN = 34;

	private static final EntityDataAccessor<Boolean> CAZANDO = SynchedEntityData.defineId(Serpiente.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> ATACANDO = SynchedEntityData.defineId(Serpiente.class, EntityDataSerializers.BOOLEAN);

	private int ultimoCambio = -1000;
	private int ultimoGrito = -1000;
	private int ultimaVista;
	private int siguienteCaza;
	private int siguientePaseo;
	private int descanso;
	private int recalcular;
	/** Servidor: ticks de ataque (-1 sin atacar) y a quien. */
	private int ataque = -1;
	/** Si guarda algo (la tarjeta roja de las Piscinas): ronda alrededor y vuelve. No se guarda. */
	private @Nullable BlockPos casa;
	private @Nullable ServerPlayer presa;
	/** Cliente: 0..1 suavizado de si caza, y ticks del ataque vistos aqui. */
	private float caza;
	private float cazaAntes;
	private int ataqueCliente = -1;

	public Serpiente(EntityType<? extends Serpiente> tipo, Level nivel) {
		super(tipo, nivel);
		this.xpReward = 0;
		this.setPersistenceRequired();
		this.moveControl = new Movimiento(this);
		this.lookControl = new SmoothSwimmingLookControl(this, 20);
		this.setPathfindingMalus(PathType.WATER, 0.0F);
	}

	/**
	 * Atributo de velocidad para BASE_BPS * TIERRA en el azulejo: en llano un mob avanza
	 * ~43,2·v² bloques/s (Bacteria#atributo).
	 */
	private static final double ATRIBUTO = Math.sqrt(BASE_BPS * TIERRA / 43.17);
	/**
	 * En el agua (travelInWater) cada tick empuja getSpeed()·|entrada| = (v·k)·(0,98·v) y
	 * conserva ARRASTRE de lo que llevaba: avanza 20·0,98·k·v² / (1 - ARRASTRE) bloques/s,
	 * con v el atributo y k este modificador, que sale de querer BASE_BPS * AGUA.
	 */
	private static final double AGUA_MOD = BASE_BPS * AGUA * (1.0 - ARRASTRE) / (20.0 * 0.98 * ATRIBUTO * ATRIBUTO);

	public static AttributeSupplier.Builder atributos() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 300.0)
			.add(Attributes.MOVEMENT_SPEED, ATRIBUTO)
			.add(Attributes.ATTACK_DAMAGE, 0.0)
			.add(Attributes.FOLLOW_RANGE, 40.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
			.add(Attributes.STEP_HEIGHT, 1.0);
	}

	@Override
	protected PathNavigation createNavigation(Level nivel) {
		return new AmphibiousPathNavigation(this, nivel);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(CAZANDO, false);
		builder.define(ATACANDO, false);
	}

	/* ---------------------------------------------------------- moverse */

	/**
	 * En el agua nada (como el ajolote): sin gravedad, con su propio empuje. Contra la orilla
	 * de una piscina se sube a ella (el ajolote no sabe: se quedaba pegada al borde mirando a
	 * quien estaba a 3 bloques).
	 */
	@Override
	protected void travelInWater(Vec3 entrada, double gravedad, boolean cayendo, double y) {
		this.moveRelative(this.getSpeed(), entrada);
		this.move(MoverType.SELF, this.getDeltaMovement());
		Vec3 v = this.getDeltaMovement().scale(ARRASTRE);
		if (this.horizontalCollision && this.orillaDelante()) {
			v = new Vec3(v.x, 0.3, v.z);
		}
		this.setDeltaMovement(v);
	}

	/**
	 * Como el de cualquier mob en seco (gira hasta 90 grados por tick y salta escalones);
	 * nadando gira GIRO_AGUA por tick y sube o baja derecho hacia el punto, sin cabecear
	 * despacio como el ajolote.
	 */
	private static final class Movimiento extends MoveControl {
		private final Serpiente s;

		Movimiento(Serpiente s) {
			super(s);
			this.s = s;
		}

		@Override
		public void tick() {
			if (!this.s.isInWater()) {
				this.s.setYya(0.0F);
				super.tick();
				return;
			}
			if (this.operation != Operation.MOVE_TO || this.s.getNavigation().isDone()) {
				this.s.setSpeed(0.0F);
				this.s.setXxa(0.0F);
				this.s.setYya(0.0F);
				this.s.setZza(0.0F);
				return;
			}
			double dx = this.wantedX - this.s.getX();
			double dy = this.wantedY - this.s.getY();
			double dz = this.wantedZ - this.s.getZ();
			double h = Math.sqrt(dx * dx + dz * dz);
			if (h * h + dy * dy < 2.5E-7) {
				this.s.setZza(0.0F);
				this.s.setYya(0.0F);
				return;
			}
			float giro = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
			this.s.setYRot(this.rotlerp(this.s.getYRot(), giro, GIRO_AGUA));
			this.s.yBodyRot = this.s.getYRot();
			float v = (float) (this.speedModifier * this.s.getAttributeValue(Attributes.MOVEMENT_SPEED));
			// si aun no mira hacia alli, casi no avanza: gira en el sitio en vez de trazar la curva
			float desvio = Math.abs(Mth.wrapDegrees(giro - this.s.getYRot()));
			float avance = desvio > 60.0F ? 0.25F : 1.0F;
			this.s.setSpeed(v * (float) AGUA_MOD);
			float cabeceo = (float) Mth.clamp(Mth.atan2(dy, Math.max(h, 0.5)), -1.1, 1.1);
			this.s.setXRot(this.rotlerp(this.s.getXRot(), -cabeceo * Mth.RAD_TO_DEG, 15.0F));
			this.s.setZza(Mth.cos(cabeceo) * v * avance);
			this.s.setYya(Mth.sin(cabeceo) * v);
		}
	}

	/** Si lo que tiene delante es un borde de 2 bloques como mucho sobre sus pies, con sitio encima. */
	private boolean orillaDelante() {
		float giro = this.getYRot() * Mth.DEG_TO_RAD;
		double fx = -Mth.sin(giro) * 0.4;
		double fz = Mth.cos(giro) * 0.4;
		for (double alto = 0.5; alto <= 2.0; alto += 0.5) {
			if (this.level().noCollision(this, this.getBoundingBox().move(fx, alto, fz))) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean canBreatheUnderwater() {
		return true;
	}

	@Override
	public boolean isPushedByFluid() {
		return false;
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
	public boolean canBeLeashed() {
		return false;
	}

	/* ------------------------------------------------------- el cerebro */

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide()) {
			this.cazaAntes = this.caza;
			this.caza += ((this.entityData.get(CAZANDO) ? 1.0F : 0.0F) - this.caza) * 0.12F;
			this.ataqueCliente = this.entityData.get(ATACANDO) ? this.ataqueCliente + 1 : -1;
		}
	}

	/** Lo que guarda (null: nada, ronda libre). */
	public void casa(@Nullable BlockPos casa) {
		this.casa = casa;
	}

	/** 0..1 interpolado entre ticks: cuanto esta cazando. */
	public float caza(float parcial) {
		return this.cazaAntes + (this.caza - this.cazaAntes) * parcial;
	}

	/** Cliente: ticks desde que empezo a atacar (con la fraccion del frame), o -1. */
	public float ataque(float parcial) {
		return this.ataqueCliente < 0 ? -1.0F : this.ataqueCliente + parcial;
	}

	@Override
	protected void customServerAiStep(ServerLevel nivel) {
		super.customServerAiStep(nivel);
		if (this.ataque >= 0) {
			this.tickAtaque(nivel);
			return;
		}
		if (this.tickCount % 10 == 0) {
			this.elegirPresa(nivel);
		}
		LivingEntity t = this.getTarget();
		this.entityData.set(CAZANDO, t != null);
		if (t instanceof ServerPlayer j) {
			this.cazar(nivel, j);
		} else {
			this.pasear();
		}
	}

	private boolean valido(ServerPlayer j) {
		return j.isAlive() && !j.isCreative() && !j.isSpectator() && j.level() == this.level() && !j.isPassenger()
			&& !Fases.get().viajando(j) && Misiones.get().enExpedicion(j);
	}

	/** El mas cercano de los que detecta; con cambio de presa solo si el nuevo esta claramente mas cerca. */
	private void elegirPresa(ServerLevel nivel) {
		if (this.tickCount < this.descanso) {
			this.setTarget(null);
			return;
		}
		ServerPlayer mejor = null;
		double dMejor = Double.MAX_VALUE;
		for (ServerPlayer j : nivel.players()) {
			if (!this.valido(j)) {
				continue;
			}
			double d = this.distanceTo(j);
			if (d > DETECCION || d >= dMejor) {
				continue;
			}
			if (d <= OIDO || this.hasLineOfSight(j)) {
				mejor = j;
				dMejor = d;
			}
		}
		LivingEntity actual = this.getTarget();
		if (actual instanceof ServerPlayer j && this.valido(j)) {
			double d = this.distanceTo(j);
			if (this.hasLineOfSight(j)) {
				this.ultimaVista = this.tickCount;
			}
			boolean sigue = d <= MANTENER && this.tickCount - this.ultimaVista < OLVIDA;
			if (sigue) {
				if (mejor != null && mejor != j && dMejor < d - CAMBIO && this.tickCount - this.ultimoCambio >= CAMBIO_TICKS) {
					this.ultimoCambio = this.tickCount;
					this.setTarget(mejor);
				}
				return;
			}
			// se le ha escapado
			this.playSound(Sonidos.SERPIENTE_GRUNIDO, 1.2F, 0.95F + this.random.nextFloat() * 0.1F);
		}
		this.setTarget(mejor);
		if (mejor != null) {
			this.ultimaVista = this.tickCount;
		}
	}

	@Override
	public void setTarget(@Nullable LivingEntity objetivo) {
		LivingEntity antes = this.getTarget();
		super.setTarget(objetivo);
		if (objetivo != null && antes == null && this.tickCount - this.ultimoGrito > 200) {
			// te ha visto
			this.ultimoGrito = this.tickCount;
			this.level().playSound(null, this.getX(), this.getEyeY(), this.getZ(), Sonidos.SERPIENTE_GRITO, SoundSource.HOSTILE, 3.0F, 0.9F + this.random.nextFloat() * 0.15F);
			this.siguienteCaza = this.tickCount + 50;
			if (objetivo instanceof ServerPlayer j) {
				Supervivencia.get().asustar(j, 4.0F);
			}
		}
	}

	private void cazar(ServerLevel nivel, ServerPlayer j) {
		this.getLookControl().setLookAt(j, 20.0F, 20.0F);
		if (this.tickCount >= this.siguienteCaza) {
			this.siguienteCaza = this.tickCount + 45 + this.random.nextInt(20);
			this.level().playSound(null, this.getX(), this.getEyeY(), this.getZ(), Sonidos.SERPIENTE_CAZA, SoundSource.HOSTILE, 2.0F, 0.9F + this.random.nextFloat() * 0.15F);
		}
		double dx = j.getX() - this.getX();
		double dz = j.getZ() - this.getZ();
		if (dx * dx + dz * dz <= ALCANCE * ALCANCE && Math.abs(j.getY() - this.getY()) < 2.5 && this.hasLineOfSight(j)) {
			this.atacar(nivel, j);
			return;
		}
		if (--this.recalcular <= 0) {
			// de cerca rehace el camino casi cada tick: si giras o te escondes tras una columna, lo nota
			this.recalcular = dx * dx + dz * dz < 144 ? 2 + this.random.nextInt(2) : 4 + this.random.nextInt(4);
			if (!this.getNavigation().moveTo(j, 1.0)) {
				this.recalcular += 6;
			}
		}
	}

	/**
	 * Sin presa: ronda despacio, de un sitio a otro, parandose a ratos. La mitad de las veces
	 * tira hacia donde esta el jugador mas cercano (si esta a mas de 30): sin ir a por nadie,
	 * acaba rondando por donde hay gente.
	 */
	private void pasear() {
		if (this.tickCount < this.siguientePaseo || !this.getNavigation().isDone()) {
			return;
		}
		this.siguientePaseo = this.tickCount + 80 + this.random.nextInt(160);
		if (this.random.nextFloat() < 0.25F) {
			return; // se queda quieta un rato
		}
		Vec3 p = null;
		if (this.casa != null) {
			// la guardiana: no se aleja de lo que guarda
			Vec3 c = Vec3.atBottomCenterOf(this.casa);
			p = this.position().distanceTo(c) > 10 ? c : DefaultRandomPos.getPosTowards(this, 8, 3, c, Math.PI);
		} else if (this.random.nextBoolean() && this.level() instanceof ServerLevel nivel) {
			ServerPlayer cerca = null;
			double dCerca = 140.0;
			for (ServerPlayer j : nivel.players()) {
				double d = this.distanceTo(j);
				if (d < dCerca && this.valido(j)) {
					cerca = j;
					dCerca = d;
				}
			}
			if (cerca != null && dCerca > 30.0) {
				p = DefaultRandomPos.getPosTowards(this, 16, 4, cerca.position(), Math.PI / 2);
			}
		}
		if (p == null) {
			p = DefaultRandomPos.getPos(this, 16, 4);
		}
		if (p != null) {
			this.getNavigation().moveTo(p.x, p.y, p.z, PASEO);
		}
	}

	/* ---------------------------------------------------------- el ataque */

	private void atacar(ServerLevel nivel, ServerPlayer j) {
		this.ataque = 0;
		this.presa = j;
		this.entityData.set(ATACANDO, true);
		this.entityData.set(CAZANDO, false);
		this.getNavigation().stop();
		this.setDeltaMovement(Vec3.ZERO);
		float giro = (float) (Mth.atan2(j.getZ() - this.getZ(), j.getX() - this.getX()) * Mth.RAD_TO_DEG) - 90.0F;
		this.setYRot(giro);
		this.yBodyRot = giro;
		this.yHeadRot = giro;
		// quieto mientras dura
		j.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, AT_FIN + 10, 9, false, false, false));
		if (ServerPlayNetworking.canSend(j, Mordisco.TYPE)) {
			ServerPlayNetworking.send(j, new Mordisco(this.getId()));
		}
		nivel.playSound(null, this.getX(), this.getEyeY(), this.getZ(), Sonidos.SERPIENTE_ATAQUE, SoundSource.HOSTILE, 3.0F, 0.95F + this.random.nextFloat() * 0.1F);
		nivel.playSound(null, j.getX(), j.getEyeY(), j.getZ(), Sonidos.VICTIMA_GRITO, SoundSource.PLAYERS, 2.2F, 0.95F + this.random.nextFloat() * 0.12F);
		Supervivencia.get().asustar(j, 30.0F);
		BackroomsEvento.LOG.info("La Serpiente ataca a {}", j.getGameProfile().name());
	}

	private void tickAtaque(ServerLevel nivel) {
		ServerPlayer j = this.presa;
		if (j == null || !j.isAlive() || j.isRemoved() || j.level() != nivel) {
			this.terminarAtaque(this.ataque < AT_MATA ? 60 : DESCANSO);
			return;
		}
		this.ataque++;
		this.getNavigation().stop();
		this.setDeltaMovement(this.getDeltaMovement().multiply(0, 1, 0));
		this.getLookControl().setLookAt(j, 60.0F, 60.0F);
		if (this.ataque == AT_MUERDE) {
			nivel.playSound(null, j.getX(), j.getEyeY(), j.getZ(), Sonidos.SERPIENTE_MORDISCO, SoundSource.HOSTILE, 2.6F, 0.95F + this.random.nextFloat() * 0.1F);
			nivel.getChunkSource().sendToTrackingPlayersAndSelf(j, new ClientboundHurtAnimationPacket(j));
			Vec3 cabeza = j.getEyePosition();
			nivel.sendParticles(new DustParticleOptions(0x5A0A06, 1.6F), cabeza.x, cabeza.y, cabeza.z, 22, 0.3, 0.3, 0.3, 0.0);
			Supervivencia.get().asustar(j, 20.0F);
		}
		if (this.ataque == AT_MATA) {
			BackroomsEvento.LOG.info("La Serpiente devora a {}", j.getGameProfile().name());
			DamageSource fuente = new DamageSource(nivel.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(Bacteria.DEVORADO), this);
			j.hurtServer(nivel, fuente, Float.MAX_VALUE);
		}
		if (this.ataque >= AT_FIN) {
			this.terminarAtaque(DESCANSO);
		}
	}

	private void terminarAtaque(int descanso) {
		this.ataque = -1;
		this.presa = null;
		this.entityData.set(ATACANDO, false);
		this.descanso = this.tickCount + descanso;
		this.setTarget(null);
	}

	/* ------------------------------------------------------------ dano */

	@Override
	public boolean doHurtTarget(ServerLevel nivel, net.minecraft.world.entity.Entity objetivo) {
		return false; // no pega: ataca (ver atacar)
	}

	@Override
	public boolean hurtServer(ServerLevel nivel, DamageSource fuente, float cantidad) {
		// ni armas, ni flechas, ni nada: solo /kill y el vacio
		return fuente.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && super.hurtServer(nivel, fuente, cantidad);
	}

	/* ------------------------------------------------------------ sonidos */

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return Sonidos.SERPIENTE_RESPIRA;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 150;
	}

	@Override
	public void playAmbientSound() {
		// cazando ya resopla, y atacando no
		if (!this.entityData.get(CAZANDO) && !this.entityData.get(ATACANDO)) {
			super.playAmbientSound();
		}
	}

	@Override
	protected float getSoundVolume() {
		return 1.4F;
	}

	@Override
	protected @Nullable SoundEvent getHurtSound(DamageSource dano) {
		return null;
	}

	@Override
	protected @Nullable SoundEvent getDeathSound() {
		return null;
	}

	/** El cuerpo que se arrastra por el azulejo: un roce cada pocos bloques, no a cada paso. */
	@Override
	protected void playStepSound(BlockPos pos, BlockState estado) {
		boolean caza = this.entityData.get(CAZANDO);
		this.playSound(Sonidos.SERPIENTE_PASO, caza ? 1.4F : 0.9F, 0.85F + this.random.nextFloat() * 0.25F);
	}

	@Override
	protected float nextStep() {
		return this.moveDist + 2.5F;
	}

	@Override
	protected SoundEvent getSwimSound() {
		return Sonidos.SERPIENTE_AGUA;
	}

	@Override
	protected SoundEvent getSwimSplashSound() {
		return Sonidos.SERPIENTE_AGUA;
	}

	@Override
	protected SoundEvent getSwimHighSpeedSplashSound() {
		return Sonidos.SERPIENTE_AGUA;
	}
}
