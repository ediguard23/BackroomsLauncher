package net.backrooms.evento.cliente.cinematica;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.io.InputStream;
import net.backrooms.evento.BackroomsEvento;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/**
 * Las texturas que usa la cinematica, juntas en una imagen de 16 x 6 casillas
 * de 16 px que el shader lee por indice (ver cinematica.fsh). Las filas 4 y 5
 * son del mod (el Nivel 0, el vestibulo y el ascensor de salida): esas se
 * escriben con su espacio de nombres ("backrooms_evento:block/...").
 *
 * Se copian en tiempo de ejecucion de los recursos que el jugador ya tiene
 * cargados (no van dentro del mod), asi el ascensor esta hecho de bloques de
 * verdad: hierro, puertas de hierro, lampara de redstone, ladrillos de piedra,
 * fuego y lava animados, las grietas de romper bloques y las particulas.
 */
public final class AtlasCinematica {
	private static final String[][] FILAS = {
		{"block/iron_block", "block/smooth_stone", "block/dark_oak_planks", "block/iron_door_top", "block/iron_door_bottom",
			"block/redstone_lamp_on", "block/redstone_lamp", "block/stone_bricks", "block/cracked_stone_bricks", "block/deepslate_bricks",
			"block/black_concrete", "block/torch", "block/redstone_torch", "block/smooth_stone_slab_side", "block/polished_andesite",
			"block/polished_blackstone"},
		{"block/fire_0#0", "block/fire_0#1", "block/fire_0#2", "block/fire_0#3", "block/fire_0#4", "block/fire_0#5", "block/fire_0#6", "block/fire_0#7",
			"block/lava_still#0", "block/lava_still#2", "block/lava_still#4", "block/lava_still#6", "block/lava_still#8", "block/lava_still#10",
			"block/lava_still#12", "block/lava_still#14"},
		{"block/destroy_stage_0", "block/destroy_stage_1", "block/destroy_stage_2", "block/destroy_stage_3", "block/destroy_stage_4",
			"block/destroy_stage_5", "block/destroy_stage_6", "block/destroy_stage_7", "block/destroy_stage_8", "block/destroy_stage_9",
			"block/quartz_block_side", "block/sea_lantern#0", "block/cobblestone", "block/magma#0", "block/blackstone", "block/chiseled_stone_bricks"},
		{"particle/flame", "particle/lava", "particle/big_smoke_0", "particle/big_smoke_4", "particle/big_smoke_8", "particle/generic_2",
			"particle/generic_5", "particle/spark_3", "block/oak_planks", "block/white_concrete", "block/glowstone", "block/stone",
			"block/iron_bars", "block/iron_chain", "block/obsidian", "block/netherrack"},
		{"backrooms_evento:block/papel_pintado", "backrooms_evento:block/papel_pintado_sucio", "backrooms_evento:block/moqueta",
			"backrooms_evento:block/moqueta_mojada", "backrooms_evento:block/techo", "backrooms_evento:block/fluorescente",
			"backrooms_evento:block/fluorescente_apagado", "backrooms_evento:block/madera_oscura", "block/light_gray_concrete",
			"block/yellow_concrete", "block/polished_deepslate", "block/quartz_block_bottom", "backrooms_evento:block/acero",
			"backrooms_evento:block/ascensor_suelo", "backrooms_evento:block/luz_ascensor", "backrooms_evento:block/panel_ascensor"},
		{"backrooms_evento:block/puerta_ascensor_izq", "backrooms_evento:block/puerta_ascensor_der", "backrooms_evento:block/bacilo"}};

	private static DynamicTexture textura;

	private AtlasCinematica() {
	}

	public static GpuTextureView vista() {
		if (textura == null) {
			crear();
		}
		return textura.getTextureView();
	}

	public static GpuSampler muestreo() {
		return RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
	}

	private static void crear() {
		NativeImage atlas = new NativeImage(256, 16 * FILAS.length, true);
		for (int fila = 0; fila < FILAS.length; fila++) {
			for (int col = 0; col < FILAS[fila].length; col++) {
				copiar(atlas, FILAS[fila][col], col, fila);
			}
		}
		textura = new DynamicTexture(() -> "backrooms_evento cinematica", atlas);
	}

	/** Copia la textura (o el fotograma `#n` de una animada) en su casilla. */
	private static void copiar(NativeImage atlas, String nombre, int col, int fila) {
		int frame = 0;
		int almohadilla = nombre.indexOf('#');
		if (almohadilla >= 0) {
			frame = Integer.parseInt(nombre.substring(almohadilla + 1));
			nombre = nombre.substring(0, almohadilla);
		}
		int dos = nombre.indexOf(':');
		Identifier id = dos < 0 ? Identifier.withDefaultNamespace("textures/" + nombre + ".png")
			: Identifier.fromNamespaceAndPath(nombre.substring(0, dos), "textures/" + nombre.substring(dos + 1) + ".png");
		try (InputStream in = Minecraft.getInstance().getResourceManager().open(id); NativeImage img = NativeImage.read(in)) {
			int w = img.getWidth();
			int fotogramas = Math.max(1, img.getHeight() / w);
			int f = frame % fotogramas;
			for (int y = 0; y < 16; y++) {
				for (int x = 0; x < 16; x++) {
					// las particulas son de 8 px: se doblan, como las dibuja el juego
					int sx = x * w / 16;
					int sy = f * w + y * w / 16;
					atlas.setPixel(col * 16 + x, fila * 16 + y, img.getPixel(sx, sy));
				}
			}
		} catch (Exception e) {
			for (int y = 0; y < 16; y++) {
				for (int x = 0; x < 16; x++) {
					atlas.setPixel(col * 16 + x, fila * 16 + y, fila == 3 ? 0 : 0xFF555555);
				}
			}
			BackroomsEvento.LOG.warn("Cinematica: falta la textura {}", id);
		}
	}
}
