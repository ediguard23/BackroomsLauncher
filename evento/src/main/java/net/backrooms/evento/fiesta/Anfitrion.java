package net.backrooms.evento.fiesta;

import java.util.List;
import java.util.UUID;
import net.backrooms.evento.BackroomsEvento;
import net.backrooms.evento.Sonidos;
import net.backrooms.evento.mision.Misiones;
import net.backrooms.evento.supervivencia.Supervivencia;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * El Anfitrion: la entidad de la Fiesta. Un disfraz de mascota de fiesta infantil (cabezota
 * redonda con la sonrisa cosida, gorrito, pajarita, guantes blancos) demasiado alto y demasiado
 * flaco; dentro hay alguien que fue un participante y recuerda muy bien como atraer a la gente.
 *
 * Siempre hay dos o tres dando vueltas por el laberinto (AnfitrionFiesta). No se va ni se le puede hacer dano.
 *  - Con LUZ: merodea despacio por la ronda del laberinto (PlanoFiesta.RONDA). No caza a nadie.
 *  - En un APAGON: un anfitrionApagon mas rapido; si te ve (a anfitrionDetecta bloques, sin paredes en
 *    medio) va a por ti a anfitrionVelocidad. Va a donde te vio por ultima vez; si no te vuelve a ver en
 *    anfitrionOlvida segundos, te deja y sigue rondando. Si te alcanza te ABRAZA: no te puedes mover y
 *    al segundo y medio mueres. Dentro de una habitacion no te persigue: es donde uno se salva.
 *  - APARICION (solo con /backrooms fiesta aparicion): de pie, lejos, mirandote; se va si le apuntas
 *    con la linterna encendida, o a los pocos segundos.
 */
public class Anfitrion extends Monster {
	public static final int APARICION = 0;
	public static final int CAZA = 1;
	/** A esta distancia (horizontal) te abraza. */
	private static final double ALCANCE = 1.6;
	/** Ticks del abrazo hasta que mueres. */
	private static final int ABRAZO = 30;

	public static final ResourceKey<DamageType> FIESTA = ResourceKey.create(Registries.DAMAGE_TYPE, BackroomsEvento.id("fiesta"));

