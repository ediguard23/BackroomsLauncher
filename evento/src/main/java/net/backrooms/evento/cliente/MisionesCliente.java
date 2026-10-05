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

	/** Rumbo absoluto (como el yaw de Minecraft) al casete mas cercano o a la salida. */
	public static float rumbo() {
		return ultimo.rumbo();
	}

	/** true si ya hizo sus misiones y el radar lleva al ascensor de salida. */
	public static boolean salida() {
		return ultimo.salida();
	}

	public static int fase() {
		return ultimo.fase();
	}

	/** 0..1 de la grabacion de la mision en curso. */
	public static float grabado() {
		return ultimo.grabado();
	}

	public static boolean grabando() {
		return ultimo.grabando();
	}

	/** Lo que hay que grabar, para el visor de la camara; null si la mision en curso no es de grabar. */
	public static String objetivoGrabacion() {
		List<TipoMision> m = misiones();
		int i = actual();
		if (i < 0 || i >= m.size()) {
			return null;
		}
		return switch (m.get(i)) {
			case LUCES_ROJAS -> "OBJETIVO: ALARMAS";
			case ENTIDAD -> "OBJETIVO: BACTERIA";
			case ENTIDAD_ALARMA -> "OBJETIVO: BACTERIA EN ALARMA";
			case SMILER -> "OBJETIVO: SMILER";
			default -> null;
		};
	}
}
