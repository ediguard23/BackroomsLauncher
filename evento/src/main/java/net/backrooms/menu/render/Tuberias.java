package net.backrooms.menu.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;

/**
 * Los shaders propios del mod (menu, cinematica, efectos del mundo) NO se registran en RenderPipelines: los
 * registrados son "obligatorios" y, si uno no compila, Minecraft no termina de cargar (se queda en la pantalla
 * de inicio recargando). Pasaba en algunos moviles con Pojav, cuyo OpenGL no traga todo el GLSL. Sin
 * registrar se compilan al usarlos por primera vez; si fallan, lo que se dibuja con ellos simplemente no sale
 * (Minecraft se salta ese dibujo) y el juego sigue.
 */
public final class Tuberias {
	private Tuberias() {
	}

	/** Si ese shader compila en esta grafica (se compila una vez y Minecraft lo guarda). */
	public static boolean valida(RenderPipeline p) {
		try {
			return RenderSystem.getDevice().precompilePipeline(p, null).isValid();
		} catch (RuntimeException e) {
			return false;
		}
	}
}
