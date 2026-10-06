package net.backrooms.evento.entidad;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.backrooms.evento.BackroomsEvento;
import net.backrooms.evento.Sonidos;
import net.backrooms.evento.ambiente.Ambiente;
import net.backrooms.evento.escondite.Arrastre;
import net.backrooms.evento.escondite.Escondites;
import net.backrooms.evento.fase.Fase;
import net.backrooms.evento.mundo.GeneradorNivel0;
import net.backrooms.evento.red.Agarrado;
import net.backrooms.evento.supervivencia.Supervivencia;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * La Bacteria: la entidad del Nivel 0. Alta, flaca, negra, hecha de hilos de
 * Hay Bacillus. Vaga por los pasillos; si te ve (o te oye correr cerca) ruge
 * y va a por ti, mas rapida que alguien andando y casi tan rapida como un
 * jugador corriendo: mientras te persigue se la oye jadear y pisar fuerte.
 *
 * No es adivina: persigue a donde te vio por ultima vez. Si doblas una esquina y
 * no te vuelve a ver, llega alli, mira alrededor y a los pocos segundos te deja
 * (y un rato no te "oye" correr). Agachado o arrastrandote te ve a la mitad de
 * distancia. Si te metes en un hueco de la pared (escondite/Hueco) renuncia del
 * todo: grune y se va lejos, y en 30 s no vuelve a por ti.
 *
 * No pega: si te alcanza te AGARRA. Te levanta del suelo con los brazos, te
 * acerca a la boca, muerde tres veces y te devora (unos 3 s; nadie puede
 * soltarte). Mientras come, los demas pueden huir.
 *
 * Con la alarma encendida esta mas furiosa: ve y oye mas lejos y corre mas
 * que tu. En cada fase es mas rapida (Fase.velocidad). No se le puede hacer dano.
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
	/** Yendose lejos de un escondite. */
	private static final double RETIRADA_BPS = 3.4;
	/** Con la alarma corre esto mas. */
	private static final double FURIA = 1.15;
	/** Ticks sin verte (yendo a donde te vio) para dejarte. */
	private static final int OLVIDA = 100;
	/** Ticks mirando alrededor donde te perdio antes de dejarte. */
	private static final int BUSCA = 50;
	/** Tras perderte, este rato no te oye correr (solo te encuentra si te ve). */
	private static final int SORDA = 100;
	/** Tras esconderte en un hueco, este rato no vuelve a por ti. */
	private static final int IGNORA_ESCONDIDO = 20 * 30;
	/** Despues de comer, este rato sin cazar. */
	private static final int DESCANSO = 20 * 8;
	/** Cada cuanto jadea mientras te persigue. */
	private static final int JADEO = 40;
	/** A esta distancia (en horizontal) te agarra. Sus brazos llegan a 1,5. */
	private static final double ALCANCE = 2.1;

	/* el agarre, en ticks desde que te coge */
	public static final int AG_LEVANTA = 8;
	public static final int AG_ARRIBA = 26;
	public static final int AG_COME = 30;
	public static final int[] AG_MUERDE = {34, 42, 50};
	public static final int AG_FIN = 56;
	/** A cuanto delante te sujeta (lo largo de sus brazos). */
	public static final double AG_DELANTE = 1.55;

	public static final ResourceKey<DamageType> DEVORADO = ResourceKey.create(Registries.DAMAGE_TYPE, BackroomsEvento.id("devorado"));

	private static final EntityDataAccessor<Boolean> CAZANDO = SynchedEntityData.defineId(Bacteria.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> AGARRANDO = SynchedEntityData.defineId(Bacteria.class, EntityDataSerializers.BOOLEAN);

	private int ultimoGrito = -1000;
	private int siguienteJadeo;
	private int descanso;
	/** Jugadores a los que no hace caso hasta tal tick (se escondieron). */
	private final Map<UUID, Integer> ignorados = new HashMap<>();
	/** Jugadores a los que no oye correr hasta tal tick (los perdio). */
	private final Map<UUID, Integer> sorda = new HashMap<>();
	/** A donde se va tras un escondite, y hasta cuando lo intenta. */
	private @Nullable Vec3 retirada;
	private int retiradaHasta;
	/** Servidor: ticks de agarre (-1 sin presa), a quien y mirando hacia donde. */
	private int agarre = -1;
	private @Nullable UUID presa;
	private float giroAgarre;
	/** Cliente: 0..1 suavizado de si esta cazando, y ticks de agarre vistos aqui. */
	private float caza;
	private float cazaAntes;
	private int agarreCliente = -1;

	public Bacteria(EntityType<? extends Bacteria> tipo, Level nivel) {
		super(tipo, nivel);
		this.xpReward = 0;
		this.setPersistenceRequired();
	}

	/** Si alguien se desconecta mientras le estan comiendo, no se libra. */
	public static void registrar() {
		ServerPlayConnectionEvents.DISCONNECT.register((h, s) -> {
			if (h.player.getVehicle() instanceof Bacteria b) {
				b.devorar(h.player);
			}
		});
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
			.add(Attributes.ATTACK_DAMAGE, 0.0)
			.add(Attributes.FOLLOW_RANGE, 64.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
			.add(Attributes.STEP_HEIGHT, 1.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(CAZANDO, false);
		builder.define(AGARRANDO, false);
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide()) {
			this.cazaAntes = this.caza;
			this.caza += ((this.entityData.get(CAZANDO) ? 1.0F : 0.0F) - this.caza) * 0.15F;
			this.agarreCliente = this.entityData.get(AGARRANDO) ? this.agarreCliente + 1 : -1;
		}
	}

	/** 0..1 interpolado entre ticks: cuanto esta en modo caza. */
	public float caza(float parcial) {
		return this.cazaAntes + (this.caza - this.cazaAntes) * parcial;
	}

	/** Cliente: ticks desde que agarro a alguien (con la fraccion del frame), o -1. */
	public float agarre(float parcial) {
		return this.agarreCliente < 0 ? -1.0F : this.agarreCliente + parcial;
	}

	public boolean agarrando() {
		return this.level().isClientSide() ? this.agarreCliente >= 0 : this.agarre >= 0;
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new Retirarse(this));
		this.goalSelector.addGoal(1, new Cazar(this));
		// el modificador multiplica el atributo, y la velocidad va con su cuadrado
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, Math.sqrt(PASEO_BPS / CAZA_BPS)));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 5, false, false, (p, nivel) -> this.detecta(p)));
	}

	private boolean furiosa() {
		return Ambiente.get().alarma(this.level());
	}

	/** Agachado o arrastrandose: se ve menos. */
	private static boolean sigilo(Player j) {
		return j.getPose() == Pose.CROUCHING || j.getPose() == Pose.SWIMMING;
	}

	/** Te ve si estas a tiro y sin paredes en medio; te oye si corres cerca. */
	private boolean detecta(LivingEntity p) {
		if (!(p instanceof ServerPlayer j) || j.isCreative() || j.isSpectator() || !j.isAlive() || j.isPassenger()) {
			return false;
		}
		if (this.agarre >= 0 || this.retirada != null || this.tickCount < this.descanso || Escondites.escondido(j)) {
			return false;
		}
		Integer ignorado = this.ignorados.get(j.getUUID());
		if (ignorado != null && ignorado > this.tickCount) {
			return false;
		}
		double d = this.distanceTo(j);
		boolean furiosa = this.furiosa();
		double vista = (furiosa ? 34 : 22) * (sigilo(j) ? 0.5 : 1.0);
		if (d <= vista && this.hasLineOfSight(j)) {
			return true;
		}
		Integer sorda = this.sorda.get(j.getUUID());
		if (sorda != null && sorda > this.tickCount) {
			return false;
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
		if (this.agarre >= 0) {
			this.entityData.set(CAZANDO, false);
			this.tickAgarre(nivel);
			return;
		}
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
			} else if (t instanceof ServerPlayer j && Escondites.escondido(j)) {
				this.retirarse(nivel, j);
			}
		}
		if (this.tickCount % 200 == 0) {
			this.ignorados.values().removeIf(hasta -> hasta <= this.tickCount);
			this.sorda.values().removeIf(hasta -> hasta <= this.tickCount);
		}
	}

	/* ------------------------------------------------- perder y retirarse */

	/** No te encuentra: te deja, y un rato no te oye correr. */
	void perder(LivingEntity t) {
		this.setTarget(null);
		this.sorda.put(t.getUUID(), this.tickCount + SORDA);
		this.level().playSound(null, this.getX(), this.getEyeY(), this.getZ(), Sonidos.BACTERIA_RENUNCIA, SoundSource.HOSTILE, 1.6F, 0.95F + this.random.nextFloat() * 0.1F);
	}

	/**
	 * Te has metido en un hueco: renuncia y se va lejos (40-55 bloques, hacia el
	 * lado contrario), sin acampar delante. En 30 s no vuelve a por ti.
	 */
	private void retirarse(ServerLevel nivel, ServerPlayer j) {
		this.setTarget(null);
		this.ignorados.put(j.getUUID(), this.tickCount + IGNORA_ESCONDIDO);
		this.getNavigation().stop();
		this.level().playSound(null, this.getX(), this.getEyeY(), this.getZ(), Sonidos.BACTERIA_RENUNCIA, SoundSource.HOSTILE, 2.4F, 0.85F + this.random.nextFloat() * 0.1F);
		Vec3 lejos = this.position().subtract(j.position()).multiply(1, 0, 1);
		if (lejos.lengthSqr() < 0.25) {
			double ang = this.random.nextDouble() * Math.PI * 2;
			lejos = new Vec3(Math.cos(ang), 0, Math.sin(ang));
		}
		// un poco de abanico para que no vayan todas por el mismo sitio
		double ang = Math.atan2(lejos.z, lejos.x) + (this.random.nextDouble() - 0.5) * 0.9;
		double d = 40 + this.random.nextDouble() * 15;
		int x = (int) Math.round(j.getX() + Math.cos(ang) * d);
		int z = (int) Math.round(j.getZ() + Math.sin(ang) * d);
		BlockPos p = nivel.getChunkSource().getGenerator() instanceof GeneradorNivel0 gen
			? gen.puntoLibre(nivel.getChunkSource().randomState(), x, z)
			: new BlockPos(x, this.getBlockY(), z);
		this.retirada = Vec3.atBottomCenterOf(p);
		this.retiradaHasta = this.tickCount + 20 * 30;
	}

	/* ------------------------------------------------------------ el agarre */

	/** Te tiene al alcance de los brazos, y no hay pared en medio. */
	boolean alAlcance(ServerPlayer j) {
		if (j.isCreative() || j.isSpectator() || !j.isAlive() || j.isPassenger() || Escondites.escondido(j)) {
			return false;
		}
		double dx = j.getX() - this.getX();
		double dz = j.getZ() - this.getZ();
		return dx * dx + dz * dz <= ALCANCE * ALCANCE && Math.abs(j.getY() - this.getY()) < 1.5 && this.hasLineOfSight(j);
	}

	void agarrar(ServerPlayer j) {
		this.presa = j.getUUID();
		this.agarre = 0;
		this.entityData.set(AGARRANDO, true);
		this.getNavigation().stop();
		this.setDeltaMovement(Vec3.ZERO);
		this.goalSelector.disableControlFlag(Goal.Flag.MOVE);
		this.goalSelector.disableControlFlag(Goal.Flag.LOOK);
		this.goalSelector.disableControlFlag(Goal.Flag.JUMP);
		this.giroAgarre = (float) (Mth.atan2(j.getZ() - this.getZ(), j.getX() - this.getX()) * Mth.RAD_TO_DEG) - 90.0F;
		this.mirar();
		Arrastre.pedir(j, false);
		if (j.isPassenger()) {
			j.stopRiding();
		}
		j.startRiding(this, true, true);
		if (ServerPlayNetworking.canSend(j, Agarrado.TYPE)) {
			ServerPlayNetworking.send(j, new Agarrado(this.getId()));
		}
		ServerLevel nivel = (ServerLevel) this.level();
		nivel.playSound(null, this.getX(), this.getEyeY(), this.getZ(), Sonidos.BACTERIA_AGARRE, SoundSource.HOSTILE, 3.0F, 0.95F + this.random.nextFloat() * 0.1F);
		nivel.playSound(null, j.getX(), j.getEyeY(), j.getZ(), Sonidos.VICTIMA_GRITO, SoundSource.PLAYERS, 2.2F, 0.95F + this.random.nextFloat() * 0.12F);
		Supervivencia.get().asustar(j, 30.0F);
		BackroomsEvento.LOG.info("La Bacteria agarra a {}", j.getGameProfile().name());
	}

	private void mirar() {
		this.setYRot(this.giroAgarre);
		this.yBodyRot = this.giroAgarre;
		this.yHeadRot = this.giroAgarre;
		this.setXRot(0.0F);
	}

	private void tickAgarre(ServerLevel nivel) {
		ServerPlayer j = this.presa == null ? null : nivel.getServer().getPlayerList().getPlayer(this.presa);
		if (j == null || !j.isAlive() || j.getVehicle() != this) {
			this.soltar();
			return;
		}
		this.agarre++;
		this.mirar();
		this.setDeltaMovement(Vec3.ZERO);
		this.getNavigation().stop();
		if (this.agarre == AG_LEVANTA) {
			nivel.playSound(null, this.getX(), this.getEyeY(), this.getZ(), Sonidos.BACTERIA_LEVANTA, SoundSource.HOSTILE, 2.4F, 0.95F + this.random.nextFloat() * 0.1F);
		}
		if (this.agarre == AG_COME) {
			nivel.playSound(null, this.getX(), this.getEyeY(), this.getZ(), Sonidos.BACTERIA_DEVORA, SoundSource.HOSTILE, 2.6F, 1.0F);
		}
		for (int k = 0; k < AG_MUERDE.length; k++) {
			if (this.agarre == AG_MUERDE[k]) {
				this.muerde(nivel, j, k);
			}
		}
		if (this.agarre >= AG_FIN) {
			this.devorar(j);
		}
	}

	private void muerde(ServerLevel nivel, ServerPlayer j, int k) {
		nivel.playSound(null, j.getX(), j.getEyeY(), j.getZ(), Sonidos.BACTERIA_MORDISCO, SoundSource.HOSTILE, 2.4F, 0.9F + k * 0.06F + this.random.nextFloat() * 0.08F);
		nivel.getChunkSource().sendToTrackingPlayersAndSelf(j, new ClientboundHurtAnimationPacket(j));
		Vec3 cabeza = j.getEyePosition();
		nivel.sendParticles(new DustParticleOptions(0x4A0A06, 1.6F), cabeza.x, cabeza.y, cabeza.z, 18, 0.25, 0.25, 0.25, 0.0);
		nivel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, net.backrooms.evento.bloques.Bloques.BACILO.defaultBlockState()),
			cabeza.x, cabeza.y, cabeza.z, 10, 0.2, 0.2, 0.2, 0.1);
		Supervivencia.get().asustar(j, 20.0F);
	}

	/** Se lo come: muere devorado (lo que pase despues es la eliminacion de siempre). */
	void devorar(ServerPlayer j) {
		ServerLevel nivel = (ServerLevel) this.level();
		BackroomsEvento.LOG.info("La Bacteria devora a {}", j.getGameProfile().name());
		j.stopRiding();
		this.terminar();
		DamageSource fuente = new DamageSource(nivel.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DEVORADO), this);
		j.hurtServer(nivel, fuente, Float.MAX_VALUE);
		this.descanso = this.tickCount + DESCANSO;
	}

	/** Se le escapa la presa (se desconecto, cambio de mundo...): sin comerla. */
	private void soltar() {
		if (this.presa != null && this.level() instanceof ServerLevel nivel) {
			ServerPlayer j = nivel.getServer().getPlayerList().getPlayer(this.presa);
			if (j != null && j.getVehicle() == this) {
				j.stopRiding();
			}
		}
		this.terminar();
	}

	private void terminar() {
		this.agarre = -1;
		this.presa = null;
		this.entityData.set(AGARRANDO, false);
		this.goalSelector.enableControlFlag(Goal.Flag.MOVE);
		this.goalSelector.enableControlFlag(Goal.Flag.LOOK);
		this.goalSelector.enableControlFlag(Goal.Flag.JUMP);
		this.setTarget(null);
	}

	/**
	 * Donde lleva a quien ha agarrado (en el servidor con sus ticks y en el
	 * cliente con los suyos, para que se vea suave): a lo largo de sus brazos
	 * delante de el, subiendolo hasta la boca, con un tiron hacia ella en cada
	 * mordisco. Las poses del modelo (ModeloBacteria) estan hechas para estas
	 * mismas distancias.
	 */
	@Override
	protected void positionRider(Entity pasajero, Entity.MoveFunction mover) {
		if (!this.hasPassenger(pasajero)) {
			return;
		}
		float t = Math.max(0, this.level().isClientSide() ? this.agarreCliente : this.agarre);
		double alto = 0.05 + 1.25 * suave((t - AG_LEVANTA) / (float) (AG_ARRIBA - AG_LEVANTA));
		double delante = AG_DELANTE - 0.14 * mordisco(t);
		double tiembla = t > AG_LEVANTA ? Math.sin(t * 2.9) * 0.025 : 0.0;
		float giro = this.getYRot() * Mth.DEG_TO_RAD;
		double dx = -Mth.sin(giro) * delante + Mth.cos(giro) * tiembla;
		double dz = Mth.cos(giro) * delante + Mth.sin(giro) * tiembla;
		mover.accept(pasajero, this.getX() + dx, this.getY() + alto, this.getZ() + dz);
	}

	private static float suave(float x) {
		float k = Mth.clamp(x, 0.0F, 1.0F);
		return k * k * (3.0F - 2.0F * k);
	}

	/** 0..1: el golpe de cada mordisco (sube de golpe y cae en 4 ticks). */
	public static float mordisco(float t) {
		float m = 0.0F;
		for (int b : AG_MUERDE) {
			float u = t - (b - 2);
			if (u >= 0 && u < 6) {
				m = Math.max(m, u < 2 ? u / 2.0F : 1.0F - (u - 2) / 4.0F);
			}
		}
		return m;
	}

	@Override
	protected boolean canAddPassenger(Entity pasajero) {
		return this.agarre >= 0 && super.canAddPassenger(pasajero);
	}

	@Override
	public boolean isPushable() {
		return !this.agarrando() && super.isPushable();
	}

	@Override
	public boolean doHurtTarget(ServerLevel nivel, Entity objetivo) {
		// no pega: agarra (ver Cazar)
		return false;
	}

	@Override
	public boolean hurtServer(ServerLevel nivel, DamageSource fuente, float cantidad) {
		// solo /kill y el vacio
		return fuente.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && super.hurtServer(nivel, fuente, cantidad);
	}

	@Override
	public void remove(Entity.RemovalReason motivo) {
		if (this.agarre >= 0) {
			this.soltar();
		}
		super.remove(motivo);
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
		// cazando ya suenan los jadeos, y comiendo no ronda
		if (!this.entityData.get(CAZANDO) && !this.entityData.get(AGARRANDO)) {
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

	/* --------------------------------------------------------------- metas */

	/**
	 * Perseguir sin hacer trampas: mientras te ve va a por ti y, si te alcanza,
	 * te agarra; si te pierde de vista va a donde te vio por ultima vez, mira
	 * alrededor y, si no te encuentra, te deja.
	 */
	static final class Cazar extends Goal {
		private final Bacteria b;
		private Vec3 ultima = Vec3.ZERO;
		private int sinVer;
		private int buscando;
		private int recalcular;

		Cazar(Bacteria b) {
			this.b = b;
			this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			LivingEntity t = this.b.getTarget();
			return t != null && t.isAlive() && this.b.agarre < 0 && this.b.retirada == null;
		}

		@Override
		public boolean canContinueToUse() {
			return this.canUse();
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void start() {
			LivingEntity t = this.b.getTarget();
			this.ultima = t == null ? this.b.position() : t.position();
			this.sinVer = 0;
			this.buscando = 0;
			this.recalcular = 0;
			this.b.setAggressive(true);
		}

		@Override
		public void stop() {
			this.b.getNavigation().stop();
			this.b.setAggressive(false);
		}

		@Override
		public void tick() {
			LivingEntity t = this.b.getTarget();
			if (t == null) {
				return;
			}
			if (this.b.getSensing().hasLineOfSight(t)) {
				this.ultima = t.position();
				this.sinVer = 0;
				this.buscando = 0;
				this.b.getLookControl().setLookAt(t, 30.0F, 30.0F);
				if (t instanceof ServerPlayer j && this.b.alAlcance(j)) {
					this.b.agarrar(j);
					return;
				}
				if (--this.recalcular <= 0) {
					this.recalcular = 4 + this.b.getRandom().nextInt(4);
					if (!this.b.getNavigation().moveTo(t, 1.0)) {
						this.recalcular += 6;
					}
				}
				return;
			}
			this.sinVer++;
			if (this.b.position().distanceToSqr(this.ultima) > 2.0) {
				if (--this.recalcular <= 0) {
					this.recalcular = 10;
					this.b.getNavigation().moveTo(this.ultima.x, this.ultima.y, this.ultima.z, 1.0);
				}
			} else {
				// donde te perdio: se para y mira a un lado y a otro
				this.b.getNavigation().stop();
				if (this.buscando++ % 15 == 0) {
					this.b.getLookControl().setLookAt(this.b.getX() + this.b.getRandom().nextDouble() * 10 - 5, this.b.getEyeY(),
						this.b.getZ() + this.b.getRandom().nextDouble() * 10 - 5);
				}
			}
			if (this.buscando > BUSCA || this.sinVer > OLVIDA) {
				this.b.perder(t);
			}
		}
	}

	/** Irse lejos de un escondite (ver retirarse). */
	static final class Retirarse extends Goal {
		private final Bacteria b;
		private int recalcular;

		Retirarse(Bacteria b) {
			this.b = b;
			this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			return this.b.retirada != null && this.b.agarre < 0;
		}

		@Override
		public boolean canContinueToUse() {
			return this.canUse();
		}

		@Override
		public void start() {
			this.recalcular = 0;
		}

		@Override
		public void stop() {
			this.b.getNavigation().stop();
		}

		@Override
		public void tick() {
			Vec3 d = this.b.retirada;
			if (d == null) {
				return;
			}
			if (this.b.position().distanceToSqr(d) < 16.0 || this.b.tickCount > this.b.retiradaHasta) {
				this.b.retirada = null;
				return;
			}
			if (--this.recalcular <= 0 || this.b.getNavigation().isDone()) {
				this.recalcular = 40;
				this.b.getNavigation().moveTo(d.x, d.y, d.z, Math.sqrt(RETIRADA_BPS / CAZA_BPS));
			}
		}
	}
}
