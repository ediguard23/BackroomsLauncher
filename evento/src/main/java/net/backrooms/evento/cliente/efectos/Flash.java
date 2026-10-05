package net.backrooms.evento.cliente.efectos;

import net.minecraft.util.Mth;
import net.minecraft.util.Util;

/**
 * El flashbang del Smiler: si lo miras directamente (sin la camara), la
 * pantalla se queda en blanco y se va apagando en unos segundos, con un
 * pitido en los oidos (eso lo pone el servidor con el sonido y la ceguera).
 */
public final class Flash {
	private static final float DURA = 4.5F;
	private static long inicio = Long.MIN_VALUE / 2;

	private Flash() {
	}

	public static void disparar() {
		inicio = Util.getMillis();
	}

	/** 0..1: blanco total al principio y se va desvaneciendo. */
	public static float blanco() {
		float t = (Util.getMillis() - inicio) / 1000.0F;
		if (t < 0.0F || t > DURA) {
			return 0.0F;
		}
		if (t < 0.6F) {
			return 1.0F;
		}
		float x = (t - 0.6F) / (DURA - 0.6F);
		return Mth.clamp(1.0F - x * x * (3.0F - 2.0F * x), 0.0F, 1.0F);
	}

	public static void olvidar() {
		inicio = Long.MIN_VALUE / 2;
	}
}
