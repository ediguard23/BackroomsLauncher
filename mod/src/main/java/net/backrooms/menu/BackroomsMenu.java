package net.backrooms.menu;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Menu del evento Backrooms. El menu de inicio de Minecraft se sustituye por
 * MenuBackrooms (ver MinecraftMixin), las pantallas de un jugador,
 * multijugador y Realms quedan inalcanzables, y el fondo de todos los menus
 * pasa a ser el pasillo del nivel del evento (ver Tema y ScreenMixin).
 */
public class BackroomsMenu implements ClientModInitializer {
	public static final Logger LOG = LoggerFactory.getLogger("backrooms");

	@Override
	public void onInitializeClient() {
		// fuerza la carga del tema: sus pipelines quedan registrados antes del primer frame
		LOG.info("Menu del evento cargado: {} ({})", Tema.actual().nivel, Tema.actual().fondo.pipeline.getLocation());
		ClientTickEvents.END_CLIENT_TICK.register(Sonidos::tick);
	}
}