	private static final EntityDataAccessor<Integer> MODO = SynchedEntityData.defineId(Anfitrion.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> CAZANDO = SynchedEntityData.defineId(Anfitrion.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> ABRAZANDO = SynchedEntityData.defineId(Anfitrion.class, EntityDataSerializers.BOOLEAN);

	private @Nullable Vec3 ultimaVista;
	private int vistoEn;
	private int siguienteRespira;
	private int siguienteVoz;
	private int ultimoGrito = -1000;
	private @Nullable Vec3 ronda;
	private int abrazo = -1;
	private @Nullable UUID presa;
	/** Aparicion: cuantos ticks lleva y a quien mira. */
	private int vida;
	private @Nullable UUID mirando;
	/** Cliente: 0..1 suavizado de si esta cazando. */
	private float caza;
	private float cazaAntes;

	public Anfitrion(EntityType<? extends Anfitrion> tipo, Level nivel) {
		super(tipo, nivel);
		this.xpReward = 0;
		this.setPersistenceRequired();
	}

	/** La velocidad del atributo para ir a tantos bloques por segundo (va al cuadrado; ver Bacteria#atributo). */
	static double atributo(double bps) {
		return Math.sqrt(bps / 43.17);
	}

	public static AttributeSupplier.Builder atributos() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 200.0)
			.add(Attributes.MOVEMENT_SPEED, atributo(5.2))
			.add(Attributes.ATTACK_DAMAGE, 0.0)
			.add(Attributes.FOLLOW_RANGE, 64.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
			.add(Attributes.STEP_HEIGHT, 1.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(MODO, APARICION);
		builder.define(CAZANDO, false);
		builder.define(ABRAZANDO, false);
	}

	public void modo(int m) {
		this.entityData.set(MODO, m);
	}

	public int modo() {
		return this.entityData.get(MODO);
	}

	public boolean abrazando() {
		return this.entityData.get(ABRAZANDO);
	}

	/** Aparicion: a quien mira hasta irse. */
	public void mirarA(ServerPlayer j) {
		this.mirando = j.getUUID();
		this.vida = 0;
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide()) {
			this.cazaAntes = this.caza;
			this.caza += ((this.entityData.get(CAZANDO) ? 1.0F : 0.0F) - this.caza) * 0.15F;
		}
	}

	public float caza(float parcial) {
		return Mth.lerp(parcial, this.cazaAntes, this.caza);
	}

	@Override
	public boolean hurtServer(ServerLevel nivel, DamageSource fuente, float cantidad) {
		return fuente.is(net.minecraft.world.damagesource.DamageTypes.GENERIC_KILL) && super.hurtServer(nivel, fuente, cantidad);
	}

	@Override
	public boolean removeWhenFarAway(double distancia) {
		return false;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState estado) {
		this.playSound(Sonidos.ANFITRION_PASO, this.modo() == CAZA ? 0.9F : 0.5F, 0.9F + this.random.nextFloat() * 0.2F);
	}

	/* ---------------------------------------------------------------- IA */

	@Override
	protected void customServerAiStep(ServerLevel nivel) {
		super.customServerAiStep(nivel);
		if (this.abrazo >= 0) {
			this.tickAbrazo(nivel);
			return;
		}
		if (this.modo() == APARICION) {
			this.tickAparicion(nivel);
			return;
		}
		ConfigFiesta c = ConfigFiesta.get();
		boolean apagon = net.backrooms.evento.ambiente.Ambiente.get().apagon(nivel);
		if (!apagon) {
			// con luz no caza: suelta a quien fuera y merodea
			if (this.getTarget() != null) {
				this.setTarget(null);
			}
			this.entityData.set(CAZANDO, false);
			this.rondar(nivel, c.anfitrionVelocidad * 0.5);
			this.hablar(nivel);
			return;
		}
		AttributeInstance a = this.getAttribute(Attributes.MOVEMENT_SPEED);
		if (a != null) {
			a.setBaseValue(atributo(c.anfitrionVelocidad * c.anfitrionApagon));
		}
		LivingTarget:
		{
			ServerPlayer t = this.getTarget() instanceof ServerPlayer p ? p : null;
			if (t != null && !this.valido(t)) {
				this.setTarget(null);
				t = null;
			}
			if (t == null) {
				ServerPlayer nuevo = this.buscar(nivel);
				if (nuevo != null) {
					this.setTarget(nuevo);
					t = nuevo;
					if (this.tickCount - this.ultimoGrito > 100) {
						this.ultimoGrito = this.tickCount;
						nivel.playSound(null, this.getX(), this.getEyeY(), this.getZ(), Sonidos.ANFITRION_DETECTA, SoundSource.HOSTILE, 3.0F, 1.0F);
						Supervivencia.get().asustar(nuevo, 5.0F);
					}
				}
			}
			this.entityData.set(CAZANDO, t != null);
			if (t == null) {
				this.rondar(nivel, c.anfitrionVelocidad * c.anfitrionApagon * 0.5);
				break LivingTarget;
			}
			if (this.ve(t)) {
				this.ultimaVista = t.position();
				this.vistoEn = this.tickCount;
			}
			if (this.tickCount - this.vistoEn > Math.round(c.anfitrionOlvida * 20)) {
				// se le ha escapado
				this.setTarget(null);
				nivel.playSound(null, this.getX(), this.getEyeY(), this.getZ(), Sonidos.ANFITRION_PIERDE, SoundSource.HOSTILE, 1.8F, 1.0F);
				this.ronda = this.ultimaVista;
				break LivingTarget;
			}
			Vec3 a2 = this.ultimaVista != null ? this.ultimaVista : t.position();
			this.getNavigation().moveTo(a2.x, a2.y, a2.z, 1.0);
			this.getLookControl().setLookAt(t, 30.0F, 30.0F);
			if (this.tickCount >= this.siguienteRespira) {
				this.siguienteRespira = this.tickCount + 50 + this.random.nextInt(20);
				nivel.playSound(null, this.getX(), this.getEyeY(), this.getZ(), Sonidos.ANFITRION_RESPIRA, SoundSource.HOSTILE, 1.8F, 0.95F + this.random.nextFloat() * 0.1F);
			}
			double dx = t.getX() - this.getX();
			double dz = t.getZ() - this.getZ();
			if (dx * dx + dz * dz <= ALCANCE * ALCANCE && Math.abs(t.getY() - this.getY()) < 1.5 && this.hasLineOfSight(t)) {
				this.abrazar(nivel, t);
			}
		}
		this.hablar(nivel);
	}

	/** De vez en cuando habla (te llama), cace o no. */
	private void hablar(ServerLevel nivel) {
		if (this.tickCount >= this.siguienteVoz) {
			this.siguienteVoz = this.tickCount + 300 + this.random.nextInt(400);
			if (this.tickCount > 40) {
				nivel.playSound(null, this.getX(), this.getEyeY(), this.getZ(), Sonidos.ANFITRION_VOZ, SoundSource.HOSTILE, 2.2F, 1.0F);
			}
		}
	}

	private boolean valido(ServerPlayer j) {
		return j.isAlive() && !j.isCreative() && !j.isSpectator() && j.level() == this.level() && Misiones.get().enExpedicion(j) && !aSalvo(j);
	}

	/** Dentro de una habitacion (cualquiera que no sea el pasillo del laberinto) no se le persigue. */
	private static boolean aSalvo(ServerPlayer j) {
		PlanoFiesta.Sala s = PlanoFiesta.sala(j.getX(), j.getZ());
		return s != null && !s.nombre().equals("laberinto");
	}

	private boolean ve(ServerPlayer j) {
		return this.distanceTo(j) <= ConfigFiesta.get().anfitrionDetecta && this.hasLineOfSight(j);
	}

	/** El que ve mas cerca, o null. */
	private @Nullable ServerPlayer buscar(ServerLevel nivel) {
		ServerPlayer mejor = null;
		double dMejor = Double.MAX_VALUE;
		for (ServerPlayer j : nivel.players()) {
			if (!this.valido(j)) {
				continue;
			}
			double d = this.distanceTo(j);
			if (d < dMejor && this.ve(j)) {
				mejor = j;
				dMejor = d;
			}
		}
		if (mejor != null) {
			this.ultimaVista = mejor.position();
			this.vistoEn = this.tickCount;
		}
		return mejor;
	}

	/** Sin presa: de un sitio de la ronda del laberinto a otro, a `bps` bloques por segundo. */
	private void rondar(ServerLevel nivel, double bps) {
		AttributeInstance a = this.getAttribute(Attributes.MOVEMENT_SPEED);
		if (a != null) {
			a.setBaseValue(atributo(bps));
		}
		if (this.ronda == null || this.position().distanceToSqr(this.ronda) < 4.0 || this.getNavigation().isDone() && this.random.nextInt(40) == 0) {
			List<int[]> ronda = AnfitrionFiesta.rondaLaberinto();
			int[] p = ronda.get(this.random.nextInt(ronda.size()));
			this.ronda = new Vec3(p[0] + 0.5, PlanoFiesta.SUELO + 1, p[1] + 0.5);
		}
		if (this.getNavigation().isDone()) {
			this.getNavigation().moveTo(this.ronda.x, this.ronda.y, this.ronda.z, 1.0);
		}
	}

	/* ------------------------------------------------------------ aparicion */

	private void tickAparicion(ServerLevel nivel) {
		this.vida++;
		this.getNavigation().stop();
		ServerPlayer j = this.mirando == null ? null : nivel.getServer().getPlayerList().getPlayer(this.mirando);
		if (j == null || j.level() != nivel) {
			this.irse(nivel);
			return;
		}
		this.getLookControl().setLookAt(j, 60.0F, 60.0F);
		if (this.vida == 2) {
			nivel.playSound(null, this.getX(), this.getEyeY(), this.getZ(), Sonidos.ANFITRION_RISA, SoundSource.HOSTILE, 1.6F, 1.0F);
		}
		if (this.vida > 15 && this.alumbrado(nivel) || this.vida > 20 * 8) {
			this.irse(nivel);
		}
	}

	/**
	 * Si alguien le esta apuntando con la linterna encendida: a menos de 20 bloques, mirando hacia el
	 * (dentro del cono del foco) y sin paredes en medio. Mirarle sin luz no le hace nada.
	 */
	private boolean alumbrado(ServerLevel nivel) {
		for (ServerPlayer j : nivel.players()) {
			if (j.isSpectator() || !j.isAlive() || !net.backrooms.evento.objetos.HerramientasServidor.get().linterna(j)) {
				continue;
			}
			Vec3 hacia = this.getBoundingBox().getCenter().subtract(j.getEyePosition());
			double d = hacia.length();
			if (d > 20.0 || d < 0.01) {
				continue;
			}
			// el foco abre unos 25 grados; de cerca, el cuerpo entero cabe en el
			double cono = d < 4.0 ? 0.80 : 0.91;
			if (j.getViewVector(1.0F).dot(hacia.scale(1.0 / d)) > cono && j.hasLineOfSight(this)) {
				return true;
			}
		}
		return false;
	}

	/** Se va: confeti y un globo que se deshincha. */
	public void irse(ServerLevel nivel) {
		nivel.playSound(null, this.getX(), this.getEyeY(), this.getZ(), Sonidos.ANFITRION_PIERDE, SoundSource.HOSTILE, 1.4F, 1.1F);
		for (int col : new int[] {0xE8384A, 0xFFD23A, 0x3AA6FF, 0x44D16A}) {
			nivel.sendParticles(new DustParticleOptions(col, 1.4F), this.getX(), this.getY() + 1.4, this.getZ(), 8, 0.4, 0.8, 0.4, 0.0);
		}
		this.discard();
	}

	/* --------------------------------------------------------------- abrazo */

	private void abrazar(ServerLevel nivel, ServerPlayer j) {
		this.abrazo = 0;
		this.presa = j.getUUID();
		this.entityData.set(ABRAZANDO, true);
		this.getNavigation().stop();
		this.setDeltaMovement(Vec3.ZERO);
		j.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ABRAZO + 10, 255, false, false));
		j.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, ABRAZO + 10, 128, false, false));
		nivel.playSound(null, this.getX(), this.getEyeY(), this.getZ(), Sonidos.ANFITRION_ABRAZO, SoundSource.HOSTILE, 3.0F, 1.0F);
		Supervivencia.get().asustar(j, 30.0F);
		BackroomsEvento.LOG.info("El Anfitrion abraza a {}", j.getGameProfile().name());
	}

	private void tickAbrazo(ServerLevel nivel) {
		ServerPlayer j = this.presa == null ? null : nivel.getServer().getPlayerList().getPlayer(this.presa);
		this.abrazo++;
		if (j == null || !j.isAlive() || j.level() != nivel) {
			this.soltar();
			return;
		}
		this.getLookControl().setLookAt(j, 90.0F, 90.0F);
		this.setDeltaMovement(Vec3.ZERO);
		if (this.abrazo >= ABRAZO) {
			DamageSource fuente = new DamageSource(nivel.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(FIESTA), this);
			j.hurtServer(nivel, fuente, Float.MAX_VALUE);
			for (int col : new int[] {0xE8384A, 0xFFD23A, 0x3AA6FF, 0x44D16A}) {
				nivel.sendParticles(new DustParticleOptions(col, 1.6F), j.getX(), j.getY() + 1.2, j.getZ(), 14, 0.4, 0.6, 0.4, 0.0);
			}
			this.soltar();
		}
	}

	private void soltar() {
		this.abrazo = -1;
		this.presa = null;
		this.entityData.set(ABRAZANDO, false);
		this.setTarget(null);
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	/** No se guardan: AnfitrionFiesta mantiene los que tiene que haber cerca de los jugadores. */
	@Override
	public boolean shouldBeSaved() {
		return false;
	}
}
