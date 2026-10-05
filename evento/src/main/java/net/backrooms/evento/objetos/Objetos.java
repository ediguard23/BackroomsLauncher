package net.backrooms.evento.objetos;

import java.util.EnumMap;
import java.util.Map;
import net.backrooms.evento.BackroomsEvento;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
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
