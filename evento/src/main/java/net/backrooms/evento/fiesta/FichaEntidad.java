package net.backrooms.evento.fiesta;

import net.backrooms.evento.BackroomsEvento;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Una ficha de la Fiesta en su sitio (PlanoFiesta.MONEDAS y TICKETS): una moneda dorada o una
 * tira de tickets que flota y gira. Es la misma para todos; quien ya la cogio no la ve (el
 * cliente mira EstadoFiesta.cogidas). Se coge al pasar por encima (JuegoFiesta), no se toca.
 */
public class FichaEntidad extends Entity {
	public static final Item MONEDA = Items.registerItem(ResourceKey.create(Registries.ITEM, BackroomsEvento.id("moneda_fiesta")), Item::new,
		new Item.Properties());
	public static final Item TICKETS = Items.registerItem(ResourceKey.create(Registries.ITEM, BackroomsEvento.id("tickets_fiesta")), Item::new,
		new Item.Properties());

	private static final EntityDataAccessor<Integer> ID = SynchedEntityData.defineId(FichaEntidad.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> TIPO = SynchedEntityData.defineId(FichaEntidad.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> VALOR = SynchedEntityData.defineId(FichaEntidad.class, EntityDataSerializers.INT);

	public FichaEntidad(EntityType<? extends FichaEntidad> tipo, Level nivel) {
		super(tipo, nivel);
		this.noPhysics = true;
		this.setNoGravity(true);
	}

	public void preparar(int id, int tipo, int valor) {
		this.entityData.set(ID, id);
		this.entityData.set(TIPO, tipo);
		this.entityData.set(VALOR, valor);
	}

	public int id() {
		return this.entityData.get(ID);
	}

	/** 0 moneda, 1 tickets. */
	public int tipo() {
		return this.entityData.get(TIPO);
	}

	public int valor() {
		return this.entityData.get(VALOR);
	}

	public Item objeto() {
		return this.tipo() == 0 ? MONEDA : TICKETS;
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
		builder.define(ID, -1);
		builder.define(TIPO, 0);
		builder.define(VALOR, 1);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput entrada) {
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput salida) {
	}

	/** Carga la clase (y sus objetos) desde el inicializador. */
	public static void iniciar() {
	}
}
