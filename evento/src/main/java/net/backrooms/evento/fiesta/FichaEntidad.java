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
 * Una moneda de la Fiesta en su sitio (PlanoFiesta.MONEDAS): tumbada encima de una mesa. Es la
 * misma para todos; quien ya la cogio no la ve ni la puede senalar (el cliente mira
 * EstadoFiesta.cogidas). Se coge con clic derecho (JuegoFiesta#cogerFicha).
 */
public class FichaEntidad extends Entity {
	public static final Item MONEDA = Items.registerItem(ResourceKey.create(Registries.ITEM, BackroomsEvento.id("moneda_fiesta")), Item::new,
		new Item.Properties());
	public static final Item TICKETS = Items.registerItem(ResourceKey.create(Registries.ITEM, BackroomsEvento.id("tickets_fiesta")), Item::new,
		new Item.Properties());

	private static final EntityDataAccessor<Integer> ID = SynchedEntityData.defineId(FichaEntidad.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> TIPO = SynchedEntityData.defineId(FichaEntidad.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> VALOR = SynchedEntityData.defineId(FichaEntidad.class, EntityDataSerializers.INT);

	/** En el cliente: si ese jugador ya cogio la ficha (FiestaCliente la pone). */
	public static java.util.function.IntPredicate oculta = id -> false;

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
	public boolean isPickable() {
		return !(this.level().isClientSide() && oculta.test(this.id()));
	}

	@Override
	public net.minecraft.world.InteractionResult interact(net.minecraft.world.entity.player.Player jugador, net.minecraft.world.InteractionHand mano) {
		if (jugador instanceof net.minecraft.server.level.ServerPlayer sp && mano == net.minecraft.world.InteractionHand.MAIN_HAND) {
			JuegoFiesta.get().cogerFicha(sp, this);
		}
		return net.minecraft.world.InteractionResult.SUCCESS;
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
