package net.backrooms.evento.cliente;

import java.util.ArrayList;
import java.util.List;
import net.backrooms.evento.mision.TipoMision;
import net.backrooms.evento.red.SyncMisiones;

/**
 * Lo que el cliente sabe de sus misiones (llega del servidor cada segundo).
 * Lo lee la pantalla de pausa para dibujar el panel de MISIONES.
 */
public final class MisionesCliente {
	private static SyncMisiones ultimo = SyncMisiones.VACIO;

	private MisionesCliente() {
	}

	public static void recibir(SyncMisiones s) {
		ultimo = s;
	}

	public static void olvidar() {
		ultimo = SyncMisiones.VACIO;
	}

	public static boolean hayExpedicion() {
		return !ultimo.misiones().isEmpty();
	}

	public static List<TipoMision> misiones() {
		List<TipoMision> l = new ArrayList<>();
		for (String n : ultimo.misiones()) {
			try {
				l.add(TipoMision.valueOf(n));
			} catch (IllegalArgumentException e) {
				// mision de una version mas nueva del servidor: se ignora
			}
		}
		return l;
	}

	public static int actual() {
		return ultimo.actual();
	}

	public static int casetes() {
		return ultimo.casetes();
	}

	public static int necesarios() {
		return ultimo.necesarios();
	}

	/** Metros al casete pendiente mas cercano, o -1. */
	public static int distancia() {
		return ultimo.distancia();
	}

	/** Rumbo absoluto (como el yaw de Minecraft) al casete mas cercano. */
	public static float rumbo() {
		return ultimo.rumbo();
	}
}
