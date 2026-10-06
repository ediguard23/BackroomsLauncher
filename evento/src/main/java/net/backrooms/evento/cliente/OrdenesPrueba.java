package net.backrooms.evento.cliente;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.backrooms.evento.BackroomsEvento;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.lwjgl.glfw.GLFW;

/**
 * Solo para pruebas automaticas (variable BACKROOMS_PRUEBAS, que ningun
 * jugador tiene): el cliente lee ordenes de <tmp>/backrooms-ordenes.txt para
 * abrir pantallas sin tocar la ventana ni robar el foco.
 *
 *   inventario | pausa | cerrar | cinematica | linterna | camara | tab [n] | andar [s] | correr [s] | captura <nombre> | cursor
 *
 * "andar <s>" y "correr <s>" dejan pulsado hacia delante (y correr) s segundos: para oir las
 * pisadas y ver el balanceo.
 *
 * "tab <n>" llena la lista con n jugadores ficticios para ver como queda con el servidor lleno.
 *
 * "captura" guarda lo que se ve (como F2) en screenshots/<nombre>.png: sirve
 * aunque la ventana este a pantalla completa.
 *
 * "cursor" apunta en el log si el cursor del sistema se ve, esta oculto o capturado (en
 * las capturas no sale).
 */
final class OrdenesPrueba {
	private static int ticks;
	/** Ticks que queda pulsado el TAB (orden "tab"). */
	private static int tab;
	/** Ticks que queda pulsado hacia delante (ordenes "andar" y "correr"). */
	private static int andar;

	private OrdenesPrueba() {
	}

	static void registrar() {
		if (System.getenv("BACKROOMS_PRUEBAS") == null) {
			return;
		}
		Path archivo = Path.of(System.getProperty("java.io.tmpdir"), "backrooms-ordenes.txt");
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			if (tab > 0 && --tab == 0) {
				mc.options.keyPlayerList.setDown(false);
			}
			if (andar > 0 && --andar == 0) {
				mc.options.keyUp.setDown(false);
				mc.options.keySprint.setDown(false);
			}
			if (++ticks % 10 != 0 || !Files.exists(archivo)) {
				return;
			}
			try {
				String orden = Files.readString(archivo, StandardCharsets.UTF_8).trim();
				Files.delete(archivo);
				ejecutar(mc, orden);
			} catch (Exception e) {
				// el archivo se esta escribiendo: se lee en la siguiente vuelta
			}
		});
	}

	private static void ejecutar(Minecraft mc, String orden) {
		if (orden.startsWith("captura")) {
			String nombre = orden.length() > 8 ? orden.substring(8).trim() + ".png" : null;
			net.minecraft.client.Screenshot.grab(mc.gameDirectory, nombre, mc.getMainRenderTarget(), 1, c -> { });
			return;
		}
		if (orden.equals("cursor")) {
			int modo = GLFW.glfwGetInputMode(mc.getWindow().handle(), GLFW.GLFW_CURSOR);
			BackroomsEvento.LOG.info("Prueba: cursor {}", modo == GLFW.GLFW_CURSOR_NORMAL ? "visible" : modo == GLFW.GLFW_CURSOR_HIDDEN ? "oculto" : "capturado");
			return;
		}
		if (mc.player == null) {
			return; // fuera de un mundo solo valen la captura y el cursor
		}
		if (orden.startsWith("tab")) {
			// "tab" o "tab <n>": el TAB pulsado 4 s, con n jugadores ficticios
			String n = orden.substring(3).trim();
			TablaJugadores.ficticios = n.isEmpty() ? 0 : Integer.parseInt(n);
			mc.options.keyPlayerList.setDown(true);
			tab = 20 * 4;
			return;
		}
		if (orden.startsWith("andar") || orden.startsWith("correr")) {
			boolean correr = orden.startsWith("correr");
			String s = orden.substring(correr ? 6 : 5).trim();
			mc.options.keyUp.setDown(true);
			mc.options.keySprint.setDown(correr);
			andar = 20 * (s.isEmpty() ? 3 : Integer.parseInt(s));
			return;
		}
		switch (orden) {
			case "inventario" -> mc.setScreen(new InventoryScreen(mc.player));
			case "pausa" -> mc.setScreen(new PauseScreen(true));
			case "cerrar" -> mc.setScreen(null);
			case "cinematica" -> net.backrooms.evento.cliente.cinematica.CinematicaCliente.empezar();
			case "linterna" -> HerramientasCliente.pulsar(net.backrooms.evento.red.AccionJugador.LINTERNA);
			case "camara" -> HerramientasCliente.pulsar(net.backrooms.evento.red.AccionJugador.CAMARA);
			default -> { }
		}
	}
}
