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

	/**
	 * Lo que hay que grabar, para el visor de la camara: solo si la mision en curso es de grabar
	 * (van en orden); null si no.
	 */
	public static String objetivoGrabacion() {
		List<TipoMision> m = misiones();
		int a = Math.max(0, actual());
		for (int i = a; i < Math.min(a + 1, m.size()); i++) {
			String o = switch (m.get(i)) {
				case LUCES_ROJAS -> "OBJETIVO: ALARMAS";
				case ENTIDAD -> "OBJETIVO: BACTERIA";
				case ENTIDAD_ALARMA -> "OBJETIVO: BACTERIA EN ALARMA";
				case SMILER -> "OBJETIVO: SMILER";
				case SERPIENTE -> "OBJETIVO: SUJETO DEL NIVEL 37";
				default -> null;
			};
			if (o != null) {
				return o;
			}
		}
		return null;
	}

	/** Si ya hizo todas sus misiones (en las Piscinas: puede cruzar la salida de emergencia). */
	public static boolean todasHechas() {
		List<TipoMision> m = misiones();
		return !m.isEmpty() && actual() >= m.size();
	}

	/** Cuando llego la ultima evidencia (ms), o -1. */
	private static long evidencia = -1;

	public static void evidencia() {
		evidencia = net.minecraft.util.Util.getMillis();
	}

	/** Segundos desde que grabo a la Serpiente (Evidencia), o -1 si no fue hace poco. */
	public static float segundosEvidencia() {
		if (evidencia < 0) {
			return -1;
		}
		float t = (net.minecraft.util.Util.getMillis() - evidencia) / 1000.0F;
		return t > 5.0F ? -1 : t;
	}
}
