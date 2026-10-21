package net.backrooms.evento.estacion;

import java.util.List;
import java.util.UUID;
import net.backrooms.evento.BackroomsEvento;
import net.backrooms.evento.Sonidos;
import net.backrooms.evento.mision.Misiones;
import net.backrooms.evento.supervivencia.Supervivencia;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * El Amasijo: la entidad de la Estacion Electrica. Un amasijo de cuerpos palidos, docenas de brazos
 * que salen en todas direcciones y una boca enorme llena de dientes en medio; anda sobre los brazos,
 * deprisa, con un repiqueteo sobre la chapa que se confunde con la maquinaria.
 *
 * Cuantos hay y cuando salen lo decide AmasijoEstacion (cada sistema que vuelve despierta alguno mas).
 * No se le puede hacer dano.
 *  - Ronda por los pasillos de los sectores, sin prisa.
 *  - Si ve a alguien (sin paredes en medio: a DETECTA bloques con luz, menos a oscuras) chilla y va a
 *    por el a VELOCIDAD (algo menos que correr: se le escapa rompiendo la linea de vision en los
 *    cruces). Si no le vuelve a ver en OLVIDA segundos, le deja.
 *  - Si le alcanza le AGARRA: no se puede mover y al segundo muere.
 */
public class Amasijo extends Monster {
	private static final double ALCANCE = 1.7;
	private static final int AGARRE = 22;
	private static final double DETECTA = 20.0;
	private static final double DETECTA_OSCURO = 11.0;
	private static final double VELOCIDAD = 5.0;
	private static final double RONDA = 2.4;
	private static final int OLVIDA = 70;

	public static final ResourceKey<DamageType> ESTACION = ResourceKey.create(Registries.DAMAGE_TYPE, BackroomsEvento.id("estacion"));

	private static final EntityDataAccessor<Boolean> CAZANDO = SynchedEntityData.defineId(Amasijo.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> AGARRANDO = SynchedEntityData.defineId(Amasijo.class, EntityDataSerializers.BOOLEAN);

	private @Nullable Vec3 ultimaVista;
	private int vistoEn;
	private int siguienteRespira;
	private int ultimoGrito = -1000;
	private @Nullable Vec3 ronda;
	private int agarre = -1;
	private @Nullable UUID presa;
	private float caza;
	private float cazaAntes;

	public Amasijo(EntityType<? extends Amasijo> tipo, Level nivel) {
		super(tipo, nivel);
		this.xpReward = 0;
	}

	static double atributo(double bps) {
		return Math.sqrt(bps / 43.17);
	}

	public static AttributeSupplier.Builder atributos() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 300.0)
			.add(Attributes.MOVEMENT_SPEED, atributo(RONDA))
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

