package net.backrooms.evento.objetos;

import java.util.EnumMap;
import java.util.Map;
import net.backrooms.evento.BackroomsEvento;
import net.backrooms.evento.red.AccionJugador;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

/**
 * Objetos del evento. El traje antirradiacion protege poco (es tela): lo
 * importante es que todos los exploradores van iguales.
 */
public final class Objetos {
	public static final ResourceKey<EquipmentAsset> TRAJE_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, BackroomsEvento.id("traje"));

	public static final ArmorMaterial TRAJE_MATERIAL = new ArmorMaterial(
		200, defensa(), 1, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F, ItemTags.REPAIRS_LEATHER_ARMOR, TRAJE_ASSET);

	public static final Item TRAJE_CASCO = traje("traje_casco", ArmorType.HELMET);
	public static final Item TRAJE_CHAQUETA = traje("traje_chaqueta", ArmorType.CHESTPLATE);
	public static final Item TRAJE_PANTALON = traje("traje_pantalon", ArmorType.LEGGINGS);
	public static final Item TRAJE_BOTAS = traje("traje_botas", ArmorType.BOOTS);

	/** El casete de las misiones (en el suelo es una entidad; el objeto es su icono). */
	public static final Item CASETE = Items.registerItem(ResourceKey.create(Registries.ITEM, BackroomsEvento.id("casete")), Item::new, new Item.Properties().stacksTo(16));

	/** Linterna (F) y camara (C): todo explorador lleva las dos y no se pueden tirar. */
	public static final Item LINTERNA = Items.registerItem(ResourceKey.create(Registries.ITEM, BackroomsEvento.id("linterna")),
		p -> new Herramienta(AccionJugador.LINTERNA, p), new Item.Properties().stacksTo(1));
	public static final Item CAMARA = Items.registerItem(ResourceKey.create(Registries.ITEM, BackroomsEvento.id("camara")),
		p -> new Herramienta(AccionJugador.CAMARA, p), new Item.Properties().stacksTo(1));

	/* ---------------------------------------------- comida del Nivel 0 */

	/** Agua de almendras: la unica que sube la cordura. */
	public static final Item AGUA_ALMENDRAS = Items.registerItem(ResourceKey.create(Registries.ITEM, BackroomsEvento.id("agua_almendras")),
		net.backrooms.evento.supervivencia.AguaAlmendras::new,
		new Item.Properties().stacksTo(4).food(new FoodProperties(2, 1.0F, true), Consumables.DEFAULT_DRINK));
	public static final Item GALLETAS = Items.registerItem(ResourceKey.create(Registries.ITEM, BackroomsEvento.id("galletas")), Item::new,
		new Item.Properties().stacksTo(8).food(new FoodProperties(4, 3.0F, false)));
	public static final Item PIZZA = Items.registerItem(ResourceKey.create(Registries.ITEM, BackroomsEvento.id("pizza")), Item::new,
		new Item.Properties().stacksTo(4).food(new FoodProperties(8, 9.6F, false)));

	/** El objeto de cada tipo de comida del plano (Plano.C_*). */
	public static Item comida(int tipo) {
		return switch (tipo) {
			case 1 -> AGUA_ALMENDRAS;
			case 2 -> GALLETAS;
			default -> PIZZA;
		};
	}

	private Objetos() {
	}

	private static Map<ArmorType, Integer> defensa() {
		Map<ArmorType, Integer> m = new EnumMap<>(ArmorType.class);
		m.put(ArmorType.HELMET, 1);
		m.put(ArmorType.CHESTPLATE, 2);
		m.put(ArmorType.LEGGINGS, 2);
		m.put(ArmorType.BOOTS, 1);
		m.put(ArmorType.BODY, 2);
		return m;
	}

	private static Item traje(String id, ArmorType tipo) {
		ResourceKey<Item> clave = ResourceKey.create(Registries.ITEM, BackroomsEvento.id(id));
		return Items.registerItem(clave, Item::new, new Item.Properties().humanoidArmor(TRAJE_MATERIAL, tipo).stacksTo(1));
	}

	public static void iniciar() {
		BackroomsEvento.LOG.info("Traje antirradiacion registrado");
	}
}
