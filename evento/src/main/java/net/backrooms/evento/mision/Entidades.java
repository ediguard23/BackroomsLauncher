package net.backrooms.evento.mision;

import net.backrooms.evento.BackroomsEvento;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/** Tipos de entidad del evento. */
public final class Entidades {
	public static final EntityType<CaseteEntidad> CASETE = registrar("casete",
		EntityType.Builder.<CaseteEntidad>of(CaseteEntidad::new, MobCategory.MISC)
			.sized(0.5F, 0.2F).noLootTable().noSave().noSummon().clientTrackingRange(4).updateInterval(40));

	/** El asiento invisible de las butacas del vestibulo. */
	public static final EntityType<net.backrooms.evento.vestibulo.Asiento> ASIENTO = registrar("asiento",
		EntityType.Builder.<net.backrooms.evento.vestibulo.Asiento>of(net.backrooms.evento.vestibulo.Asiento::new, MobCategory.MISC)
			.sized(0.01F, 0.01F).noLootTable().noSummon().clientTrackingRange(8).updateInterval(20));

	/** La Bacteria: 3,2 bloques de alto, cabe en los pasillos de 4. */
	public static final EntityType<net.backrooms.evento.entidad.Bacteria> BACTERIA = registrar("bacteria",
		EntityType.Builder.<net.backrooms.evento.entidad.Bacteria>of(net.backrooms.evento.entidad.Bacteria::new, MobCategory.MONSTER)
			.sized(0.8F, 3.2F).eyeHeight(2.9F).clientTrackingRange(10));
	public static final EntityType<net.backrooms.evento.entidad.Smiler> SMILER = registrar("smiler",
		EntityType.Builder.<net.backrooms.evento.entidad.Smiler>of(net.backrooms.evento.entidad.Smiler::new, MobCategory.MONSTER)
			.sized(0.7F, 2.2F).eyeHeight(1.95F).clientTrackingRange(8));
	/** La Serpiente de las Piscinas: caja de 2,4 de alto (el craneo asoma por encima) para que pase por los techos de 2,5 de las terrazas; la cola va tumbada detras. */
	public static final EntityType<net.backrooms.evento.entidad.Serpiente> SERPIENTE = registrar("serpiente",
		EntityType.Builder.<net.backrooms.evento.entidad.Serpiente>of(net.backrooms.evento.entidad.Serpiente::new, MobCategory.MONSTER)
			.sized(1.2F, 2.4F).eyeHeight(2.2F).clientTrackingRange(10));

	/** Comida tirada en la moqueta (agua de almendras, galletas, pizza). */
	public static final EntityType<net.backrooms.evento.supervivencia.ComidaEntidad> COMIDA = registrar("comida",
		EntityType.Builder.<net.backrooms.evento.supervivencia.ComidaEntidad>of(net.backrooms.evento.supervivencia.ComidaEntidad::new, MobCategory.MISC)
			.sized(0.5F, 0.3F).noLootTable().noSave().noSummon().clientTrackingRange(4).updateInterval(40));

	/** Una de las tres tarjetas de la salida de las Piscinas, tirada donde este (Tarjetas). */
	public static final EntityType<net.backrooms.evento.piscinas.TarjetaEntidad> TARJETA = registrar("tarjeta",
		EntityType.Builder.<net.backrooms.evento.piscinas.TarjetaEntidad>of(net.backrooms.evento.piscinas.TarjetaEntidad::new, MobCategory.MISC)
			.sized(0.5F, 0.5F).noLootTable().noSave().noSummon().clientTrackingRange(6).updateInterval(20));

	/** Un cuerpo de las Piscinas (Cadaveres): la caja es para verlo, no choca. */
	public static final EntityType<net.backrooms.evento.piscinas.CadaverEntidad> CADAVER = registrar("cadaver",
		EntityType.Builder.<net.backrooms.evento.piscinas.CadaverEntidad>of(net.backrooms.evento.piscinas.CadaverEntidad::new, MobCategory.MISC)
			.sized(0.8F, 0.6F).noLootTable().noSave().noSummon().clientTrackingRange(8).updateInterval(100));

	private Entidades() {
	}

	private static <T extends net.minecraft.world.entity.Entity> EntityType<T> registrar(String id, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> clave = ResourceKey.create(Registries.ENTITY_TYPE, BackroomsEvento.id(id));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, clave, builder.build(clave));
	}

	public static void iniciar() {
		net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(BACTERIA, net.backrooms.evento.entidad.Bacteria.atributos());
		net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(SMILER, net.backrooms.evento.entidad.Smiler.atributos());
		net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(SERPIENTE, net.backrooms.evento.entidad.Serpiente.atributos());
		BackroomsEvento.LOG.info("Entidades registradas ({})", BuiltInRegistries.ENTITY_TYPE.getKey(CASETE));
	}
}
