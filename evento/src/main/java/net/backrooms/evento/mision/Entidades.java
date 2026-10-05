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

	private Entidades() {
	}

	private static <T extends net.minecraft.world.entity.Entity> EntityType<T> registrar(String id, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> clave = ResourceKey.create(Registries.ENTITY_TYPE, BackroomsEvento.id(id));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, clave, builder.build(clave));
	}

	public static void iniciar() {
		BackroomsEvento.LOG.info("Entidades registradas ({})", BuiltInRegistries.ENTITY_TYPE.getKey(CASETE));
	}
}
