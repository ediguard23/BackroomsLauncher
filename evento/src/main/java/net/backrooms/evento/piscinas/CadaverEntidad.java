package net.backrooms.evento.piscinas;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Un cuerpo de las Piscinas (PlanoPiscinas#cadaverEnCelda): tirado, sentado contra la pared o
 * colgado del techo. Solo se mira: no choca, no se coge, no se guarda (lo pone y lo quita
 * Cadaveres). Lo dibuja CadaverRenderer con la piel y la postura que le tocaron.
 */
public class CadaverEntidad extends Entity {
	private static final EntityDataAccessor<Integer> PIEL = SynchedEntityData.defineId(CadaverEntidad.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> POSTURA = SynchedEntityData.defineId(CadaverEntidad.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> MUTILADO = SynchedEntityData.defineId(CadaverEntidad.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> CUERDA = SynchedEntityData.defineId(CadaverEntidad.class, EntityDataSerializers.FLOAT);

	public CadaverEntidad(EntityType<? extends CadaverEntidad> tipo, Level nivel) {
		super(tipo, nivel);
		this.noPhysics = true;
		this.setNoGravity(true);
	}

	public void preparar(PlanoPiscinas.Cadaver c) {
		this.entityData.set(PIEL, c.piel());
		this.entityData.set(POSTURA, c.postura());
		this.entityData.set(MUTILADO, c.mutilado());
		this.entityData.set(CUERDA, c.cuerda());
		this.setYRot(c.giro());
		this.setPos(c.x(), c.y(), c.z());
	}

	public int piel() {
		return this.entityData.get(PIEL);
	}

	public int postura() {
		return this.entityData.get(POSTURA);
	}

	public int mutilado() {
		return this.entityData.get(MUTILADO);
	}

	public float cuerda() {
		return this.entityData.get(CUERDA);
	}

	/** La luz con que se dibuja: la del cuerpo, no la del techo del que cuelga. */
	@Override
	public net.minecraft.world.phys.Vec3 getLightProbePosition(float parcial) {
		int p = this.postura();
		double baja = p == PlanoPiscinas.COLGADO_PIES || p == PlanoPiscinas.COLGADO_CUELLO ? this.cuerda() + 1.0 : -0.5;
		return this.getPosition(parcial).add(0, -baja, 0);
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
		builder.define(PIEL, 0);
		builder.define(POSTURA, 0);
		builder.define(MUTILADO, 0);
		builder.define(CUERDA, 0.0F);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput entrada) {
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput salida) {
	}
}
