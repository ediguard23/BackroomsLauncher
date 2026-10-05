package net.backrooms.evento.cliente.efectos;

import net.minecraft.util.Mth;
import net.minecraft.util.Util;

/**
 * Lo que se ve con la cordura baja (la cordura la lleva el servidor y llega
 * con el estado del jugador). Por debajo del 50 % la imagen empieza a
 * ondular y a perder color, y a menos, la vista se cierra; los sustos son
 * fogonazos de un instante que manda la alucinacion.
 */
public final class Cordura {
	private static float cordura = 100.0F;
	private static float suave = 100.0F;
	private static long susto = Long.MIN_VALUE / 2;

	private Cordura() {
	}

	public static void poner(float valor) {
		cordura = valor;
	}

	public static float valor() {
		return cordura;
	}

	/** Cada tick: la cifra en pantalla y el efecto se mueven poco a poco. */
	public static void tick() {
		suave += (cordura - suave) * 0.08F;
	}

	public static float mostrada() {
		return suave;
	}

	/** 0 con la cordura por encima del 50 %, 1 a cero. */
	public static float efecto() {
		return Mth.clamp((50.0F - suave) / 50.0F, 0.0F, 1.0F);
	}

	public static void asustar() {
		susto = Util.getMillis();
	}

	/** Un fogonazo de 0,15 s. */
	public static float susto() {
		float t = (Util.getMillis() - susto) / 1000.0F;
		return t >= 0.0F && t < 0.15F ? 1.0F - t / 0.15F : 0.0F;
	}

	public static void olvidar() {
		cordura = 100.0F;
		suave = 100.0F;
		susto = Long.MIN_VALUE / 2;
	}
}