	public boolean agarrando() {
		return this.entityData.get(AGARRANDO);
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide()) {
			this.cazaAntes = this.caza;
			this.caza += ((this.entityData.get(CAZANDO) ? 1.0F : 0.0F) - this.caza) * 0.15F;
		}
	}

	/** Cliente: 0..1 de lo que esta cazando (el modelo agita mas los brazos). */
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
		this.playSound(Sonidos.AMASIJO_PASOS, this.entityData.get(CAZANDO) ? 1.4F : 0.7F, 0.9F + this.random.nextFloat() * 0.25F);
	}

	/* ---------------------------------------------------------------- IA */

	@Override
	protected void customServerAiStep(ServerLevel nivel) {
		super.customServerAiStep(nivel);
		if (this.agarre >= 0) {
			this.tickAgarre(nivel);
			return;
		}
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
				if (this.tickCount - this.ultimoGrito > 120) {
					this.ultimoGrito = this.tickCount;
					nivel.playSound(null, this.getX(), this.getEyeY(), this.getZ(), Sonidos.AMASIJO_GRITO, SoundSource.HOSTILE, 3.5F, 0.9F + this.random.nextFloat() * 0.2F);
					Supervivencia.get().asustar(nuevo, 8.0F);
				}
			}
		}
		this.entityData.set(CAZANDO, t != null);
		this.respirar(nivel, t != null);
		if (t == null) {
			this.rondar(RONDA);
			return;
		}
		AttributeInstance a = this.getAttribute(Attributes.MOVEMENT_SPEED);
		if (a != null) {
			a.setBaseValue(atributo(VELOCIDAD));
		}
		if (this.ve(nivel, t)) {
			this.ultimaVista = t.position();
			this.vistoEn = this.tickCount;
		}
		if (this.tickCount - this.vistoEn > OLVIDA) {
			// se le ha escapado: va a donde le vio por ultima vez y sigue rondando
			this.setTarget(null);
			this.ronda = this.ultimaVista;
			return;
		}
		Vec3 a2 = this.ultimaVista != null ? this.ultimaVista : t.position();
		this.getNavigation().moveTo(a2.x, a2.y, a2.z, 1.0);
		this.getLookControl().setLookAt(t, 30.0F, 30.0F);
		double dx = t.getX() - this.getX();
		double dz = t.getZ() - this.getZ();
		if (dx * dx + dz * dz <= ALCANCE * ALCANCE && Math.abs(t.getY() - this.getY()) < 1.5 && this.hasLineOfSight(t)) {
			this.agarrar(nivel, t);
		}
	}

	private void respirar(ServerLevel nivel, boolean cazando) {
		if (this.tickCount >= this.siguienteRespira) {
			this.siguienteRespira = this.tickCount + (cazando ? 40 : 120) + this.random.nextInt(60);
			nivel.playSound(null, this.getX(), this.getEyeY(), this.getZ(), Sonidos.AMASIJO_RESPIRA, SoundSource.HOSTILE, cazando ? 2.0F : 1.2F,
				0.9F + this.random.nextFloat() * 0.2F);
		}
	}

	private boolean valido(ServerPlayer j) {
		return j.isAlive() && !j.isCreative() && !j.isSpectator() && j.level() == this.level() && Misiones.get().enExpedicion(j);
	}

	/** Le ve: sin paredes en medio y cerca (con luz, mas lejos: la luz te deja expuesto). */
	private boolean ve(ServerLevel nivel, ServerPlayer j) {
		boolean oscuro = !JuegoEstacion.get().corriente();
		return this.distanceTo(j) <= (oscuro ? DETECTA_OSCURO : DETECTA) && this.hasLineOfSight(j);
	}

	private @Nullable ServerPlayer buscar(ServerLevel nivel) {
		ServerPlayer mejor = null;
		double dMejor = Double.MAX_VALUE;
		for (ServerPlayer j : nivel.players()) {
			if (!this.valido(j)) {
				continue;
			}
			double d = this.distanceTo(j);
			if (d < dMejor && this.ve(nivel, j)) {
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

	/** Sin presa: de un sitio de la ronda de los sectores a otro. */
	private void rondar(double bps) {
		AttributeInstance a = this.getAttribute(Attributes.MOVEMENT_SPEED);
		if (a != null) {
			a.setBaseValue(atributo(bps));
		}
		if (this.ronda == null || this.position().distanceToSqr(this.ronda) < 4.0 || this.getNavigation().isDone() && this.random.nextInt(40) == 0) {
			List<int[]> ronda = AmasijoEstacion.ronda();
			// mas a menudo cerca de donde esta (no cruza el mapa de punta a punta todo el rato)
			int[] mejor = ronda.get(this.random.nextInt(ronda.size()));
			for (int k = 0; k < 4; k++) {
				int[] p = ronda.get(this.random.nextInt(ronda.size()));
				if (Math.hypot(p[0] - this.getX(), p[1] - this.getZ()) < Math.hypot(mejor[0] - this.getX(), mejor[1] - this.getZ()) && this.random.nextBoolean()) {
					mejor = p;
				}
			}
			this.ronda = new Vec3(mejor[0] + 0.5, PlanoEstacion.SUELO + 1, mejor[1] + 0.5);
		}
		if (this.getNavigation().isDone()) {
			this.getNavigation().moveTo(this.ronda.x, this.ronda.y, this.ronda.z, 1.0);
		}
	}

	/* --------------------------------------------------------------- agarre */

	private void agarrar(ServerLevel nivel, ServerPlayer j) {
		this.agarre = 0;
		this.presa = j.getUUID();
		this.entityData.set(AGARRANDO, true);
		this.getNavigation().stop();
		this.setDeltaMovement(Vec3.ZERO);
		j.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, AGARRE + 10, 255, false, false));
		j.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, AGARRE + 10, 128, false, false));
		nivel.playSound(null, this.getX(), this.getEyeY(), this.getZ(), Sonidos.AMASIJO_ATAQUE, SoundSource.HOSTILE, 3.0F, 1.0F);
		Supervivencia.get().asustar(j, 30.0F);
		BackroomsEvento.LOG.info("El Amasijo agarra a {}", j.getGameProfile().name());
	}

	private void tickAgarre(ServerLevel nivel) {
		ServerPlayer j = this.presa == null ? null : nivel.getServer().getPlayerList().getPlayer(this.presa);
		this.agarre++;
		if (j == null || !j.isAlive() || j.level() != nivel) {
			this.soltar();
			return;
		}
		this.getLookControl().setLookAt(j, 90.0F, 90.0F);
		this.setDeltaMovement(Vec3.ZERO);
		if (this.agarre >= AGARRE) {
			DamageSource fuente = new DamageSource(nivel.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(ESTACION), this);
			j.hurtServer(nivel, fuente, Float.MAX_VALUE);
			nivel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState()), j.getX(), j.getY() + 1.0, j.getZ(),
				30, 0.4, 0.6, 0.4, 0.1);
			this.soltar();
		}
	}

	private void soltar() {
		this.agarre = -1;
		this.presa = null;
		this.entityData.set(AGARRANDO, false);
		this.setTarget(null);
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	/** No se guardan: AmasijoEstacion mantiene los que tiene que haber. */
	@Override
	public boolean shouldBeSaved() {
		return false;
	}
}
