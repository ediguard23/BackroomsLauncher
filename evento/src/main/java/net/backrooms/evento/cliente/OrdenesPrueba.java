package net.backrooms.evento.cliente;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

/**
 * Solo para pruebas automaticas (variable BACKROOMS_PRUEBAS, que ningun
 * jugador tiene): el cliente lee ordenes de <tmp>/backrooms-ordenes.txt para
 * abrir pantallas sin tocar la ventana ni robar el foco.
 *
 *   inventario | pausa | cerrar | cinematica
 */
final class OrdenesPrueba {
	private static int ticks;

	private OrdenesPrueba() {
	}

	static void registrar() {
		if (System.getenv("BACKROOMS_PRUEBAS") == null) {
			return;
		}
		Path archivo = Path.of(System.getProperty("java.io.tmpdir"), "backrooms-ordenes.txt");
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			if (++ticks % 10 != 0 || mc.player == null || !Files.exists(archivo)) {
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
		switch (orden) {
			case "inventario" -> mc.setScreen(new InventoryScreen(mc.player));
			case "pausa" -> mc.setScreen(new PauseScreen(true));
			case "cerrar" -> mc.setScreen(null);
			case "cinematica" -> net.backrooms.evento.cliente.cinematica.CinematicaCliente.empezar();
			default -> { }
		}
	}
}
