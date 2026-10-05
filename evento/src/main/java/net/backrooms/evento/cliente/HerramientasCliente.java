package net.backrooms.evento.cliente;

import com.mojang.blaze3d.platform.InputConstants;
import net.backrooms.evento.BackroomsEvento;
import net.backrooms.evento.objetos.Objetos;
import net.backrooms.evento.red.AccionJugador;
import net.backrooms.evento.red.Herramientas;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Item;
import org.lwjgl.glfw.GLFW;

/**
 * Teclas de la linterna (F) y de la camara (C), reasignables en Controles,
 * y lo que el cliente sabe de ellas.
 *
 * La F es tambien la de cambiar de mano en Minecraft: mientras las dos
 * compartan tecla, la del juego no hace nada (en el evento la otra mano no
 * se usa). Si el jugador mueve la linterna a otra tecla, la F vuelve a ser
 * la de siempre.
 */
public final class HerramientasCliente {
	public static final KeyMapping.Category CATEGORIA = KeyMapping.Category.register(BackroomsEvento.id("backrooms"));
	public static final KeyMapping LINTERNA = KeyBindingHelper.registerKeyBinding(
		new KeyMapping("key.backrooms_evento.linterna", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F, CATEGORIA));
	public static final KeyMapping CAMARA = KeyBindingHelper.registerKeyBinding(
		new KeyMapping("key.backrooms_evento.camara", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, CATEGORIA));

	private static boolean linterna;
	private static boolean camara;
	private static int[] linternasCerca = new int[0];
	/** Para la animacion de subir y bajar la camara (0 bajada, 1 levantada). */
	private static float subida;
	private static float subidaAntes;

	private HerramientasCliente() {
	}

	public static void registrar() {
		ClientPlayNetworking.registerGlobalReceiver(Herramientas.TYPE, (p, ctx) -> {
			linterna = p.linterna();
			camara = p.camara();
			linternasCerca = p.linternasCerca();
		});
		ClientTickEvents.START_CLIENT_TICK.register(mc -> {
			// antes de que el juego mire sus teclas: la F de la linterna no cambia de mano
			if (mc.player != null && LINTERNA.same(mc.options.keySwapOffhand)) {
				while (mc.options.keySwapOffhand.consumeClick()) {
					// nada
				}
			}
		});
		ClientTickEvents.END_CLIENT_TICK.register(HerramientasCliente::tick);
	}

	private static void tick(Minecraft mc) {
		subidaAntes = subida;
		subida += ((camara ? 1.0F : 0.0F) - subida) * 0.35F;
		LocalPlayer j = mc.player;
		if (j == null) {
			while (LINTERNA.consumeClick()) {
				// nada
			}
			while (CAMARA.consumeClick()) {
				// nada
			}
			return;
		}
		while (LINTERNA.consumeClick()) {
			cambiar(j, AccionJugador.LINTERNA, Objetos.LINTERNA);
		}
		while (CAMARA.consumeClick()) {
			cambiar(j, AccionJugador.CAMARA, Objetos.CAMARA);
		}
	}

	/** Como pulsar la tecla (lo usan las ordenes de prueba). */
	public static void pulsar(int accion) {
		LocalPlayer j = Minecraft.getInstance().player;
		if (j != null) {
			cambiar(j, accion, accion == AccionJugador.LINTERNA ? Objetos.LINTERNA : Objetos.CAMARA);
		}
	}

	private static void cambiar(LocalPlayer j, int accion, Item objeto) {
		if (j.isSpectator() || !j.getInventory().contains(s -> s.is(objeto))) {
			return;
		}
		boolean nuevo = accion == AccionJugador.LINTERNA ? !linterna : !camara;
		// se aplica ya (el servidor lo confirma o lo corrige enseguida)
		if (accion == AccionJugador.LINTERNA) {
			linterna = nuevo;
		} else {
			camara = nuevo;
		}
		ClientPlayNetworking.send(new AccionJugador(accion, nuevo));
	}

	public static void olvidar() {
		linterna = false;
		camara = false;
		linternasCerca = new int[0];
		subida = 0;
		subidaAntes = 0;
	}

	public static boolean linterna() {
		return linterna;
	}

	public static boolean camara() {
		return camara;
	}

	public static int[] linternasCerca() {
		return linternasCerca;
	}

	/** 0..1, suavizado: la camara sube y baja en unos pocos ticks. */
	public static float subida(float parcial) {
		return subidaAntes + (subida - subidaAntes) * parcial;
	}
}
